FROM criluc/play1:1.4.0-patched

USER root

RUN apt-get update && \
    apt-get install -y postgresql-client cron && \
    apt-get clean && \
    rm -r /var/lib/apt/lists/* && \
    sed -e '/pam_loginuid.so/ s/^#*/#/' -i /etc/pam.d/cron && \
    useradd -m epas

ADD / /home/epas/epas/

WORKDIR /home/epas/epas/
 
RUN touch conf/dev.conf && \
    play clean && \
    play deps --sync && \
    play precompile && \
    mkdir attachments logs tools backups && \
    chown -R epas:epas /home/epas

VOLUME ["/home/epas/epas/logs", "/home/epas/epas/data/attachments", "/home/epas/epas/backups"]

ENTRYPOINT ["/home/epas/epas/docker_conf/init"]

CMD ["app:run"]
