Consultazione situazione ferie e permessi dei dipendenti via REST
=================================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione della situazione 
delle ferie residue e maturate dei dipendenti di una sede.

Permessi
--------

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo 
di *Lettore Informazioni* oppure di *Gestore Assenze* per la sede su cui si vuole effettuare le operazioni. 
I nuovi utenti possono essere definiti dagli utenti che hanno il ruolo di *amministratore tecnico*. 

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla 
vostra sede tipo *istituto_xxx_person_day_reader* (cambiate il nome o in futuro andrà in 
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il 
ruolo *Lettore informazioni* (oppure *Gestore Assenze*).

Inoltre è possibile utilizzare un utente di sistema con ruolo di *Gestore Assenze* per accedere 
alle informazioni sulle assenze di tutte le sedi. Questo utente è utiizzato per l'eventuale 
integrazione con sistemi esterni (per esempio di rendicontazione) a livello di tutte le sedi. 
L'utente di sistema con ruolo di *Gestore Assenze* non può essere creato dalle singole sedi ma
può essere creato tra un utente con ruolo di *Amministratore* di ePAS.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.


Situazione ferie e permessi, residui e maturati di un dipendente in un anno
---------------------------------------------------------------------------

Le informazioni relative al assenze di un singolo dipendente in uno spefico intervallo temporatale
anno sono disponibili tramite una HTTP GET all'indirizzo
**/rest/v3/vacations/byPersonAndYear**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*.
Il periodo può essere specificato tramite il parametro *year*. Il parametro *year* è opzionale,
nel caso non venga specificato viene mostrata la situazione dell'anno corrente.

Nell'esempio successivo viene utilizzato il parametro email=galileo.galilei@cnr.it,
cambiatelo con un utente appropriato per la vostra sede.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/vacations/byPersonAndYear
      email==galileo.galilei@cnr.it year==2021

La risposta sarà del tipo:

.. code-block:: json

  [
    {
       "contract": {
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
          "updatedAt": "2021-01-15T17:55:05.966788"
        },
        "currentYear": {
           "accrued": 4,
           "total": 28,
           "usable": 28,
           "usableTotal": 28,
           "used": 0,
           "year": 2021
        },
        "date": "2021-02-18",
        "lastYear": {
           "accrued": 28,
           "total": 28,
           "usable": 12,
           "usableTotal": 12,
           "used": 16,
           "year": 2020
        },
        "permissions": {
           "accrued": 1,
           "total": 4,
           "usable": 4,
           "usableTotal": 4,
           "used": 0,
           "year": 2021
        },
        "year": 2021
    }
  ]

La lista restituita contiene tutti i contratti della persona selezionata nell'anno richiesto.
Per ogni contratto sono mostrate le ferie dell'anno selezionato, dell'anno precedente ed i permessi
legge dell'anno selezionato.
