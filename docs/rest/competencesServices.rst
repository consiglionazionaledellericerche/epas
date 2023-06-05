Consultazione situazione competenze via REST
============================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione della lista
delle competenze presenti su ePAS e delle competenze abilitate per una persona. 

Permessi
--------

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo 
di *Lettore Informazioni* per la sede su cui si vuole effettuare le operazioni. 
I nuovi utenti possono essere definiti dagli utenti che hanno il ruolo di *amministratore tecnico*. 

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla 
vostra sede tipo *istituto_xxx_person_day_reader* (cambiate il nome o in futuro andrà in 
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il 
ruolo *Lettore informazioni*.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Riepilogo tipologie competenze presenti nel sistema
---------------------------------------------------

La lista delle tipologie di competenze presenti nel sistema è disponibile tramite una
HTTP GET all'indirizzo **/rest/v3/competences/list**.

Per esempio:

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/competences/list 

La risposta sarà del tipo:

.. code-block:: json

  [
    {
        "code": "207",
        "codeToPresence": "207",
        "description": "Ind.ta' Reper.ta' Feriale",
        "id": 2
    },
    {
        "code": "S2",
        "codeToPresence": "S2",
        "description": "Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi",
        "id": 6
    }
  ]

Nell'esempio sopra sono riportate per semplicità solo alcune competenze ma l'elenco reale
sarà più lungo.

Visualizzazione attributi di una competenza
-------------------------------------------

Si può visualizzare i dettagli degli attributi relativi ad una competenza tramite
una *HTTP GET* all'endopoint **/rest/v3/competences/show**.

Per individuare la tipologia di competenza è possibile passare il campo **id** oppure
il campo **code** che corrisponde al codice della competenza (per esempio *S1*).

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/competences/show
      id==3
      
Il risultato sarà del tipo:

.. code-block:: json

  {
    "code": "208",
    "codeToPresence": "208",
    "competenceCodeGroup": {
        "id": 1,
        "label": "Gruppo reperibilità"
    },
    "description": "Ind.ta' Reper.ta' Festiva",
    "disabled": false,
    "id": 3,
    "limitType": "monthly",
    "limitUnit": "days",
    "limitValue": 16
  }

Le competenze hanno associato anche un *competenceCodeGroup* che ne definisce alcuni
comportamenti e limiti.


Visualizzazione dettagli di un gruppo di competenze
---------------------------------------------------

Si può visualizzare i dettagli relativi ad un gruppo di competenze tramite
una *HTTP GET* all'endopoint **/rest/v3/competencegroups/show**.

Per individuare la tipologia di competenza è possibile passare il campo **id**.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/competencegroups/show
      id==3

Il risultato sarà del tipo:

.. code-block:: bash

  {
    "competenceCodes": [
        {
            "code": "207",
            "codeToPresence": "207",
            "description": "Ind.ta' Reper.ta' Feriale",
            "id": 2
        },
        {
            "code": "208",
            "codeToPresence": "208",
            "description": "Ind.ta' Reper.ta' Festiva",
            "id": 3
        }
    ],
    "id": 1,
    "label": "Gruppo reperibilità",
    "limitType": "monthly",
    "limitUnit": "days",
    "limitValue": 16
  }


Riepilogo gruppi di competenza presenti nel sistema
---------------------------------------------------

La lista dei gruppi di competenze presenti nel sistema è disponibili tramite una
HTTP GET all'indirizzo **/rest/v3/competencegroups/list**.

Per esempio:

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/competencegroups/list 

La risposta sarà del tipo:

.. code-block:: json

  [
    {
        "id": 1,
        "label": "Gruppo reperibilità"
    },
    {
        "id": 2,
        "label": "Gruppo straordinari"
    },
    {
        "id": 3,
        "label": "Gruppo turni"
    },
    {
        "id": 4,
        "label": "Gruppo Ind.tà rischio"
    }
  ]

Competenze abilitate per una persona
------------------------------------

Le informazioni relative alle competenze abilitate per uno specifico dipendente
sono disponibili tramite una HTTP GET all'indirizzo **/rest/v3/competences/personCompetenceCodes**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*, l'anno tramite il campo *year*.
Negli esempi successivi viene utilizzato il parametro email=galileo.galilei@cnr.it,
cambiatelo con un utente appropriato per la vostra sede.

Nel caso si voglia sapere le competenze abilitate ad una certa data è possibile passare
il parametro **date**, se questo parametro non viene passato il default è la data corrente.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/competences/personCompetenceCodes
      email==galileo.galilei@cnr.it

Il JSON restituto contiene una lista delle competenze abilitate per una persona con le date
di inizio e fine abilitazione della competenza.

.. code-block:: json

  [
    {
        "beginDate": "2012-07-30",
        "competenceCode": {
            "code": "S2",
            "codeToPresence": "S2",
            "description": "Straordinario diurno nei giorni festivi o notturno nei giorni lavorativi",
            "id": 6
        },
        "endDate": null,
        "id": 71
    }
  ]
