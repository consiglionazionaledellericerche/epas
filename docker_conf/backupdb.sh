#!/bin/bash
date=`date +%Y%m%d-%H%M`

BACKUP_DIR=/home/epas/epas/backups
# password scritta da docker_conf/init (l'immagine base imposta HOME=/opt/play)
export PGPASSFILE=/root/.pgpass
find $BACKUP_DIR -mtime +60 | xargs -r rm

pg_dump -U {{DB_USER}} -h {{DB_HOST}} -p {{DB_PORT}} -O -f $BACKUP_DIR/{{EPAS_HOST}}-$date.sql {{DB_NAME}} >> $BACKUP_DIR/{{EPAS_HOST}}-dump.log 2>&1
gzip -9 $BACKUP_DIR/{{EPAS_HOST}}-$date.sql
