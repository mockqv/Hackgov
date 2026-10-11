package br.com.hackgov.demo;

import br.com.hackgov.ia.ResultadoClassificacao;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

/**
 * Gera, de forma determinística (semente fixa), uma massa de chamados de
 * zeladoria de abril a setembro de 2026 para demonstrar as análises da Fase 6.
 *
 * Os dados são SINTÉTICOS. Os parâmetros (volume, mistura de tipos, tempos de
 * resolução) foram escolhidos para parecer uma operação plausível e para
 * exercitar as consultas — não são medições reais. Padrões modelados:
 *  - adoção crescente do canal (volume mensal sobe de abril a setembro);
 *  - sazonalidade: mais buracos em abril (fim das chuvas), mais poda em
 *    setembro (ventos/temporais), mais pedidos de iluminação no inverno;
 *  - tempo de resolução por tipo (log-normal) e bairros periféricos mais lentos;
 *  - cidadãos que escolhem o tipo errado (muitas vezes "Outros"), com retrabalho;
 *  - pontos reincidentes (mesmo problema voltando no mesmo trecho de rua) e
 *    chamados duplicados abertos por vizinhos a poucas dezenas de metros.
 *
 * O estado de cada chamado é o de {@link #DATA_REFERENCIA} (30/09/2026, 23h59).
 */
public final class GeradorDadosDemo {

    public static final long SEMENTE_PADRAO = 2026L;
    public static final LocalDate INICIO = LocalDate.of(2026, 4, 1);
    public static final LocalDate FIM = LocalDate.of(2026, 9, 30);
    public static final LocalDateTime DATA_REFERENCIA = LocalDateTime.of(2026, 9, 30, 23, 59);

    /** SLA prometido por tipo — os mesmos valores cadastrados pelo DevDataSeeder. */
    public static final Map<String, Integer> SLA_DIAS = Map.of(
            "BURACO", 7, "ILUMINACAO", 5, "PODA", 14, "ENTULHO", 7, "OUTROS", 10);

    static final String[] TIPOS = {"BURACO", "ILUMINACAO", "PODA", "ENTULHO", "OUTROS"};
    private static final double[] MISTURA = {0.30, 0.24, 0.15, 0.19, 0.12};
    private static final double[] MEDIANA_DIAS = {8.5, 3.2, 10.5, 5.0, 6.0};
    private static final int[] VOLUME_MENSAL = {210, 240, 265, 290, 330, 395}; // abril..setembro
    private static final double CHANCE_CANCELAMENTO = 0.05;

    private static final double GRAU_LAT_M = 111_195.0;

    private final Random rnd;
    private final Function<String, ResultadoClassificacao> ia;
    private final List<DadosDemo.Bairro> bairros = bairros();
    private final double[] pesoBairro = {1.3, 0.8, 0.7, 0.9, 1.0, 1.0, 1.0, 0.9, 0.8, 0.8, 1.3, 1.4};
    private final double[] fatorTempoBairro = {0.9, 1.0, 1.0, 0.9, 0.9, 1.0, 1.0, 1.0, 1.05, 1.0, 1.35, 1.4};
    private final List<DadosDemo.Servidor> servidores = new ArrayList<>();
    private final List<DadosDemo.Cidadao> cidadaos = new ArrayList<>();
    private final List<DadosDemo.Chamado> chamados = new ArrayList<>();
    private final TextosDemo textos;

    private GeradorDadosDemo(long semente, Function<String, ResultadoClassificacao> ia) {
        this.rnd = new Random(semente);
        this.ia = ia;
        this.textos = new TextosDemo(rnd);
    }

    /**
     * @param semente semente do gerador (use {@link #SEMENTE_PADRAO} para reproduzir a documentação)
     * @param ia      classificador usado para preencher a sugestão da IA (pode ser null)
     */
    public static DadosDemo gerar(long semente, Function<String, ResultadoClassificacao> ia) {
        return new GeradorDadosDemo(semente, ia).executar();
    }

