Consultazione e Gestione dati Anagrafici dei dipendenti via REST
================================================================

Di seguito una breve spiegazione dell'API REST relativa alla gestione dei dati anagrafici dei
dipendenti che comprende i metodi per la visualizzazione, la creazione, la modifica e la
cancellazione delle persone.

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

Show
====

La visualizzazione dei dati di una persona è tramite una *HTTP GET* all'indirizzo
**/rest/v2/persons/show**.

Per individuare la persona è possibile utilizzare una delle cinque chiavi candidate presenti sulle
persone:

 - *id*, *email*, *eppn*, *perseoPersonId*, *fiscalCode*. 

::
  $ http -a isti_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/persons/show?email=galileo.galilei@cnr.it

::
  {
     "badges": [
         "15409"
     ],
     "email": "galileo.galilei@cnr.it",
     "eppn": "galileo.galilei@cnr.it",
     "fax": null,
     "fiscalCode": "GLLGLL74P10G702B",
     "id": 1234,
     "mobile": null,
     "name": "Galileo",
     "number": "9802",
     "office": {
       "code": "044000",
       "codeId": "223400",
       "id": 101,
       "name": "IIT - Pisa"
     },
     "othersSurnames": null,
     "qualification": 1,
     "surname": "Galilei",
     "telephone": null
  }


La stessa GET può essere effettuata passando l'id della persona nei due modi seguenti:

::
  $ http -a isti_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/persons/show/1234

::
  $ http -a isti_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/persons/show/id=1234

Oppure per esempio per codice fiscale con questa chiamata:

::
  $ http -a isti_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/persons/show?fiscalCode=GLLGLL74P10G702B

Nel caso vengano passati più parametri nella ricerca della persona l'ordine con cui viene cercata
la persona è **id, email, eppn, perseoPersonId, fiscalCode**.

Create
======

La creazione di una persona è possibile tramite una *HTTP POST* all'indirizzo
**/rest/v2/persons/create**.

`$ http -a isti_registry_manager POST https://epas-demo.devel.iit.cnr.it/rest/v2/persons/create number=99999 name=John surname=Doe email=john.doe@cnr.it qualification=5 officeId=101`

L'unica cosa da notare è che per associare la persona è necessario indicare il campo officeId (201 nel caso di epas-demo.devel.iit.cnr.it per ISTI - Pisa).

Update
======

La creazione di una persona è possibile tramite una *HTTP PUT* all'indirizzo
**/rest/v2/persons/update**.

Per individuare la persona da aggiornare si utilizzano gli stessi parametri previsti per la show:

  - **id, email, eppn, perseoPersonId, fiscalCode**.

::
  $ http -a isti_registry_manager PUT https://epas-demo.devel.iit.cnr.it/rest/v2/persons/update?email=john.doe@isti.cnr.it number=99991 name=John surname=Doe email=john.doe@cnr.it qualification=5 officeId=101


Delete
======

La cancellazione di una persona è possibile tramite una *HTTP DELETE* all'indirizzo
**/rest/v2/persons/delete**.

Per individuare la persona da eliminare si utilizzano gli stessi parametri previsti per la show:

  - **id, email, eppn, perseoPersonId, fiscalCode**.

::
  $ http -a isti_registry_manager DELETE https://epas-demo.devel.iit.cnr.it/rest/v2/persons/delete?email=john.doe@cnr.it


List
====

E' possibile avere la lista delle persone presenti nella sede tramite un *HTTP GET* all'indirizzo
**/rest/v2/persons/list**.

Per individuare l'ufficio è possibile utilizzare una delle due chiavi candidate presenti sugli uffici:
 - id, codeId (corrisponde al *sede id* di Attestati).

::
  $ http -a isti_registry_manager GET https://epas-demo.devel.iit.cnr.it/rest/v2/persons/list?id=101
