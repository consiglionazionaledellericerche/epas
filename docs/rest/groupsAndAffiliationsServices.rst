Consultazione e Gestione Gruppi e Affiliazione via REST
=======================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione e gestione dei gruppi
e delle affiliazioni del personale ai gruppi, comprende i metodi per la visualizzazione,
la creazione, la modifica, la disattivazione e la cancellazione dei gruppi ed i metodi per la 
gestione delle associazioni "temporizzate" delle persone ai gruppi.

Ogni sede potrà utilizzare l'API accedendo con un utente apposito opportunamente configurato ai
soli dati della propria sede. 

Gli esempi sono per semplicità basati sulla `httpie <https://httpie.org/>`_ ed utilizzano la demo
disponibile all'indirizzo `https://epas-demo.devel.iit.cnr.it <https://epas-demo.devel.iit.cnr.it>`_ .

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

Group Create
------------

La creazione di una persona è possibile tramite una *HTTP POST* all'indirizzo 
**/rest/v2/persons/create**.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager 
      POST https://epas-demo.devel.iit.cnr.it/rest/v2/groups/create
      name="Gruppo Test" description="Gruppo di test" officeId=101 
      managerId=1234 externalId="gruppoTestExId"

La risposta alla creazione del gruppo sarà del tipo:

.. code-block:: json

  {
     "description": "Gruppo di test",
     "endDate": null,
     "externalId": "gruppoTestExId",
     "id": 4,
     "manager": {
        "email": "galileo.galilei@cnr.it",
        "eppn": "galileo.galilei@cnr.it",
        "fiscalCode": "GLLGLL74P10G702B",
        "fullname": "Galilei Galileo",
        "id": 1234,
        "number": "9802"
    },
    "name": "Gruppo Test",
    "office": {
        "code": "044000",
        "codeId": "223400",
        "id": 101,
        "name": "IIT - Pisa"
    },
    "people": []
  }


