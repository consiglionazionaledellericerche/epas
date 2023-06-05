#!python
# require fabric

import os.path

from fabric import task

BACKUP_DIR= "/data/backups/epas"

@task
def localbackup(c, dbname="epas-devel", dbuser="epas", destination="/tmp/epas.pgsql"):
    """
    create a database backup using custom postgresql format
    """

    c.local("pg_dump -U %s -O %s -Fc -n public -T spatial_ref_sys -f %s" %(dbuser, dbname, destination))

@task
def restorebackup(c, dbname="epas-devel", dbuser="epas", source="/tmp/epas.pgsql"):
    """
    restore a previous database backup using custom postgresql format
    """

    recreatedb(c, dbname, dbuser)
    c.local("zcat %s | psql -U %s %s" % (source, dbuser, dbname))

@task
def copydb(c, dbname="epas-devel", dbuser="epas", remotedb="epas"):
    """
    copy production db into local database
    """

    c.run("pg_dump -U %s -O %s -n public -T spatial_ref_sys | gzip -c > /tmp/db.sql.gz" % (remotedb, remotedb))
    c.get("/tmp/db.sql.gz", "/tmp/db.sql.gz")
    recreatedb(c, dbname, dbuser)
    c.local("zcat /tmp/db.sql.gz | psql -U %s %s" % (dbuser, dbname))

@task
def fastrestoredb(c, dbname="epas-devel", dbuser="epas"):
    """
    restore backup from a pgsql format file
    """

    recreatedb(c, dbname, dbuser)
    c.local("pg_restore -U %s -n public -d %s -j 2 /tmp/db.pgsql" % (dbuser, dbname))
    
@task
def fastcopydb(c, dbname="epas-devel", dbuser="epas", remotedb="epas"):
    """
    copy database using custom postgresql format
    """
    c.run("pg_dump -U %s -O %s -Fc -n public -T spatial_ref_sys -f /tmp/db.pgsql" % (remotedb, remotedb))
    c.get("/tmp/db.pgsql", "/tmp/db.pgsql")
    fastrestoredb(c, dbname, dbuser)

@task
def copybackup(c, dbname="epas-devel", dbuser="epas", force=False):
    """
    restore local database from last server backup
    """
    with c.cd(BACKUP_DIR):
        lastbackup = c.run("ls -lt | head -2 | tail -1 | awk '{print $9}'").stdout.strip()
        if force or not os.path.exists("/tmp/" + lastbackup):
            c.get(os.path.join(BACKUP_DIR, lastbackup), "/tmp/" + lastbackup)

    recreatedb(c, dbname, dbuser)
    c.local("zcat %s | psql -U %s %s" % ("/tmp/" + lastbackup, dbuser, dbname))

def recreatedb(c, dbname, dbuser):
    try:
        c.local("psql -lqt | cut -d \| -f 1 | grep -w %s" % (dbname, ))
    except:
        pass
    else:
        try:
            c.local("dropdb %s" % (dbname, ))
        except:
            print("database non presente?")
    c.local("createdb -O %s %s" % (dbuser, dbname))
