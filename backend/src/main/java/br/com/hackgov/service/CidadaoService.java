package br.com.hackgov.service;

import br.com.hackgov.dto.CidadaoResponse;
import br.com.hackgov.exception.NotFoundException;
import br.com.hackgov.repository.CidadaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CidadaoService {

    private final CidadaoRepository cidadaoRepo;

    public CidadaoResponse buscarPorId(Long id) {
        return CidadaoResponse.from(
            cidadaoRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Cidadão não encontrado"))
        );
    }
}
