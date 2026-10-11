package br.com.hackgov.config;

import br.com.hackgov.demo.DadosDemo;
import br.com.hackgov.demo.GeradorDadosDemo;
import br.com.hackgov.model.Avaliacao;
import br.com.hackgov.model.Bairro;
import br.com.hackgov.model.Cidadao;
import br.com.hackgov.model.HistoricoStatus;
import br.com.hackgov.model.Localizacao;
import br.com.hackgov.model.Perfil;
import br.com.hackgov.model.Servidor;
import br.com.hackgov.model.Solicitacao;
import br.com.hackgov.model.StatusSolicitacao;
import br.com.hackgov.model.TipoServico;
import br.com.hackgov.repository.AvaliacaoRepository;
import br.com.hackgov.repository.BairroRepository;
import br.com.hackgov.repository.CidadaoRepository;
import br.com.hackgov.repository.HistoricoStatusRepository;
import br.com.hackgov.repository.ServidorRepository;
import br.com.hackgov.repository.SolicitacaoRepository;
import br.com.hackgov.repository.TipoServicoRepository;
import br.com.hackgov.service.TriagemIaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * Massa de demonstração da Fase 6 (perfil "demo"): ~1.850 solicitações de
 * abril a setembro de 2026, com histórico de status, avaliações e a sugestão
 * da IA de triagem — base das consultas SQL e dos agrupamentos da Parte 4.
 *
 * Os dados são gerados por {@link GeradorDadosDemo} com semente fixa, então
 * qualquer banco limpo recebe exatamente as mesmas linhas (os números da
 * documentação são reprodutíveis). Idempotente: se o primeiro cidadão de
 * demonstração já existe, não faz nada. Roda depois do DevDataSeeder.
 *
 * Uso: SPRING_PROFILES_ACTIVE=dev,demo (já configurado no docker-compose).
 * Todos os usuários de demonstração usam a senha Hackgov@123.
 */
