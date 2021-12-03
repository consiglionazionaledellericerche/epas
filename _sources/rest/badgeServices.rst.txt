Consultazione e Gestione dei badge dei dipendenti via REST
==========================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione e gestione dei badge
che comprende i metodi per la visualizzazione, la creazione, la modifica e la cancellazione dei
badge.
I badge sono legati ad una sorgente timbratura (denominata come BadgeReader o lettore badge) ed
anche al gruppo badge (citato anche come BadgeSystem). 
Le informazioni relative alle sorgenti timbrature ed ai gruppi badge della propria sede sono
fondamentali quando si va a creare un nuovo badge, per questo motivo nell'interfaccia REST
sono state inserite le funzionalità per mostrare i BagdeReader e BagdeSystem.

Permessi
--------

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo
di *Gestore badge* per la sede su cui si vuole effettuare le operazioni (lo stesso ruolo
utilizzato per la gestione delle persone e dei gruppi).

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla
vostra sede tipo *istituto_xxx_badge_manager* (cambiate il nome o in futuro andrà in
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il
ruolo *Gestore Badge*.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.


BadgeReader byOffice
--------------------

La lista delle sorgenti timbrature (BadgeReader) di una sede è fruibile tramite una HTTP GET
all'indirizzo **/rest/v3/badgeReaders/byOffice**.

Per individuare l'ufficio è possibile utilizzare una delle due chiavi candidate presenti sugli uffici:
  - id, codeId (corrisponde al *sede id* di Attestati).

.. code-block:: bash

  $ http -a istituto_xxx_badge_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/badgeReaders/byOffice
      id==1

.. code-block:: json

  [
     {
        "badgeSystems": [
          {
             "enabled": true,
             "id": 1,
             "name": "Badge IIT Pisa",
             "office": {
               "code": "044000",
               "codeId": "223400",
               "id": 1,
               "name": "IIT - Pisa",
               "updatedAt": "2021-02-08T09:00:50.648975"
             }
          }
       ],
       "code": "pisa-xxx-yyy",
       "description": null,
       "enabled": true,
       "id": 1,
        "location": "Area della Ricerca di Pisa",
        "username": "pisa-xxx-yyy"
     },
     {
       "badgeSystems": [
         {
           "enabled": true,
           "id": 2,
           "name": "Badge IIT Cosenza",
           "office": {
             "code": "044001",
             "codeId": "223410",
             "id": 2,
             "name": "IIT - UOS Cosenza",
             "updatedAt": "2021-02-08T09:00:50.648975"
           }
         }
       ],
       "code": "cosenza-xxx-yyy",
       "description": null,
       "enabled": true,
        "id": 2,
        "location": "Sede di Cosenza",
        "username": "cosenza-xxx-yyy"
    }
  ]


BadgeReader show
--------------------

Per ogni BadgeReader è possibile avere le informazioni ad esso collegate tramite una HTTP GET
all'indirizzo **/rest/v3/badgeReaders/show**.

Per individuare il BadgeReader è necessario utilizzare il suo **id**.

.. code-block:: bash

  $ http -a istituto_xxx_badge_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/badgeReaders/show id==1

.. code-block:: json

  {
    "badgeSystems": [
        {
            "badgeReaders": [
                {
                    "code": "pisa-xxx-yyy",
                    "enabled": true,
                    "id": 1,
                    "username": "pisa-xxx-yyy"
                }
            ],
            "description": "",
            "enabled": true,
            "id": 1,
            "name": "Badge IIT Pisa",
            "office": {
                "code": "044000",
                "codeId": "223400",
                "id": 1,
                "name": "IIT - Pisa",
                "updatedAt": "2021-02-08T09:00:50.648975"
            }
        }
    ],
    "badges": [
        {
            "code": "1234111",
            "id": 99,
            "person": {
                "email": "galileo.galileo@cnr.it",
                "eppn": "galileo.galilei@cnr.it",
                "fiscalCode": "GLLGLL74P10G702B",
                "fullname": "Gallilei Gallileo",
                "id": 1234,
                "number": "9802"
            }
        },
        {
            "code": "1235111",
            "id": 140,
            "person": {
                "email": "leonardo.fibonacci@cnr.it",
                "eppn": "leonardo.fibonacci@cnr.it",
                "fiscalCode": "FBCLNR74P10G702B",
                "fullname": "Fibonacci Leonardo",
                "id": 12345,
                "number": "9801"
            }
        }],
      "code": "pisa-xxx-yyy",
      "description": null,
      "enabled": true,
      "id": 1,
      "location": "Area della Ricerca di Pisa",
      "username": "pisa-xxx-yyy"

  }


BadgeSystem byOffice
--------------------

La lista dei gruppi badge (BadgeSystem) di una sede è fruibile tramite una HTTP GET
all'indirizzo **/rest/v3/badgeSystems/byOffice**.

Per individuare l'ufficio è possibile utilizzare una delle due chiavi candidate presenti sugli uffici:
  - id, codeId (corrisponde al *sede id* di Attestati).

.. code-block:: bash

  $ http -a istituto_xxx_badge_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/badgeSystems/byOffice
      id==1

.. code-block:: json

  [
    {
        "badgeReaders": [
            {
                "code": "pisa-xxx-yyy",
                "enabled": true,
                "id": 1,
                "username": "pisa-xxx-yyy"
            }
        ],
        "description": "Gruppo Badge sede di Pisa",
        "enabled": true,
        "id": 1,
        "name": "Badge IIT Pisa",
        "office": {
            "code": "044000",
            "codeId": "223400",
            "id": 1,
            "name": "IIT - Pisa",
            "updatedAt": "2021-02-08T09:00:50.648975"
        }
    }
  ]
  
BadgeSystem show
--------------------

Per ogni BadgeSystem è possibile avere le informazioni ad esso collegate tramite una HTTP GET
all'indirizzo **/rest/v3/badgesystems/show**.

Per individuare il BadgeSystem è necessario utilizzare il suo **id**.

.. code-block:: bash

  $ http -a istituto_xxx_badge_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/badgeSystems/show id==1


Badge byOffice
--------------

La lista di tutti i badge associati ad una sede (cioè quelli associati a tutti gruppi badge della sede)
è possibile ottenerla una HTTP GET all'indirizzo **/rest/v3/badges/byOffice**.

Per individuare l'ufficio è possibile utilizzare una delle due chiavi candidate presenti sugli uffici:
  - id, codeId (corrisponde al *sede id* di Attestati).
  
.. code-block:: bash

  $ http -a istituto_xxx_badge_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/badges/byOffice codeId==223400


Badge byBadgeSystem
-------------------

La lista di tutti i badge associati ad un gruppo è possibile ottenerla una HTTP GET all'indirizzo **/rest/v3/badges/byBadgeSystem**.

Per individuare il BadgeSystem è necessario utilizzare il suo **id**.
 
.. code-block:: bash

  $ http -a istituto_xxx_badge_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/badges/byBadgeSystem codeId==223400


Badge byBadgeSystem
-------------------

La lista di tutti i badge associati ad una persone è possibile ottenerla una HTTP GET
all'indirizzo **/rest/v3/badges/byPerson**.

Per individuare la persona da aggiornare si utilizzano gli stessi parametri previsti per la show:

  - **id, email, eppn, perseoPersonId, fiscalCode, number**.
 
.. code-block:: bash

  $ http -a istituto_xxx_badge_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/badges/byPerson eppn==galileo.galilei@cnr.it

.. code-block:: json

  [
    {
        "badgeReaderId": 1,
        "badgeSystemId": 1,
        "code": "9802",
        "id": 161,
        "person": {
            "email": "galileo.galilei@cnr.it",
            "eppn": "galileo.galilei@cnr.it",
            "fiscalCode": "GLLGLL74P10G702B",
            "fullname": "Galileo Galilei",
            "id": 1234,
            "number": "9802"
        }
    }
  ]

Badge show
-----------

Per ogni Badge è possibile avere le informazioni ad esso collegate tramite una HTTP GET
all'indirizzo **/rest/v3/badges/show**.

Per individuare il Badge è necessario utilizzare il suo **id**.

.. code-block:: bash

  $ http -a istituto_xxx_badge_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/badges/show id==161


Badge create
------------

La creazione di un badge è possibile tramite una *HTTP POST* all'indirizzo
**/rest/v2/badges/create** passando un json contenente i quattro campi obbligatori per la creazione di un badge:

 - **code**, **personId**, **badge_reader_id**, **badgeSystemId**

.. code-block:: bash

  $ http -a istituto_xxx_badge_manager
      POST https://epas-demo.devel.iit.cnr.it/rest/v2/badges/create
      code=9801bis personId=1234 badgeReaderId=1 badgeSystemId=1

Badge Update
------------

L'aggiornamento di un badge è possibile tramite una *HTTP PUT* ad un indirizzo del tipo
**/rest/v2/badges/update?id={id_badge_da_aggiornare}**.

Per individuare il Badge è necessario utilizzare il suo **id** e passarlo come parametro nell'url, non nel JSON.

Nel corpo della PUT, come JSON, è possibile passare solo il campo **code** che è l'unico modificabile.
Per cambiare il campo **badgeReaderId** o il campo **badgeSystemId** è necessario eliminare il badge
e re-inserirlo con i dati corretti. 

.. code-block:: bash

  $ http -a istituto_xxx_badge_manager
      PUT https://epas-demo.devel.iit.cnr.it/rest/v2/badges/update
      id==161 code=9801tris


Badge Delete
------------

La cancellazione di un badge è possibile tramite una *HTTP DELETE* all'indirizzo
**/rest/v2/persons/delete**.

Per individuare il Badge è necessario utilizzare il suo **id** e passarlo come parametro nell'url, non nel JSON.

.. code-block:: bash

  $ http -a istituto_xxx_badge_manager
      DELETE https://epas-demo.devel.iit.cnr.it/rest/v2/badges/delete?id=310
