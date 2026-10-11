package br.com.hackgov.service;

import br.com.hackgov.algoritmos.AgrupadorEspacial;
import br.com.hackgov.algoritmos.PontoGeo;
import br.com.hackgov.dto.AgrupamentosResponse;
import br.com.hackgov.model.Solicitacao;
import br.com.hackgov.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Análise espacial dos chamados (Fase 6, Parte 4): agrupa solicitações do mesmo
 * tipo que estão a até N metros umas das outras.
 *
 *  - Só abertas: grupos = possíveis duplicados (o mesmo problema aberto por
 *    vizinhos diferentes) — o painel de triagem marca esses chamados.
 *  - Histórico completo: grupos grandes = pontos críticos (o problema volta
 *    sempre ao mesmo trecho), insumo para obra definitiva em vez de reparo.
 *
 * Algoritmo: KD-Tree para buscar os vizinhos de cada ponto + Union-Find para
 * juntar os componentes conexos do grafo de proximidade — O(n log n) em média,
 * contra O(n²) da comparação de todos os pares (ver BenchmarkAgrupamento).
 */
@Service
public class AnaliseEspacialService {

    private static final int MAX_MEMBROS_LISTADOS = 12;

    private final SolicitacaoRepository solicitacaoRepo;

    public AnaliseEspacialService(SolicitacaoRepository solicitacaoRepo) {
        this.solicitacaoRepo = solicitacaoRepo;
    }

    @Transactional(readOnly = true)
    public AgrupamentosResponse agrupar(double raioMetros, boolean apenasAbertas, int tamanhoMinimo, int limite) {
        List<Solicitacao> base = apenasAbertas ? solicitacaoRepo.findTodasAbertas() : solicitacaoRepo.findAllParaMapa();

        Map<Long, Solicitacao> porId = new HashMap<>();
        List<PontoGeo> pontos = new ArrayList<>(base.size());
        for (Solicitacao s : base) {
            if (s.getLocalizacao() == null || s.getLocalizacao().getLatitude() == null
                    || s.getLocalizacao().getLongitude() == null) {
                continue;
            }
            porId.put(s.getId(), s);
            pontos.add(new PontoGeo(s.getId(), s.getLocalizacao().getLatitude(),
                    s.getLocalizacao().getLongitude(), s.getTipoServico().getCodigo()));
        }

        long inicio = System.nanoTime();
        List<AgrupadorEspacial.Grupo> grupos = AgrupadorEspacial.agrupar(pontos, raioMetros, tamanhoMinimo);
        List<AgrupadorEspacial.Grupo> maiores = AgrupadorEspacial.maiores(grupos, limite);
        double tempoMs = (System.nanoTime() - inicio) / 1_000_000.0;

        List<AgrupamentosResponse.Grupo> resposta = maiores.stream()
                .map(g -> paraResposta(g, porId))
                .toList();
        return new AgrupamentosResponse(raioMetros, apenasAbertas, pontos.size(), grupos.size(),
                Math.round(tempoMs * 100.0) / 100.0, "KD-Tree + Union-Find (componentes conexos)", resposta);
    }

    private AgrupamentosResponse.Grupo paraResposta(AgrupadorEspacial.Grupo g, Map<Long, Solicitacao> porId) {
        List<Solicitacao> membros = g.membros().stream().map(p -> porId.get(p.id())).toList();
        Solicitacao primeiro = membros.get(0);

        String bairro = maisFrequente(membros.stream()
                .map(s -> s.getLocalizacao().getBairro().getNomeBairro()).toList());
        String logradouro = maisFrequente(membros.stream()
                .map(s -> s.getLocalizacao().getLogradouro()).filter(l -> l != null && !l.isBlank()).toList());
        int abertas = (int) membros.stream().filter(s -> !s.getStatus().isTerminal()).count();
        LocalDateTime primeira = membros.stream().map(Solicitacao::getDataAbertura).min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime ultima = membros.stream().map(Solicitacao::getDataAbertura).max(Comparator.naturalOrder()).orElse(null);

        List<AgrupamentosResponse.Membro> listados = membros.stream()
                .sorted(Comparator.comparing(Solicitacao::getDataAbertura).reversed())
                .limit(MAX_MEMBROS_LISTADOS)
                .map(s -> new AgrupamentosResponse.Membro(s.getId(), s.getProtocolo(), s.getStatus().name(), s.getDataAbertura()))
                .toList();

        return new AgrupamentosResponse.Grupo(
                g.categoria(), primeiro.getTipoServico().getDescricao(), g.tamanho(), abertas,
                g.latitudeCentro(), g.longitudeCentro(), bairro, logradouro, primeira, ultima, listados);
    }

    private static String maisFrequente(List<String> valores) {
        return valores.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.<String, Long>comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
