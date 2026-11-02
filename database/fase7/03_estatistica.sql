-- ════════════════════════════════════════════════════════════════════════════
--  HackGov · Fase 7 · Parte 3 — Correlação e regressão em SQL (Oracle 19c+/21c XE)
--  Rodar depois de database/fase6/01_camada_analitica.sql (usa a view
--  VW_SOLICITACAO_ANALITICA). Mesmas definições do endpoint
--  GET /api/analise/estatistica (EstatisticaService.java):
--    atraso          = dias de resolução − SLA do tipo (negativo = antes do prazo)
--    semana          = TRUNC(data, 'IW') (segunda a domingo), só semanas completas
--  Funções estatísticas nativas do Oracle: CORR, REGR_SLOPE, REGR_INTERCEPT,
--  REGR_R2, REGR_COUNT, REGR_SXX, REGR_SYY e REGR_SXY.
--  Estatística t da inclinação: t = r·√((n−2)/(1−r²)); o valor-p vem da
--  distribuição t com n−2 graus de liberdade (calculado na aplicação).
-- ════════════════════════════════════════════════════════════════════════════

-- E1 · Atraso em relação ao SLA × nota do cidadão (por chamado avaliado)
SELECT REGR_COUNT(nota, atraso)                                    AS n,
       ROUND(CORR(nota, atraso), 4)                                AS r,
       ROUND(REGR_SLOPE(nota, atraso), 4)                          AS inclinacao,
       ROUND(REGR_INTERCEPT(nota, atraso), 4)                      AS intercepto,
       ROUND(REGR_R2(nota, atraso), 4)                             AS r2,
       ROUND(CORR(nota, atraso) * SQRT((REGR_COUNT(nota, atraso) - 2)
             / (1 - POWER(CORR(nota, atraso), 2))), 2)             AS t
FROM (SELECT v.nota, v.dias_resolucao - v.sla_dias AS atraso
      FROM vw_solicitacao_analitica v
      WHERE v.nota IS NOT NULL AND v.dias_resolucao IS NOT NULL);

-- E1b · Nota média por faixa de atraso (a mesma relação, sem regressão)
SELECT CASE WHEN atraso <= 0  THEN '1. no prazo'
            WHEN atraso <= 5  THEN '2. até 5 dias de atraso'
            WHEN atraso <= 10 THEN '3. 6 a 10 dias'
            ELSE                   '4. mais de 10 dias' END        AS faixa,
       COUNT(*)                                                    AS chamados,
       ROUND(AVG(nota), 2)                                         AS nota_media
FROM (SELECT v.nota, v.dias_resolucao - v.sla_dias AS atraso
      FROM vw_solicitacao_analitica v
      WHERE v.nota IS NOT NULL AND v.dias_resolucao IS NOT NULL)
GROUP BY CASE WHEN atraso <= 0  THEN '1. no prazo'
              WHEN atraso <= 5  THEN '2. até 5 dias de atraso'
              WHEN atraso <= 10 THEN '3. 6 a 10 dias'
              ELSE                   '4. mais de 10 dias' END
ORDER BY faixa;

-- E2 · Bairros: tempo médio de resolução × nota média (bairros com 5+ avaliações)
WITH bairros AS (
    SELECT bairro, AVG(dias_resolucao) AS tempo_medio, AVG(nota) AS nota_media, COUNT(nota) AS avaliacoes
    FROM vw_solicitacao_analitica
    GROUP BY bairro
    HAVING COUNT(nota) >= 5
)
SELECT REGR_COUNT(nota_media, tempo_medio)                         AS n,
       ROUND(CORR(nota_media, tempo_medio), 4)                     AS r,
       ROUND(REGR_SLOPE(nota_media, tempo_medio), 4)               AS inclinacao,
       ROUND(REGR_INTERCEPT(nota_media, tempo_medio), 4)           AS intercepto,
       ROUND(REGR_R2(nota_media, tempo_medio), 4)                  AS r2
FROM bairros;

