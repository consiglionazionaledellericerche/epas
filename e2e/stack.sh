#!/bin/bash
#
# Avvia o ferma lo stack usato dai test end-to-end: un postgres vuoto e
# l'immagine docker di ePAS, raggiungibile su http://localhost:$E2E_PORT.
#
#   docker build -t epas:e2e . && e2e/stack.sh up epas:e2e
#   (cd e2e && npm ci && npx playwright install chromium && npx playwright test)
#   e2e/stack.sh down
#
# Variabili opzionali:
#   E2E_PORT        porta locale di ePAS (default 9000)
#   POSTGRES_IMAGE  immagine di postgres (default postgres:12, come nel docker-compose)
#   E2E_TIMEOUT     secondi di attesa per l'avvio di ePAS (default 300)
#   CSRF_MODE       verifica anti-CSRF di ePAS (default enforce: i test sul token
#                   si aspettano che le richieste senza token siano rifiutate)

set -euo pipefail

E2E_PORT=${E2E_PORT:-9000}
POSTGRES_IMAGE=${POSTGRES_IMAGE:-postgres:12}
E2E_TIMEOUT=${E2E_TIMEOUT:-300}
CSRF_MODE=${CSRF_MODE:-enforce}

NETWORK=epas-e2e
PG=epas-e2e-pg
APP=epas-e2e-app
DB_PASS=e2e-test

log() { echo "[e2e] $*"; }

down() {
  docker rm -f "$APP" "$PG" >/dev/null 2>&1 || true
  docker network rm "$NETWORK" >/dev/null 2>&1 || true
}

fail() {
  echo "[e2e] ERRORE: $*" >&2
  docker logs "$APP" 2>&1 | tail -n 200 >&2 || true
  exit 1
}

up() {
  local image=${1:?Uso: $0 up <immagine ePAS>}
  down
  docker network create "$NETWORK" >/dev/null

  log "avvio di $POSTGRES_IMAGE"
  docker run -d --name "$PG" --network "$NETWORK" --network-alias postgres \
    -e POSTGRES_PASSWORD="$DB_PASS" -e POSTGRES_DB=epas "$POSTGRES_IMAGE" >/dev/null
  # -h localhost: la connessione TCP. Sul socket unix risponde anche il server
  # temporaneo dell'inizializzazione, che poi viene fermato e riavviato.
  for _ in $(seq 1 60); do
    docker exec "$PG" pg_isready -h localhost -U postgres -q 2>/dev/null && break
    sleep 2
  done
  docker exec "$PG" pg_isready -h localhost -U postgres -q || fail "postgres non risponde"

  # SESSION_SECURE=true: i cookie hanno l'attributo Secure come dietro https. I
  # browser trattano localhost come contesto sicuro e li accettano anche in http.
  # JOBS_ACTIVE=true serve al job di bootstrap, che crea l'utente admin e la sede
  # iniziale su un database vuoto. CSRF_MODE=enforce: le richieste senza token
  # anti-CSRF sono rifiutate, così i test verificano che il javascript lo invii.
  log "avvio di $image su http://localhost:$E2E_PORT"
  docker run -d --name "$APP" --network "$NETWORK" -p "127.0.0.1:$E2E_PORT:9000" \
    -e VIRTUAL_HOST=localhost \
    -e DB_PASS="$DB_PASS" \
    -e APPLICATION_SECRET="$(head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')" \
    -e SESSION_SECURE=true \
    -e JOBS_ACTIVE=true \
    -e SKIP_IP_CHECK=true \
    -e CSRF_MODE="$CSRF_MODE" \
    "$image" >/dev/null

  log "attesa dell'avvio di ePAS (massimo ${E2E_TIMEOUT}s)"
  for _ in $(seq 1 $((E2E_TIMEOUT / 5))); do
    [ "$(docker inspect -f '{{.State.Running}}' "$APP" 2>/dev/null)" = "true" ] \
      || fail "il container ePAS si e' fermato durante l'avvio"
    if [ "$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$E2E_PORT/login")" = "200" ]; then
      log "OK /login risponde 200"
      return 0
    fi
    sleep 5
  done
  fail "/login non risponde 200 entro ${E2E_TIMEOUT}s"
}

case "${1:-}" in
  up) up "${2:-}" ;;
  down) down ;;
  logs) docker logs "$APP" ;;
  *) echo "Uso: $0 up <immagine ePAS> | down | logs" >&2; exit 2 ;;
esac
