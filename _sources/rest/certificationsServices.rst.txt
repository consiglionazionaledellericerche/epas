Consultazione situazione riepiloghi/attestati mensili via REST
==============================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione della rendicontazione 
mensile delle *assenze / competenze / buoni pasto / ore di formazione* dei dipendenti di una sede. 

Permessi
--------

Per poter accedere a queste interfacce REST è necessario utilizzare un utente che abbia il ruolo 
di *Lettore Informazioni* per la sede su cui si vuole effettuare le operazioni. 
I nuovi utenti possono essere definiti dagli utenti che hanno il ruolo di *amministratore tecnico*. 

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla 
vostra sede tipo *istituto_xxx_person_day_reader* (cambiate il nome o in futuro andrà in 
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il 
ruolo *Lettore informazioni*.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Riepilogo situazione mensile assenze/competenze/buoni pasto/ore formazione
--------------------------------------------------------------------------

Le informazioni relative alle situazione mensile assenze/competenze/buoni pasto/ore formazione
di un singolo dipendente in uno spefico anno sono disponibili tramite una HTTP GET all'indirizzo
**/rest/v2/certifications/getMonthSituation**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*, l'anno tramite il campo *year*, il mese con il
campo *month*.
Negli esempi successivi viene utilizzato il parametro email=galileo.galilei@cnr.it,
cambiatelo con un utente appropriato per la vostra sede.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/certifications/getMonthSituation 
      email==galileo.galilei@cnr.it year==2020 month==10

La risposta sarà del tipo:

.. code-block:: json

  {
     "absences": [
        {
            "code": "COVID19BP",
            "from": "2020-12-01",
            "justifiedTime": 432,
            "justifiedType": "assign_all_day",
            "to": "2020-12-02"
        },
        {
            "code": "37",
            "from": "2020-12-03",
            "justifiedTime": 432,
            "justifiedType": "all_day",
            "to": "2020-12-03"
        }
     ]
     "competences": [
        {
            "code": "207",
            "quantity": 3
        },
        {
            "code": "208",
            "quantity": 3
        }
     ],
     "fullName": "Galileo Galilei",
     "mealTickets": [
        { 
           "quantity": 10 
        }
      ],
    "month": 12,
    "number": "9802",
    "trainingHours": [],
    "year": 2020
  }

Per ottenere lo stesso riepilogo ma per tutti i dipendenti di una sede è possibile utilizzare il metodo:

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/certifications/getMonthSituationByOffice
      sedeId==223400 year==2020 month==10

Il risultato sarà una lista dei riepilogi strutturati come quello dell'esempio precedente per il
singolo dipendente.


Verifica validazione attestati
------------------------------

Sono disponibili due metodi relativi ai servizi REST della parte *certifications*, 
questi metodi sono solamente un _proxy_ rispetto ad *Attestati* nel senso che ePAS effettua 
le chiamate REST ad attestati per sapere se i cartellini sono stati validati o meno.

**QUESTI DUE METODI HANNO SENSO SOLO PER IL CNR DOVE E' PRESENTE IL SERVIZIO ATTESTATI**.

I due metodi sono:

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/certifications/getMonthValidationStatusByPerson
      email==galileo.galilei@cnr.it year==2020 month==10

Questo metodo ritorna il valore **true** se l'attestato è stato validato, **false** altrimenti.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader 
      GET https://epas-demo.devel.iit.cnr.it/rest/v2/certifications/getMonthValidationStatusByOffice
      sedeId==223400 year==2020 month==10

Il risultato è del tipo:

.. code-block:: json

  {
     "allCertificationsValidated": false,
     "notValidatedPersons": [
        {
           "email": "galileo.galilei@cnr.it",
           "eppn": "galileo.galilei@cnr.it",
           "fax": null,
           "fiscalCode": "GLLGLL74P10G702B",
           "id": 1234,
        }
     ],
    "validatedPersons": [
        {
           "email": "leonardo.fibonacci@cnr.it",
           "eppn": "leonardo.fibonacci@cnr.it",
           "fiscalCode": "FBNLRD74P10G702G",
           "fullname": "Fibonacci Leonardo",
           "id": 1235,
           "number": "9801"
       }
    ]
  }

**ATTENZIONE QUESTO METODO PUO' ESSERE MOLTO LENTO, perché effettua una chiamata ad Attestati per ogni dipendente**
