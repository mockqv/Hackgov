package br.com.hackgov.service;

import br.com.hackgov.dto.BairroResponse;
import br.com.hackgov.repository.BairroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Catálogo de bairros para o formulário de nova solicitação (antes o cidadão digitava o ID). */
@Service
public class BairroService {

    private final BairroRepository bairroRepo;

    public BairroService(BairroRepository bairroRepo) {
        this.bairroRepo = bairroRepo;
    }

    @Transactional(readOnly = true)
    public List<BairroResponse> listar() {
        Collator ordemAlfabetica = Collator.getInstance(Locale.forLanguageTag("pt-BR"));
        return bairroRepo.findAll().stream()
                .map(BairroResponse::from)
                .sorted(Comparator.comparing(BairroResponse::nome, ordemAlfabetica))
                .toList();
    }
}