    private DadosDemo executar() {
        gerarServidores();
        gerarCidadaos();

        // 1) volume "normal", mês a mês
        for (int m = 0; m < VOLUME_MENSAL.length; m++) {
            LocalDate primeiroDia = INICIO.plusMonths(m);
            for (int i = 0; i < VOLUME_MENSAL[m]; i++) {
                String tipo = sortearTipo(primeiroDia.getMonthValue());
                int bairro = sortearIndice(pesoBairro);
                double[] p = pontoNoBairro(bairro);
                LocalDateTime abertura = sortearAbertura(primeiroDia);
                DadosDemo.Chamado c = criarChamado(tipo, bairro, p[0], p[1], abertura, null, "normal");
                // 4% viram duplicata: um vizinho abre o mesmo problema a 15–45 m, dias depois
                if (rnd.nextDouble() < 0.04) {
                    double distancia = 15 + rnd.nextDouble() * 30;
                    double angulo = rnd.nextDouble() * 2 * Math.PI;
                    double[] q = deslocar(p[0], p[1], distancia * Math.cos(angulo), distancia * Math.sin(angulo));
                    LocalDateTime depois = abertura.plusHours(6 + rnd.nextInt(96));
                    if (!depois.isAfter(DATA_REFERENCIA.minusHours(2))) {
                        criarChamado(c.codigoTipoReal(), bairro, q[0], q[1], depois, c.logradouro(), "duplicata");
                    }
                }
            }
        }

        // 2) pontos reincidentes: o mesmo problema volta ao mesmo trecho de rua
        gerarReincidencias();

        chamados.sort((a, b) -> a.dataAbertura().compareTo(b.dataAbertura()));
        return new DadosDemo(List.copyOf(bairros), List.copyOf(servidores), List.copyOf(cidadaos), List.copyOf(chamados));
    }

    // ── chamados ─────────────────────────────────────────────

    private DadosDemo.Chamado criarChamado(String tipoReal, int bairro, double lat, double lon,
                                           LocalDateTime abertura, String logradouro, String origem) {
        String tipoEscolhido = tipoEscolhidoPeloCidadao(tipoReal);
        boolean malClassificado = !tipoEscolhido.equals(tipoReal);
        String descricao = textos.descricao(tipoReal);

        // duração planejada até a conclusão (log-normal em torno da mediana do tipo)
        int t = indiceTipo(tipoReal);
        double mediana = MEDIANA_DIAS[t] * fatorTempoBairro[bairro];
        if (malClassificado) mediana = mediana * 1.25 + 3; // retrabalho: foi para a equipe errada
        double duracaoDias = Math.max(0.15, mediana * Math.exp(0.55 * rnd.nextGaussian()));

        boolean cancelar = rnd.nextDouble() < CHANCE_CANCELAMENTO;
        LocalDate previsao = abertura.toLocalDate().plusDays(SLA_DIAS.get(tipoEscolhido));
        int responsavel = rnd.nextInt(servidores.size());

        List<DadosDemo.Transicao> historico = new ArrayList<>();
        String status = "RECEBIDO";
        LocalDateTime conclusao = null;
        DadosDemo.Avaliacao avaliacao = null;

        if (cancelar) {
            LocalDateTime analise = abertura.plusHours(2 + rnd.nextInt(20));
            LocalDateTime cancelamento = analise.plusHours(4 + rnd.nextInt(72));
            if (!analise.isAfter(DATA_REFERENCIA)) {
                historico.add(new DadosDemo.Transicao("RECEBIDO", "EM_ANALISE", analise, responsavel, null));
                status = "EM_ANALISE";
            }
            if (!cancelamento.isAfter(DATA_REFERENCIA)) {
                historico.add(new DadosDemo.Transicao("EM_ANALISE", "CANCELADO", cancelamento, responsavel,
                        textos.justificativaCancelamento(origem)));
                status = "CANCELADO";
            }
        } else {
            LocalDateTime fim = horarioComercial(abertura, duracaoDias);
            String[] etapas = {"EM_ANALISE", "AGENDADO", "EM_EXECUCAO", "CONCLUIDO"};
            double[] fracoes = {0.12, 0.42, 0.75, 1.0};
            String anterior = "RECEBIDO";
            LocalDateTime ultimo = abertura;
            for (int i = 0; i < etapas.length; i++) {
                LocalDateTime quando = i == etapas.length - 1
                        ? fim
                        : abertura.plus(Duration.ofMinutes((long) (duracaoDias * fracoes[i] * 24 * 60)));
                if (!quando.isAfter(ultimo)) quando = ultimo.plusMinutes(30);
                if (quando.isAfter(DATA_REFERENCIA)) break;
                historico.add(new DadosDemo.Transicao(anterior, etapas[i], quando, responsavel, null));
                anterior = etapas[i];
                ultimo = quando;
                status = etapas[i];
            }
            if ("CONCLUIDO".equals(status)) {
                conclusao = ultimo;
                avaliacao = avaliar(conclusao, previsao);
            }
        }

        ResultadoClassificacao sugestao = ia == null ? null : ia.apply(descricao);
        DadosDemo.Bairro b = bairros.get(bairro);
        DadosDemo.Chamado chamado = new DadosDemo.Chamado(
                protocolo(), sortearCidadao(), tipoEscolhido, tipoReal, bairro, lat, lon,
                logradouro != null ? logradouro : b.logradouros().get(rnd.nextInt(b.logradouros().size())),
                String.valueOf(10 + rnd.nextInt(3_500)),
                b.prefixoCep() + String.format(Locale.ROOT, "%05d", rnd.nextInt(100_000)),
                descricao, status, abertura, previsao, conclusao,
                historico.isEmpty() ? null : responsavel,
                List.copyOf(historico), avaliacao,
                sugestao == null || sugestao.semSinal() ? null : sugestao.classe(),
                sugestao == null || sugestao.semSinal() ? null : arredondar(sugestao.confianca(), 4),
                origem);
        chamados.add(chamado);
        return chamado;
    }

