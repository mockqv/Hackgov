package br.com.hackgov.service;

import br.com.hackgov.dto.EstatisticaResponse;
import br.com.hackgov.dto.EstatisticaResponse.Analise;
import br.com.hackgov.dto.EstatisticaResponse.Ponto;
import br.com.hackgov.dto.EstatisticaResponse.Projecao;
import br.com.hackgov.estatistica.Estatistica;
import br.com.hackgov.estatistica.Estatistica.Regressao;
import br.com.hackgov.model.Solicitacao;
import br.com.hackgov.model.StatusSolicitacao;
import br.com.hackgov.repository.AvaliacaoRepository;
import br.com.hackgov.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Relatório estatístico da Fase 7 (Parte 3): quatro perguntas de gestão
 * respondidas com correlação de Pearson e regressão linear, cada uma com
 * teste de significância e uma leitura para decisão.
 *
 * <p>As definições são as mesmas de database/fase7/03_estatistica.sql
 * (CORR, REGR_SLOPE, REGR_INTERCEPT e REGR_R2 do Oracle), para que a tela e o
 * SQL deem o mesmo número:
 * <ul>
 *   <li>dias de resolução = data da conclusão − data da abertura (dias corridos);</li>
 *   <li>atraso = dias de resolução − SLA do tipo (negativo = antes do prazo);</li>
 *   <li>semanas de segunda a domingo, só as completas.</li>
 * </ul>
 */
@Service
public class EstatisticaService {

    private static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final double ALFA = 0.05;
    private static final int MIN_AVALIACOES_BAIRRO = 5;
    private static final int SEMANAS_PROJECAO = 4;
    /** Semanas recentes têm chamados ainda abertos: só os rápidos já concluíram (viés de censura). */
    private static final int DIAS_MATURACAO = 30;

    private final SolicitacaoRepository solicitacaoRepo;
    private final AvaliacaoRepository avaliacaoRepo;

    public EstatisticaService(SolicitacaoRepository solicitacaoRepo, AvaliacaoRepository avaliacaoRepo) {
        this.solicitacaoRepo = solicitacaoRepo;
        this.avaliacaoRepo = avaliacaoRepo;
    }

    @Transactional(readOnly = true)
    public EstatisticaResponse relatorio() {
        List<Solicitacao> todas = solicitacaoRepo.findAllParaMapa();
        if (todas.size() < 10) {
            return new EstatisticaResponse(LocalDate.now(), todas.size(), List.of());
        }
        Map<Long, Integer> notas = new HashMap<>();
        for (Object[] linha : avaliacaoRepo.findNotasPorSolicitacao()) {
            notas.put((Long) linha[0], ((Number) linha[1]).intValue());
        }
        LocalDate inicio = todas.stream().map(s -> s.getDataAbertura().toLocalDate()).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate referencia = todas.stream().map(s -> s.getDataAbertura().toLocalDate()).max(Comparator.naturalOrder()).orElseThrow();

        List<Analise> analises = new ArrayList<>();
        adicionar(analises, () -> atrasoVersusNota(todas, notas));
        adicionar(analises, () -> bairroTempoVersusNota(todas, notas));
        adicionar(analises, () -> volumeVersusTempo(todas, inicio, referencia));
        adicionar(analises, () -> tendenciaSemanal(todas, inicio, referencia));
        return new EstatisticaResponse(referencia, todas.size(), analises);
    }

    private interface Calculo { Analise calcular(); }

    /** Uma análise sem dados suficientes (ex.: base nova) é omitida, sem derrubar o relatório. */
    private static void adicionar(List<Analise> lista, Calculo c) {
        try {
            Analise a = c.calcular();
            if (a != null) lista.add(a);
        } catch (IllegalArgumentException semDados) {
            // menos de 3 pontos ou x constante
        }
    }

    // ── A. atraso em relação ao SLA × nota do cidadão (por chamado) ──────────

    private Analise atrasoVersusNota(List<Solicitacao> todas, Map<Long, Integer> notas) {
        List<double[]> pares = new ArrayList<>();
        for (Solicitacao s : todas) {
            Integer nota = notas.get(s.getId());
            Long dias = diasResolucao(s);
            if (nota == null || dias == null) continue;
            pares.add(new double[]{dias - s.getTipoServico().getSlaDias(), nota});
        }
        Regressao r = Estatistica.regressao(col(pares, 0), col(pares, 1));
        // a reta resume a tendência, mas a nota cai em degrau quando o prazo estoura:
        // comparar as médias dos dois lados do prazo mostra o efeito de limiar
        double somaNo = 0, somaFora = 0;
        int nNo = 0, nFora = 0;
        for (double[] p : pares) {
            if (p[0] <= 0) { somaNo += p[1]; nNo++; } else { somaFora += p[1]; nFora++; }
        }
        double mediaNo = nNo == 0 ? Double.NaN : somaNo / nNo;
        double mediaFora = nFora == 0 ? Double.NaN : somaFora / nFora;
        String decisao = String.format(BR,
                "A reta indica %.2f ponto a menos por dia de atraso, mas a queda real é um degrau: nota média %.2f "
                + "quando o prazo é cumprido e %.2f quando estoura (faixas na consulta E1b). "
                + "Cumprir o SLA vale mais do que encurtar um atraso já consumado: priorizar na fila o que está "
                + "perto de vencer e avisar o cidadão antes de o prazo estourar. O atraso explica %.0f%% da variação "
                + "da nota; o resto vem de outros fatores (qualidade do serviço, comunicação).",
                Math.abs(r.inclinacao()), mediaNo, mediaFora, r.r2() * 100);
        List<Ponto> pontos = pares.stream().map(p -> new Ponto(p[0], p[1], null)).toList();
        return analise("atraso-nota", "Atraso no SLA × satisfação",
                "Quanto o atraso em relação ao prazo derruba a nota que o cidadão dá ao atendimento?",
                "Atraso em relação ao SLA (dias; negativo = antes do prazo)", "Nota da avaliação (1 a 5)",
                r, decisao, pontos, List.of());
    }

