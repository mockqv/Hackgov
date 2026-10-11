package br.com.hackgov.controller;

import br.com.hackgov.dto.ApiResponse;
import br.com.hackgov.dto.BairroResponse;
import br.com.hackgov.service.BairroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bairros")
@Tag(name = "Bairros")
public class BairroController {

    private final BairroService service;

    public BairroController(BairroService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista os bairros atendidos (seletor do formulário de nova solicitação)")
    public ResponseEntity<ApiResponse<List<BairroResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(service.listar()));
    }
}
