Consultazione delle tipologie di assenze via REST
=================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione delle tipologie di 
assenze dei dipendenti presenti in ePAS. 

Permessi
--------

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo
di *Gestore Assenze* oppure di *Lettore Informazioni*.
I nuovi utenti possono essere definiti dagli utenti che hanno il ruolo di *amministratore tecnico*.

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla
vostra sede tipo *istituto_xxx_absence_manager* oppure *istituto_xxx_personday_reader*
(cambiate il nome o in futuro andrà in conflitto con quello di altri istituti) ed una volta creato
l'utente assegnateli il ruolo *Gestore Assenze*.

Inoltre è possibile utilizzare un utente di sistema con ruolo di *Gestore Assenze* su tutto il
sistema. Questo utente è utiizzato per l'eventuale integrazione con sistemi esterni (per esempio
di rendicontazione) a livello di tutte le sedi. 
L'utente di sistema con ruolo di *Gestore Assenze* non può essere creato dalle singole sedi ma
può essere creato tra un utente con ruolo di *Amministratore* di ePAS.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Lista delle tipologie di codici di assenza
------------------------------------------

Le informazioni relative alle tipologie di assenze presenti è disponibile tramite una HTTP GET all'indirizzo
**/rest/v3/absencesTypes/list**.

La lista delle assenze può essere filtrata utilizzando due parametri *onlyActive*, *used*.
Entrambi i parametri sono opzionali. Il parametro *onlyActive* se impostato a "true" fa in modo
che siano fornite solo le tipologie di assenze attive al momento, se il parametro non viene passato
vengono restituiti tutti le tipologie presenti nel sistema.
Il parametro *used* è anch'esso opzionale e può essere passato come "true" o "false".
Se il parametro *used* è passato come "true" vengono mostrate solo le tipologie di assenze utilizzate, se
passato come "false" verranno mostrare solo quelle non utilizzate, se il parametro non viene indicato
allora verranno mostrare le assenze indipendentemente dal loro utilizzo.
   
.. code-block:: bash

  $ http -a istituto_xxx_absence_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/absenceTypes/list 
      onlyActive==true used==true

La risposta sarà del tipo

.. code-block:: json

  [
	  {
	    "id": 259,
	    "code": "01",
	    "certificateCode": "01",
	    "description": "Assemblea completamento 1 ora",
	    "validFrom": null,
	    "validTo": null,
	    "internalUse": false,
	    "consideredWeekEnd": false,
	    "justifiedTime": 0
	  },
	  {
	    "id": 273,
	    "code": "02",
	    "certificateCode": "02",
	    "description": "Assemblea completamento 2 ore",
	    "validFrom": null,
	    "validTo": null,
	    "internalUse": false,
	    "consideredWeekEnd": false,
	    "justifiedTime": 0
	  }
  ]

AbsenceType Show
----------------

La visualizzazione dei dati di una tipologia di assenza è possibile tramite una HTTP GET all'indirizzo
**/rest/v3/absenceTypes/show**.

Per individuare la tipologia di assenza è possibile utilizzare solo il campo *id*.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/absenceTypes/show 
      id==297

.. code-block:: json

  {
    "certificateCode": "31",
    "code": "31",
    "consideredWeekEnd": false,
    "description": "Ferie anno precedente",
    "documentation": null,
    "id": 297,
    "internalUse": false,
    "justifiedTime": 0,
    "justifiedTypesPermitted": [
        "all_day"
    ],
    "reperibilityCompatible": false,
    "replacingTime": 0,
    "timeForMealTicket": false,
    "validFrom": null,
    "validTo": null
  }
