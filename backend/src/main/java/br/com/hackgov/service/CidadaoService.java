package br.com.hackgov.service;

import br.com.hackgov.dto.CidadaoRequest;
import br.com.hackgov.dto.CidadaoResponse;
import br.com.hackgov.exception.BusinessException;
import br.com.hackgov.exception.NotFoundException;
import br.com.hackgov.model.Cidadao;
import br.com.hackgov.repository.CidadaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Transactional
public class CidadaoService {

    private final CidadaoRepository cidadaoRepo;

    public CidadaoResponse cadastrar(CidadaoRequest req) {
        if (cidadaoRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new BusinessException("E-mail já cadastrado");
        }
        Cidadao c = Cidadao.builder()
                .nome(req.getNome())
                .email(req.getEmail())
                .cpfHash(sha256(req.getCpf()))
                .telefone(req.getTelefone())
                .build();
        return CidadaoResponse.from(cidadaoRepo.save(c));
    }

    @Transactional(readOnly = true)
    public CidadaoResponse buscarPorId(Long id) {
        return CidadaoResponse.from(
            cidadaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Cidadão não encontrado"))
        );
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash", e);
        }
    }
}
