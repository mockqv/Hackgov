# HackGov — Monorepo

Sistema de Zeladoria Urbana · FIAP 2SIOA · Richard V. F. Silva · RM565628

```
hackgov-monorepo/
├── backend/          → Spring Boot 3.2 + Java 17 + Oracle
├── frontend/         → React 18 + Vite + TailwindCSS
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
