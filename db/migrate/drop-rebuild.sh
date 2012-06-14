#!/bin/bash

CURRENT_DIR=`dirname "$0"`
PROJECT_DIR=$CURRENT_DIR/../../

DB_NAME=epas
DB_USER=epas

function log {
  echo "`date`: $1"
} 

cd $PROJECT_DIR

play stop
log "Stoppato il play"

git pull origin master
log "Eseguita la git pull"

play deps
log "Aggiornate le dipendenze"

#play db:export --drop --output=$PROJECT_DIR/db/migrate/drop.ddl
#echo "Creato il file drop.dll contenente le info per il drop delle sequenze e tabelle del database"

play db:export --create --output=$PROJECT_DIR/db/migrate/create.ddl
log "Creato il file drop.dll contenente le info per le craete delle tabelle del database"

dropdb -U $DB_USER $DB_NAME
log "Cancellato il database $DB_NAME"

createdb -U $DB_USER $DB_NAME
log "Creato il nuovo database $DB_NAME"

#psql -U $DB_USER $DB_NAME < db/migrate/drop.ddl
#echo "Eseguite le drop sul database $DB_NAME"

psql -U $DB_USER $DB_NAME < db/migrate/create.ddl
log "Eseguite le create sul database $DB_NAME"

play start