    // ── B. tempo médio × nota média, por bairro ──────────────────────────────

    private Analise bairroTempoVersusNota(List<Solicitacao> todas, Map<Long, Integer> notas) {
        Map<String, double[]> acc = new TreeMap<>(); // soma dias, n concluídos, soma notas, n notas
        for (Solicitacao s : todas) {
            String bairro = s.getLocalizacao().getBairro().getNomeBairro();
            double[] a = acc.computeIfAbsent(bairro, b -> new double[4]);
            Long dias = diasResolucao(s);
            if (dias != null) { a[0] += dias; a[1]++; }
            Integer nota = notas.get(s.getId());
            if (nota != null) { a[2] += nota; a[3]++; }
        }
        List<double[]> pares = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();
        acc.forEach((bairro, a) -> {
            if (a[1] > 0 && a[3] >= MIN_AVALIACOES_BAIRRO) {
                pares.add(new double[]{a[0] / a[1], a[2] / a[3]});
                rotulos.add(bairro);
            }
        });
        Regressao r = Estatistica.regressao(col(pares, 0), col(pares, 1));
        int pior = 0;
        for (int i = 1; i < pares.size(); i++) if (pares.get(i)[0] > pares.get(pior)[0]) pior = i;
        String decisao = String.format(BR,
                "Cada dia a mais no tempo médio do bairro corresponde a %.2f ponto a menos na nota média. O bairro mais "
                + "lento é %s (%.1f dias, nota %.2f): é onde reforçar equipe ou rever a rota rende mais satisfação. "
                + "Cuidado ao ler: correlação entre médias de grupos sai mais forte do que entre chamados individuais "
                + "(falácia ecológica), e %d bairros é uma amostra pequena.",
                Math.abs(r.inclinacao()), rotulos.get(pior), pares.get(pior)[0], pares.get(pior)[1], pares.size());
        List<Ponto> pontos = new ArrayList<>();
        for (int i = 0; i < pares.size(); i++) pontos.add(new Ponto(pares.get(i)[0], pares.get(i)[1], rotulos.get(i)));
        return analise("bairro-tempo-nota", "Bairros: tempo médio × nota média",
                "Os bairros atendidos mais devagar são os que avaliam pior o serviço?",
                "Tempo médio de resolução do bairro (dias)", "Nota média do bairro",
                r, decisao, pontos, List.of());
    }

    // ── C. volume semanal × tempo médio de resolução ─────────────────────────

    private Analise volumeVersusTempo(List<Solicitacao> todas, LocalDate inicio, LocalDate referencia) {
        LocalDate limite = referencia.minusDays(DIAS_MATURACAO);
        TreeMap<LocalDate, double[]> semanas = new TreeMap<>(); // volume, soma dias, n concluídos
        for (Solicitacao s : todas) {
            LocalDate semana = segunda(s.getDataAbertura().toLocalDate());
            if (!semanaCompleta(semana, inicio, limite)) continue;
            double[] a = semanas.computeIfAbsent(semana, k -> new double[3]);
            a[0]++;
            Long dias = diasResolucao(s);
            if (dias != null) { a[1] += dias; a[2]++; }
        }
        List<double[]> pares = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();
        semanas.forEach((semana, a) -> {
            if (a[2] > 0) { pares.add(new double[]{a[0], a[1] / a[2]}); rotulos.add("semana de " + semana.format(DIA)); }
        });
        Regressao r = Estatistica.regressao(col(pares, 0), col(pares, 1));
        String decisao = r.significativa(ALFA)
                ? String.format(BR,
                    "Semanas mais cheias resolvem %s: cada chamado a mais na semana muda o tempo médio em %.3f dia. "
                    + "Vale escalar equipe extra nas semanas de pico previstas pela tendência.",
                    r.inclinacao() > 0 ? "mais devagar" : "mais rápido", r.inclinacao())
                : String.format(BR,
                    "O volume da semana não explica o tempo de atendimento (p = %.2f): semanas de pico não são mais "
                    + "lentas. Contratar só para os picos não resolveria; o atraso está ligado ao bairro (análise B) e "
                    + "ao tipo errado na abertura (+3 dias de retrabalho, medido na Fase 6) — onde a IA de triagem atua.",
                    r.valorP());
        List<Ponto> pontos = new ArrayList<>();
        for (int i = 0; i < pares.size(); i++) pontos.add(new Ponto(pares.get(i)[0], pares.get(i)[1], rotulos.get(i)));
        return analise("volume-tempo", "Volume da semana × tempo de atendimento",
                "Semanas com mais chamados são atendidas mais devagar (falta de capacidade)?",
                "Chamados abertos na semana", "Tempo médio de resolução (dias)",
                r, decisao, pontos, List.of());
    }