    private void gerarReincidencias() {
        // {índice do bairro, tipo, quantidade, deslocamento norte (m), deslocamento leste (m), eixo da rua (graus)}
        Object[][] pontos = {
                {10, "BURACO", 9, 120.0, -300.0, 10.0},
                {11, "BURACO", 8, -200.0, 150.0, 75.0},
                {11, "ENTULHO", 10, 300.0, 250.0, 0.0},
                {5, "ILUMINACAO", 6, -150.0, 80.0, 90.0},
                {3, "PODA", 5, 60.0, -120.0, 35.0},
                {0, "ENTULHO", 7, -90.0, 140.0, 120.0},
                {7, "BURACO", 6, 220.0, -60.0, 5.0},
                {4, "ILUMINACAO", 5, -260.0, -100.0, 60.0},
                {6, "PODA", 5, 140.0, 200.0, 150.0},
                {10, "ENTULHO", 6, -320.0, -220.0, 100.0},
        };
        for (Object[] p : pontos) {
            int bairro = (Integer) p[0];
            String tipo = (String) p[1];
            int quantidade = (Integer) p[2];
            DadosDemo.Bairro b = bairros.get(bairro);
            double[] centro = deslocar(b.latitude(), b.longitude(), (Double) p[3], (Double) p[4]);
            double eixo = Math.toRadians((Double) p[5]);
            String rua = b.logradouros().get(rnd.nextInt(b.logradouros().size()));
            List<double[]> usados = new ArrayList<>();
            for (int i = 0; i < quantidade; i++) {
                double[] escolhido = null;
                for (int tentativa = 0; tentativa < 30 && escolhido == null; tentativa++) {
                    double aoLongo = -40 + rnd.nextDouble() * 80;   // trecho de ~80 m da rua
                    double lateral = rnd.nextGaussian() * 4;
                    double norte = aoLongo * Math.cos(eixo) - lateral * Math.sin(eixo);
                    double leste = aoLongo * Math.sin(eixo) + lateral * Math.cos(eixo);
                    double[] candidato = {norte, leste};
                    boolean longe = true;
                    for (double[] u : usados) {
                        if (Math.hypot(u[0] - norte, u[1] - leste) < 11) longe = false;
                    }
                    if (longe) escolhido = candidato;
                }
                if (escolhido == null) continue;
                usados.add(escolhido);
                double[] q = deslocar(centro[0], centro[1], escolhido[0], escolhido[1]);
                LocalDate dia = INICIO.plusDays(rnd.nextInt((int) (FIM.toEpochDay() - INICIO.toEpochDay()) + 1));
                criarChamado(tipo, bairro, q[0], q[1], dia.atTime(7 + rnd.nextInt(14), rnd.nextInt(60)), rua, "reincidencia");
            }
        }
    }

