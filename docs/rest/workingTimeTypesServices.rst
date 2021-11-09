Consultazione delle tipologie di orario di lavoro via REST
==========================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione delle tipologie
di orario di lavoro configurate per una sede.

Permessi
--------

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo
di *Gestore anagrafica* per la sede su cui si vuole effettuare le operazioni (lo stesso ruolo
utilizzato per la gestione delle persone e dei gruppi).

u https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla
vostra sede tipo *istituto_xxx_registry_manager* (cambiate il nome o in futuro andrà in
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il
ruolo *Gestore Anagrafica*.

Inoltre è possibile utilizzare un utente di sistema con ruolo di *Gestore anagrafica* per accedere 
alle informazioni sulle persone di tutte le sedi. Questo utente è utiizzato per l'eventuale 
integrazione con sistemi esterni (per esempio di rendicontazione) a livello di tutte le sedi. 
L'utente di sistema con ruolo di *Gestore anagrafica* non può essere creato dalle singole sedi ma
può essere creato tra un utente con ruolo di *Amministratore* di ePAS.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.


Elenco delle tipologie di orario di lavoro disponibili
------------------------------------------------------

Ogni sede ha la possibilità di visualizzare la lista delle tipologie di orario di lavoro
definite per la propria sede tramite una *HTTP GET* all'endopoint
**/rest/v2/workingtimetypes/list**.

Per individuare l'ufficio è possibile utilizzare una delle due chiavi candidate presenti sugli uffici:
 - id, codeId (corrisponde al *sede id* di Attestati).

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      GET https://epas-demo.devel.iit.cnr.it//rest/v2/workingtimetypes/list
      codeId==223400

La risposta sarà del tipo:

.. code-block:: json

  [
     {
        "description": "Normale",
        "disabled": false,
        "externalId": null,
        "horizontal": true,
        "id": 1,
        "office": null,
        "updatedAt": "2021-02-03T09:49:05.231072"
     },
     {
        "description": "50%",
        "disabled": false,
        "externalId": null,
        "horizontal": true,
        "id": 4,
        "office": null,
        "updatedAt": "2021-02-03T09:49:05.231072"
    }
  ]


Visualizzazione dettagli di una tipologia di orario di lavoro
-------------------------------------------------------------

Si può visualizzare i dettagli della configurazione di una tipologia di orario di lavoro tramite
una *HTTP GET* all'endopoint **/rest/v2/workingtimetypes/show**.

Per individuare la tipologia di orario di lavoro è necessario passare il campo **id**.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/workingtimetypes/show
      id==1

Il risultato sarà del tipo:

.. code-block:: json

  {
     "description": "Normale",
     "disabled": false,
     "externalId": null,
     "horizontal": true,
     "id": 1,
     "office": null,
     "updatedAt": "2021-02-03T09:49:05.231072",
     "workingTimeTypeDays": [
        {
            "breakTicketTime": 30,
            "dayOfWeek": 1,
            "holiday": false,
            "id": 1,
            "ticketAfternoonThreshold": 0,
            "ticketAfternoonWorkingTime": 0,
            "timeMealFrom": 0,
            "timeMealTo": 0,
            "updatedAt": "2021-02-03T09:49:05.279608",
            "workingTime": 432
        },
        {
            "breakTicketTime": 30,
            "dayOfWeek": 2,
            "holiday": false,
            "id": 2,
            "ticketAfternoonThreshold": 0,
            "ticketAfternoonWorkingTime": 0,
            "timeMealFrom": 0,
            "timeMealTo": 0,
            "updatedAt": "2021-02-03T09:49:05.279608",
            "workingTime": 432
        },
        {
            "breakTicketTime": 30,
            "dayOfWeek": 3,
            "holiday": false,
            "id": 3,
            "ticketAfternoonThreshold": 0,
            "ticketAfternoonWorkingTime": 0,
            "timeMealFrom": 0,
            "timeMealTo": 0,
            "updatedAt": "2021-02-03T09:49:05.279608",
            "workingTime": 432
        },
      ]
  }