-- E2b · Resíduo por bairro: quem avalia melhor ou pior do que o tempo explicaria
WITH bairros AS (
    SELECT bairro, AVG(dias_resolucao) AS tempo_medio, AVG(nota) AS nota_media
    FROM vw_solicitacao_analitica
    GROUP BY bairro
    HAVING COUNT(nota) >= 5
), reta AS (
    SELECT REGR_SLOPE(nota_media, tempo_medio) AS b1, REGR_INTERCEPT(nota_media, tempo_medio) AS b0 FROM bairros
)
SELECT b.bairro,
       ROUND(b.tempo_medio, 1)                                     AS tempo_medio,
       ROUND(b.nota_media, 2)                                      AS nota_media,
       ROUND(r.b0 + r.b1 * b.tempo_medio, 2)                       AS nota_prevista,
       ROUND(b.nota_media - (r.b0 + r.b1 * b.tempo_medio), 2)      AS residuo
FROM bairros b CROSS JOIN reta r
ORDER BY b.tempo_medio DESC;

-- E3 · Volume da semana × tempo médio de resolução
--      (só semanas completas e com 30+ dias de maturação: nas recentes, só os
--       chamados rápidos já concluíram, o que puxaria o tempo médio para baixo)
WITH limites AS (
    SELECT MIN(dia_abertura) AS inicio, MAX(dia_abertura) - 30 AS fim FROM vw_solicitacao_analitica
), semanas AS (
    SELECT TRUNC(v.dia_abertura, 'IW') AS semana, COUNT(*) AS volume, AVG(v.dias_resolucao) AS tempo_medio
    FROM vw_solicitacao_analitica v CROSS JOIN limites l
    WHERE TRUNC(v.dia_abertura, 'IW') >= l.inicio AND TRUNC(v.dia_abertura, 'IW') + 6 <= l.fim
    GROUP BY TRUNC(v.dia_abertura, 'IW')
)
SELECT REGR_COUNT(tempo_medio, volume)                             AS n,
       ROUND(CORR(tempo_medio, volume), 4)                         AS r,
       ROUND(REGR_SLOPE(tempo_medio, volume), 4)                   AS inclinacao,
       ROUND(REGR_INTERCEPT(tempo_medio, volume), 4)               AS intercepto,
       ROUND(REGR_R2(tempo_medio, volume), 4)                      AS r2
FROM semanas;

-- E4 · Tendência do volume semanal e projeção das 4 semanas seguintes
WITH limites AS (
    SELECT MIN(dia_abertura) AS inicio, MAX(dia_abertura) AS fim FROM vw_solicitacao_analitica
), semanas AS (
    SELECT TRUNC(v.dia_abertura, 'IW') AS semana, COUNT(*) AS volume
    FROM vw_solicitacao_analitica v CROSS JOIN limites l
    WHERE TRUNC(v.dia_abertura, 'IW') >= l.inicio AND TRUNC(v.dia_abertura, 'IW') + 6 <= l.fim
    GROUP BY TRUNC(v.dia_abertura, 'IW')
), serie AS (
    SELECT semana, volume, ROW_NUMBER() OVER (ORDER BY semana) AS x FROM semanas
), reta AS (
    SELECT REGR_SLOPE(volume, x) AS b1, REGR_INTERCEPT(volume, x) AS b0, REGR_R2(volume, x) AS r2,
           CORR(volume, x) AS r, COUNT(*) AS n, MAX(x) AS ultimo_x, MAX(semana) AS ultima_semana
    FROM serie
)
SELECT TO_CHAR(r.ultima_semana + 7 * k.passo, 'DD/MM/YYYY')        AS semana,
       ROUND(r.b0 + r.b1 * (r.ultimo_x + k.passo), 1)              AS volume_previsto,
       ROUND(r.b1, 3)                                              AS crescimento_semanal,
       ROUND(r.r2, 4)                                              AS r2
FROM reta r
CROSS JOIN (SELECT LEVEL AS passo FROM dual CONNECT BY LEVEL <= 4) k
ORDER BY k.passo;