    // ── sorteios ─────────────────────────────────────────────

    private String sortearTipo(int mes) {
        double[] pesos = MISTURA.clone();
        if (mes == 4) pesos[0] *= 1.25;                          // buracos depois das chuvas de verão
        if (mes >= 6 && mes <= 8) pesos[1] *= 1.15;              // inverno: noites mais longas
        if (mes == 9) pesos[2] *= 1.6;                           // ventos e temporais de setembro
        return TIPOS[sortearIndice(pesos)];
    }

    private String tipoEscolhidoPeloCidadao(String tipoReal) {
        double r = rnd.nextDouble();
        if (!"OUTROS".equals(tipoReal)) {
            if (r < 0.09) return "OUTROS";                      // na dúvida, o cidadão marca "Outros"
            if (r < 0.13) return outroTipoEspecifico(tipoReal);  // ou escolhe um tipo errado
            return tipoReal;
        }
        return r < 0.08 ? outroTipoEspecifico(tipoReal) : tipoReal;
    }

    private String outroTipoEspecifico(String excluir) {
        String t;
        do {
            t = TIPOS[rnd.nextInt(4)];
        } while (t.equals(excluir));
        return t;
    }

    private int sortearIndice(double[] pesos) {
        double total = 0;
        for (double p : pesos) total += p;
        double r = rnd.nextDouble() * total;
        for (int i = 0; i < pesos.length; i++) {
            r -= pesos[i];
            if (r < 0) return i;
        }
        return pesos.length - 1;
    }

    private LocalDateTime sortearAbertura(LocalDate primeiroDia) {
        int dias = primeiroDia.lengthOfMonth();
        double[] pesos = new double[dias];
        for (int d = 0; d < dias; d++) {
            DayOfWeek dow = primeiroDia.plusDays(d).getDayOfWeek();
            pesos[d] = switch (dow) {
                case MONDAY -> 1.2;
                case TUESDAY -> 1.15;
                case WEDNESDAY -> 1.05;
                case THURSDAY -> 1.0;
                case FRIDAY -> 0.95;
                case SATURDAY -> 0.85;
                case SUNDAY -> 0.8;
            };
        }
        LocalDate dia = primeiroDia.plusDays(sortearIndice(pesos));
        int hora = 7 + (int) Math.min(15, Math.abs(rnd.nextGaussian() * 4 + 5));
        return dia.atTime(hora, rnd.nextInt(60));
    }

    private int sortearCidadao() {
        // alguns cidadãos usam muito o canal (distribuição com cauda longa)
        double u = rnd.nextDouble();
        return (int) Math.min(cidadaos.size() - 1, Math.floor(cidadaos.size() * Math.pow(u, 1.8)));
    }

    private DadosDemo.Avaliacao avaliar(LocalDateTime conclusao, LocalDate previsao) {
        if (rnd.nextDouble() > 0.55) return null;
        boolean noPrazo = !conclusao.toLocalDate().isAfter(previsao);
        double[] pesos = noPrazo ? new double[]{0.02, 0.03, 0.10, 0.35, 0.50} : new double[]{0.15, 0.20, 0.30, 0.25, 0.10};
        int nota = sortearIndice(pesos) + 1;
        LocalDateTime data = conclusao.plusHours(2 + rnd.nextInt(70));
        if (data.isAfter(DATA_REFERENCIA)) data = DATA_REFERENCIA;
        return new DadosDemo.Avaliacao(nota, textos.comentarioAvaliacao(nota), data);
    }

