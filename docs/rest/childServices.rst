Consultazione e Gestione figli/figlie dei dipendenti via REST
=============================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione e gestione dei figli
e figlie dei dipenenti che comprende i metodi per la visualizzazione, la creazione, la modifica e la
la cancellazione dei figli/figlie.

Permessi
--------

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo
di *Gestore anagrafica* per la sede su cui si vuole effettuare le operazioni (lo stesso ruolo
utilizzato per la gestione delle persone e dei gruppi).

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla
vostra sede tipo *istituto_xxx_registry_manager* (cambiate il nome o in futuro andrà in
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il
ruolo *Gestore Anagrafica*.

Inoltre è possibile utilizzare un utente di sistema con ruolo di *Gestore anagrafica* per accedere 
alle informazioni sulle persone di tutte le sedi. Questo utente è utiizzato per l'eventuale 
integrazione con sistemi esterni (per esempio di rendicontazione) a livello di tutte le sedi. 
L'utente di sistema con ruolo di *Gestore anagrafica* non può essere creato dalle singole sedi ma
può essere creato tra un utente con ruolo di *Amministratore* di ePAS.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.


Child byPerson
-----------------

La lista dei figli/figlie di una persona è fruibile tramite una HTTP GET all'indirizzo
**/rest/v2/child/byPerson**.

La persona può essere individuata passando i soliti parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/child/byPerson
      email==galileo.galilei@cnr.it

.. code-block:: json

  [
     {
        "bornDate": 1600-08-13,
        "externalId": null,
        "fiscalCode": "GLLVGN00M53G702I"
        "id": 1236,
        "name": "Virginia",
        "surname": "Galilei",        
        "updatedAt": "2021-01-15T17:55:05.966788"
     }
  ]


Children Show
-------------

La visualizzazione dei dati di un figlio/figlia è possibile tramite una HTTP GET all'indirizzo
**/rest/v2/child/show**.

Per individuare il figlio/figlia è possibile utilizzare solo il campo *id*.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/child/show 
      id==1236

.. code-block:: json

   {
      "bornDate": 1600-08-13,
      "externalId": null,
      "fiscalCode": "GLLVGN00M53G702I"
      "id": 1236,
      "name": "Virginia",
      "surname": "Galilei",        
      "updatedAt": "2021-01-15T17:55:05.966788"
   }

La stessa GET può essere effettuata passando l'id del figlio/figlia nel modo seguente:

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/child/show/1236


Children Update
---------------

La modifica di un figlio/figlia è possibile tramite una HTTP PUT all'indirizzo
**/rest/v2/child/update**.

Per individuare il figlio/figlia è possibile utilizzare solo il campo *id*.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      https://epas-demo.devel.iit.cnr.it/rest/v2/child/update id=1236 
      name=Virginia surname=Galilei fiscalCode=GLLVGN00M53G702I
      bornDate=1600-08-13 externalId=xyz

La risposta sarà del tipo:

.. code-block:: json

   {
      "bornDate": "1600-08-13",
      "externalId": "xyz",
      "fiscalCode": "GLLVGN00M53G702I",
      "id": 1236,
      "name": "Virginia",
      "surname": "Galilei",        
      "updatedAt": "2021-01-15T17:55:05.966788"
   }


Children Create
---------------

La creazione di un figlio/figlia di una persona è possibile tramite una HTTP POST all'indirizzo
**/rest/v2/child/create**.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager 
      POST https://epas-demo.devel.iit.cnr.it/rest/v2/child/create
      name=Vincenzio surname=Galilei fiscalCode=VNCGLL00M21G702U
      bornDate=1606-08-21 externalId=xyzw
      personId=1234

La risposta sarà del tipo:

.. code-block:: json

   {
      "bornDate": "1606-08-21",
      "externalId": "xyzw",
      "fiscalCode": "GLLVGN00M53G702I",
      "id": 1237,
      "name": "Vincenzio",
      "surname": "Galilei",        
      "updatedAt": "2021-01-15T17:55:05.966788"
   }

Le uniche cosa da notare sono la necessità di indicare obbligatoriamente il campo *personId*
(1234 nell'esempio).


Children Delete
---------------

La cancellazione di un figlio/figlia è possibile tramite una HTTP DELETE all'indirizzo
**/rest/v2/child/delete**.

Per individuare il figlio/figlia da eliminare si utilizza il parametro *id*.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      DELETE https://epas-demo.devel.iit.cnr.it/rest/v2/child/delete?id=1237
