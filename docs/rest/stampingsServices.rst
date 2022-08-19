Gestione delle timbrature via REST
==================================

Di seguito una breve spiegazione dell'API REST relativa alle operazioni sulle timbrature dei
dipendenti di una sede. 
Ogni sede potrà utilizzare l'API, accedendo con un utente apposito opportunamente configurato, ai 
soli dati della propria sede. 
**Questa è l'interfaccia WEB utilizzata dagli epas-client**, per l'inserimento delle timbrature, 
se possibile, è consigliato utilizzare uno degli ePAS client opensource disponibili:

  * https://github.com/consiglionazionaledellericerche/epas-client
  * https://github.com/consiglionazionaledellericerche/epas-client-sql

Permessi
--------

Per poter accedere a questa interfaccia REST è necessario utilizzare le credenziali associate
ad una **Sorgente Timbratura** della propria sede. Eventualmente verificare nella documentazione
di ePAS come creare le Sorgenti Timbrature.

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare una nuova Sorgente Timbratura
utente associato alla vostra sede, con uno username tipo *client_timbrature_sede_xxx*.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.


Inserimento di una timbratura (versione utilizzata dagli epas-client)
---------------------------------------------------------------------

Per effettuare l'inserimento di una timbratura è necessario effettuare una *HTTP POST* all'endpoint
**/stampingsfromclient/create**. La *POST* deve contenere nel body un json con i campi della
timbratura che sono:

  - *operazione*: è un numero obbligatorio. 0 indica un'entrata, 1 un uscita.
  - *causale*: è una stringa opzionale. che contiene la causale della timbraura 
    I valori attualmente gestiti sono *pausaPranzo*, *motiviDiServizio* e *lavoroFuoriSede*.
  - *anno*, *mese*, *giorno*, *ora*, *minuti*: sono tutti campi numerici obbligatori
  - *matricolaFirma*: è una stringa obbligatoria. Indica il numero di badge
  - *terminale* o *lettore*: è una stringa opzionale. Può contenere la *zona di timbratura*, utilizzata
    in alcuni rari casi per gestire le timbrature continuative tra due zone definite
  - *note*: è una stringa opzionale
  - *luogo*: è una stringa opzionale
  - *motivazione*: è una stringa opzionale

Un esempio è il seguente:

.. code-block:: bash

  $ http -a client_timbrature_sede_xxx
      POST https://epas-demo.devel.iit.cnr.it/stampingsfromclient/create
      matricolaFirma=9802 operazione=0 anno=2021 mese=02 giorno=08 ora=08 minuti=10 causale=lavoroFuoriSede luogo=Pisa motivazione='ero sotto la torre'

