package br.com.hackgov.service;

import br.com.hackgov.dto.IntegridadeDtos.AncorarRequest;
import br.com.hackgov.dto.IntegridadeDtos.LoteDia;
import br.com.hackgov.dto.IntegridadeDtos.ProvaChamado;
import br.com.hackgov.dto.IntegridadeDtos.ProvaEvento;
import br.com.hackgov.dto.IntegridadeDtos.ResumoIntegridade;
import br.com.hackgov.exception.BusinessException;
import br.com.hackgov.exception.NotFoundException;
import br.com.hackgov.integridade.MerkleSha256;
import br.com.hackgov.integridade.RegistroCanonico;
import br.com.hackgov.model.AncoraAuditoria;
import br.com.hackgov.model.HistoricoStatus;
import br.com.hackgov.model.Solicitacao;
import br.com.hackgov.repository.AncoraAuditoriaRepository;
import br.com.hackgov.repository.HistoricoStatusRepository;
import br.com.hackgov.repository.SolicitacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Integridade da trilha de auditoria (Fase 7, Partes 1 e 4).
 *
 * <p>Cada dia de HISTORICO_STATUS vira um lote: os eventos, em ordem de ID, são
 * transformados em texto canônico, cada texto em uma folha SHA-256, e as folhas
 * em uma árvore de Merkle. A raiz do dia é ancorada no contrato HackGovRegistro
 * (script blockchain/scripts/demo.js), e o banco guarda só a referência.
 *
 * <p>Para verificar, o sistema recalcula a raiz a partir do banco atual e compara
 * com a ancorada. Se alguém alterar um único evento depois da ancoragem — mesmo
 * com acesso direto ao Oracle, sem passar pela API — o dia aparece DIVERGENTE.
 * É o "I" da tríade CIA garantido por algo que nem o administrador do banco controla.
 */