@Component
@Profile("demo")
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String SENHA_DEMO = "Hackgov@123";

    private final TipoServicoRepository tipoServicoRepo;
    private final BairroRepository bairroRepo;
    private final CidadaoRepository cidadaoRepo;
    private final ServidorRepository servidorRepo;
    private final SolicitacaoRepository solicitacaoRepo;
    private final HistoricoStatusRepository historicoRepo;
    private final AvaliacaoRepository avaliacaoRepo;
    private final PasswordEncoder passwordEncoder;
    private final TriagemIaService triagemIa;

    public DemoDataSeeder(TipoServicoRepository tipoServicoRepo, BairroRepository bairroRepo,
                          CidadaoRepository cidadaoRepo, ServidorRepository servidorRepo,
                          SolicitacaoRepository solicitacaoRepo, HistoricoStatusRepository historicoRepo,
                          AvaliacaoRepository avaliacaoRepo, PasswordEncoder passwordEncoder,
                          TriagemIaService triagemIa) {
        this.tipoServicoRepo = tipoServicoRepo;
        this.bairroRepo = bairroRepo;
        this.cidadaoRepo = cidadaoRepo;
        this.servidorRepo = servidorRepo;
        this.solicitacaoRepo = solicitacaoRepo;
        this.historicoRepo = historicoRepo;
        this.avaliacaoRepo = avaliacaoRepo;
        this.passwordEncoder = passwordEncoder;
        this.triagemIa = triagemIa;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        DadosDemo dados = GeradorDadosDemo.gerar(GeradorDadosDemo.SEMENTE_PADRAO,
                texto -> triagemIa.sugerir(texto).orElse(null));

        if (cidadaoRepo.findByEmail(dados.cidadaos().get(0).email()).isPresent()) {
            log.info("Massa de demonstração já carregada — nada a fazer.");
            return;
        }
        long inicio = System.currentTimeMillis();
        log.info("===== DemoDataSeeder: carregando {} solicitações de demonstração =====", dados.chamados().size());

        Map<String, TipoServico> tipos = garantirTipos();
        List<Bairro> bairros = garantirBairros(dados.bairros());
        String senhaHash = passwordEncoder.encode(SENHA_DEMO); // um único BCrypt para todos (rápido)

        List<Servidor> servidores = new ArrayList<>();
        for (DadosDemo.Servidor s : dados.servidores()) {
            servidores.add(servidorRepo.findByEmailFuncional(s.email()).orElseGet(() -> servidorRepo.save(Servidor.builder()
                    .nome(s.nome())
                    .matricula(s.matricula())
                    .emailFuncional(s.email())
                    .cargo(s.cargo())
                    .senhaHash(senhaHash)
                    .perfil(Perfil.SERVIDOR)
                    .ativo("S")
                    .build())));
        }

        List<Cidadao> cidadaos = new ArrayList<>();
        for (DadosDemo.Cidadao c : dados.cidadaos()) {
            cidadaos.add(cidadaoRepo.save(Cidadao.builder()
                    .nome(c.nome())
                    .email(c.email())
                    .cpfHash(sha256(c.cpf()))
                    .telefone(c.telefone())
                    .senhaHash(senhaHash)
                    .perfil(Perfil.CIDADAO)
                    .dataCadastro(c.dataCadastro())
                    .ativo("S")
                    .build()));
        }

        int historicos = 0;
        int avaliacoes = 0;
        int n = 0;
        for (DadosDemo.Chamado c : dados.chamados()) {
            Localizacao local = Localizacao.builder()
                    .latitude(c.latitude())
                    .longitude(c.longitude())
                    .logradouro(c.logradouro())
                    .numero(c.numero())
                    .cep(c.cep())
                    .bairro(bairros.get(c.indiceBairro()))
                    .build();

            Solicitacao solicitacao = solicitacaoRepo.save(Solicitacao.builder()
                    .protocolo(c.protocolo())
                    .cidadao(cidadaos.get(c.indiceCidadao()))
                    .tipoServico(tipos.get(c.codigoTipoEscolhido()))
                    .localizacao(local)
                    .servidor(c.indiceServidorResponsavel() != null ? servidores.get(c.indiceServidorResponsavel()) : null)
                    .descricao(c.descricao())
                    .status(StatusSolicitacao.valueOf(c.status()))
                    .dataAbertura(c.dataAbertura())
                    .dataPrevisao(c.dataPrevisao())
                    .dataConclusao(c.dataConclusao())
                    .tipoSugeridoIa(c.codigoSugeridoIa() != null ? tipos.get(c.codigoSugeridoIa()) : null)
                    .confiancaIa(c.confiancaIa())
                    .build());

            List<HistoricoStatus> trilha = new ArrayList<>();
            for (DadosDemo.Transicao t : c.historico()) {
                trilha.add(HistoricoStatus.builder()
                        .solicitacao(solicitacao)
                        .servidor(servidores.get(t.indiceServidor()))
                        .statusAnterior(StatusSolicitacao.valueOf(t.statusAnterior()))
                        .statusNovo(StatusSolicitacao.valueOf(t.statusNovo()))
                        .dataHora(t.dataHora())
                        .justificativa(t.justificativa())
                        .build());
            }
            historicoRepo.saveAll(trilha);
            historicos += trilha.size();

            if (c.avaliacao() != null) {
                avaliacaoRepo.save(Avaliacao.builder()
                        .solicitacao(solicitacao)
                        .nota(c.avaliacao().nota())
                        .comentario(c.avaliacao().comentario())
                        .dataAvaliacao(c.avaliacao().data())
                        .build());
                avaliacoes++;
            }
            if (++n % 500 == 0) log.info("  ... {} de {} solicitações", n, dados.chamados().size());
        }

        log.info("===== DemoDataSeeder concluído em {} s: {} solicitações, {} transições de status, {} avaliações =====",
                (System.currentTimeMillis() - inicio) / 1000, n, historicos, avaliacoes);
        log.info("Logins de demonstração (senha {}): {} (servidor) e {} (cidadão)",
                SENHA_DEMO, dados.servidores().get(0).email(), dados.cidadaos().get(0).email());
    }

    /** Os 5 tipos de serviço (normalmente já criados pelo DevDataSeeder). */
    private Map<String, TipoServico> garantirTipos() {
        Map<String, TipoServico> porCodigo = new HashMap<>();
        for (TipoServico t : tipoServicoRepo.findAll()) {
            porCodigo.put(t.getCodigo().toUpperCase(), t);
        }
        String[][] padrao = {
                {"BURACO", "Buraco na via"}, {"ILUMINACAO", "Iluminação queimada"}, {"PODA", "Poda de árvore"},
                {"ENTULHO", "Remoção de entulho"}, {"OUTROS", "Outros"}};
        for (String[] p : padrao) {
            porCodigo.computeIfAbsent(p[0], codigo -> tipoServicoRepo.save(TipoServico.builder()
                    .codigo(codigo)
                    .descricao(p[1])
                    .slaDias(GeradorDadosDemo.SLA_DIAS.get(codigo))
                    .ativo("S")
                    .build()));
        }
        return porCodigo;
    }

    /** Reaproveita bairros com o mesmo nome (ex.: "Centro" do DevDataSeeder) e cria os que faltam. */
    private List<Bairro> garantirBairros(List<DadosDemo.Bairro> demo) {
        Map<String, Bairro> existentes = new HashMap<>();
        for (Bairro b : bairroRepo.findAll()) {
            existentes.putIfAbsent(b.getNomeBairro().toLowerCase(), b);
        }
        List<Bairro> resultado = new ArrayList<>();
        for (DadosDemo.Bairro b : demo) {
            Bairro bairro = existentes.get(b.nome().toLowerCase());
            if (bairro == null) {
                bairro = bairroRepo.save(Bairro.builder()
                        .nomeBairro(b.nome())
                        .municipio("São Paulo")
                        .uf("SP")
                        .build());
            }
            resultado.add(bairro);
        }
        return resultado;
    }

    private static String sha256(String valor) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Falha gerando SHA-256", e);
        }
    }
}
