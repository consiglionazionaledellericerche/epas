FROM criluc/play1:1.5.3-temurin-11-patched

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
    mkdir attachments logs tools backups && \
    chown -R $user:$user ${APP_HOME}

VOLUME ["/home/epas/epas/logs", "/home/epas/epas/data/attachments", "/home/epas/epas/backups"]

ENTRYPOINT ["/home/epas/epas/docker_conf/init"]

CMD ["app:run"]
