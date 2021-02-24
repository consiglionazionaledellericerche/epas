#!/bin/bash

# ePAS for Linux installation script
#
# See https://github.com/consiglionazionaledellericerce/epas for more details.
#
# This script is meant for quick & easy install via:
#   $ curl -fsSL https://raw.githubusercontent.com/consiglionazionaledellericerche/epas/master/epas-first-setup.sh -o epas-first-setup.sh && sh epas-first-setup.sh

# NOTE: Make sure to verify the contents of the script
#       you downloaded matches the contents of epas-first-setup.sh
#       located at https://github.com/consiglionazionaledellericerche/epas/epas-first-setup.sh
#
# This script need docker and docker-compose to be installed to completed successfully.

INSTALL_DIR=${INSTALL_DIR:-.}

echo "ePAS installation script."
echo "Before running this script install docker, docker compose and add current user to docker group." 
read -p "Press enter to continue, Ctrl+C to abort" ready
command -v docker -v >/dev/null 2>&1 || { echo >&2 "Docker not found.  Aborting."; exit 1; }
command -v docker-compose -v >/dev/null 2>&1 || { echo >&2 "Docker-compose not found.  Aborting."; exit 1; }

mkdir -p $INSTALL_DIR/epas/attachments $INSTALL_DIR/epas/logs $INSTALL_DIR/postgres/data
cd $INSTALL_DIR

#curl https://raw.githubusercontent.com/consiglionazionaledellericerche/epas/main/docker-compose.yml -o docker-compose.yml
#curl https://raw.githubusercontent.com/consiglionazionaledellericerche/epas/main/.env -o .env

# Avvio del postgres e creazione del DB vuoto epas
docker-compose up -d postgres
# Attesa che il container docker sia pronto
sleep 10
docker-compose exec postgres createdb -U postgres epas

# Avvio di ePAS che si occuperà anche di popolare il db 
docker-compose up -d
