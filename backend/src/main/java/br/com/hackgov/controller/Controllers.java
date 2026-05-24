package br.com.hackgov.controller;

import br.com.hackgov.dto.*;
import br.com.hackgov.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// ════════════════════════════════════════════════════════════════
// SOLICITAÇÕES
// ════════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Solicitações", description = "Ciclo de vida das solicitações de zeladoria")
class SolicitacaoController {

    private final SolicitacaoService service;

    // POST /api/solicitacoes — cidadão abre chamado
    @PostMapping
    @Operation(summary = "Abrir nova solicitação", description = "Valida duplicata por geolocalização (raio 10m) antes de criar")
    public ResponseEntity<ApiResponse<SolicitacaoDetalheResponse>> criar(
            @Valid @RequestBody SolicitacaoRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Solicitação criada com sucesso", service.criar(req)));
    }

    // GET /api/solicitacoes/protocolo/{protocolo} — cidadão acompanha
    @GetMapping("/protocolo/{protocolo}")
    @Operation(summary = "Acompanhar por protocolo")
    public ResponseEntity<ApiResponse<SolicitacaoDetalheResponse>> buscarPorProtocolo(
            @PathVariable String protocolo) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorProtocolo(protocolo)));
    }

    // GET /api/solicitacoes/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Buscar por ID")
    public ResponseEntity<ApiResponse<SolicitacaoDetalheResponse>> buscarPorId(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }

    // GET /api/solicitacoes/cidadao/{cidadaoId} — minhas solicitações
    @GetMapping("/cidadao/{cidadaoId}")
    @Operation(summary = "Listar solicitações do cidadão")
    public ResponseEntity<ApiResponse<List<SolicitacaoResumoResponse>>> listarPorCidadao(
            @PathVariable Long cidadaoId) {
        return ResponseEntity.ok(ApiResponse.ok(service.listarPorCidadao(cidadaoId)));
    }

    // GET /api/solicitacoes/mapa — mapa público (todos os pins)
    @GetMapping("/mapa")
    @Operation(summary = "Listar todas para o mapa público")
    public ResponseEntity<ApiResponse<List<SolicitacaoResumoResponse>>> listarParaMapa() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarParaMapa()));
    }

    // GET /api/solicitacoes/abertas — painel do servidor
    @GetMapping("/abertas")
    @Operation(summary = "Listar solicitações abertas (painel servidor)")
    public ResponseEntity<ApiResponse<List<SolicitacaoResumoResponse>>> listarAbertas() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarAbertas()));
    }

    // PATCH /api/solicitacoes/{id}/status — servidor atualiza status
    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status (servidor)", description = "Registra histórico imutável e notifica cidadão")
    public ResponseEntity<ApiResponse<SolicitacaoDetalheResponse>> atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Status atualizado", service.atualizarStatus(id, req)));
    }

    // GET /api/solicitacoes/{id}/historico — linha do tempo
    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico de status da solicitação")
    public ResponseEntity<ApiResponse<List<HistoricoStatusResponse>>> historico(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarHistorico(id)));
    }

    // POST /api/solicitacoes/{id}/avaliar — cidadão avalia
    @PostMapping("/{id}/avaliar")
    @Operation(summary = "Avaliar serviço realizado", description = "Disponível apenas para solicitações CONCLUÍDAS")
    public ResponseEntity<ApiResponse<Void>> avaliar(
            @PathVariable Long id,
            @Valid @RequestBody AvaliacaoRequest req) {
        service.avaliar(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Avaliação registrada com sucesso", null));
    }
}

// ════════════════════════════════════════════════════════════════
// CIDADÃOS
// ════════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/cidadaos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Cidadãos", description = "Cadastro e consulta de cidadãos")
class CidadaoController {

    private final CidadaoService service;

    // POST /api/cidadaos
    @PostMapping
    @Operation(summary = "Cadastrar cidadão", description = "CPF é convertido para hash SHA-256 (LGPD)")
    public ResponseEntity<ApiResponse<CidadaoResponse>> cadastrar(
            @Valid @RequestBody CidadaoRequest req) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cidadão cadastrado", service.cadastrar(req)));
    }

    // GET /api/cidadaos/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Buscar cidadão por ID")
    public ResponseEntity<ApiResponse<CidadaoResponse>> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }
}

// ════════════════════════════════════════════════════════════════
// DASHBOARD / ANALYTICS
// ════════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard", description = "Métricas e indicadores gerenciais")
class DashboardController {

    private final DashboardService service;

    // GET /api/dashboard
    @GetMapping
    @Operation(summary = "Dashboard geral", description = "KPIs, contagem por tipo, tempo médio de resolução e nota média")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.ok(service.gerarDashboard()));
    }
}

// ════════════════════════════════════════════════════════════════
// TIPOS DE SERVIÇO
// ════════════════════════════════════════════════════════════════
@RestController
@RequestMapping("/api/tipos-servico")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Tipos de Serviço", description = "Categorias disponíveis para solicitação")
class TipoServicoController {

    private final TipoServicoService service;

    // GET /api/tipos-servico
    @GetMapping
    @Operation(summary = "Listar tipos de serviço ativos")
    public ResponseEntity<ApiResponse<List<TipoServicoResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarAtivos()));
    }
}
