package br.com.hackgov.repository;

import br.com.hackgov.model.Cidadao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CidadaoRepository extends JpaRepository<Cidadao, Long> {
    Optional<Cidadao> findByEmail(String email);
}
