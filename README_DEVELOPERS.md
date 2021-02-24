ePAS - Electronic Personnel Attendance System
==============================================

ePAS è un'applicazione java basata su Play Framework versione 1.5.x e 
database postgresql.
Per il setup dell'ambiente di sviluppo è necessario aver installato sul 
proprio PC java 11, il [play framework 1.5.x](https://www.playframework.com/documentation/1.5.x/install)
ed avere accesso ad un dabatase postgresql.

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
visto che non più compatibile con Java 11.

La configurazione per l'accesso al db e per sovrascriscrivere i paremetri tipici
dello sviluppo è nel file _conf/dev.conf_.

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
