package br.com.hackgov.repository;

import br.com.hackgov.model.HistoricoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
    List<HistoricoStatus> findBySolicitacaoIdOrderByDataHoraAsc(Long solicitacaoId);

    /** Eventos de um intervalo, já com o protocolo (join fetch evita N+1 ao montar o texto canônico). */
    @Query("""
            SELECT h FROM HistoricoStatus h JOIN FETCH h.solicitacao s
            WHERE h.dataHora >= :inicio AND h.dataHora < :fim
            ORDER BY h.id
            """)
    List<HistoricoStatus> findEventosDoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /** Toda a trilha, em ordem, para agrupar por dia. */
    @Query("SELECT h FROM HistoricoStatus h JOIN FETCH h.solicitacao s ORDER BY h.id")
    List<HistoricoStatus> findTodosComSolicitacao();
}
