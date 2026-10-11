# HackGov — Monorepo

Sistema de Zeladoria Urbana · FIAP 2SIOA · Richard V. F. Silva · RM565628

```
hackgov-monorepo/
├── backend/          → Spring Boot 3.2 + Java 17 + Oracle
├── frontend/         → React 18 + Vite + TailwindCSS
├── database/fase6/   → camada analítica (índices + view) e consultas SQL da Fase 6
├── docker/           → Dockerfiles
├── docker-compose.yml
└── README.md
```

## Subir tudo com um comando

```bash
# Configure o Oracle antes (ver abaixo), depois:
docker-compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

## Oracle local (pré-requisito)

O Oracle roda **fora** do Docker (instalado localmente).

```sql
-- Execute como SYSDBA:
CREATE USER hackgov IDENTIFIED BY hackgov123;
GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO hackgov;
-- Depois execute o script DDL da Fase 3
```

Ajuste `ORACLE_URL` no `docker-compose.yml` se necessário.

## Fase 6 — IA, análise de dados e estruturas de dados

### Massa de demonstração
O `docker-compose` sobe o backend com `SPRING_PROFILES_ACTIVE=dev,demo`:
- `dev` cria tipos de serviço, o bairro Centro e os usuários de teste;
- `demo` carrega ~1.850 solicitações sintéticas (abril–setembro/2026) com histórico de status,
  avaliações e a sugestão da IA. Geração determinística (semente fixa): um banco limpo recebe
  sempre as mesmas linhas, então as consultas reproduzem os números da documentação.
  Leva alguns segundos na primeira subida e não roda de novo depois.

Logins (senha `Hackgov@123`): `gestor@hackgov.local`, `servidor@hackgov.local`, `cidadao@hackgov.local`
e os usuários de demonstração (ex.: `ana.1@demo.hackgov.local`, servidor).

### O que entrou
| Recurso | Onde |
|---|---|
| IA de triagem (Naive Bayes, sem API externa) | `backend/.../ia/`, `POST /api/ia/classificar`, `GET /api/ia/modelo` |
| Sugestão da IA no formulário + aviso de chamados próximos | `frontend/src/pages/cidadao/NovaSolicitacao.jsx` |
| Agrupamento espacial (KD-Tree + Union-Find) | `backend/.../algoritmos/`, `GET /api/analise/agrupamentos` |
| Painel usando a fila de prioridade do backend + trilha de auditoria (pilha) | `frontend/src/pages/servidor/Painel.jsx` |
| Tela Inteligência (métricas da IA + pontos críticos no mapa) | `frontend/src/pages/servidor/Inteligencia.jsx` |
| Seletor de bairro (`GET /api/bairros`) | formulário de nova solicitação |
| Upload real de fotos (`POST /api/arquivos`) | `ArquivoService`, `PhotoUpload.jsx` |
| Correção do KPI "SLA cumprido" e busca de duplicidade indexável | `SolicitacaoRepository` |
| Camada analítica e consultas SQL | `database/fase6/01_camada_analitica.sql`, `02_consultas_analiticas.sql` |

### SQL (Oracle)
Depois de subir o backend uma vez (o Hibernate cria as colunas novas), rode no SQL Developer, como `hackgov`:
1. `database/fase6/01_camada_analitica.sql` — índices + view `VW_SOLICITACAO_ANALITICA` (idempotente);
2. `database/fase6/02_consultas_analiticas.sql` — consultas Q0 a Q6.

### Testes e benchmark
```bash
cd backend
mvn test                      # 30 testes: IA, KD-Tree, Union-Find, agrupamento, gerador da massa demo
mvn test-compile && java -cp target/classes:target/test-classes br.com.hackgov.algoritmos.BenchmarkAgrupamento
```
