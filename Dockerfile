FROM criluc/play1:latest

USER root

WORKDIR /tmp

RUN apt-get update
RUN apt-get install -y git postgresql-client logcheck

RUN useradd -m epas
WORKDIR /home/epas

RUN mkdir -p /home/epas/epas/data/attachments
RUN mkdir -p /home/epas/epas/logs 
RUN mkdir -p /home/epas/epas/tools
RUN mkdir -p /home/epas/epas/backups
ADD / /home/epas/epas/

RUN chown -R epas:epas /home/epas/*

WORKDIR epas
 
#prod o dev da parametrizzare
USER epas
RUN play deps
RUN play precompile

USER root
VOLUME ["/home/epas/epas/logs", "/home/epas/epas/data/attachments", "/home/epas/epas/backups"]

#Fix for crontab 
RUN sed -e '/pam_loginuid.so/ s/^#*/#/' -i /etc/pam.d/cron

ENTRYPOINT ["/home/epas/epas/docker_conf/init"]

CMD ["app:run"]