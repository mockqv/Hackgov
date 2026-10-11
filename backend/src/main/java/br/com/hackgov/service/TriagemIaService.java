package br.com.hackgov.service;

import br.com.hackgov.dto.ClassificacaoResponse;
import br.com.hackgov.dto.ModeloIaResponse;
import br.com.hackgov.ia.AvaliacaoModelo;
import br.com.hackgov.ia.AvaliadorModelo;
import br.com.hackgov.ia.ClassificadorNaiveBayes;
import br.com.hackgov.ia.CorpusTriagem;
import br.com.hackgov.ia.ExemploRotulado;
import br.com.hackgov.ia.ResultadoClassificacao;
import br.com.hackgov.model.TipoServico;
import br.com.hackgov.repository.TipoServicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * IA de triagem (Fase 6): sugere o tipo de serviço a partir da descrição livre
 * do cidadão, com um classificador Naive Bayes treinado no próprio servidor.
 *
 * Por que local e não uma API de IA generativa: a descrição pode conter dado
 * pessoal (endereço, nome, telefone) e não sai da infraestrutura da prefeitura
 * (LGPD); não há custo por requisição nem dependência de terceiros; a resposta
 * leva microssegundos; e a decisão é explicável (termos que mais pesaram).
 *
 * O modelo é treinado uma única vez, na subida da aplicação (~0,1 s). Se o
 * corpus não puder ser lido, o serviço fica "indisponível" e o restante do
 * sistema funciona normalmente — a IA é apoio, nunca bloqueio.
 */
@Service
public class TriagemIaService {

    private static final Logger log = LoggerFactory.getLogger(TriagemIaService.class);

    /** Abaixo deste valor a tela não sugere tipo (curva cobertura × acurácia, Parte 3). */
    public static final double LIMIAR_CONFIANCA = 0.60;
    private static final double ALFA_LAPLACE = 1.0;

    private final TipoServicoRepository tipoServicoRepo;
    private final ClassificadorNaiveBayes modelo;
    private final AvaliacaoModelo avaliacao;
    private final LocalDateTime treinadoEm;

    public TriagemIaService(TipoServicoRepository tipoServicoRepo) {
        this.tipoServicoRepo = tipoServicoRepo;
        ClassificadorNaiveBayes treinado = null;
        AvaliacaoModelo metricas = null;
        try {
            List<ExemploRotulado> corpus = CorpusTriagem.carregarPadrao();
            treinado = ClassificadorNaiveBayes.treinado(corpus, ALFA_LAPLACE);
            metricas = AvaliadorModelo.validacaoCruzada(corpus, 5, 42L, ALFA_LAPLACE);
            log.info("IA de triagem pronta: {} exemplos, vocabulário de {} termos, acurácia 5-fold {}",
                    treinado.totalDocumentos(), treinado.tamanhoVocabulario(),
                    String.format("%.1f%%", metricas.acuracia() * 100));
        } catch (RuntimeException e) {
            log.error("IA de triagem indisponível — o sistema segue sem sugestões automáticas", e);
        }
        this.modelo = treinado;
        this.avaliacao = metricas;
        this.treinadoEm = LocalDateTime.now();
    }

    public boolean disponivel() {
        return modelo != null;
    }

    /** Resultado bruto do modelo (usado na abertura da solicitação e pelo seeder de demonstração). */
    public Optional<ResultadoClassificacao> sugerir(String texto) {
        if (modelo == null || texto == null || texto.isBlank()) return Optional.empty();
        ResultadoClassificacao r = modelo.classificar(texto);
        return r.semSinal() ? Optional.empty() : Optional.of(r);
    }

    /** Sugestão já traduzida para o catálogo de tipos de serviço, para a tela de nova solicitação. */
    @Transactional(readOnly = true)
    public ClassificacaoResponse classificar(String texto) {
        if (modelo == null) return ClassificacaoResponse.indisponivel(LIMIAR_CONFIANCA);
        ResultadoClassificacao r = modelo.classificar(texto);
        if (r.semSinal()) return ClassificacaoResponse.semSinal(LIMIAR_CONFIANCA);

        Map<String, TipoServico> catalogo = new HashMap<>();
        for (TipoServico t : tipoServicoRepo.findAll()) {
            catalogo.put(t.getCodigo().toUpperCase(), t);
        }
        TipoServico tipo = catalogo.get(r.classe());
        boolean tipoAtivo = tipo != null && "S".equals(tipo.getAtivo());

        List<ClassificacaoResponse.Alternativa> alternativas = r.ranking().stream()
                .limit(3)
                .map(p -> {
                    TipoServico t = catalogo.get(p.classe());
                    return new ClassificacaoResponse.Alternativa(p.classe(),
                            t != null ? t.getDescricao() : p.classe(), arredondar(p.probabilidade()));
                })
                .toList();

        return new ClassificacaoResponse(
                true,
                tipoAtivo && r.confiavel(LIMIAR_CONFIANCA),
                tipoAtivo ? tipo.getId() : null,
                r.classe(),
                tipo != null ? tipo.getDescricao() : r.classe(),
                arredondar(r.confianca()),
                alternativas,
                r.termosRelevantes(),
                LIMIAR_CONFIANCA);
    }

    public ModeloIaResponse modelo() {
        if (modelo == null || avaliacao == null) {
            return new ModeloIaResponse(false, "Naive Bayes multinomial", 0, 0, Map.of(), LIMIAR_CONFIANCA, null, null);
        }
        ModeloIaResponse.Avaliacao cv = new ModeloIaResponse.Avaliacao(
                avaliacao.folds(), avaliacao.exemplos(), arredondar(avaliacao.acuracia()), arredondar(avaliacao.f1Macro()),
                avaliacao.classes(), avaliacao.matrizConfusao(),
                avaliacao.porClasse().stream()
                        .map(m -> new ModeloIaResponse.MetricaClasse(m.classe(), arredondar(m.precisao()),
                                arredondar(m.revocacao()), arredondar(m.f1()), m.suporte()))
                        .toList(),
                avaliacao.curvaLimiar().stream()
                        .map(p -> new ModeloIaResponse.PontoLimiar(p.limiar(), arredondar(p.cobertura()), arredondar(p.acuracia())))
                        .toList());
        return new ModeloIaResponse(true,
                "Naive Bayes multinomial (unigramas + bigramas, suavização de Laplace α = 1)",
                modelo.totalDocumentos(), modelo.tamanhoVocabulario(), modelo.documentosPorClasse(),
                LIMIAR_CONFIANCA, treinadoEm, cv);
    }

    private static double arredondar(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