    /** Conclusão cai em horário de expediente (8h–17h) do dia previsto pela duração. */
    private LocalDateTime horarioComercial(LocalDateTime abertura, double duracaoDias) {
        LocalDateTime bruto = abertura.plus(Duration.ofMinutes((long) (duracaoDias * 24 * 60)));
        LocalDateTime fim = bruto.toLocalDate().atTime(LocalTime.of(8 + rnd.nextInt(9), rnd.nextInt(60)));
        if (!fim.isAfter(abertura.plusHours(1))) fim = abertura.plusHours(2);
        return fim;
    }

    // ── geografia ────────────────────────────────────────────

    private double[] pontoNoBairro(int indice) {
        DadosDemo.Bairro b = bairros.get(indice);
        return deslocar(b.latitude(), b.longitude(), rnd.nextGaussian() * 450, rnd.nextGaussian() * 450);
    }

    private static double[] deslocar(double lat, double lon, double norteMetros, double lesteMetros) {
        double novaLat = lat + norteMetros / GRAU_LAT_M;
        double novaLon = lon + lesteMetros / (GRAU_LAT_M * Math.cos(Math.toRadians(lat)));
        return new double[]{arredondar(novaLat, 7), arredondar(novaLon, 7)};
    }

    // ── cadastros ────────────────────────────────────────────

    private void gerarServidores() {
        String[][] dados = {
                {"Ana Paula Ribeiro", "Engenheira Civil"}, {"Carlos Eduardo Nunes", "Técnico de Pavimentação"},
                {"Fernanda Lopes", "Analista de Zeladoria"}, {"João Batista Moreira", "Encarregado de Equipe"},
                {"Mariana Costa", "Engenheira Agrônoma"}, {"Rafael Teixeira", "Eletrotécnico"},
                {"Patrícia Gomes", "Fiscal de Posturas"}, {"Bruno Almeida", "Técnico de Manutenção"}};
        for (int i = 0; i < dados.length; i++) {
            String login = dados[i][0].toLowerCase(Locale.ROOT).split(" ")[0] + "." + (i + 1);
            servidores.add(new DadosDemo.Servidor(dados[i][0], String.format(Locale.ROOT, "SRV%03d", 101 + i),
                    sem(login) + "@demo.hackgov.local", dados[i][1]));
        }
    }

    private void gerarCidadaos() {
        String[] nomes = {"Ana", "Bruno", "Camila", "Daniel", "Eduarda", "Felipe", "Gabriela", "Henrique", "Isabela",
                "João", "Larissa", "Lucas", "Mariana", "Matheus", "Natália", "Otávio", "Paula", "Rafael", "Sabrina",
                "Thiago", "Vanessa", "Vinícius", "Beatriz", "Gustavo", "Juliana", "Leonardo", "Marcos", "Priscila",
                "Renata", "Rodrigo", "Simone", "Tatiane", "Wagner", "Aline", "Diego", "Elaine", "Fábio", "Helena"};
        String[] sobrenomes = {"Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Alves", "Pereira",
                "Lima", "Gomes", "Costa", "Ribeiro", "Martins", "Carvalho", "Almeida", "Lopes", "Soares", "Fernandes",
                "Vieira", "Barbosa", "Rocha", "Dias", "Nascimento", "Andrade", "Moreira", "Nunes", "Marques", "Mendes"};
        for (int i = 0; i < 320; i++) {
            String nome = nomes[rnd.nextInt(nomes.length)] + " " + sobrenomes[rnd.nextInt(sobrenomes.length)];
            String email = sem(nome.toLowerCase(Locale.ROOT).replace(' ', '.')) + "." + (i + 1) + "@demo.hackgov.local";
            String telefone = "119" + String.format(Locale.ROOT, "%08d", rnd.nextInt(100_000_000));
            LocalDate cadastro = INICIO.minusDays(rnd.nextInt(120));
            cidadaos.add(new DadosDemo.Cidadao(nome, email, cpfValido(), telefone, cadastro));
        }
    }

