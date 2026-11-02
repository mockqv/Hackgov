#!/usr/bin/env bash
# HackGov · Fase 7 — restauração de um backup gerado por backup_oracle.sh
# Uso: ./scripts/restaurar_oracle.sh backups/hackgov_20261031_0200.dmp
# Confere o SHA-256 antes de importar: um arquivo alterado não é restaurado.
set -euo pipefail
CONTAINER="${CONTAINER:-oracle-hackgov}"
SENHA_SYSTEM="${ORACLE_PASSWORD:-hackgov123}"
DUMP="${1:?informe o arquivo .dmp}"

( cd "$(dirname "$DUMP")" && sha256sum -c "$(basename "$DUMP").sha256" ) \
    || { echo "✘ hash não confere: backup corrompido ou adulterado"; exit 1; }

docker exec "$CONTAINER" mkdir -p /opt/oracle/oradata/backup
docker cp "$DUMP" "$CONTAINER:/opt/oracle/oradata/backup/$(basename "$DUMP")"
# table_exists_action=replace: substitui as tabelas atuais pelas do backup
docker exec "$CONTAINER" impdp "system/${SENHA_SYSTEM}@XEPDB1" \
    schemas=HACKGOV directory=BACKUP_DIR dumpfile="$(basename "$DUMP")" \
    table_exists_action=replace logfile="restauracao_$(date +%Y%m%d_%H%M).log"
echo "✔ restaurado. Depois de restaurar, abra a tela Integridade: os dias ancorados"
echo "  devem continuar ÍNTEGROS — é a prova de que o backup trouxe a auditoria intacta."
