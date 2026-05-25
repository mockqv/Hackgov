package br.com.hackgov.repository;

import br.com.hackgov.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    Optional<Avaliacao> findBySolicitacaoId(Long solicitacaoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a")
    Double findAvgNota();
}
