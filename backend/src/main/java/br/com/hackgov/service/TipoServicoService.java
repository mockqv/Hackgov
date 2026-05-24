// ── TipoServicoService ──────────────────────────────────────────
package br.com.hackgov.service;

import br.com.hackgov.dto.TipoServicoResponse;
import br.com.hackgov.repository.TipoServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TipoServicoService {

    private final TipoServicoRepository repo;

    public List<TipoServicoResponse> listarAtivos() {
        return repo.findByAtivoOrderByDescricao("S")
                .stream().map(TipoServicoResponse::from).toList();
    }
}
