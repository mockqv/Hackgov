package br.com.hackgov.controller;

import br.com.hackgov.dto.*;
import br.com.hackgov.service.TipoServicoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tipos-servico")
@RequiredArgsConstructor
@Tag(name = "Tipos de Serviço")
public class TipoServicoController {

    private final TipoServicoService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TipoServicoResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(service.listarAtivos()));
    }
}
