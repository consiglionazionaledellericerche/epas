ePAS - Electronic Personnel Attendance System
==============================================

ePAS è un'applicazione java basata su Play Framework versione 1.5.x e
database postgresql.
Per il setup dell'ambiente di sviluppo è necessario aver installato sul 
proprio PC java 11, il [play framework 1.5.x](https://www.playframework.com/documentation/1.5.x/install)
ed avere accesso ad un dabatase postgresql (dalla versione 9.5 in poi).

Di seguito alcune istruzioni per poter eseguire il debug o aggiungere nuove
funzionalità avviando un'istanza di ePAS in locale sulla proprio workstation.

Le configurazioni successive non sono necessarie se si sta semplicemente
utilizzando ePAS con l'immagine docker pubblica.

Avvio da Eclipse
----------------

Per il setup del repository come progetto Eclipse è possibile 
utilizzare il comando play

```
play eclipsify --%dev
```

e poi importare il progetto come progetto eclipse esistente.

Il comando _play eclipsify --%dev_ genera anche una _run configuration_ di Eclipse,
all'interno della quale è necessario rimuovere dagli _arguments_ questa opzione
```
 -Djava.endorsed.dirs="/home/cristian/java/play-1.5.3/framework/endorsed"
```
visto che non più compatibile con Java 11 (naturalmente verificate il path corretto della vostra home).

La configurazione per l'accesso al db e per sovrascriscrivere i paremetri tipici
dello sviluppo è nel file _conf/dev.conf_.


Inizializzazione ed aggiornamento database
------------------------------------------

L'applicazione utilizza un sistema di versione del database.
Al primo avvio (a database vuoto) è necessario lanciare il comando 

```
play evolutions:apply
```

Questo comando si occupa di applicare tutte le evoluzioni del database contenute 
nella directory db/evolutions le quali si occupano di creare tutte le tabelle 
necessarie.

Al primo avvio a db vuoto di ePAS, è necessario nel file *dev.conf* impostare il 
paramentro *jobs.active* a *true*, questo per attivare i job che si occupano di popolare
il database vuoto con alcuni da utili per il setup e la configurazione di ePAS a db vuoto.

Il camando ```play evolutions:apply``` dovrà essere lanciato ogni qual volta ci sono
delle nuove evoluzioni del db.


Generazione della documentazione
--------------------------------

Tutta la documentazione è presente nella cartella docs ed è scritta in formato restructuredText.
Per poter generare la documentazione è necessario installare le dipendente python tramite il comando

```
pip install -r requirements.txt
``` 

La generazione della documentazione è possibile tramite il comando 

```
make html
```

La documentazione in formato html sarà generata nella cartella *_build*.


Test
----

Strumenti utilizzati:
 - jailer- http://jailer.sourceforge.net/ per l'estrazione dei dati dalla base di dati di produzione
   - la base di dati per i test è stata creata a partire dai dati delle "Person" admin e Cristian Lucchesi al 28 febbraio 2014

 - dbUnit - http://dbunit.sourceforge.net/
  - i dati sono importati e disponibili per i test tramite l'importazione del dataset db unit fatto dalla procedura Startup dentro il
    package dei test

 - junit
   - alcuni test di base sono fatti tramite l'integrazione in play della junit

Per lanciare tutti i test presente utilizzare il comando
```
play autotest
```

QueryDSL
--------
Per ricompilare i Q<model>:

$ ant build -Dplay.path=<il-path-del-play>

Esempio di query sulle person:

        SearchResults<?> results = PersonDao.list(Optional.of(""),
                ImmutableSet.of(Office.<Office>findById(1L)), true)
                .paginated(page);
        ...


Restore del db con fabric
------------------------

Il fabric si può installare in una virtualenv apposita (vedere virtualenvwrapper). 
Il comando tipico è:

	pip install fabric

Volendo, a prescindere dalla virtualenv corrente, è possibile inserire nel path
un link al comando fab, che comunque riporta il python path corretto.

Inoltre c'è un comando per recuperare l'ultimo backup del database di
produzione di Pisa, eliminare la copia locale e sostituirla con quella prelevata.
Ad esempio:

 $ fab -H epas.tools.iit.cnr.it copybackup epas-devel
 
Generazione del Jar con le classi per l'interazione con KeyCloak
----------------------------------------------------------------

La definizione OpenAPI delle interfacce REST del keycloak può essere presa da un progetto come 
questo:
 - https://github.com/ccouzens/keycloak-openapi

Per generare le classi JAVA relative a api, client e modello è possibile utilizzare un generatore
come questo:
 - https://openapi-generator.tech/

Nel caso di ePAS le classi sono state generate in questo modo:

    $ java -jar openapi-generator-cli.jar generate -i 16.1.json -g java -o ./keycloak-client \
      --additional-properties=groupId=it.iit.cnr,apiPackage=it.cnr.iit.keycloak.api,modelPackage=it.cnr.iit.keycloak.model,invokerPackage=it.cnr.iit.keycloak.invoker,library=feign

Una volta generate le classi è possibile generare il jar da includere tra le dipendenze del progetto:

    $ cd keycloak-client/src/main/ && jar cvf keycloak-client.jar -C java/ .
