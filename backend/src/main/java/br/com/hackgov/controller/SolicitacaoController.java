package br.com.hackgov.controller;

import br.com.hackgov.dto.*;
import br.com.hackgov.service.SolicitacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes")
@RequiredArgsConstructor
@Tag(name = "Solicitações")
public class SolicitacaoController {

    private final SolicitacaoService service;

    @PostMapping
    @PreAuthorize("hasRole('CIDADAO')")
    @Operation(summary = "Abrir nova solicitação (cidadão autenticado)")
    public ResponseEntity<ApiResponse<SolicitacaoDetalheResponse>> criar(
            @Valid @RequestBody SolicitacaoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Solicitação criada", service.criar(req)));
    }

    @GetMapping("/protocolo/{protocolo}")
    @Operation(summary = "Acompanhar por protocolo (público)")
    public ResponseEntity<ApiResponse<SolicitacaoDetalheResponse>> porProtocolo(
            @PathVariable String protocolo) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorProtocolo(protocolo)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SolicitacaoDetalheResponse>> porId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasRole('CIDADAO')")
    @Operation(summary = "Listar solicitações do cidadão autenticado")
    public ResponseEntity<ApiResponse<List<SolicitacaoResumoResponse>>> minhas() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarMinhas()));
    }

    @GetMapping("/mapa")
    @Operation(summary = "Pins para o mapa público")
    public ResponseEntity<ApiResponse<List<SolicitacaoResumoResponse>>> mapa() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarParaMapa()));
    }

    @GetMapping("/abertas")
    @PreAuthorize("hasAnyRole('SERVIDOR','GESTOR')")
    @Operation(summary = "Painel de triagem do servidor")
    public ResponseEntity<ApiResponse<List<SolicitacaoResumoResponse>>> abertas() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarAbertas()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SERVIDOR','GESTOR')")
    @Operation(summary = "Atualizar status (servidor)")
    public ResponseEntity<ApiResponse<SolicitacaoDetalheResponse>> atualizarStatus(
            @PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Status atualizado", service.atualizarStatus(id, req)));
    }

    @GetMapping("/{id}/historico")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<HistoricoStatusResponse>>> historico(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarHistorico(id)));
    }

    @PostMapping("/{id}/avaliar")
    @PreAuthorize("hasRole('CIDADAO')")
    @Operation(summary = "Avaliar solicitação concluída")
    public ResponseEntity<ApiResponse<Void>> avaliar(
            @PathVariable Long id, @Valid @RequestBody AvaliacaoRequest req) {
        service.avaliar(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Avaliação registrada", null));
    }
}
