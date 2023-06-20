Consultazione situazione giornaliera dipendenti via REST
========================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione della situazione 
giornaliera delle presenze e assenze dei dipendenti di una sede. 

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


Cartellino byPerson (giornaliero e/o mensile)
---------------------------------------------

Le informazioni di un singolo giorno sulle timbrature e assenze di una persona è fruibile tramite 
una HTTP GET all'indirizzo **/rest/v3/personDays/getDaySituation**.

La persona può essere individuata passando i parametri identificativi delle persone: 
*id, email, eppn, perseoPersonId, fiscalCode, number*, la data tramite il campo *date*.
Negli esempi successivi viene utilizzato il parametro email=galileo.galilei@cnr.it, 
cambiatelo con un utente appropriato per la vostra sede.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/persondays/getdaysituation
      email==galileo.galilei@cnr.it date==2020-10-28

Le informazioni mensili di una persona si ottengono in modo simile al precedente passando al posto 
del parametro *date* i parametri *year* e *month* con un GET all'indirizzo 
**/rest/v3/personDays/getMonthSituationByPerson**.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/persondays/getMonthSituationByPerson
      email==galileo.galilei@cnr.it year==2020 month==10

Cartellino byOffice (giornaliero e/o mensile)
---------------------------------------------

Analogamente ai metodi precedenti è possibile avere le informazioni giornaliere o mensili di tutti 
i dipendenti di una sede.
La sede è individuata tramite il parametro *sedeId*, per esempio per l'IIT corrisponde a *223400*.
Negli esempio successivi sostituite *223400* con il *sedeId* della vostra sede.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/persondays/getdaysituationbyoffice
      sedeId==223400 date==2020-10-28

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/persondays/getmonthsituationbyoffice
      sedeId==223400 year==2020 month==10

Timbrature per lavoro fuori sede o per motivi di servizio fuori sede con luogo e/o motivazione
----------------------------------------------------------------------------------------------

Sono disponibili due endpoint per prelevare le informazioni relative alla timbrature per lavori
fuori o per motivi di servizio di servizio fuori sede con impostato luogo e/o motivazione.
Queste informazioni possono per esempio essere utilizzate da un eventuale sistema esterno di
rendicontazione dei progetti.

Per prelevare la lista delle giornate con timbrature per lavoro fuori sede o per motivi di
servizio con luogo e/o motivazione di un dipendente è possibile utilizzare una GET alll'endpoint
**/rest/v3/personDays/offSiteWorkByPersonAndMonth**.

La persona può essere individuata passando i parametri identificativi delle persone: 
*id, email, eppn, perseoPersonId, fiscalCode, number*, i parametri *year* e *month* sono utilizzati per
individuare l'anno ed il mese.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/personDays/offSiteWorkByPersonAndMonth
      email==galileo.galilei@cnr.it year==2021 month==02
  
Il risultato sarà del tipo:

.. code-block:: json

  [
    {
        "absences": [],
        "date": "2021-02-04",
        "difference": -222,
        "id": 363239,
        "isHoliday": false,
        "isTicketAvailable": false,
        "progressive": -222,
        "stampings": [
            {
                "date": "2021-02-04T09:00:00",
                "id": 398918,
                "markedByAdmin": false,
                "markedByEmployee": false,
                "note": null,
                "place": "Torre di Pisa",
                "reason": "Verificare ipotesi caduta dei gravi",
                "stampType": "LAVORO_FUORI_SEDE",
                "way": "in"
            },
            {
                "date": "2021-02-04T12:30:00",
                "id": 398919,
                "markedByAdmin": false,
                "markedByEmployee": false,
                "note": null,
                "place": "Torre di Pisa",
                "reason": "Terminato esperimento caduta dei gravi",
                "stampType": "LAVORO_FUORI_SEDE",
                "way": "out"
            }
        ],
        "timeAtWork": 210
    }
  ]

Analogamente è possibile ottenere le stesse informazioni ma per tutto il personale dipendente
di una sede utilizzando una GET all'indirizzo **/rest/v3/personDays/offSiteWorkByOfficeAndMonth**.

La sede è individuata tramite il parametro *sedeId*.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/personDays/offSiteWorkByOfficeAndMonth
      sedeId==223400 year==2021 month==02

Un esempio di risultato è il seguente:

