#!python
# require fabric

from fabric.api import *
from hashlib import md5

env.hosts = ["epas@epas-r1.tools.iit.cnr.it"]
#env.hosts = ["epas@epas-r1.tools.iit.cnr.it"]

APP = "epas"
PLAY = "/home/epas/bin/play/play"

BACKUP_DIR= "/data/backups/epas"

@task
def update():
    with cd(APP):
        run("git pull origin master")
        run("git describe --all --long > VERSION")
    dependencies()

@task
def dependencies():
    with cd(APP):
        run(PLAY + " deps --sync")
        run(PLAY + " precompile")

@task
def stop():
    with cd(APP):
        with warn_only():
            run(PLAY + " stop")

@task
def start():
    with cd(APP):
        run("nohup " + PLAY + " start -Dprecompiled=true")
    logtail()

@task
def evolutions():
    stop()
    with cd(APP):
        run(PLAY + " evolutions:apply")
    start()

@task
def logtail():
    with cd(APP):
        run("tail -f logs/epas.log")

@task
def changepassword(username, password, dbname="epas-devel", dbuser="epas"):
    """
    change password of the user identified by username
    """

    # using obsolete md5... please migrate to sha512 or bcrypt.
    sql = "update users set password = '%s' where username = '%s'" % \
        (md5(password).hexdigest(), username)
    local("psql -U %s -c \"%s\" %s" % (dbuser, sql, dbname))

def recreatedb(dbname, dbuser):
    try:
        local("psql -lqt | cut -d \| -f 1 | grep -w %s" % (dbname, ))
    except:
        pass
    else:
        local("dropdb %s" % (dbname, ))
    local("createdb -O %s %s" % (dbuser, dbname))

@task
def copydb(dbname, dbuser="epas", remotedb="epas"):
    """
    copy production db into local database
    """
    run("pg_dump -U %s -O %s | gzip -c > /tmp/db.sql.gz" % (remotedb, remotedb))
    get("/tmp/db.sql.gz", "/tmp")
    recreatedb(dbname, dbuser)
    local("zcat /tmp/db.sql.gz | psql -U %s %s" % (dbuser, dbname))

@task
def copybackup(dbname, dbuser="epas"):
    """
    restore local database from last server backup

    """
    with cd(BACKUP_DIR):
        lastbackup = run("ls -lt | head -2 | tail -1 | awk '{print $9}'")
        get(lastbackup, "/tmp")
    recreatedb(dbname, dbuser)
    local("zcat %s | psql -U %s %s" % ("/tmp/" + lastbackup, dbuser, dbname))
