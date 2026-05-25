package br.com.hackgov.controller;

import br.com.hackgov.dto.*;
import br.com.hackgov.service.CidadaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cidadaos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Cidadãos")
public class CidadaoController {

    private final CidadaoService service;

    @PostMapping
    public ResponseEntity<ApiResponse<CidadaoResponse>> cadastrar(
            @Valid @RequestBody CidadaoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Cidadão cadastrado", service.cadastrar(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CidadaoResponse>> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }
}
