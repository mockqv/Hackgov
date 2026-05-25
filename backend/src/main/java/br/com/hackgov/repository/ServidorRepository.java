package br.com.hackgov.repository;

import br.com.hackgov.model.Servidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ServidorRepository extends JpaRepository<Servidor, Long> {
    Optional<Servidor> findByMatricula(String matricula);
    Optional<Servidor> findByEmailFuncional(String emailFuncional);
}
