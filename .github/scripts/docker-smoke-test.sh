#!/bin/bash
#
# Smoke test dell'immagine docker di ePAS: avvia l'immagine indicata con un
# postgres vuoto e verifica che l'applicazione parta e che i servizi del
# container funzionino. Usato dai workflow di pubblicazione prima del push,
# si puo' lanciare anche in locale:
#
#   docker build -t epas:test . && .github/scripts/docker-smoke-test.sh epas:test
#
# Variabili opzionali:
#   POSTGRES_IMAGE  immagine di postgres (default postgres:12, come nel docker-compose)
#   SMOKE_TIMEOUT   secondi di attesa per l'avvio di ePAS (default 300)

set -euo pipefail
# Nelle pipeline si usa "grep ... >/dev/null" e non "grep -q": con pipefail,
# grep -q esce alla prima corrispondenza e il comando a monte fallisce per SIGPIPE.

IMAGE=${1:?Uso: $0 <immagine ePAS>}
POSTGRES_IMAGE=${POSTGRES_IMAGE:-postgres:12}
SMOKE_TIMEOUT=${SMOKE_TIMEOUT:-300}

NAME=epas-smoke-$$
NETWORK=$NAME
PG=$NAME-pg
APP=$NAME-app
# La password serve a verificare il file .pgpass usato dal backup. Non puo'
# contenere ':' o '@' perche' ePAS la inserisce nell'url del database.
DB_PASS=smoke-test

log() { echo "[smoke-test] $*"; }

fail() {
  echo "[smoke-test] ERRORE: $*" >&2
  echo "[smoke-test] ---- log del container ePAS ----" >&2
  docker logs "$APP" 2>&1 | tail -n 200 >&2 || true
  exit 1
}

cleanup() {
  docker rm -f "$APP" >/dev/null 2>&1 || true
  docker stop "$PG" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker network create "$NETWORK" >/dev/null

log "avvio di $POSTGRES_IMAGE"
docker run --rm -d --name "$PG" --network "$NETWORK" --network-alias postgres \
  -e POSTGRES_PASSWORD="$DB_PASS" -e POSTGRES_DB=epas "$POSTGRES_IMAGE" >/dev/null
for _ in $(seq 1 60); do
  docker exec "$PG" pg_isready -U postgres -q 2>/dev/null && break
  sleep 2
done
docker exec "$PG" pg_isready -U postgres -q || fail "postgres non risponde"

log "avvio di $IMAGE"
# senza --rm, per poter leggere i log se il container si ferma
docker run -d --name "$APP" --network "$NETWORK" \
  -e VIRTUAL_HOST=localhost \
  -e DB_PASS="$DB_PASS" \
  -e APPLICATION_SECRET="$(head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')" \
  -e BACKUP_CRON="0 1 * * *" \
  "$IMAGE" >/dev/null

log "attesa dell'avvio di ePAS (massimo ${SMOKE_TIMEOUT}s)"
started=false
for _ in $(seq 1 $((SMOKE_TIMEOUT / 5))); do
  [ "$(docker inspect -f '{{.State.Running}}' "$APP" 2>/dev/null)" = "true" ] \
    || fail "il container ePAS si e' fermato durante l'avvio"
  if docker exec "$APP" wget -q -S --max-redirect=0 -O /dev/null http://localhost:9000/login 2>&1 \
      | grep 'HTTP/[0-9.]* 200' >/dev/null; then
    started=true
    break
  fi
  sleep 5
done
$started || fail "/login non risponde 200 entro ${SMOKE_TIMEOUT}s"
log "OK /login risponde 200"

docker logs "$APP" 2>&1 | grep "Evolutions script successfully applied" >/dev/null \
  || fail "le evolutions non risultano applicate"
log "OK evolutions applicate"

docker exec "$APP" pgrep -x cron >/dev/null || fail "cron non e' attivo"
docker exec "$APP" crontab -l | grep backupdb.sh >/dev/null || fail "il backup non e' nel crontab"
log "OK cron attivo con il backup"

docker exec "$APP" /home/epas/epas/tools/backupdb.sh \
  || fail "backupdb.sh fallito: $(docker exec "$APP" sh -c 'cat /home/epas/epas/backups/*-dump.log')"
docker exec "$APP" sh -c 'zcat /home/epas/epas/backups/localhost-*.sql.gz | grep "CREATE TABLE" >/dev/null' \
  || fail "il backup del database e' vuoto o non valido"
log "OK backup del database"

docker exec "$APP" java -version 2>&1 | grep 'version "11\.' >/dev/null || fail "java 11 non trovato"
docker exec "$APP" play version 2>/dev/null | grep '^1\.5\.3' >/dev/null || fail "play 1.5.3 non trovato"
log "OK java 11 e play 1.5.3"

log "tutti i controlli sono passati"
