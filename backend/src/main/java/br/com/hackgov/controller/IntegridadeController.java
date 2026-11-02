package br.com.hackgov.controller;

import br.com.hackgov.dto.ApiResponse;
import br.com.hackgov.dto.IntegridadeDtos.AncorarRequest;
import br.com.hackgov.dto.IntegridadeDtos.LoteDia;
import br.com.hackgov.dto.IntegridadeDtos.ProvaChamado;
import br.com.hackgov.dto.IntegridadeDtos.ResumoIntegridade;
import br.com.hackgov.service.IntegridadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/integridade")
@Tag(name = "Integridade da auditoria (blockchain)")
public class IntegridadeController {

    private final IntegridadeService service;

    public IntegridadeController(IntegridadeService service) {
        this.service = service;
    }

    @GetMapping("/lotes")
    @PreAuthorize("hasAnyRole('SERVIDOR','GESTOR')")
    @Operation(summary = "Recalcula a raiz de Merkle de cada dia de auditoria e compara com a âncora gravada na blockchain")
    public ResponseEntity<ApiResponse<ResumoIntegridade>> lotes() {
        return ResponseEntity.ok(ApiResponse.ok(service.resumo()));
    }

    @GetMapping("/lotes/{data}")
    @PreAuthorize("hasAnyRole('SERVIDOR','GESTOR')")
    @Operation(summary = "Lote de auditoria de um dia (raiz atual, período em epoch e âncora)")
    public ResponseEntity<ApiResponse<LoteDia>> lote(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(ApiResponse.ok(service.lote(data)));
    }

    @PostMapping("/lotes/{data}/ancora")
    @PreAuthorize("hasRole('GESTOR')")
    @Operation(summary = "Registra a transação que ancorou o dia no contrato HackGovRegistro (imutável)")
    public ResponseEntity<ApiResponse<LoteDia>> ancorar(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @Valid @RequestBody AncorarRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Dia ancorado", service.ancorar(data, req)));
    }

    @GetMapping("/prova/{protocolo}")
    @PreAuthorize("hasAnyRole('SERVIDOR','GESTOR')")
    @Operation(summary = "Prova de Merkle de cada evento de um chamado, para conferir no contrato")
    public ResponseEntity<ApiResponse<ProvaChamado>> prova(@PathVariable String protocolo) {
        return ResponseEntity.ok(ApiResponse.ok(service.prova(protocolo)));
    }
}