.. code-block:: json

  [
    {
        "absences": [],
        "date": "2021-02-04",
        "difference": -222,
        "id": 363239,
        "isHoliday": false,
        "isTicketAvailable": false,
        "person": {
            "email": "galileo.galilei@cnr.it",
            "eppn": "galileo.galilei@cnr.it",
            "fiscalCode": "GLLGLL74P10G702B",
            "fullname": "Galilei Galileo",
            "id": 1234,
            "number": "9802"
        },
        "progressive": -222,
        "stampings": [
            {
                "date": "2021-02-04T09:00:00",
                "id": 398918,
                "markedByAdmin": false,
                "markedByEmployee": false,
                "note": null,
                "place": "Torre di Pisa",
                "reason": "Controllore accelerazione di gravità di due corpi",
                "stampType": "LAVORO_FUORI_SEDE",
                "way": "in"
            },
            {
                "date": "2021-02-04T12:30:00",
                "id": 398919,
                "markedByAdmin": false,
                "markedByEmployee": false,
                "note": null,
                "place": "Torre di Pisa",
                "reason": "Terminato esperimento caduta dei gravi",
                "stampType": "LAVORO_FUORI_SEDE",
                "way": "out"
            }
        ],
        "timeAtWork": 210
    },
    {
        "absences": [],
        "date": "2021-02-08",
        "difference": -432,
        "id": 363244,
        "isHoliday": false,
        "isTicketAvailable": false,
        "person": {
            "email": "leonardo.fibonacci@cnr.it",
            "eppn": null,
            "fiscalCode": "FBNLRD74P10G702G",
            "fullname": "Fibonacci Leonardo",
            "id": 1235,
            "number": "9801"
        },
        "progressive": -432,
        "stampings": [
            {
                "date": "2021-02-08T08:00:00",
                "id": 398920,
                "markedByAdmin": false,
                "markedByEmployee": false,
                "note": null,
                "place": "Lungarno Pisano",
                "reason": "Esperimento su successioni numeriche",
                "stampType": "LAVORO_FUORI_SEDE",
                "way": "in"
            }
        ],
        "timeAtWork": 0
    }
  ]

Timbrature per motivi di servizio
---------------------------------

È disponibile un endpoint per prelevare le informazioni relative alla timbrature con
causale motivi di servizio.

Per prelevare la lista delle giornate con timbrature con causale motivi di
servizio di una sede è possibile utilizzare una GET alll'endpoint
**/rest/v3/personDays/serviceExitByPersonAndMonth**.

La sede è individuata tramite il parametro *sedeId*.

.. code-block:: bash

  $ http -a istituto_xxx_person_day_reader
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/personDays/serviceExitByOfficeAndMonth
      sedeId==223400 year==2022 month==10

La risposta ottenuta è analoga a quella descritta nel paragrafo precedente relativamente
al metodo /rest/v3/personDays/offSiteWorkByOfficeAndMonth.


Modifica decisioni su assegnazione buono pasto in un giorno
-----------------------------------------------------------

È disponibile un endpoint per impostare la politica di assegnazione di un buono pasto in un giorno.
A differenza degli altri endpoint di questa parte delle API REST, ci sono dei ruoli da utilizzare 
che permettono di modificare i dati del sistema (e non solo di visualizzarli).

Per poter accedere a questo endpoint REST è necessario utilizzare un utente che abbia il ruolo 
di *Gestore Assenze* per la sede su cui si vuole effettuare le operazioni, oppure il ruolo di sistema
*Gestore assenze* (per poter operare sugli utenti di tutte le sedi).

Per modificare la politica di assegnazione di un buono pasto è possibile effettuare 
una *HTTP POST* all'endpoint **/rest/v3/persondays/setMealTicketBehavior**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*. 
La data deve essere specificata tramite il campo *date* e deve essere nel formato *YYYY-MM-dd*.
Le tipologie di assegnazione di buono pasto sono da indicare attraverso il campo *mealTicketDecision*, 
i valori che si possono assegnare sono:

 - *COMPUTED* (Calcolato)
 - *FORCED_TRUE* (Forzato si) 
 - *FORCED_FALSE* (Forzato no)

E' anche possibile passare un campo *note* con cui assegnare delle note al giorno indicato.

.. code-block:: bash

  $ http -a istituto_xxx_absence_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/v3/personDays/setMealTicketBehavior 
      email==galileo.galilei@cnr.it date==2023-06-20 mealTicketDecision==FORCED_TRUE 
      note=='ne aveva proprio diritto oggi'

Il risultato sarà un json contenente le info principali sul giorno appena modificato.