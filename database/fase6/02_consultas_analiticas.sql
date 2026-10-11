-- ════════════════════════════════════════════════════════════════════════════
--  HackGov · Fase 6 · Parte 4 — Consultas analíticas (Oracle 19c+/21c XE)
--  Rodar depois de 01_camada_analitica.sql. Data de referência: 30/09/2026.
--  Recursos usados: GROUP BY, agregações, CASE, CTE (WITH), subconsultas
--  (escalar e em FROM), funções de janela (LAG, SUM/AVG OVER, RANK),
--  PERCENTILE_CONT, ROLLUP/GROUPING, NULLIF/COALESCE e FETCH FIRST.
-- ════════════════════════════════════════════════════════════════════════════

-- Q0 · Impacto do bug no KPI "SLA cumprido" (fórmula da Fase 5 × corrigida)
-- CAST(TIMESTAMP AS DATE) no Oracle mantém a hora; DATA_PREVISAO é DATE à
-- meia-noite. Assim, um chamado concluído às 14h do próprio dia do prazo
-- caía como "atrasado". Correção: data_conclusao < data_previsao + 1.
SELECT COUNT(*)                                                         AS concluidas,
       ROUND(100.0 * SUM(CASE WHEN CAST(s.data_conclusao AS DATE) <= s.data_previsao
                              THEN 1 ELSE 0 END) / COUNT(*), 1)          AS pct_sla_formula_fase5,
       ROUND(100.0 * SUM(CASE WHEN s.data_conclusao < s.data_previsao + 1
                              THEN 1 ELSE 0 END) / COUNT(*), 1)          AS pct_sla_corrigido,
       SUM(CASE WHEN TRUNC(CAST(s.data_conclusao AS DATE)) = s.data_previsao
                THEN 1 ELSE 0 END)                                       AS concluidas_no_dia_do_prazo
FROM solicitacao s
WHERE s.status = 'CONCLUIDO'
  AND s.data_conclusao IS NOT NULL
  AND s.data_previsao IS NOT NULL;

-- Q1 · Evolução mensal: volume, variação sobre o mês anterior (LAG),
--      acumulado (SUM OVER) e média móvel de 3 meses (AVG OVER)
WITH mensal AS (
    SELECT mes_abertura                                       AS mes,
           COUNT(*)                                           AS total,
           SUM(CASE WHEN status = 'CONCLUIDO' THEN 1 ELSE 0 END) AS concluidas
    FROM vw_solicitacao_analitica
    GROUP BY mes_abertura
)
SELECT mes,
       total,
       concluidas,
       ROUND(100.0 * (total - LAG(total) OVER (ORDER BY mes))
                   / LAG(total) OVER (ORDER BY mes), 1)                         AS variacao_pct,
       SUM(total) OVER (ORDER BY mes ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS acumulado,
       ROUND(AVG(total) OVER (ORDER BY mes ROWS BETWEEN 2 PRECEDING AND CURRENT ROW), 1) AS media_movel_3m
FROM mensal
ORDER BY mes;

-- Q2 · Desempenho por tipo de serviço (chamados concluídos): média, mediana
--      e percentil 90 do tempo de resolução, % dentro do SLA e ranking do pior SLA
SELECT tipo,
       sla_dias,
       COUNT(*)                                                                 AS concluidas,
       ROUND(AVG(dias_resolucao), 1)                                            AS media_dias,
       CAST(PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY dias_resolucao) AS NUMERIC(6,1)) AS mediana_dias,
       CAST(PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY dias_resolucao) AS NUMERIC(6,1)) AS p90_dias,
       ROUND(100.0 * SUM(dentro_sla) / COUNT(*), 1)                             AS pct_dentro_sla,
       RANK() OVER (ORDER BY 1.0 * SUM(dentro_sla) / COUNT(*))                  AS ranking_pior_sla
FROM vw_solicitacao_analitica
WHERE status = 'CONCLUIDO'
GROUP BY tipo, sla_dias
ORDER BY ranking_pior_sla;

-- Q3 · Equidade territorial: tempo médio de resolução por bairro comparado à
--      média da cidade (subconsulta escalar dentro de uma subconsulta em FROM)
SELECT bairro,
       concluidas,
       media_dias,
       ROUND(media_dias - media_cidade, 1)             AS dif_media_cidade,
       pct_dentro_sla,
       RANK() OVER (ORDER BY media_dias DESC)          AS posicao
