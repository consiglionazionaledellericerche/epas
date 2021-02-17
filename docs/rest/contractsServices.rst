Consultazione e Gestione Contratti dei dipendenti via REST
==========================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione e gestione dei contratti
che comprende i metodi per la visualizzazione, la creazione, la modifica, la cancellazione e
l'impostazione della continuità dei contratti.

Ogni sede potrà utilizzare l'API accedendo con un utente apposito opportunamente configurato ai
soli dati della propria sede. 

Gli esempi sono per semplicità basati sulla `httpie https://httpie.org/`_ ed utilizzano la demo
disponibile all'indirizzo *https://epas-demo.devel.iit.cnr.it*.

Naturalmente gli stessi comandi che trovate di seguito potete farli anche nella istanza in
produzione del vostro ente.

Permessi
========

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo
di *Gestore anagrafica* per la sede su cui si vuole effettuare le operazioni (lo stesso ruolo
utilizzato per la gestione delle persone e dei gruppi).

u https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla
vostra sede tipo *istituto_xxx_registry_manager* (cambiate il nome o in futuro andrà in
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il
ruolo *Gestore Anagrafica*.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Contract byPerson
=================
La lista dei contratti di una persona è fruibile tramite una HTTP GET all'indirizzo
**/rest/v2/contracts/byPerson**.

La persona può essere individuata passando i soliti parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode*.

::
  $ http -a istituto_xxx_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/contracts/byPerson?email=galileo.galilei@cnr.it

::
  [
     {
        "beginDate": "2018-12-27",
        "endContract": null,
        "endDate": null,
        "externalId": null,
        "id": 4284,
        "onCertificate": true,
        "person": {
           "email": "galileo.galilei@cnr.it",
           "eppn": "galileo.galilei@cnr.it",
           "fiscalCode": "GLLGLL74P10G702B",
           "fullname": "Galilei Galileo",
           "id": 1234,
           "number": "9802"
        },
        "previousContract": null,
        "workingTimeTypes": [
           {
              "beginDate": "2018-12-27",
              "endDate": null,
              "workingTimeType": {
                 "description": "Normale",
                 "disabled": false,
                 "externalId": null,
                 "horizontal": true,
                 "id": 1,
                 "office": null
              }
           }
        ]
     }
  ]


Contract Show
=============

La visualizzazione dei dati di un contratto è possibile tramite una HTTP GET all'indirizzo
**/rest/v2/contracts/show**.

Per individuare il contratto è possibile utilizzare solo il campo *id*.

::
  $ http -a istituto_xxx_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/contracts/show?id=4284

::
  {
     "beginDate": "2018-12-27",
     "endContract": null,
     "endDate": null,
     "externalId": null,
     "id": 4284,
     "onCertificate": true,
     "person": {
         "email": "galileo.galilei@cnr.it",
         "eppn": "galileo.galilei@cnr.it",
         "fiscalCode": "GLLGLL74P10G702B",
         "fullname": "Galilei Galileo",
         "id": 4222,
         "number": "9802"
     },
     "previousContract": null,
     "workingTimeTypes": [
        {
            "beginDate": "2018-12-27",
            "endDate": null,
            "workingTimeType": {
                "description": "Normale",
                "disabled": false,
                "externalId": null,
                "horizontal": true,
                "id": 1,
                "office": null
            }
        }
     ]
  }


La stessa GET può essere effettuata passando l'id del gruppo nei due modi seguenti:

::
  $ http -a istituto_xxx_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/contracts/show/4284

::
  $ http -a istituto_xxx_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/contracts/show/id=4284


Contract Update
===============

La modifica di un contratto è possibile tramite una HTTP PUT all'indirizzo
**/rest/v2/contracts/update**.

Per individuare il contratto è possibile utilizzare solo il campo *id*.

::
  $ http -a istituto_xxx_registry_manager https://epas-demo.devel.iit.cnr.it/rest/v2/contracts/update?id=4284 beginDate=2018-12-27 endDate=2020-10-20 personId=1234

La risposta sarà del tipo:

::
  {
     "beginDate": "2018-12-27",
     "endContract": null,
     "endDate": "2020-10-20",
     "externalId": null,
     "id": 4284,
     "onCertificate": true,
     "person": {
         "email": "galileo.galilei@cnr.it",
         "eppn": "galileo.galilei@cnr.it",
         "fiscalCode": "GLLGLL74P10G702B",
         "fullname": "Galilei Galileo",
         "id": 4222,
         "number": "9802"
     },
     "workingTimeTypes": [
        {
            "beginDate": "2018-12-27",
            "endDate": null,
            "workingTimeType": {
                "description": "Normale",
                "disabled": false,
                "externalId": null,
                "horizontal": true,
                "id": 1,
                "office": null
            }
        }
     ]
     "previousContract": null
  }


Contract Create
===============

La creazione di una persona è possibile tramite una HTTP POST all'indirizzo
**/rest/v2/contracts/create**.

::
  $ http -a istituto_xxx_registry_manager POST https://epas-demo.devel.iit.cnr.it/rest/v2/contracts/create beginDate=2020-10-21 personId=1234

La risposta sarà del tipo:

::
  {
     "beginDate": "2020-10-21",
     "endContract": null,
     "endDate": null,
     "externalId": null,
     "id": 4678,
     "onCertificate": true,
     "person": {
         "email": "galileo.galilei@cnr.it",
         "eppn": "galileo.galilei@cnr.it",
         "fiscalCode": "GLLGLL74P10G702B",
         "fullname": "Galilei Galileo",
         "id": 4222,
         "number": "9802"
     },
     "workingTimeTypes": [
        {
            "beginDate": "2020-10-21",
            "endDate": null,
            "workingTimeType": {
                "description": "Normale",
                "disabled": false,
                "externalId": null,
                "horizontal": true,
                "id": 1,
                "office": null
            }
        }
     ]
    "previousContract": null
}

Le uniche cosa da notare sono la necessità di indicare obbligatoriamente il campo *personId*
(1234 nell'esempio) ed il campo *beginDate*.
È anche possibile impostare un campo *workingTimeTypeId* che contiene l'id che riferisce il tipo
di orario di lavoro del dipendente da associare a questo contratto.
La lista dei tipi di orario di lavoro è ancora disponibile con un apposito servizio REST.
Se il campo workingTimeTypeId non viene passato il contratto viene creato con tipo orario di
lavoro "*Normale*", quello con 7:12 giornalieri.

Sia nella creazione che nell'aggiornamento sono presenti i controlli che le date del contratto non
si intersechino con quelle di altri contratti già esistenti.

Continuazione di due contratti consecutivi
==========================================

È possibile impostare che un contratto è continuativo rispetto al precedente e che qundi ne erediti
le ferie non godute precedenti. 
Questa funzionalità è da utilizzare per esempio per alcune stabilizzazioni dove il dipendente
mantiene dal precedente contratto la situazione delle ferie non godute.
Per impostare e rimuovere che un contratto è continuativo rispetto al precedente è possibile
utilizzare con un HTTP PUT i metodi:

  - **/rest/v2/contract/setPreviousContract**
  - **/rest/v2/contract/unsetPreviousContract**

::
  $ http -a istituto_xxx_registry_manager PUT https://epas-demo.devel.iit.cnr.it/rest/v2/contract/setPreviousContract?id=4678

::
  $ http -a istituto_xxx_registry_manager PUT https://epas-demo.devel.iit.cnr.it/rest/v2/contract/unsetPreviousContract?id=4678


Contract Delete
===============

La cancellazione di un contratto è possibile tramite una HTTP DELETE all'indirizzo
**/rest/v2/contract/delete**.

Per individuare il gruppo da eliminare si utilizza il parametro *id* del contratto.

::
  $ http -a istituto_xxx_registry_manager DELETE https://epas-demo.devel.iit.cnr.it/rest/v2/contract/delete?id=4678
