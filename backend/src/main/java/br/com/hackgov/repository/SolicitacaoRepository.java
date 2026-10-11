package br.com.hackgov.repository;

import br.com.hackgov.model.Solicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoRepository extends JpaRepository<Solicitacao, Long> {

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

    // Busca por proximidade (Fase 6): caixa de coordenadas com BETWEEN — predicado
    // "sargável", que aproveita o índice IX_LOC_LAT_LON. A distância exata (Haversine)
    // é conferida depois, em Java (ver br.com.hackgov.algoritmos.Geo).
    @Query("""
        SELECT s FROM Solicitacao s
        WHERE s.tipoServico.id = :tipoId
          AND s.status NOT IN (
              br.com.hackgov.model.StatusSolicitacao.CONCLUIDO,
              br.com.hackgov.model.StatusSolicitacao.CANCELADO
          )
          AND s.localizacao.latitude  BETWEEN :latMin AND :latMax
          AND s.localizacao.longitude BETWEEN :lonMin AND :lonMax
    """)
    List<Solicitacao> findAbertasNaCaixa(@Param("tipoId") Long tipoId,
                                         @Param("latMin") double latMin,
                                         @Param("latMax") double latMax,
                                         @Param("lonMin") double lonMin,
                                         @Param("lonMax") double lonMax);

    @Query("SELECT s FROM Solicitacao s ORDER BY s.dataAbertura DESC")
    List<Solicitacao> findAllParaMapa();

    @Query("SELECT s.status, COUNT(s) FROM Solicitacao s GROUP BY s.status")
    List<Object[]> countByStatus();

    @Query("SELECT s.tipoServico.descricao, COUNT(s) FROM Solicitacao s GROUP BY s.tipoServico.descricao ORDER BY COUNT(s) DESC")
    List<Object[]> countByTipo();

    @Query(value = """
        SELECT ts.descricao,
               AVG(CAST(s.data_conclusao AS DATE) - CAST(s.data_abertura AS DATE))
        FROM solicitacao s
        JOIN tipo_servico ts ON s.id_tipo = ts.id_tipo
        WHERE s.status = 'CONCLUIDO'
          AND s.data_conclusao IS NOT NULL
        GROUP BY ts.descricao
        """, nativeQuery = true)
    List<Object[]> avgTempoResolucaoByTipo();

    // Linha única: [quantidade concluída dentro do SLA, total concluído]. Base do
    // KPI "% SLA cumprido" no dashboard.
    // Correção da Fase 6: no Oracle, CAST(TIMESTAMP AS DATE) mantém a hora e
    // DATA_PREVISAO é uma DATE à meia-noite — um chamado concluído às 14h do
    // próprio dia do prazo era contado como atrasado. "< previsão + 1" compara
    // com o fim do dia do prazo (na massa demo o KPI subiu de 56,0% para 64,7%).
    @Query(value = """
        SELECT
            SUM(CASE WHEN s.data_conclusao < s.data_previsao + 1 THEN 1 ELSE 0 END),
            COUNT(*)
        FROM solicitacao s
        WHERE s.status = 'CONCLUIDO'
          AND s.data_conclusao IS NOT NULL
          AND s.data_previsao IS NOT NULL
        """, nativeQuery = true)
    List<Object[]> slaCumprimento();

    // Top 5 bairros por volume de solicitações — insumo do ranking exibido no dashboard.
    @Query(value = """
        SELECT b.nome_bairro, COUNT(*) AS qtd
        FROM solicitacao s
        JOIN localizacao l ON s.id_localizacao = l.id_localizacao
        JOIN bairro b      ON l.id_bairro      = b.id_bairro
        GROUP BY b.nome_bairro
        ORDER BY COUNT(*) DESC
        FETCH FIRST 5 ROWS ONLY
        """, nativeQuery = true)
    List<Object[]> topBairros();
}