Le uniche cosa da notare sono la necessità di indicare il campo officeId (101 nell'esempio) ed il
campo managerId (l'id del responsabile del gruppo).

Group Show
----------

La visualizzazione dei dati di un gruppo è tramite una *HTTP* GET all'indirizzo 
**/rest/v2/groups/show**.

Per individuare il gruppo è possibile utilizzare solo il campo **id**. 

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/groups/show?id=101

.. code-block:: json

  {
     "description": "Gruppo di di test",
     "endDate": null,
     "id": 4,
     "externalId": "gruppoTestExId",
     "manager": {
        "email": "galileo.galilei@cnr.it",
        "eppn": "galileo.galilei@cnr.it",
        "fiscalCode": "GLLGLL74P10G702B",
        "fullname": "Galilei Galileo",
        "id": 1234,
        "number": "9802"
     },
     "name": "Gruppo Test",
     "office": {
        "code": "074000",
        "codeId": "225200",
        "id": 201,
        "name": "ISTI - Pisa"
    },
    "people": []
  }


La stessa GET può essere effettuata passando l'id del gruppo nel modo seguente:

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager 
     GET https://epas-demo.devel.iit.cnr.it/rest/v2/groups/show/101


Group List
----------

La lista dei gruppi di un ufficio è possibile tramite una *HTTP GET* all'indirizzo 
**/rest/v2/groups/list**.

Per individuare l'ufficio è possibile utilizzare una delle due chiavi candidate presenti sugli uffici:

  - id, codeId (corrisponde al *sede id* di Attestati).

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/groups/list?id=101

.. code-block:: json

  [
     {
        "description": "Gruppo di test",
        "endDate": null,
        "id": 4,
        "externalId": "gruppoTestExId",
        "manager": {
           "email": "galileo.galilei@cnr.it",
           "eppn": "galileo.galilei@cnr.it",
           "fiscalCode": "GLLGLL74P10G702B",
           "fullname": "Galilei Galileo",
           "id": 1234,
           "number": "9802"
        },
        "name": "Gruppo Test"
    }
  ]


Group Update
------------

La modifica di un gruppo è possibile tramite una *HTTP PUT* all'indirizzo 
**/rest/v2/groups/update**.

Per individuare il gruppo è possibile utilizzare solo il campo **id**. 

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      PUT https://epas-demo.devel.iit.cnr.it/rest/v2/groups/update?id=4
      name="Gruppo Test" description="Gruppo di test" officeId=101 managerId=1235 externalId="gruppoTestExId


Group Delete
------------

La cancellazione di un gruppo è possibile tramite una HTTP DELETE all'indirizzo **/rest/v2/groups/delete**

Per individuare il gruppo da eliminare si utilizza lo stesso parametro previsti per la show: **id**.

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      DELETE https://epas-demo.devel.iit.cnr.it/rest/v2/gropus/delete?id=4


Gestione delle Associazioni ai gruppi
-------------------------------------

La gestione delle associazione ai gruppi è effettuata con degli endpoint separati.

I metodi sono:

  - **/rest/v2/affiliations/byGroup**
  - **/rest/v2/affiliations/byPerson**
  - **/rest/v2/affiliations/show**
  - **/rest/v2/affiliations/create**
  - **/rest/v2/affiliations/update**
  - **/rest/v2/affiliations/delete**


Affiliation Create
------------------

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      POST https://epas-demo.devel.iit.cnr.it/rest/v2/affiliations/create
      groupId=4 personId=1235 percentage=80.0 beginDate=2020-10-12

.. code-block:: json

  {
    "beginDate": "2020-10-12",
    "endDate": null,
    "externalId": null,
    "group": {
        "description": "Gruppo ISTI di test",
        "endDate": null,
        "id": 4,
        "externalId": "gruppoTestExId",
        "manager": {
           "email": "galileo.galilei@cnr.it",
           "eppn": "galileo.galilei@cnr.it",
           "fiscalCode": "GLLGLL74P10G702B",
           "fullname": "Galilei Galileo",
           "id": 1234,
           "number": "9802"
        },
        "name": "Gruppo Test"
    },
    "id": 4,
    "percentage": 80.0,
    "person": {
        "email": "leonardo.fibonacci@cnr.it",
        "eppn": "leonardo.fibonacci@cnr.it",
        "fiscalCode": "FBNLRD74P10G702G",
        "fullname": "Fibonacci Leonardo",
        "id": 1235,
        "number": "9801"
    }
  }

Affiliation byGroup or byPerson
-------------------------------

**Affiliation byGroup**

.. code-block:: bash

  http -a istituto_xxx_registry_manager
    GET https://epas-demo.devel.iit.cnr.it/rest/v2/affiliations/byGroup
    id==4 includeInactive==true

.. code-block:: json

  [
    {
        "beginDate": "2020-10-12",
        "endDate": null,
        "externalId": null,
        "group": {
            "description": "Gruppo di test",
            "endDate": null,
            "id": 4,
            "externalId": "gruppoTestExId",
            "manager": {
              "email": "galileo.galilei@cnr.it",
              "eppn": "galileo.galilei@cnr.it",
              "fiscalCode": "GLLGLL74P10G702B",
              "fullname": "Galilei Galileo",
              "id": 1234,
              "number": "9802"
            },
            "name": "Gruppo Test"
        },
        "id": 4,
        "percentage": 80.0,
        "person": {
	      "email": "leonardo.fibonacci@cnr.it",
	      "eppn": "leonardo.fibonacci@cnr.it",
	      "fiscalCode": "FBNLRD74P10G702G",
	      "fullname": "Fibonacci Leonardo",
	      "id": 1235,
	      "number": "9801"
        }
    }
  ]


Il parametro *includeInactive* è opzionale, se passato ed uguale a *true* mostra anche le
affiliazioni che non sono più attive alla data corrente.


**Affiliation byPerson**

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/affiliations/byPerson
      id==4298


La persona può essere individuata passando i soliti parametri identificativi delle persone: 

  - *id, email, eppn, perseoPersonId, fiscalCode, number*.


Affiliation Show
----------------

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/affiliations/show
      id==4


Affiliation Update
------------------

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      PUT https://epas-demo.devel.iit.cnr.it/rest/v2/affiliations/update
      id==4 groupId=4 personId=1235 percentage=80.0 beginDate=2020-10-12 endDate=2021-01-31


Affiliation Delete
------------------

.. code-block:: bash

  $ http -a istituto_xxx_registry_manager
      DELETE https://epas-demo.devel.iit.cnr.it/rest/v2/affiliations/delete
      id==4
