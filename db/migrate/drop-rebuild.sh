#!/bin/bash

PROJECT_DIR=/home/cristian/git/epas

DB_NAME=epas
DB_USER=epas

cd $PROJECT_DIR

play stop

#play db:export --drop --output=$PROJECT_DIR/db/migrate/drop.ddl
#echo "Creato il file drop.dll contenente le info per il drop delle sequenze e tabelle del database"

play db:export --create --output=$PROJECT_DIR/db/migrate/create.ddl
echo "Creato il file drop.dll contenente le info per le craete delle tabelle del database"

dropdb -U $DB_USER $DB_NAME
echo "Cancellato il database $DB_NAME"

createdb -U $DB_USER $DB_NAME
echo "Creato il nuovo database $DB_NAME"

#psql -U $DB_USER $DB_NAME < db/migrate/drop.ddl
#echo "Eseguite le drop sul database $DB_NAME"

psql -U $DB_USER $DB_NAME < db/migrate/create.ddl
echo "Eseguite le create sul database $DB_NAME"

play start
