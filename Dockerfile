FROM criluc/play1:1.5.3-temurin-11-patched@sha256:d5484cc61982c71c0f9d0fa4b1c81cd4649a88e08d87efbc568f7a6c5642c92f

ENV user=epas
ENV APP=ePas
ENV APP_HOME=/home/epas/epas

USER root

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends postgresql-client cron && \
    apt-get clean && \
    rm -r /var/lib/apt/lists/* && \
    sed -e '/pam_loginuid.so/ s/^#*/#/' -i /etc/pam.d/cron && \
    useradd -m $user

ADD / ${APP_HOME}

WORKDIR ${APP_HOME}

RUN touch conf/dev.conf && \
    play clean && \
    play deps --sync --forProd && \
    play precompile && \
    # con --forProd le dipendenze sono copiate in lib/ e modules/: la cache ivy
    # non serve a runtime. I moduli di play non usati (testrunner serve solo per
    # play test) sono rimossi perche' includono librerie non aggiornate.
    rm -rf /root/.ivy2 && \
    (cd /opt/play/play-${PLAY_VERSION}/modules && rm -rf testrunner docviewer grizzly crud) && \
    # librerie di play sostituite da versioni aggiornate in lib/ (conf/dependencies.yml)
    # e librerie non usate a runtime: driver mysql, h2 (solo test e console /@db
    # in dev) e c3p0 (il pool di connessioni di play e' HikariCP, db.factory non e'
    # impostato). Di ehcache resta ehcache-core 2.6.11, sufficiente per la cache di play:
    # la 2.10.6 contiene al suo interno versioni vulnerabili di jackson e jetty (per la
    # gestione REST, non usata). ivy invece resta: play la usa all'avvio per caricare i moduli
    (cd /opt/play/play-${PLAY_VERSION}/framework/lib && \
      rm -f log4j-1.2.17.jar commons-fileupload-1.3.2.jar xstream-1.4.9.jar snakeyaml-1.17.jar \
        dom4j-1.6.1.jar commons-io-2.5.jar commons-codec-1.10.jar postgresql-42.2.4.jar \
        commons-beanutils-1.9.2.jar commons-email-1.4.jar gson-2.8.5.jar \
        c3p0-0.9.5.2.jar c3p0-oracle-thin-extras-0.9.5.jar mchange-commons-java-0.2.12.jar \
        hibernate-c3p0-5.4.1.Final.jar \
        mysql-connector-java-5.1.46.jar h2-1.4.196.jar ehcache-2.10.6.jar) && \
    # log4j del modulo pdf: c'e' gia' reload4j in lib/
    rm -f modules/pdf-0.9/lib/log4j-1.2.16.jar && \
    mkdir attachments logs tools backups && \
    chown -R $user:$user ${APP_HOME}

VOLUME ["/home/epas/epas/logs", "/home/epas/epas/data/attachments", "/home/epas/epas/backups"]

ENTRYPOINT ["/home/epas/epas/docker_conf/init"]

CMD ["app:run"]
