FROM criluc/play1:1.4.0-patched

USER root

WORKDIR /tmp

RUN apt-get update && \
    apt-get install -y postgresql-client cron && \
    apt-get clean && \
    rm -r /var/lib/apt/lists/* && \
    sed -e '/pam_loginuid.so/ s/^#*/#/' -i /etc/pam.d/cron && \
    useradd -m epas && \
    mkdir -p /home/epas/epas/data/attachments && \
    mkdir -p /home/epas/epas/logs && \
    mkdir -p /home/epas/epas/tools && \
    mkdir -p /home/epas/epas/backups

ADD / /home/epas/epas/

RUN chown -R epas:epas /home/epas

WORKDIR /home/epas/epas/
 
#prod o dev da parametrizzare
USER epas

RUN play clean && \
    play deps --sync && \
    play precompile

USER root

VOLUME ["/home/epas/epas/logs", "/home/epas/epas/data/attachments", "/home/epas/epas/backups"]

ENTRYPOINT ["/home/epas/epas/docker_conf/init"]

CMD ["app:run"]
