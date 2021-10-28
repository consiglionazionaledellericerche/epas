Consultazione e Gestione dei badge dei dipendenti via REST
==========================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione e gestione dei badge
che comprende i metodi per la visualizzazione, la creazione, la modifica e la cancellazione dei
badge.
I badge sono legati ad una sorgente timbratura (denominate come BadgeReader o lettore badge) ed
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


BagdeReader byOffice
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
       "code": "pisaBadge",
       "description": null,
       "enabled": true,
       "id": 1,
        "location": null,
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
       "code": "cosenzaBadge",
       "description": null,
       "enabled": true,
        "id": 2,
        "location": null,
        "username": "cosenza-xxx-yyy"
    }
  ]


