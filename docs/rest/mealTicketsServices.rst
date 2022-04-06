Gestione dei buoni pasto via REST
==================================

Di seguito una breve spiegazione dell'API REST relativa alle operazioni sui buoni pasto dei
dipendenti di una sede. 
Ogni sede potrà utilizzare l'API, accedendo con un utente apposito opportunamente configurato, ai 
soli dati della propria sede. 

Permessi
--------

Per poter accedere a questa interfaccia REST è necessario utilizzare un utente che abbia il ruolo di
**Gestore buoni pasto** per la sede su cui si vuole effettuare le operazioni (lo stesso ruolo utilizzato
per la gestione delle persone e dei gruppi).

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla
vostra sede tipo *istituto_xxx_mealtickets_manager* (cambiate il nome o in futuro andrà in
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il
ruolo *Gestore buoni pasto*.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.


Inserimento di un blocchetto di buoni pasto per Persona
-------------------------------------------------------

Per effettuare l'inserimento di un blocchetto di buoni pasto per singola persona è necessario effettuare una *HTTP POST* all'endpoint
**/mealtickets/createByPerson**.
La *POST* deve contenere nel body un json con i campi del bloccheto dei buoni pasto che sono obbligatori:

  - *codeBlock*: è un numero obbligatorio. indica il codice del blocchetto dei buoni pasto da inserire nel sistema.
  - *blockType*, è una stringa obbligatoria *papery* oppure *electronic* indica la tipologia del blocchetto da inserire.
  - *first*: è un intero obbligatorio. Indica il numero del primo buono pasto.
  - *last*: è un intero obbligatorio. Indica il numero dell'ultimo buono pasto.
  - *expiredDate*: è una data obbligatoria. indica la data di scadenza del blocchetto di buoni pasto.
  - *deliveryDate*: è una data obbligatoria. indica la data in cui è stato consegnato il blocchetto di buoni pasto.
  - *adminId*: è l'id in ePAS dell'amministrativo che ha consegnato i buoni pasto

Inoltre è necessario specificare nel json uno o più dei seguenti campi che servono ad identificare la persona a cui
assegnare il blocchetto dei buoni pasto:

  - *personId*: numero che rappresenta l'Id associato alla persona a cui aggiungere il blocchetto dei buoni pasto.
  - *fullname*: stringa che rappresenta il nome della persona a cui aggiungere il blocchetto dei buoni pasto.
  - *fiscalCode*: stringa che rappresenta il codice fiscale della persona a cui aggiungere il blocchetto dei buoni pasto.
  - *email*: stringa che rappresenta l'email della persona a cui aggiungere il blocchetto dei buoni pasto.
  - *number*: stringa che rappresenta la matricola della persona a cui aggiungere il blocchetto dei buoni pasto.
  - *eppn*: stringa che rappresenta eppn della persona a cui aggiungere il blocchetto dei buoni pasto.
  - *personPerseoId*: numero che rappresenta il perseoId della persona a cui aggiungere il blocchetto dei buoni pasto.


Un esempio è il seguente:

.. code-block:: bash

  $ http -a istituto_xxx_mealtickets_manager
      POST https://epas-demo.devel.iit.cnr.it/rest/v3/mealtickets/createByPerson
      personId=4605 codeBlock=5789 blockType=papery first=1 last=10 expiredDate=2022-05-31 deliveryDate=2022-04-01 adminId=123

Il metodo restituisce una risposta vuota con uno dei seguenti codici HTTP di risposta:

 - *200*: blocchetto dei buoni pasto inserito correttamente
 - *400*: body delle richiesta HTTP non presente

Inserimento di un blocchetto di buoni pasto con contractId
----------------------------------------------------------

Per effettuare l'inserimento di un blocchetto di buoni pasto avendo il contractId è necessario effettuare una *HTTP POST* all'endpoint
**/mealtickets/create**.
La *POST* deve contenere nel body un json con i campi del bloccheto dei buoni pasto che sono:

  - *contractId*: è un numero obbligatorio. indica l'id del contratto associato a una persona.
  - *codeBlock*: è un numero obbligatorio. indica il codice del blocchetto dei buoni pasto da inserire nel sistema.
  - *blockType*, è una stringa obbligatoria *papery* oppure *electronic* indica la tipologia del blocchetto da inserire.
  - *first*: è un intero obbligatorio. Indica il numero del primo buono pasto.
  - *last*: è un intero obbligatorio. Indica il numero dell'ultimo buono pasto.
  - *expiredDate*: è una data obbligatoria. indica la data di scadenza del blocchetto di buoni pasto.
  - *deliveryDate*: è una data obbligatoria. indica la data in cui è stato consegnato il blocchetto di buoni pasto.
  - *adminId*: è l'id in ePAS dell'amministrativo che ha consegnato i buoni pasto

Un esempio è il seguente:

.. code-block:: bash

  $ http -a istituto_xxx_mealtickets_manager
      POST https://epas-demo.devel.iit.cnr.it/rest/v3/mealtickets/create
      contractId=1017 codeBlock=5789 blockType=papery first=1 last=10 expiredDate=2022-05-31 deliveryDate=2022-04-01  adminId=123

Il metodo restituisce una risposta vuota con uno dei seguenti codici HTTP di risposta:

 - *200*: blocchetto dei buoni pasto inserito correttamente
 - *400*: body delle richiesta HTTP non presente

MealTicket List
---------------

La lista di tutti i blocchetti dei buoni pasto associati ad una persona (contratto) è possibile ottenerla una HTTP GET
all'indirizzo **/rest/v3/mealtickets/list**.
La *GET* deve avere come parametri obbligatori i seguenti campi:

  - *contractId*: è un numero obbligatorio. indica l'id del contratto associato a una persona.

.. code-block:: bash

  $ http -a istituto_xxx_mealtickets_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/mealtickets/list contractId==1017

La risposta sarà del tipo

.. code-block:: json

    [
        {
            "adminId": 111,
            "blockType": "papery",
            "codeBlock": "5789",
            "contractId": 1017
            "first": 6,
            "last": 10,
            "adminId": 123,
            "person": {
                "email": "kinzica.desismondi@iit.cnr.it",
                "eppn": "kinzica.desismondi@cnr.it",
                "fiscalCode": null,
                "fullname": "De Sismondi Kinzica",
                "id": 966,
                "number": "9535"
            }
        },
        {
            "blockType": "papery",
            "codeBlock": "123456",
            "first": 1,
            "last": 22,
            "person": {
                "email": "kinzica.desismondi@iit.cnr.it",
                "eppn": "kinzica.desismondi@cnr.it",
                "fiscalCode": null,
                "fullname": "De Sismondi Kinzica",
                "id": 966,
                "number": "9535"
            }
        }
    ]

MealTicket Show
---------------
La visualizzazione dei dati di un blocchetto di buoni pasto è tramite una *HTTP* GET all'indirizzo
**/rest/v3/mealtickets/show**.

Per individuare il blocchetto la *GET* deve avere come parametri obbligatori i seguenti campi:

  - *contractId*: è un numero obbligatorio. indica l'id del contratto associato a una persona.
  - *codeBlock*: è un numero obbligatorio. indica il codice del blocchetto dei buoni pasto da inserire nel sistema.

.. code-block:: bash

  $ http -a istituto_xxx_mealtickets_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/mealtickets/show contractId==1017 codeBlock==123456

La risposta sarà del tipo

.. code-block:: json

    [
        {
            "blockType": "papery",
            "codeBlock": "123456",
            "first": 1,
            "last": 22,
            "person": {
                "email": "kinzica.desismondi@iit.cnr.it",
                "eppn": "kinzica.desismondi@cnr.it",
                "fiscalCode": null,
                "fullname": "De Sismondi Kinzica",
                "id": 966,
                "number": "9535"
            }
        }
    ]

MealTicket Delete
-----------------

L'eliminazione di un blocchetto di buoni pasto dal contratto di un utente o di alcuni buoni pasto ad esso accosiati,
è possibile tramite una *HTTP DELETE* all'indirizzo
**/rest/v3/mealtickets/delete**.

Per individuare il blocchetto dei buoni pasto da eliminare è necessario utilizzare i seguenti campi
che sono parametri obbligatori:

  - *contractId*: è un numero obbligatorio. indica l'id del contratto associato a una persona.
  - *codeBlock*: è un numero obbligatorio. indica il codice del blocchetto dei buoni pasto da inserire nel sistema.
  - *first*: è un intero obbligatorio. Indica il numero del primo buono pasto.
  - *last*: è un intero obbligatorio. Indica il numero dell'ultimo buono pasto.

.. code-block:: bash

  $ http -a istituto_xxx_mealtickets_manager
      DELETE https://epas-demo.devel.iit.cnr.it/rest/v3/mealtickets/delete
      contractId==1017 codeBlock==5789 first==1 last==22

MealTicket Convert
------------------

E' possibile modificare la tipologia di un blocchetto di buoni pasto (da cartaceo a elettronico e viceversa)
tramite una *HTTP* GET all'indirizzo
**/rest/v3/mealtickets/convert**.

Per individuare il blocchetto la *GET* deve avere come parametri obbligatori i seguenti campi:

  - *contractId*: è un numero obbligatorio. indica l'id del contratto associato a una persona.
  - *codeBlock*: è un numero obbligatorio. indica il codice del blocchetto dei buoni pasto da inserire nel sistema.

.. code-block:: bash

  $ http -a istituto_xxx_mealtickets_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/mealtickets/convert contractId==1017 codeBlock==123456

MealTicket returnBlock
----------------------

E' possibile effettuare la riconsegna del blocchetto di buoni pasto (intero o parte di esso) alla sede centrale
tramite una *HTTP* GET all'indirizzo
**/rest/v3/mealtickets/returnBlock**.

Per individuare il blocchetto la *GET* deve avere come parametri obbligatori i seguenti campi:

  - *contractId*: è un numero obbligatorio. indica l'id del contratto associato a una persona.
  - *codeBlock*: è un numero obbligatorio. indica il codice del blocchetto dei buoni pasto da inserire nel sistema.
  - *first*: è un intero obbligatorio. Indica il numero del primo buono pasto.
  - *last*: è un intero obbligatorio. Indica il numero dell'ultimo buono pasto.

.. code-block:: bash

  $ http -a istituto_xxx_mealtickets_manager
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/mealtickets/returnBlock
      contractId==1017 codeBlock==5789 first==1 last==5
