#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════════════
#  HackGov · Fase 7 — backup lógico do schema HACKGOV (Oracle Data Pump)
#
#  Uso (na raiz do projeto, com o docker-compose no ar):
#     ./scripts/backup_oracle.sh                # gera backups/hackgov_AAAAMMDD_HHMM.dmp
#     RETENCAO=14 ./scripts/backup_oracle.sh    # mantém os 14 mais recentes (padrão 7)
#
#  Política (Parte 1, item 2): diário, retenção de 7 cópias locais, hash
#  SHA-256 de cada arquivo para provar que o backup não foi corrompido nem
#  trocado, e uma cópia fora do servidor (regra 3-2-1: 3 cópias, 2 mídias,
#  1 fora do local). Agendar no cron do host, ex.: 0 2 * * * /opt/hackgov/scripts/backup_oracle.sh
# ════════════════════════════════════════════════════════════════════════
set -euo pipefail

CONTAINER="${CONTAINER:-oracle-hackgov}"
SENHA_SYSTEM="${ORACLE_PASSWORD:-hackgov123}"
SENHA_APP="${APP_USER_PASSWORD:-hackgov123}"
RETENCAO="${RETENCAO:-7}"
DESTINO="${DESTINO:-$(dirname "$0")/../backups}"
CARIMBO="$(date +%Y%m%d_%H%M)"
ARQUIVO="hackgov_${CARIMBO}.dmp"

mkdir -p "$DESTINO"
echo "▸ preparando diretório do Data Pump dentro do contêiner"
docker exec "$CONTAINER" mkdir -p /opt/oracle/oradata/backup
docker exec -i "$CONTAINER" sqlplus -s "system/${SENHA_SYSTEM}@XEPDB1" <<SQL
WHENEVER SQLERROR EXIT FAILURE
CREATE OR REPLACE DIRECTORY BACKUP_DIR AS '/opt/oracle/oradata/backup';
GRANT READ, WRITE ON DIRECTORY BACKUP_DIR TO hackgov;
EXIT
SQL

echo "▸ exportando o schema HACKGOV"
docker exec "$CONTAINER" expdp "hackgov/${SENHA_APP}@XEPDB1" \
    schemas=HACKGOV directory=BACKUP_DIR dumpfile="$ARQUIVO" logfile="hackgov_${CARIMBO}.log" \
    reuse_dumpfiles=yes > /dev/null

echo "▸ copiando para $DESTINO"
docker cp "$CONTAINER:/opt/oracle/oradata/backup/$ARQUIVO" "$DESTINO/$ARQUIVO"
docker exec "$CONTAINER" rm -f "/opt/oracle/oradata/backup/$ARQUIVO"
( cd "$DESTINO" && sha256sum "$ARQUIVO" > "$ARQUIVO.sha256" )

echo "▸ retenção: mantendo os $RETENCAO backups mais recentes"
ls -1t "$DESTINO"/hackgov_*.dmp 2>/dev/null | tail -n +"$((RETENCAO + 1))" | while read -r antigo; do
    rm -f "$antigo" "$antigo.sha256"
done

echo "✔ backup pronto: $DESTINO/$ARQUIVO ($(du -h "$DESTINO/$ARQUIVO" | cut -f1))"
echo "  conferir depois:  (cd $DESTINO && sha256sum -c $ARQUIVO.sha256)"
