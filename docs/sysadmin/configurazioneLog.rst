Configurazione dei log di ePAS
==============================

I log in ePAS sono gestiti tramite la Log4J e la loro configurazione può essere
 effettuata tramite alcuni parametri impostabili nel *docker_compose.yml*.
 
 È possibile utilizzare quattro tipo di appender diversi a cui inviare i log:
 *stdout*, *stderr*, *file* e *graylog2*.

Tramite il docker compose è possibile configurare il livello dei log, gli
appender da utilizzare e gli eventuali parametri per l'invio dei log ad un
`graylog <https://www.graylog.org/>`_ server esterno.

.. code-block:: yaml

  #  [...]
   - environment:
  #  [...]
  #      - LOG_LEVEL=                            # Opzionale. default: INFO   -- (OFF,FATAL,ERROR,WARN,INFO,DEBUG,TRACE,ALL)
         - APPENDERS=file                        # Opzionale. default: stdout, stderr -- (stdout, stderr, file, graylog2). Abilita i log sulla console, file e server graylog
  #      - GRAYLOG_HOST=                         # Obbligatorio se attivato log sull'appender graylog2. default: null
  #      - GRAYLOG_PORT=                         # Opzionale. default: 3514
  #      - GRAYLOG_ORIGIN_HOST=                  # Opzionale. default: valore in VIRTUAL_HOST

Se non specificato tramite la variabile APPENDERS gli appender abilitati 
di default sono *stdout* e *stderr* che inviano i log sullo standard output e
standard error a seconda del livello di log (ERROR e FATAL vanno su *stderr*).

Gli appender possono essere combinati tra di loro, nel senso che possono essere
abilitati più appender contemporanemente, per utilizzare più appender scrivere
i nomi degli appender separati da virgola (es.: APPENDERS=file, graylog2).


Appender di tipo stdout e stderr
--------------------------------

Gli appender di tipo *stdout* e *stderr* inviano i log sullo standard output e
standard error a seconda del livello di log (ERROR e FATAL vanno su *stderr*).

La configurazione specifica per questi appender da impostare nel file
**log4j.prod.properties** è la seguente:

.. code-block::

  # Standard output appender
  log4j.appender.stdout=org.apache.log4j.ConsoleAppender
  log4j.appender.stdout.Target=System.out
  log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
  log4j.appender.stdout.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss,SSS} [%p](%c) - %m%n
  log4j.appender.stdout.filter.a=org.apache.log4j.varia.LevelRangeFilter
  log4j.appender.stdout.filter.a.LevelMin=DEBUG
  log4j.appender.stdout.filter.a.LevelMax=WARN

  # Standard error appender
  log4j.appender.stderr=org.apache.log4j.ConsoleAppender
  log4j.appender.stderr.Target=System.err
  log4j.appender.stderr.layout=org.apache.log4j.PatternLayout
  log4j.appender.stderr.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss,SSS} [%p](%c) - %m%n
  log4j.appender.stderr.filter.a=org.apache.log4j.varia.LevelRangeFilter
  log4j.appender.stderr.filter.a.LevelMin=ERROR
  log4j.appender.stderr.filter.a.LevelMax=FATAL


Appender di tipo file
----------------------
 
Nel caso si utilizzi l'appender di tipo file i log vengono salvati all'interno
del container nel file log/epas.log, si consiglia quindi di effettuare all'interno
del **docker-compose.yml** un mapping del volume tipo:

.. code-block:: yaml

  # [...]
  volumes:
    - ${PWD}/logs:/home/epas/epas/logs

I log sono di tipo DailyRollingFileAppender, quindi vengono ruotati automaticamente
ogni giorno.

La configurazione specifica per questo appender da impostare nel file 
**log4j.prod.properties** è la seguente:

.. code-block::

  log4j.appender.file.Threshold=DEBUG
  log4j.appender.file=org.apache.log4j.DailyRollingFileAppender
  log4j.appender.file.File=logs/epas.log
  log4j.appender.file.DatePattern='.'yyyy-MM-dd
  log4j.appender.file.layout=org.apache.log4j.PatternLayout
  log4j.appender.file.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss,SSS} [%p](%c) - %m%n


Appender di tipo Graylog
------------------------

Le informazioni sull'host graylog a cui inviare i log sono configurabili tramite 
i parametri: *GRAYLOG_HOST*, *GRAYLOG_PORT* e *GRAYLOG_ORIGIN_HOST*, vedere i 
commenti nel *docker-compose.yml* per maggiori informazioni.

La configurazione specifica per questo appender da impostare nel file
**log4j.prod.properties** è la seguente:

.. code-block::

  # Define the graylog2 destination
  log4j.appender.graylog2=org.graylog2.log.GelfAppender
  log4j.appender.graylog2.graylogHost={{GRAYLOG_HOST}}
  log4j.appender.graylog2.graylogPort={{GRAYLOG_PORT}}
  log4j.appender.graylog2.originHost={{GRAYLOG_ORIGIN_HOST}}
  log4j.appender.graylog2.layout=org.apache.log4j.PatternLayout
  log4j.appender.graylog2.extractStacktrace=true
  log4j.appender.graylog2.addExtendedInformation=true
  log4j.appender.graylog2.additionalFields={'environment': 'PROD', 'tag': 'epas'}