FROM (
    SELECT bairro,
           COUNT(*)                                     AS concluidas,
           ROUND(AVG(dias_resolucao), 1)                AS media_dias,
           ROUND(100.0 * SUM(dentro_sla) / COUNT(*), 1) AS pct_dentro_sla,
           (SELECT AVG(dias_resolucao)
              FROM vw_solicitacao_analitica
             WHERE status = 'CONCLUIDO')                AS media_cidade
    FROM vw_solicitacao_analitica
    WHERE status = 'CONCLUIDO'
    GROUP BY bairro
) por_bairro
ORDER BY posicao;

-- Q4 · Backlog em 30/09/2026 por faixa de atraso, com subtotal geral (ROLLUP)
SELECT CASE WHEN GROUPING(tipo) = 1 THEN 'TOTAL' ELSE tipo END                AS tipo,
       COUNT(*)                                                                AS abertas,
       SUM(CASE WHEN data_previsao >= DATE '2026-09-30' THEN 1 ELSE 0 END)     AS no_prazo,
       SUM(CASE WHEN DATE '2026-09-30' - data_previsao BETWEEN 1 AND 7
                THEN 1 ELSE 0 END)                                             AS atraso_ate_7d,
       SUM(CASE WHEN DATE '2026-09-30' - data_previsao > 7 THEN 1 ELSE 0 END)  AS atraso_mais_7d,
       MAX(DATE '2026-09-30' - dia_abertura)                                   AS mais_antiga_dias
FROM vw_solicitacao_analitica
WHERE aberto = 1
GROUP BY ROLLUP (tipo)
ORDER BY GROUPING(tipo), abertas DESC;

-- Q5 · IA de triagem: o tipo escolhido pelo cidadão concorda com a sugestão da
--      IA? E o que acontece com o prazo quando diverge? (CTE + janela)
WITH base AS (
    SELECT v.*,
           CASE WHEN tipo_ia_codigo IS NULL OR confianca_ia < 0.6 THEN 'IA sem sugestão'
                WHEN tipo_ia_codigo = tipo_codigo              THEN 'Concorda'
                ELSE 'Diverge'
           END AS situacao_ia
    FROM vw_solicitacao_analitica v
)
SELECT situacao_ia,
       COUNT(*)                                                                    AS solicitacoes,
       ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 1)                          AS pct_do_total,
       CAST(AVG(confianca_ia) AS NUMERIC(5,3))                                     AS confianca_media,
       ROUND(AVG(CASE WHEN status = 'CONCLUIDO' THEN dias_resolucao END), 1)       AS media_dias_resolucao,
       ROUND(100.0 * SUM(dentro_sla)
             / NULLIF(SUM(CASE WHEN status = 'CONCLUIDO' THEN 1 ELSE 0 END), 0), 1) AS pct_dentro_sla
FROM base
GROUP BY situacao_ia
ORDER BY solicitacoes DESC;

-- Q5b · Onde estão as divergências: tipo escolhido × tipo sugerido (top 5)
SELECT tipo_codigo     AS escolhido_pelo_cidadao,
       tipo_ia_codigo  AS sugerido_pela_ia,
       COUNT(*)        AS solicitacoes
FROM vw_solicitacao_analitica
WHERE tipo_ia_codigo <> tipo_codigo
  AND confianca_ia >= 0.6
GROUP BY tipo_codigo, tipo_ia_codigo
ORDER BY solicitacoes DESC
FETCH FIRST 5 ROWS ONLY;

-- Q6 · Reincidência por célula de grade (~100 m): mesma categoria repetida no
--      mesmo "quadrado" de coordenadas. Simples em SQL, mas parte um ponto
--      crítico em dois quando ele cai na borda da célula — motivação para o
--      agrupamento por KD-Tree + grafo da Parte 4.2.
SELECT tipo,
       bairro,
       CAST(latitude AS NUMERIC(9,3))   AS lat_celula,
       CAST(longitude AS NUMERIC(9,3))  AS lon_celula,
       COUNT(*)                         AS ocorrencias,
       MIN(dia_abertura)                AS primeira,
       MAX(dia_abertura)                AS ultima
FROM vw_solicitacao_analitica
GROUP BY tipo, bairro, CAST(latitude AS NUMERIC(9,3)), CAST(longitude AS NUMERIC(9,3))
HAVING COUNT(*) >= 4
ORDER BY ocorrencias DESC, tipo
FETCH FIRST 8 ROWS ONLY;