Il metodo restituisce una risposta vuota con uno dei seguenti codici HTTP di risposta:

 - *200*: timbratura inserita correttamente
 - *400*: body delle richiesta HTTP non presente
 - *404*: numero badge non trovato per la sorgente timbratura utilizzare per l'inserimento
 - *409*: in caso timbratura già presente (il sistema non ne inserisce un'altra uguale)


Stamping Create
---------------

Dalla versione 3 dell'API è presente un nuovo metodo per l'inserimento di una timbratura.
La nuova versione è utilizzabile con una *HTTP POST* all'endpoint
**/rest/v3/stampings/create**. 

La *POST* deve contenere nel body un json con i campi della timbratura che sono:
  - *wayType*: il campo è obbligatorio. I possibili valori sono *in* o *out*
  - *reasonType*: è una stringa opzionale. che contiene la causale della timbraura 
    I valori attualmente gestiti sono *pausaPranzo*, *motiviDiServizio* e *lavoroFuoriSede*.
  - *dateTime*: è un obbligatorio. Deve contenere la data e l'ora della timbratura in formato ISO, 
    per esempio *2021-02-12T10:00*
  - *badgeNumber*: è una stringa obbligatoria. Indica il numero di badge
  - *zone*: è una stringa opzionale. Può contenere la *zona di timbratura*, utilizzata
    in alcuni rari casi per gestire le timbrature continuative tra due zone definite
  - *note*: è una stringa opzionale
  - *place*: è una stringa opzionale
  - *reason*: è una stringa opzionale

Per esempio:

.. code-block:: bash

  $ http -a client_timbrature_sede_xxx
      POST https://epas-demo.devel.iit.cnr.it/rest/v3/stampings/create
      badgeNumber=9802 dateTime='2021-02-12T08:20' wayType=in note='Note eccezionali veramente'

Il metodo restituisce uno dei seguenti codici HTTP di risposta:

 - *200*: timbratura inserita correttamente
 - *400*: body delle richiesta HTTP non presente
 - *404*: numero badge non trovato per la sorgente timbratura utilizzare per l'inserimento
 - *409*: in caso timbratura già presente (il sistema non ne inserisce un'altra uguale)
 
Se l'inserimento va a buon fine (con codice HTTP *200*) il metodo restituisce anche le informazioni
della timbratura inserita.

.. code-block:: json


  {
    "date": "2021-02-12T08:20:00",
    "id": 398946,
    "markedByAdmin": false,
    "markedByEmployee": false,
    "markedByTelework": false,
    "note": "Note eccezionali veramente",
    "person": {
        "email": "galileo.galilei@cnr.it",
        "eppn": "galileo.galilei@cnr.it",
        "fiscalCode": "GLLGLL74P10G702B",
        "fullname": "Galilei Galileo",
        "id": 1234,
        "number": "9802"
    },
    "place": null,
    "reason": null,
    "stampType": null,
    "stampingZone": null,
    "way": "in"
  }

Stamping Show
-------------

La visualizzazione dei dati di una timbratura è tramite una *HTTP* GET all'indirizzo 
**/rest/v3/stampings/show**.

Per individuare la timbratura è possibile utilizzare solo il campo **id**. 

.. code-block:: bash

  $ http -a client_timbrature_sede_xxx
      POST https://epas-demo.devel.iit.cnr.it/rest/v3/stampings/show id==398946

Il risultato sarà uguale a quello dell'esempio riportato sopra come risposta all'inserimento
di una timbratura

Stamping Update
---------------

La modifica di la timbratura è possibile tramite una *HTTP PUT* all'indirizzo 
**/rest/v3/stampings/update**.

Per individuare il gruppo è possibile utilizzare solo il campo **id**. 
I campi che è possibile modificare sono i seguenti:

  - *wayType*: il campo è obbligatorio. I possibili valori sono *in* o *out*
  - *reasonType*: è una stringa opzionale. che contiene la causale della timbraura 
    I valori attualmente gestiti sono *pausaPranzo*, *motiviDiServizio* e *lavoroFuoriSede*.
  - *zone*: è una stringa opzionale. Può contenere la *zona di timbratura*, utilizzata
    in alcuni rari casi per gestire le timbrature continuative tra due zone definite
  - *note*: è una stringa opzionale
  - *place*: è una stringa opzionale
  - *reason*: è una stringa opzionale

**ATTENZIONE** non è possibile modificare la persona a cui è associata la timbratura o 
la data e ora della timbratura, se è necessario modificare uno di questi campi allora
è opportuno cancellare la vecchia timbratura ed inserirne una nuova.

.. code-block:: bash

  $ http -a client_timbrature_sede_xxx
      PUT https://epas-demo.devel.iit.cnr.it/rest/v3/stampings/update/398946
      wayType=in place=='Torre di Pisa' reason=='Esperimento caduta gravi' reasonType=lavoroFuoriSede


Stamping Delete
---------------

La cancellazione di una timbratura è possibile tramite una HTTP DELETE all'indirizzo **/rest/v3/stampings/delete**

Per individuare la timbratura da eliminare si utilizza lo stesso parametro previsti per la show: **id**.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      DELETE https://epas-demo.devel.iit.cnr.it/rest/v2/stampings/delete?id=398946
