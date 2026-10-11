package br.com.hackgov.controller;

import br.com.hackgov.dto.ApiResponse;
import br.com.hackgov.dto.ArquivoResponse;
import br.com.hackgov.service.ArquivoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/arquivos")
@Tag(name = "Arquivos")
public class ArquivoController {

    private final ArquivoService service;

    public ArquivoController(ArquivoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Envia uma foto (JPG, PNG ou WEBP, até 5 MB) e devolve o caminho a gravar na solicitação")
    public ResponseEntity<ApiResponse<ArquivoResponse>> enviar(@RequestParam("arquivo") MultipartFile arquivo) {
        String caminho = service.salvarImagem(arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Imagem enviada", new ArquivoResponse(caminho)));
    }

    @GetMapping("/{nome}")
    @Operation(summary = "Exibe uma foto enviada (público, como o acompanhamento por protocolo)")
    public ResponseEntity<Resource> obter(@PathVariable String nome) {
        Resource arquivo = service.carregar(nome);
        return ResponseEntity.ok()
                .contentType(service.tipoDe(nome))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(arquivo);
    }
}
