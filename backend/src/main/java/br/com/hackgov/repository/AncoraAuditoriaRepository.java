package br.com.hackgov.repository;

import br.com.hackgov.model.AncoraAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AncoraAuditoriaRepository extends JpaRepository<AncoraAuditoria, Long> {
    Optional<AncoraAuditoria> findByDataReferencia(LocalDate dataReferencia);
}