    private String cpfValido() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) d[i] = rnd.nextInt(10);
        for (int k = 9; k <= 10; k++) {
            int soma = 0;
            for (int i = 0; i < k; i++) soma += d[i] * (k + 1 - i);
            int resto = 11 - (soma % 11);
            d[k] = resto >= 10 ? 0 : resto;
        }
        StringBuilder sb = new StringBuilder();
        for (int x : d) sb.append(x);
        return sb.toString();
    }

    private String protocolo() {
        return String.format(Locale.ROOT, "ZEL-2026-%08X", rnd.nextInt() & 0x7fffffff);
    }

    private static List<DadosDemo.Bairro> bairros() {
        return List.of(
                new DadosDemo.Bairro("Centro", -23.5505, -46.6340, false, "010",
                        List.of("Rua Direita", "Rua XV de Novembro", "Av. São João", "Rua Boa Vista")),
                new DadosDemo.Bairro("Bela Vista", -23.5587, -46.6492, false, "013",
                        List.of("Rua Treze de Maio", "Rua Rui Barbosa", "Av. Brigadeiro Luís Antônio")),
                new DadosDemo.Bairro("Liberdade", -23.5590, -46.6350, false, "015",
                        List.of("Rua Galvão Bueno", "Rua dos Estudantes", "Rua da Glória")),
                new DadosDemo.Bairro("Pinheiros", -23.5664, -46.6930, false, "054",
                        List.of("Rua Teodoro Sampaio", "Rua dos Pinheiros", "Rua Cardeal Arcoverde")),
                new DadosDemo.Bairro("Vila Mariana", -23.5890, -46.6340, false, "040",
                        List.of("Rua Domingos de Morais", "Rua Vergueiro", "Rua França Pinto")),
                new DadosDemo.Bairro("Mooca", -23.5600, -46.5990, false, "031",
                        List.of("Rua da Mooca", "Rua Juventus", "Rua Taquari")),
                new DadosDemo.Bairro("Tatuapé", -23.5400, -46.5760, false, "030",
                        List.of("Rua Tuiuti", "Rua Serra de Bragança", "Av. Celso Garcia")),
                new DadosDemo.Bairro("Santana", -23.5020, -46.6250, false, "020",
                        List.of("Av. Cruzeiro do Sul", "Rua Voluntários da Pátria", "Rua Alfredo Pujol")),
                new DadosDemo.Bairro("Butantã", -23.5710, -46.7080, false, "055",
                        List.of("Av. Vital Brasil", "Rua Alvarenga", "Rua Camargo")),
                new DadosDemo.Bairro("Lapa", -23.5200, -46.7040, false, "050",
                        List.of("Rua Doze de Outubro", "Rua Guaicurus", "Rua Clélia")),
                new DadosDemo.Bairro("Itaquera", -23.5400, -46.4560, true, "082",
                        List.of("Av. Itaquera", "Rua Gregório Ramalho", "Av. Jacu-Pêssego")),
                new DadosDemo.Bairro("Capão Redondo", -23.6720, -46.7790, true, "058",
                        List.of("Estrada de Itapecerica", "Av. Ellis Maas", "Rua Cachoeira Morena")));
    }

    // ── utilitários ──────────────────────────────────────────

    private static int indiceTipo(String codigo) {
        for (int i = 0; i < TIPOS.length; i++) {
            if (TIPOS[i].equals(codigo)) return i;
        }
        throw new IllegalArgumentException("Tipo desconhecido: " + codigo);
    }

    private static String sem(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static double arredondar(double v, int casas) {
        double f = Math.pow(10, casas);
        return Math.round(v * f) / f;
    }
}
