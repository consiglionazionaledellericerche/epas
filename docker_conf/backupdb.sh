#!/bin/bash
date=`date +%Y%m%d-%H%M`

BACKUP_DIR=/home/epas/epas/backups
find $BACKUP_DIR -mtime +60 | xargs -r rm

pg_dump -U {{DB_USER_PASS}} -h {{DB_HOST}} -p {{DB_PORT}} -O -f $BACKUP_DIR/{{EPAS_HOST}}-$date.sql {{DB_NAME}} >> $BACKUP_DIR/{{EPAS_HOST}}-dump.log
gzip -9 $BACKUP_DIR/{{EPAS_HOST}}-$date.sql
