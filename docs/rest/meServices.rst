Servizi REST self-service per il dipendente
===========================================

Di seguito una breve spiegazione delle API REST con cui il dipendente autenticato consulta la
propria situazione e gestisce le proprie richieste di assenza. Sono pensate per l'app mobile di
ePAS ma possono essere utilizzate da qualunque client che agisca per conto del dipendente.

A differenza degli altri servizi REST, la persona non viene mai indicata nei parametri della
richiesta: i dati restituiti sono sempre quelli della persona associata all'utente autenticato.

Permessi
--------

Per poter accedere a queste interfaccie REST è sufficiente il ruolo di *Dipendente*: un
dipendente può leggere e modificare solo i propri dati. Un utente senza persona associata oppure
senza il ruolo di *Dipendente* riceve una risposta *403 Forbidden*, mentre una richiesta senza
autenticazione riceve una risposta *401 Unauthorized*.

Autenticazione
--------------

Oltre all'autenticazione *Basic Auth* con le credenziali del dipendente, questi servizi possono
essere chiamati con un *access token* rilasciato dall'Identity Provider OIDC configurato in ePAS
(vedi la configurazione dell'autenticazione OIDC nella documentazione per i sistemisti),
passato nell'header *Authorization*. Gli access token sono accettati solo se l'autenticazione
OIDC è abilitata (*OAUTH_LOGIN=true*).

.. code-block:: bash

  $ http GET https://epas-demo.devel.iit.cnr.it/rest/v4/me
      "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lk..."

ePAS accetta gli access token rilasciati al proprio client OIDC (*KEYCLOAK_CLIENT_ID*) e agli
eventuali altri client dello stesso realm indicati nella variabile d'ambiente
**KEYCLOAK_ADDITIONAL_CLIENT_IDS** (parametro *keycloak.additional_client_ids*), separati da
virgola. Per l'app mobile è opportuno creare sull'Identity Provider un client *pubblico* dedicato
(senza secret, con PKCE) e indicarlo in questa variabile, per esempio
*KEYCLOAK_ADDITIONAL_CLIENT_IDS=epas-mobile*.

Nei token il dipendente viene riconosciuto con lo stesso campo utilizzato per il login OIDC
(*email* o quello impostato in *KEYCLOAK_JWT_FIELD*).

Negli esempi successivi per semplicità viene utilizzata l'autenticazione *Basic Auth* con
l'utente *galileo.galilei*, cambiatelo con un dipendente della vostra sede.

Errori
------

Tutte le risposte di errore sono in JSON con il messaggio nel campo *error*, per esempio:

.. code-block:: json

  {
    "error": "Richiesta di assenza non trovata"
  }

L'unica eccezione è l'inserimento di una richiesta di assenza non inseribile, che riporta
l'elenco dei problemi riscontrati nel campo *errors* (vedi sotto).

I codici di stato utilizzati sono:

* *400 Bad Request*: parametri o body della richiesta non validi
* *401 Unauthorized*: autenticazione assente o non valida (con l'header *WWW-Authenticate*)
* *403 Forbidden*: utente senza persona associata o senza il ruolo di *Dipendente*
* *404 Not Found*: dato non presente, oppure richiesta di assenza inesistente o di un'altra
  persona
* *405 Method Not Allowed*: metodo HTTP non ammesso dal servizio (con l'header *Allow*)
* *409 Conflict*: operazione non più possibile sulla richiesta di assenza

Profilo del dipendente
----------------------

Le informazioni sulla persona, sulla sua sede, sul contratto attuale e sui tipi di richiesta di
assenza che può effettuare sono disponibili tramite una HTTP GET all'indirizzo
**/rest/v4/me**.

.. code-block:: bash

  $ http -a galileo.galilei GET https://epas-demo.devel.iit.cnr.it/rest/v4/me

La risposta sarà del tipo (per brevità della sede sono riportati solo alcuni campi):

.. code-block:: json

  {
    "person": {
      "id": 1234,
      "fullname": "Galilei Galileo",
      "fiscalCode": "GLLGLL74P10G702B",
      "email": "galileo.galilei@cnr.it",
      "number": "9802",
      "eppn": "galileo.galilei@cnr.it"
    },
    "office": {
      "id": 1,
      "name": "IIT - Pisa",
      "code": "044000",
      "codeId": "223400"
    },
    "qualification": 6,
    "topQualification": false,
    "currentContractBeginDate": "2018-12-27",
    "currentContractEndDate": null,
    "absenceRequestTypes": [
      {
        "type": "VACATION_REQUEST",
        "allDay": true,
        "available": true,
        "problems": []
      },
      {
        "type": "COMPENSATORY_REST",
        "allDay": true,
        "available": true,
        "problems": []
      },
      {
        "type": "PERSONAL_PERMISSION",
        "allDay": true,
        "available": false,
        "problems": [
          "Approvazione del responsabile di gruppo richiesta. La persona Galilei Galileo non ha impostato nessun responsabile di gruppo e non appartiene ad alcun gruppo. Contattare l'ufficio del personale."
        ]
      }
    ]
  }

Il campo *available* indica se la configurazione della sede permette al dipendente di
effettuare quel tipo di richiesta; in caso contrario il campo *problems* ne riporta i motivi.

Situazione mensile
------------------

I giorni del mese con timbrature e assenze, il residuo orario e i buoni pasto residui a fine mese
sono disponibili tramite una HTTP GET all'indirizzo **/rest/v4/me/month**.

I parametri *year* e *month* sono opzionali, se non specificati viene mostrato il mese corrente.

.. code-block:: bash

  $ http -a galileo.galilei GET https://epas-demo.devel.iit.cnr.it/rest/v4/me/month
      year==2026 month==10

La risposta sarà del tipo:

.. code-block:: json

  {
    "year": 2026,
    "month": 10,
    "basedWorkingDays": 22,
    "residualMinutes": 1835,
    "mealTicketResidual": 12,
    "personDays": [
      {
        "id": 98765,
        "date": "2026-10-01",
        "timeAtWork": 432,
        "difference": 0,
        "progressive": 1820,
        "stampingsTime": 462,
        "decurtedMeal": 30,
        "isTicketAvailable": true,
        "isHoliday": false,
        "stampings": [
          {
            "id": 445566,
            "date": "2026-10-01T08:30:00",
            "way": "in",
            "stampType": null,
            "place": null,
            "reason": null,
            "markedByAdmin": false,
            "markedByEmployee": false,
            "note": null
          },
          {
            "id": 445567,
            "date": "2026-10-01T16:12:00",
            "way": "out",
            "stampType": null,
            "place": null,
            "reason": null,
            "markedByAdmin": false,
            "markedByEmployee": false,
            "note": null
          }
        ],
        "absences": []
      }
    ]
  }

Per brevità nell'esempio è riportato un solo giorno, la lista *personDays* contiene tutti i
giorni del mese già calcolati da ePAS. *residualMinutes* è il residuo orario in minuti (anno
precedente più anno corrente).

Dettaglio di un giorno
----------------------

Il dettaglio di un singolo giorno, con timbrature e assenze, è disponibile tramite una HTTP GET
all'indirizzo **/rest/v4/me/day**. Il parametro *date* è opzionale, se non specificato viene
mostrato il giorno corrente. Il formato del giorno è lo stesso degli elementi di *personDays*
della situazione mensile.

.. code-block:: bash

  $ http -a galileo.galilei GET https://epas-demo.devel.iit.cnr.it/rest/v4/me/day
      date==2026-10-01

Se ePAS non ha ancora calcolato il giorno richiesto (per esempio per i giorni futuri) la risposta
è *404 Not Found*.

Situazione ferie e permessi
---------------------------

La situazione delle ferie e dei permessi, residui e maturati, per ciascun contratto attivo
nell'anno è disponibile tramite una HTTP GET all'indirizzo **/rest/v4/me/vacations**.
Il parametro *year* è opzionale, se non specificato viene mostrata la situazione dell'anno
corrente.

.. code-block:: bash

  $ http -a galileo.galilei GET https://epas-demo.devel.iit.cnr.it/rest/v4/me/vacations
      year==2026

La risposta ha lo stesso formato del servizio **/rest/v3/vacations/byPersonAndYear** descritto
nella sezione sulla consultazione della situazione ferie e permessi.

Buoni pasto residui
-------------------

I buoni pasto residui ad oggi sono disponibili tramite una HTTP GET all'indirizzo
**/rest/v4/me/mealTickets**.

.. code-block:: bash

  $ http -a galileo.galilei GET https://epas-demo.devel.iit.cnr.it/rest/v4/me/mealTickets

La risposta sarà del tipo:

.. code-block:: json

  {
    "number": "9802",
    "dateOfResidual": "2026-10-23",
    "mealTicketResidual": 12
  }

Richieste di assenza
--------------------

Tramite queste API il dipendente può gestire le proprie richieste di *ferie*
(*VACATION_REQUEST*), di *riposo compensativo* (*COMPENSATORY_REST*) e di *permesso personale*
(*PERSONAL_PERMISSION*). Le richieste seguono lo stesso flusso di approvazione di quelle
effettuate tramite l'interfaccia web di ePAS, con le stesse notifiche ai responsabili.

Elenco delle richieste
~~~~~~~~~~~~~~~~~~~~~~

L'elenco delle richieste di assenza del dipendente è disponibile tramite una HTTP GET
all'indirizzo **/rest/v4/me/absenceRequests**.

I parametri sono tutti opzionali:

* *type*: il tipo di richiesta, se non specificato vengono restituiti tutti i tipi gestiti
* *active*: *true* per le sole richieste con il flusso in corso, *false* per le sole richieste
  con il flusso terminato, se non specificato vengono restituite tutte
* *from*: la data da cui cercare le richieste (in base alla data di inizio), se non specificata
  le richieste che iniziano da un mese prima dell'inizio dell'anno corrente

.. code-block:: bash

  $ http -a galileo.galilei GET https://epas-demo.devel.iit.cnr.it/rest/v4/me/absenceRequests
      type==VACATION_REQUEST active==true

La risposta è una lista di richieste ordinate dalla più recente, ognuna del tipo:

.. code-block:: json

  {
    "id": 4321,
    "type": "VACATION_REQUEST",
    "status": "PENDING",
    "startAt": "2026-11-02",
    "endTo": "2026-11-06",
    "hours": null,
    "minutes": 0,
    "note": null,
    "absenceCode": null,
    "managerApprovalRequired": true,
    "managerApproved": "2026-10-22T09:41:12",
    "administrativeApprovalRequired": false,
    "administrativeApproved": null,
    "officeHeadApprovalRequired": true,
    "officeHeadApproved": null,
    "deletable": false,
    "events": [
      {
        "createdAt": "2026-10-22T09:41:12",
        "eventType": "MANAGER_APPROVAL",
        "description": null,
        "owner": "Newton Isaac"
      },
      {
        "createdAt": "2026-10-21T17:05:30",
        "eventType": "STARTING_APPROVAL_FLOW",
        "description": null,
        "owner": "Galilei Galileo"
      }
    ]
  }

Il campo *status* riassume lo stato della richiesta e può valere:

* *PENDING*: il flusso di approvazione è in corso
* *APPROVED*: la richiesta è stata approvata e le assenze sono state inserite
* *REFUSED*: la richiesta è stata respinta
* *DELETED*: la richiesta è stata ritirata dal dipendente
* *EXPIRED*: la richiesta è scaduta

Gli eventi del flusso (*events*) sono ordinati dal più recente. Il campo *deletable* indica se
il dipendente può ancora ritirare la richiesta.

Dettaglio di una richiesta
~~~~~~~~~~~~~~~~~~~~~~~~~~

Il dettaglio di una singola richiesta è disponibile tramite una HTTP GET all'indirizzo
**/rest/v4/me/absenceRequests/{id}**, con lo stesso formato degli elementi dell'elenco.

.. code-block:: bash

  $ http -a galileo.galilei GET https://epas-demo.devel.iit.cnr.it/rest/v4/me/absenceRequests/4321

Se la richiesta non esiste oppure è di un'altra persona la risposta è *404 Not Found*.

Inserimento di una richiesta
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Una nuova richiesta di assenza può essere inserita tramite una HTTP POST all'indirizzo
**/rest/v4/me/absenceRequests**, con un body JSON con questi campi:

* *type*: il tipo di richiesta (obbligatorio)
* *startAt*: il primo giorno della richiesta (obbligatorio)
* *endTo*: l'ultimo giorno della richiesta, se non specificato la richiesta è per il solo giorno
  di inizio
* *hours* e *minutes*: solo per i permessi personali orari, che devono essere per un solo
  giorno; se non specificati il permesso personale è giornaliero
* *note*: la motivazione, obbligatoria per le richieste nel passato

.. code-block:: bash

  $ http -a galileo.galilei POST https://epas-demo.devel.iit.cnr.it/rest/v4/me/absenceRequests
      type=VACATION_REQUEST startAt=2026-11-02 endTo=2026-11-06

Prima dell'inserimento vengono effettuati gli stessi controlli della form di inserimento di
ePAS: configurazione dei responsabili che devono approvare la richiesta, date, sovrapposizione
con altre richieste, mesi già inviati ad Attestati, contratti del dipendente e simulazione
dell'inserimento delle assenze. Se la richiesta non è inseribile la risposta è
*400 Bad Request* con l'elenco dei problemi riscontrati:

.. code-block:: json

  {
    "errors": [
      "Esiste già una richiesta in questa data"
    ]
  }

Se la richiesta termina dopo l'ultimo giorno in cui è possibile inserire l'assenza, la data di
fine viene adeguata come nella form di ePAS.

Se l'inserimento va a buon fine la risposta è *201 Created* con la richiesta inserita, nello
stesso formato del dettaglio. A seconda della configurazione la richiesta può risultare già
approvata, per esempio per i livelli I-III che non necessitano di autorizzazione o per il
responsabile di sede.

Ritiro di una richiesta
~~~~~~~~~~~~~~~~~~~~~~~

Il dipendente può ritirare una propria richiesta finché il flusso non è terminato e nessuno l'ha
ancora approvata, tramite una HTTP DELETE all'indirizzo
**/rest/v4/me/absenceRequests/{id}**.

.. code-block:: bash

  $ http -a galileo.galilei DELETE https://epas-demo.devel.iit.cnr.it/rest/v4/me/absenceRequests/4321

Se il ritiro va a buon fine la risposta è *200 OK* con la richiesta aggiornata, nello stesso
formato del dettaglio e con *status* uguale a *DELETED*. Se la richiesta è già stata approvata,
anche solo in parte, oppure il flusso è terminato la risposta è *409 Conflict*. I responsabili
che dovevano approvare la richiesta ricevono una notifica del ritiro.
