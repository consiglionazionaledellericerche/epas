FROM criluc/play1:1.4.2-patched

ENV user epas
ENV APP ePas
ENV APP_HOME /home/epas/epas

USER root

RUN apt-get update && \
    apt-get install -y postgresql-client cron && \
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
    mkdir attachments logs tools backups && \
    chown -R $user:$user ${APP_HOME}

VOLUME ["/home/epas/epas/logs", "/home/epas/epas/data/attachments", "/home/epas/epas/backups"]

ENTRYPOINT ["/home/epas/epas/docker_conf/init"]

CMD ["app:run"]
