package br.com.hackgov.repository;

import br.com.hackgov.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// ── BAIRRO ──────────────────────────────────────────────────
@Repository
interface BairroRepository extends JpaRepository<Bairro, Long> {}

// ── CIDADAO ─────────────────────────────────────────────────
@Repository
interface CidadaoRepository extends JpaRepository<Cidadao, Long> {
    Optional<Cidadao> findByEmail(String email);
}

// ── SERVIDOR ────────────────────────────────────────────────
@Repository
interface ServidorRepository extends JpaRepository<Servidor, Long> {
    Optional<Servidor> findByMatricula(String matricula);
}

// ── TIPO_SERVICO ─────────────────────────────────────────────
@Repository
interface TipoServicoRepository extends JpaRepository<TipoServico, Long> {
    List<TipoServico> findByAtivoOrderByDescricao(String ativo);
}

// ── LOCALIZACAO ──────────────────────────────────────────────
@Repository
interface LocalizacaoRepository extends JpaRepository<Localizacao, Long> {

    @Query("""
        SELECT l FROM Localizacao l
        WHERE ABS(l.latitude  - :lat) * 111000 <= 15
          AND ABS(l.longitude - :lon) * 111000 <= 15
    """)
    List<Localizacao> findProximas(@Param("lat") double lat, @Param("lon") double lon);
}

// ── SOLICITACAO ──────────────────────────────────────────────
@Repository
interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

    Optional<Solicitacao> findByProtocolo(String protocolo);

    List<Solicitacao> findByCidadaoIdOrderByDataAberturaDesc(Long cidadaoId);

    @Query("""
        SELECT s FROM Solicitacao s
        WHERE s.status NOT IN (
            br.com.hackgov.model.StatusSolicitacao.CONCLUIDO,
            br.com.hackgov.model.StatusSolicitacao.CANCELADO
        )
        ORDER BY s.dataAbertura ASC
    """)
    List<Solicitacao> findTodasAbertas();

    @Query("""
        SELECT s FROM Solicitacao s
        WHERE s.localizacao.id IN (
            SELECT l.id FROM Localizacao l
            WHERE ABS(l.latitude  - :lat) * 111000 <= 10
              AND ABS(l.longitude - :lon) * 111000 <= 10
        )
        AND s.tipoServico.id = :tipoId
        AND s.status NOT IN (
            br.com.hackgov.model.StatusSolicitacao.CONCLUIDO,
            br.com.hackgov.model.StatusSolicitacao.CANCELADO
        )
    """)
    List<Solicitacao> findDuplicatas(@Param("lat") double lat,
                                     @Param("lon") double lon,
                                     @Param("tipoId") Long tipoId);

    // Para o mapa público
    @Query("""
        SELECT s FROM Solicitacao s
        ORDER BY s.dataAbertura DESC
    """)
    List<Solicitacao> findAllParaMapa();

    // Dashboard: contagem por status
    @Query("SELECT s.status, COUNT(s) FROM Solicitacao s GROUP BY s.status")
    List<Object[]> countByStatus();

    // Dashboard: contagem por tipo de serviço
    @Query("SELECT s.tipoServico.descricao, COUNT(s) FROM Solicitacao s GROUP BY s.tipoServico.descricao ORDER BY COUNT(s) DESC")
    List<Object[]> countByTipo();

    // Dashboard: tempo médio de resolução por tipo (em dias)
    @Query("""
        SELECT s.tipoServico.descricao,
               AVG(FUNCTION('DAYS_BETWEEN', s.dataAbertura, s.dataConclusao))
        FROM Solicitacao s
        WHERE s.status = br.com.hackgov.model.StatusSolicitacao.CONCLUIDO
          AND s.dataConclusao IS NOT NULL
        GROUP BY s.tipoServico.descricao
    """)
    List<Object[]> avgTempoResolucaoByTipo();
}

// ── HISTORICO_STATUS ─────────────────────────────────────────
@Repository
interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
    List<HistoricoStatus> findBySolicitacaoIdOrderByDataHoraAsc(Long solicitacaoId);
}

// ── AVALIACAO ────────────────────────────────────────────────
@Repository
interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    Optional<Avaliacao> findBySolicitacaoId(Long solicitacaoId);
    Double findAvgNota(); // para o dashboard
}