@Service
public class IntegridadeService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final HistoricoStatusRepository historicoRepo;
    private final AncoraAuditoriaRepository ancoraRepo;
    private final SolicitacaoRepository solicitacaoRepo;

    public IntegridadeService(HistoricoStatusRepository historicoRepo, AncoraAuditoriaRepository ancoraRepo,
                              SolicitacaoRepository solicitacaoRepo) {
        this.historicoRepo = historicoRepo;
        this.ancoraRepo = ancoraRepo;
        this.solicitacaoRepo = solicitacaoRepo;
    }

    static String textoCanonico(HistoricoStatus h, String protocolo) {
        return RegistroCanonico.texto(h.getId(), protocolo, h.getStatusAnterior().name(), h.getStatusNovo().name(),
                h.getDataHora(), h.getServidor() == null ? null : h.getServidor().getId(), h.getJustificativa());
    }

    private static byte[] folha(HistoricoStatus h) {
        return MerkleSha256.folha(textoCanonico(h, h.getSolicitacao().getProtocolo()));
    }

    /** Todos os dias com eventos, do mais recente ao mais antigo, com a situação de cada um. */
    @Transactional(readOnly = true)
    public ResumoIntegridade resumo() {
        TreeMap<LocalDate, List<byte[]>> porDia = new TreeMap<>();
        Map<LocalDate, String> exemplo = new LinkedHashMap<>();
        for (HistoricoStatus h : historicoRepo.findTodosComSolicitacao()) {
            LocalDate dia = h.getDataHora().toLocalDate();
            porDia.computeIfAbsent(dia, d -> new ArrayList<>()).add(folha(h));
            exemplo.putIfAbsent(dia, h.getSolicitacao().getProtocolo());
        }
        Map<LocalDate, AncoraAuditoria> ancoras = ancoraRepo.findAll().stream()
                .collect(Collectors.toMap(AncoraAuditoria::getDataReferencia, Function.identity()));

        List<LoteDia> lotes = new ArrayList<>();
        long eventos = 0;
        int integros = 0, divergentes = 0, pendentes = 0;
        for (Map.Entry<LocalDate, List<byte[]>> e : porDia.descendingMap().entrySet()) {
            LoteDia l = montar(e.getKey(), e.getValue(), ancoras.get(e.getKey()), exemplo.get(e.getKey()));
            lotes.add(l);
            eventos += l.eventos();
            switch (l.situacao()) {
                case "INTEGRO" -> integros++;
                case "DIVERGENTE" -> divergentes++;
                default -> pendentes++;
            }
        }
        // âncora de um dia cujos eventos sumiram todos do banco também é divergência
        for (AncoraAuditoria a : ancoras.values()) {
            if (!porDia.containsKey(a.getDataReferencia())) {
                lotes.add(new LoteDia(a.getDataReferencia(), 0, null, inicio(a.getDataReferencia()), fim(a.getDataReferencia()),
                        "DIVERGENTE", a.getRaizMerkle(), a.getRede(), a.getTxHash(), a.getIndiceLote(), a.getDataAncoragem(), null));
                divergentes++;
            }
        }
        return new ResumoIntegridade(lotes.size(), integros + divergentes, integros, divergentes, pendentes, eventos, lotes);
    }

    @Transactional(readOnly = true)
    public LoteDia lote(LocalDate data) {
        List<HistoricoStatus> eventos = eventosDoDia(data);
        List<byte[]> folhas = eventos.stream().map(IntegridadeService::folha).toList();
        AncoraAuditoria ancora = ancoraRepo.findByDataReferencia(data).orElse(null);
        if (folhas.isEmpty() && ancora == null) throw new NotFoundException("Nenhum evento de auditoria em " + data);
        return montar(data, folhas, ancora, eventos.isEmpty() ? null : eventos.get(0).getSolicitacao().getProtocolo());
    }

    /**
     * Registra a âncora de um dia já fechado. A raiz enviada (a que foi gravada na
     * blockchain) precisa bater com a recalculada agora, e uma âncora existente
     * nunca é sobrescrita: corrigir um dia exige um evento novo, não reescrever o antigo.
     */
    @Transactional
    public LoteDia ancorar(LocalDate data, AncorarRequest req) {
        if (!data.isBefore(LocalDate.now(ZONA))) {
            throw new BusinessException("Só dias encerrados podem ser ancorados (o dia de hoje ainda recebe eventos)");
        }
        if (ancoraRepo.findByDataReferencia(data).isPresent()) {
            throw new BusinessException("Este dia já foi ancorado; âncoras são imutáveis");
        }
        List<byte[]> folhas = folhasDoDia(data);
        if (folhas.isEmpty()) throw new BusinessException("Não há eventos de auditoria em " + data);
        String raiz = MerkleSha256.hex(MerkleSha256.raiz(folhas));
        if (!raiz.equalsIgnoreCase(req.raiz())) {
            throw new BusinessException("A raiz informada não confere com a auditoria atual do dia");
        }
        AncoraAuditoria a = ancoraRepo.save(AncoraAuditoria.builder()
                .dataReferencia(data).raizMerkle(raiz).qtdEventos(folhas.size())
                .rede(req.rede()).txHash(req.txHash().toLowerCase()).indiceLote(req.indiceLote())
                .build());
        return montar(data, folhas, a, null);
    }

    /** Prova de inclusão de cada evento de um chamado, pronta para conferir no contrato. */
    @Transactional(readOnly = true)
    public ProvaChamado prova(String protocolo) {
        Solicitacao s = solicitacaoRepo.findByProtocolo(protocolo)
                .orElseThrow(() -> new NotFoundException("Protocolo não encontrado"));
        List<HistoricoStatus> doChamado = historicoRepo.findBySolicitacaoIdOrderByDataHoraAsc(s.getId());

        Map<LocalDate, List<HistoricoStatus>> diasCache = new LinkedHashMap<>();
        List<ProvaEvento> provas = new ArrayList<>();
        for (HistoricoStatus h : doChamado) {
            LocalDate dia = h.getDataHora().toLocalDate();
            List<HistoricoStatus> doDia = diasCache.computeIfAbsent(dia, d -> eventosDoDia(d));
            List<byte[]> folhas = doDia.stream().map(IntegridadeService::folha).toList();
            int indice = -1;
            for (int i = 0; i < doDia.size(); i++) {
                if (doDia.get(i).getId().equals(h.getId())) { indice = i; break; }
            }
            if (indice < 0) continue;
            byte[] raiz = MerkleSha256.raiz(folhas);
            List<byte[]> prova = MerkleSha256.prova(folhas, indice);
            AncoraAuditoria a = ancoraRepo.findByDataReferencia(dia).orElse(null);
            boolean confere = a != null
                    && MerkleSha256.verificar(folhas.get(indice), prova, MerkleSha256.deHex(a.getRaizMerkle()));
            provas.add(new ProvaEvento(h.getId(), h.getDataHora(), h.getStatusAnterior().name(), h.getStatusNovo().name(),
                    MerkleSha256.hex(folhas.get(indice)), prova.stream().map(MerkleSha256::hex).toList(),
                    MerkleSha256.hex(raiz), a == null ? null : a.getRaizMerkle(), a == null ? null : a.getTxHash(),
                    a == null ? null : a.getIndiceLote(), confere));
        }
        String hashProtocolo = MerkleSha256.hex(MerkleSha256.sha256(protocolo.getBytes(StandardCharsets.UTF_8)));
        return new ProvaChamado(protocolo, hashProtocolo, provas);
    }

    // ── apoio ─────────────────────────────────────────────────────

    private List<HistoricoStatus> eventosDoDia(LocalDate dia) {
        return historicoRepo.findEventosDoPeriodo(dia.atStartOfDay(), dia.plusDays(1).atStartOfDay());
    }

    private List<byte[]> folhasDoDia(LocalDate dia) {
        return eventosDoDia(dia).stream().map(IntegridadeService::folha).toList();
    }

    private static long inicio(LocalDate d) {
        return d.atStartOfDay(ZONA).toEpochSecond();
    }

    private static long fim(LocalDate d) {
        return d.plusDays(1).atStartOfDay(ZONA).toEpochSecond() - 1;
    }

    private static LoteDia montar(LocalDate data, List<byte[]> folhas, AncoraAuditoria a, String exemploProtocolo) {
        String raizAtual = folhas.isEmpty() ? null : MerkleSha256.hex(MerkleSha256.raiz(folhas));
        String situacao = a == null ? "PENDENTE"
                : (raizAtual != null && raizAtual.equalsIgnoreCase(a.getRaizMerkle()) && folhas.size() == a.getQtdEventos()
                    ? "INTEGRO" : "DIVERGENTE");
        return new LoteDia(data, folhas.size(), raizAtual, inicio(data), fim(data), situacao,
                a == null ? null : a.getRaizMerkle(), a == null ? null : a.getRede(),
                a == null ? null : a.getTxHash(), a == null ? null : a.getIndiceLote(),
                a == null ? null : a.getDataAncoragem(), exemploProtocolo);
    }
}
