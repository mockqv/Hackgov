package br.com.hackgov.controller;

import br.com.hackgov.dto.*;
import br.com.hackgov.service.CidadaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cidadaos")
@RequiredArgsConstructor
@Tag(name = "Cidadãos")
public class CidadaoController {

    private final CidadaoService service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CidadaoResponse>> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.buscarPorId(id)));
    }
}
