package br.com.hackgov.controller;

import br.com.hackgov.dto.ApiResponse;
import br.com.hackgov.dto.ClassificacaoRequest;
import br.com.hackgov.dto.ClassificacaoResponse;
import br.com.hackgov.dto.ModeloIaResponse;
import br.com.hackgov.service.TriagemIaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ia")
@Tag(name = "Inteligência Artificial")
public class IaController {

    private final TriagemIaService service;

    public IaController(TriagemIaService service) {
        this.service = service;
    }

    @PostMapping("/classificar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Sugere o tipo de serviço a partir da descrição (Naive Bayes local, sem API externa)")
    public ResponseEntity<ApiResponse<ClassificacaoResponse>> classificar(@Valid @RequestBody ClassificacaoRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.classificar(req.descricao())));
    }

    @GetMapping("/modelo")
    @PreAuthorize("hasAnyRole('SERVIDOR','GESTOR')")
    @Operation(summary = "Ficha técnica e métricas de validação do modelo de triagem")
    public ResponseEntity<ApiResponse<ModeloIaResponse>> modelo() {
        return ResponseEntity.ok(ApiResponse.ok(service.modelo()));
    }
}
