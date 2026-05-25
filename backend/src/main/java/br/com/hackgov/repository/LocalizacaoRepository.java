package br.com.hackgov.repository;

import br.com.hackgov.model.Localizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {

    @Query("""
        SELECT l FROM Localizacao l
        WHERE ABS(l.latitude  - :lat) * 111000 <= 15
          AND ABS(l.longitude - :lon) * 111000 <= 15
    """)
    List<Localizacao> findProximas(@Param("lat") double lat, @Param("lon") double lon);
}
