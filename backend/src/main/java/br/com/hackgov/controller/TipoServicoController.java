package br.com.hackgov.controller;

import br.com.hackgov.dto.*;
import br.com.hackgov.service.TipoServicoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-servico")
@RequiredArgsConstructor
@Tag(name = "Tipos de Serviço")
public class TipoServicoController {

    private final TipoServicoService service;

    // GET /api/tipos-servico — público (formulário de Nova Solicitação)
    @GetMapping
    public ResponseEntity<ApiResponse<List<TipoServicoResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarAtivos()));
    }

    // GET /api/tipos-servico/todos — GESTOR (inclui inativos, para o catálogo administrativo)
    @GetMapping("/todos")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<ApiResponse<List<TipoServicoResponse>>> listarTodos() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarTodos()));
    }

    // GET /api/tipos-servico/{id} — GESTOR
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<ApiResponse<TipoServicoResponse>> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }

    // POST /api/tipos-servico — GESTOR — 201 Created + Location
    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<ApiResponse<TipoServicoResponse>> criar(@Valid @RequestBody TipoServicoRequest req) {
        TipoServicoResponse criado = service.criar(req);
        return ResponseEntity.created(URI.create("/api/tipos-servico/" + criado.getId()))
                .body(ApiResponse.ok("Tipo de serviço criado", criado));
    }

    // PUT /api/tipos-servico/{id} — GESTOR — 200 OK
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<ApiResponse<TipoServicoResponse>> atualizar(@PathVariable Long id,
                                                                       @Valid @RequestBody TipoServicoRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Tipo de serviço atualizado", service.atualizar(id, req)));
    }

    // DELETE /api/tipos-servico/{id} — GESTOR — soft delete (ativo = 'N') — 204 No Content
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        service.inativar(id);
        return ResponseEntity.noContent().build();
    }
}
