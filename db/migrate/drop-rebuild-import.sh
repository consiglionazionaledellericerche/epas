#!/bin/bash

PATH="/home/epas/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games"

/home/epas/epas/db/migrate/drop-rebuild.sh >> /tmp/drop-rebuild.log 
sleep 10
/home/epas/epas/db/migrate/import-from-orologio.sh