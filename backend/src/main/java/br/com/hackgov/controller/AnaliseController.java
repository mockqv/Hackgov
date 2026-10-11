package br.com.hackgov.controller;

import br.com.hackgov.dto.AgrupamentosResponse;
import br.com.hackgov.dto.ApiResponse;
import br.com.hackgov.service.AnaliseEspacialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analise")
@Tag(name = "Análise de dados")
public class AnaliseController {

    private final AnaliseEspacialService service;

    public AnaliseController(AnaliseEspacialService service) {
        this.service = service;
    }

    @GetMapping("/agrupamentos")
    @PreAuthorize("hasAnyRole('SERVIDOR','GESTOR')")
    @Operation(summary = "Agrupa chamados do mesmo tipo próximos entre si (KD-Tree + Union-Find): "
            + "possíveis duplicados (só abertas) ou pontos críticos (histórico completo)")
    public ResponseEntity<ApiResponse<AgrupamentosResponse>> agrupamentos(
            @RequestParam(defaultValue = "50") double raioMetros,
            @RequestParam(defaultValue = "true") boolean apenasAbertas,
            @RequestParam(defaultValue = "2") int minimo,
            @RequestParam(defaultValue = "20") int limite) {
        double raio = Math.max(5, Math.min(raioMetros, 500));     // limites evitam consultas abusivas
        int tamanhoMinimo = Math.max(2, Math.min(minimo, 50));
        int top = Math.max(1, Math.min(limite, 200));
        return ResponseEntity.ok(ApiResponse.ok(service.agrupar(raio, apenasAbertas, tamanhoMinimo, top)));
    }
}
