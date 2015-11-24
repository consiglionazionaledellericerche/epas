FROM criluc/play1:1.4.0

ENV LANG it_IT.UTF-8
ENV LANGUAGE it:en

USER root

WORKDIR /tmp

RUN apt-get update && \
    apt-get install -y postgresql-client \
    locales && \
    apt-get clean && \
    rm -r /var/lib/apt/lists/*

# Set the locale
RUN sed -i -e 's/# it_IT.UTF-8 UTF-8/it_IT.UTF-8 UTF-8/' /etc/locale.gen && \
    locale-gen && \
    dpkg-reconfigure --frontend=noninteractive locales

RUN useradd -m epas
WORKDIR /home/epas

RUN mkdir -p /home/epas/epas/data/attachments && \
    mkdir -p /home/epas/epas/logs && \
    mkdir -p /home/epas/epas/tools && \
    mkdir -p /home/epas/epas/backups

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
#RUN sed -e '/pam_loginuid.so/ s/^#*/#/' -i /etc/pam.d/cron

ENTRYPOINT ["/home/epas/epas/docker_conf/init"]

CMD ["app:run"]