    // ── D. tendência do volume semanal e projeção ────────────────────────────

    private Analise tendenciaSemanal(List<Solicitacao> todas, LocalDate inicio, LocalDate referencia) {
        TreeMap<LocalDate, Integer> semanas = new TreeMap<>();
        for (Solicitacao s : todas) {
            LocalDate semana = segunda(s.getDataAbertura().toLocalDate());
            if (semanaCompleta(semana, inicio, referencia)) semanas.merge(semana, 1, Integer::sum);
        }
        List<double[]> pares = new ArrayList<>();
        List<String> rotulos = new ArrayList<>();
        int i = 1;
        for (Map.Entry<LocalDate, Integer> e : semanas.entrySet()) {
            pares.add(new double[]{i++, e.getValue()});
            rotulos.add("semana de " + e.getKey().format(DIA));
        }
        Regressao r = Estatistica.regressao(col(pares, 0), col(pares, 1));
        List<Projecao> projecao = new ArrayList<>();
        LocalDate ultima = semanas.lastKey();
        for (int k = 1; k <= SEMANAS_PROJECAO; k++) {
            double x = pares.size() + k;
            double[] ip = r.intervaloPredicao(x, 0.95);
            projecao.add(new Projecao(ultima.plusWeeks(k).format(DIA), r.prever(x), Math.max(0, ip[0]), ip[1]));
        }
        Projecao fim = projecao.get(projecao.size() - 1);
        String decisao = String.format(BR,
                "O volume cresce %.1f chamados por semana. Para a semana de %s a projeção é de %.0f chamados "
                + "(intervalo de 95%%: %.0f a %.0f). É o número para dimensionar as equipes do próximo mês; o "
                + "intervalo largo (R² = %.0f%%) mostra que sazonalidade e eventos pesam, então a projeção deve ser "
                + "refeita toda semana com os dados novos.",
                r.inclinacao(), fim.semana(), fim.previsto(), fim.minimo(), fim.maximo(), r.r2() * 100);
        List<Ponto> pontos = new ArrayList<>();
        for (int k = 0; k < pares.size(); k++) pontos.add(new Ponto(pares.get(k)[0], pares.get(k)[1], rotulos.get(k)));
        return analise("tendencia-volume", "Tendência do volume semanal",
                "Quantos chamados esperar nas próximas semanas?",
                "Semana (1 = primeira semana completa)", "Chamados abertos",
                r, decisao, pontos, projecao);
    }

    // ── apoio ─────────────────────────────────────────────────────

    private static Long diasResolucao(Solicitacao s) {
        if (s.getStatus() != StatusSolicitacao.CONCLUIDO || s.getDataConclusao() == null) return null;
        return ChronoUnit.DAYS.between(s.getDataAbertura().toLocalDate(), s.getDataConclusao().toLocalDate());
    }

    private static LocalDate segunda(LocalDate d) {
        return d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private static boolean semanaCompleta(LocalDate segunda, LocalDate inicio, LocalDate fim) {
        return !segunda.isBefore(inicio) && !segunda.plusDays(6).isAfter(fim);
    }

    private static double[] col(List<double[]> pares, int j) {
        double[] v = new double[pares.size()];
        for (int i = 0; i < v.length; i++) v[i] = pares.get(i)[j];
        return v;
    }

    private static Analise analise(String id, String titulo, String pergunta, String eixoX, String eixoY,
                                   Regressao r, String decisao, List<Ponto> pontos, List<Projecao> projecao) {
        boolean sig = r.significativa(ALFA);
        String sentido = r.r() < 0 ? "negativa" : "positiva";
        String p = r.valorP() < 0.001 ? "p < 0,001" : String.format(BR, "p = %.3f", r.valorP());
        String leitura = String.format(BR,
                "r = %.2f: correlação %s %s. R² = %.0f%%: o eixo X explica essa fração da variação do eixo Y. %s — %s. "
                + "Reta de mínimos quadrados: y = %.3f %s %.3f·x (n = %d).",
                r.r(), Estatistica.forca(r.r()), sentido, r.r2() * 100, p,
                sig ? "a relação dificilmente é acaso da amostra" : "não dá para descartar que seja acaso (p ≥ 0,05)",
                r.intercepto(), r.inclinacao() < 0 ? "−" : "+", Math.abs(r.inclinacao()), r.n());
        return new Analise(id, titulo, pergunta, eixoX, eixoY, r.n(), r.r(), r.r2(), r.inclinacao(), r.intercepto(),
                r.valorP(), sig, Estatistica.forca(r.r()), leitura, decisao, pontos, projecao);
    }
}
