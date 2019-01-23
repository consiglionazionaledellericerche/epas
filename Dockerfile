FROM openjdk:8-jdk as builder
# Converrebbe avere un'immagine con la jdk e il play già disponibile

ENV PLAY_VERSION 1.4.3
ENV APP_HOME /epas
ENV PLAY_PATH /play-${PLAY_VERSION}

RUN apt-get update && \
    apt-get install -y wget unzip ant && \
    wget -q https://downloads.typesafe.com/play/${PLAY_VERSION}/play-${PLAY_VERSION}.zip && \
    unzip -q play-${PLAY_VERSION}.zip && \
    ln -sf $PLAY_PATH/play /usr/local/bin

ADD / ${APP_HOME}
WORKDIR $APP_HOME

RUN touch conf/dev.conf && \
    play clean && \
    play deps --sync --forProd && \
    ant build -Dplay.path=$PLAY_PATH && \
    play precompile && \
    mkdir attachments logs tools backups

FROM criluc/play1:1.4.2-patched

ENV APP_HOME /home/epas/epas
ENV user epas
ENV APP ePas

USER root

RUN groupadd -r $user -g 1001 && \
    useradd -u 1001 -r -g $user -M -d /home/epas -s /sbin/nologin $user

USER $user
WORKDIR $APP_HOME

COPY --chown=epas --from=builder /epas $APP_HOME

VOLUME ["/home/epas/epas/logs", "/home/epas/epas/data/attachments", "/home/epas/epas/backups"]

ENTRYPOINT ["/home/epas/epas/docker_conf/init"]

CMD ["app:run"]
