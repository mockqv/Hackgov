package br.com.hackgov.repository;

import br.com.hackgov.model.TipoServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TipoServicoRepository extends JpaRepository<TipoServico, Long> {
    List<TipoServico> findByAtivoOrderByDescricao(String ativo);
    List<TipoServico> findAllByOrderByDescricao();
    boolean existsByCodigoIgnoreCase(String codigo);
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
    Optional<TipoServico> findByCodigoIgnoreCase(String codigo);
}
