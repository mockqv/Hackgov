-- ════════════════════════════════════════════════════════════════════════════
--  HackGov · Fase 6 · Parte 4 — Camada analítica (Oracle 19c+/21c XE)
--  Richard Vinicius Ferreira da Silva · RM565628 · 2SIOA
--
--  O que este script faz (pode ser executado mais de uma vez):
--   1. cria índices B-tree para os padrões de consulta do sistema;
--   2. cria a view VW_SOLICITACAO_ANALITICA, que concentra as junções e as
--      colunas derivadas (dias de resolução, dentro do SLA, mês) usadas
--      pelas consultas do arquivo 02_consultas_analiticas.sql.
--
--  Pré-requisito: o backend já ter subido ao menos uma vez (o Hibernate cria
--  as tabelas e as colunas novas ID_TIPO_SUGERIDO_IA e CONFIANCA_IA). Para ter
--  dados, suba com SPRING_PROFILES_ACTIVE=dev,demo.
--
--  Privilégio: criar a view exige CREATE VIEW, que os papéis CONNECT/RESOURCE
--  não incluem. Se aparecer ORA-01031, rode como SYSTEM no XEPDB1:
--      GRANT CREATE VIEW TO hackgov;
-- ════════════════════════════════════════════════════════════════════════════

-- ── 1. Índices ───────────────────────────────────────────────────────────────
-- Um índice B-tree é uma árvore balanceada: localizar uma chave custa O(log n)
-- em vez da varredura completa O(n). O Oracle NÃO cria índice automaticamente
-- em chaves estrangeiras, só em PK/UNIQUE — por isso as FKs mais consultadas
-- ganham índice aqui. O bloco PL/SQL ignora "índice já existe" (ORA-00955) e
-- "colunas já indexadas" (ORA-01408), tornando o script idempotente.
DECLARE
    PROCEDURE criar(p_ddl VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_ddl;
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE NOT IN (-955, -1408) THEN RAISE; END IF;
    END;
BEGIN
    -- Fila de atendimento / painel: WHERE status NOT IN (...) ORDER BY data_abertura
    criar('CREATE INDEX IX_SOLIC_STATUS_ABERTURA ON SOLICITACAO (STATUS, DATA_ABERTURA)');
    -- Junções e agregações por tipo de serviço (dashboard, consultas por tipo)
    criar('CREATE INDEX IX_SOLIC_TIPO ON SOLICITACAO (ID_TIPO)');
    -- Linha do tempo e pilha de auditoria: WHERE id_solicitacao = ? ORDER BY data_hora
    criar('CREATE INDEX IX_HIST_SOLIC_DATA ON HISTORICO_STATUS (ID_SOLICITACAO, DATA_HORA)');
    -- Agregações por bairro (junção LOCALIZACAO → BAIRRO)
    criar('CREATE INDEX IX_LOC_BAIRRO ON LOCALIZACAO (ID_BAIRRO)');
    -- Busca por proximidade (caixa de coordenadas) usada na checagem de duplicidade:
    -- o predicado agora é "latitude BETWEEN :min AND :max", que aproveita o índice
    -- (a versão antiga, ABS(latitude - :lat) * 111000 <= 10, obrigava varredura completa).
    criar('CREATE INDEX IX_LOC_LAT_LON ON LOCALIZACAO (LATITUDE, LONGITUDE)');
END;
/

-- ── 2. View analítica ────────────────────────────────────────────────────────
-- Concentra as junções e padroniza as datas em DIA (TRUNC), evitando o erro de
-- comparar um TIMESTAMP com hora contra uma DATE à meia-noite — exatamente o
-- bug encontrado no KPI de SLA do dashboard (ver 02_consultas_analiticas.sql, Q0).
CREATE OR REPLACE VIEW VW_SOLICITACAO_ANALITICA AS
SELECT s.id_solicitacao,
       s.protocolo,
       s.status,
       ts.codigo                                        AS tipo_codigo,
       ts.descricao                                     AS tipo,
       ts.sla_dias,
       tia.codigo                                       AS tipo_ia_codigo,
       s.confianca_ia,
       b.nome_bairro                                    AS bairro,
       l.latitude,
       l.longitude,
       TRUNC(CAST(s.data_abertura AS DATE))             AS dia_abertura,
       TO_CHAR(s.data_abertura, 'YYYY-MM')              AS mes_abertura,
       TRUNC(CAST(s.data_conclusao AS DATE))            AS dia_conclusao,
       s.data_previsao,
       CASE WHEN s.status = 'CONCLUIDO'
            THEN TRUNC(CAST(s.data_conclusao AS DATE)) - TRUNC(CAST(s.data_abertura AS DATE))
       END                                              AS dias_resolucao,
       CASE WHEN s.status = 'CONCLUIDO' AND s.data_previsao IS NOT NULL
            THEN CASE WHEN TRUNC(CAST(s.data_conclusao AS DATE)) <= s.data_previsao THEN 1 ELSE 0 END
       END                                              AS dentro_sla,
       CASE WHEN s.status IN ('CONCLUIDO', 'CANCELADO') THEN 0 ELSE 1 END AS aberto,
       a.nota
FROM solicitacao s
JOIN tipo_servico ts       ON ts.id_tipo = s.id_tipo
JOIN localizacao l         ON l.id_localizacao = s.id_localizacao
JOIN bairro b              ON b.id_bairro = l.id_bairro
LEFT JOIN tipo_servico tia ON tia.id_tipo = s.id_tipo_sugerido_ia
LEFT JOIN avaliacao a      ON a.id_solicitacao = s.id_solicitacao;
