Consultazione ed inserimento assenze via REST
=============================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione ed inserimento 
delle assenze dei dipendenti di una sede. 

Permessi
--------

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo
di *Gestore Assenze* o *Rest Client*, oppure di *Lettore Informazioni* per le operazioni di sola lettura.
I nuovi utenti possono essere definiti dagli utenti che hanno il ruolo di *amministratore tecnico*.

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla
vostra sede tipo *istituto_xxx_absence_manager* oppure *istituto_xxx_personday_reader*
(cambiate il nome o in futuro andrà in conflitto con quello di altri istituti) ed una volta creato
l'utente assegnateli il ruolo *Gestore Assenze*.

Inoltre è possibile utilizzare un utente di sistema con ruolo di *Gestore Assenze* per accedere 
alle informazioni sulle assenze di tutte le sedi. Questo utente è utiizzato per l'eventuale 
integrazione con sistemi esterni (per esempio di rendicontazione) a livello di tutte le sedi. 
L'utente di sistema con ruolo di *Gestore Assenze* non può essere creato dalle singole sedi ma
può essere creato tra un utente con ruolo di *Amministratore* di ePAS.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Assenze di un dipendente in un periodo
--------------------------------------

Le informazioni relative al assenze di un singolo dipendente in uno spefico intervallo temporatale
anno sono disponibili tramite una HTTP GET all'indirizzo
**/rest/absences/absencesInPeriod**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*. 
Il periodo può essere specificato tramite le variabili *begin* ed *end* con data nel formato
*YYYY-MM-dd*.

Negli esempi successivi viene utilizzato il parametro email=galileo.galilei@cnr.it,
cambiatelo con un utente appropriato per la vostra sede.

.. code-block:: bash

  $ http -a istituto_xxx_absence_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/absences/absencesInPeriod 
      email==galileo.galilei@cnr.it begin==2020-12-01 end==2021-12-31

La risposta sarà del tipo

.. code-block:: json

  [
    {
      "absenceCode": "37",
      "absenceTypeId": 293,
      "date": "2020-12-21",
      "description": "ferie anno precedente (dopo il 31/8)",
      "hasAttachment": false,
      "id": 107109,
      "name": "Galileo",
      "surname": "Galilei"
    },
    {
      "absenceCode": "94",
      "absenceTypeId": 380,
      "date": "2020-12-28",
      "description": "festività soppresse (ex legge 937/77)",
      "hasAttachment": false,
      "id": 107110,
      "name": "Galileo",
      "surname": "Galilei"
    }
  ]

Verifica della possibilità di inserire un'assenza
-------------------------------------------------

È possibile verificare se è possibile inserire un'assenza per un dipendente senza effettuare
l'effettivo inserimento. Questa operazione è fruibile utilizzando l'endpoint
**/rest/absences/checkAbsence**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*. 
Il periodo può essere specificato tramite le variabili *begin* ed *end* con data nel formato
*YYYY-MM-dd*.
Il codice dell'assenza deve essere indicato con il parametro *absenceCode*.

.. code-block:: bash

  $ http -a istituto_xxx_absence_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/absences/checkAbsence 
      email==galileo.galilei@cnr.it begin==2021-02-02 end==2021-02-03 absenceCode==31

Il risultato conterrà i giorni in cui sarebbe possibile inserire l'assenza, con un formato
tipo il seguente:

.. code-block:: json

  [
     {
        "absenceCode": "31",
        "absenceTypeId": 297,
        "date": "2021-02-02",
        "isOk": true,
        "reason": ""
     },
     {
        "absenceCode": "31",
        "absenceTypeId": 297,
        "date": "2021-02-03",
        "isOk": true,
        "reason": ""
     }
  ]

Inserimento nuova assenza
-------------------------

Analogamente al metodo precedente per controllare un'assenza è possibile effettuare l'operazione di 
inserimento di una assenza tramite una *HTTP PUT* all'endpoint **/rest/absences/insertAbsence**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*. 
Il periodo può essere specificato tramite le variabili *begin* ed *end* con data nel formato
*YYYY-MM-dd*.
Il codice dell'assenza deve essere indicato con il parametro *absenceCode*.
Nel caso di tratti di un'assenza oraria è possibile indicare i campi *hours* and *minutes*.

.. code-block:: bash

  $ http -a istituto_xxx_absence_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/absences/insertAbsence 
      email==galileo.galilei@cnr.it begin==2021-02-02 end==2021-02-03 absenceCode==31 note=="Una nota"

Il risultato sarà un json contenente i codici effettivamente inseriti nel sistema nei vari giorni.
Con un risultato tipo il seguente:

.. code-block:: json

  [
     {
        "absenceCode": "31",
        "absenceTypeId": 297,
        "date": "2021-02-02",
        "isOk": true,
        "note": "Una nota",
        "reason": ""
     },
     {
        "absenceCode": "31",
        "absenceTypeId": 297,
        "date": "2021-02-03",
        "isOk": true,
        "note": "Una nota",
        "reason": ""
     }
  ]

Per esempio nel caso di inserimento di giorni di ferie in un periodo che comprende giorni festivi
il sistema inserirà i codice relativi alle ferie solo nei giorni feriali.

Inserimento di un giorno di ferie/permesso con codice assenza calcolato da ePAS
-------------------------------------------------------------------------------

Al fine di utilizzare la funzionalità già presente nell'interfacccia WEB di ePAS che calcola in 
autonomia il codice di ferie più vantaggioso da inserire per il cliente (tra i 31, 32 e 94), è disponibile
un metodo REST per l'inserimento delle assenze di tipo ferie in cui non viene passato il codice da utilizzare.
L'inserimento di una assenza di tipo ferie è possibile tramite una *HTTP PUT* all'endpoint 
**/rest/absences/insertVacation**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*. 
Il periodo può essere specificato tramite le variabili *begin* ed *end* con data nel formato
*YYYY-MM-dd*.

.. code-block:: bash

  $ http -a istituto_xxx_absence_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/absences/insertVacation
      email==galileo.galilei@cnr.it begin==2021-03-05 end==2021-03-08

Il risultato sarà un json contenente i codici effettivamente inseriti nel sistema nei vari giorni.
Con un risultato tipo il seguente.

.. code-block:: json

  [
      {
        "absenceCode": "31",
        "absenceTypeId": 297,
        "date": "2021-03-05",
        "description": "Ferie anno precedente",
        "hasAttachment": false,
        "id": 107159,
        "name": "Galileo",
        "surname": "Galilei"
    },
    {
        "absenceCode": "31",
        "absenceTypeId": 297,
        "date": "2021-03-08",
        "description": "Ferie anno precedente",
        "hasAttachment": false,
        "id": 107160,
        "name": "Galileo",
        "surname": "Galilei"
    }
  ]

Anche con questo metodo, nel caso di inserimento di giorni di ferie in un periodo che comprende giorni festivi,
il sistema inserirà i codice relativi alle ferie solo nei giorni feriali.

Impostare le note in un'assenza
-------------------------------

È possibile impostare le note in assenza già presente tramite una HTTP POST all'indirizzo
**/rest/absences/setNote**.

Per individuare l'assenza sui cui impostare le si utilizza il parametro *id* dell'assenza, il testo
delle note deve essere indicato nel campo *note*.

.. code-block:: bash

  $ http -a istituto_xxx_absence_manager 
      DELETE https://epas-demo.devel.iit.cnr.it/rest/absences/setNote 
      id==107109 note=="Note inserite via REST"

Cancellazione di un'assenza
---------------------------

La cancellazione di un'assenza è possibile tramite una HTTP DELETE all'indirizzo
**/rest/absences/delete**.

Per individuare l'assenza da eliminare si utilizza il parametro *id* dell'assenza.

.. code-block:: bash

  $ http -a istituto_xxx_absence_manager 
      DELETE https://epas-demo.devel.iit.cnr.it/rest/absences/delete 
      id==107109

Cancellazione delle assenze di uno stesso tipo in un periodo
------------------------------------------------------------

È possibile cancellare più assenze di una persona che siano dello stesso tipo specificando
i limiti temporali di inizio e fine delle assenze da cancellare.
Questa operazione può essere seguita con una *HTTP DELETE* all'endpoint **/rest/absences/deleteAbsencesInPeriod**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode, number*. 
Il periodo può essere specificato tramite le variabili *begin* ed *end* con data nel formato
*YYYY-MM-dd*.
Il codice dell'assenze da cancellare deve essere indicato con il parametro *absenceCode*.

.. code-block:: bash

  $ http -a istituto_iit_absence_manager DELETE https://epas-demo.devel.iit.cnr.it/rest/absences/deleteAbsencesInPeriod email==galileo.galilei@cnr.it begin==2021-02-15 end==2021-02-16 absenceCode==31


Scaricamento allegato di un'assenza
-----------------------------------

Le assenze possono avere un allegato (per esempio un file PDF con dichiarazioni del dipendente o un file con
la certificazione di una visita medifica).
L'allegato può essere scaricato con una *HTTP GET* all'indirizzo **/rest/absences/attachment**.

Per individuare l'assenza di cui prelevare l'allegato si utilizza il parametro *id* dell'assenza.

.. code-block:: bash

  $ http -a istituto_iit_absence_manager 
      GET https://epas-demo.devel.iit.cnr.it/rest/absences/attachment
      id==107122

La risposta sarà del tipo:

.. code-block::

  HTTP/1.1 200 OK
  Content-Disposition: attachment; filename="assenza-Galilei-Galileo-2021-02-12.pdf"
  Content-Length: 410830
  Content-Type: application/pdf
  Date: Fri, 19 Feb 2021 10:28:47 GMT

  +-----------------------------------------+
  | NOTE: binary data not shown in terminal |
  +-----------------------------------------+

Nel caso l'allegato non sia presente verrà restituito un codice *HTTP 404*.


Inserimento di un allegato ad un'assenza
----------------------------------------

Per inserire l'allegato è possibile utilizzare una *HTTP POST* all'indirizzo **/rest/absences/addAttachment**.

Per individuare l'assenza a cui associare l'allegato si utilizza il parametro *id* dell'assenza.
La *HTTP POST* deve essere di tipo *Multipart/form-data* e l'allegato deve essere passato con il nome *file*.

Esempio:

.. code-block:: bash

  $ http -a istituto_iit_absence_manager --form 
      POST https://epas-demo.devel.iit.cnr.it/rest/absences/addAttachment
      id==107122 file@assenza-Galilei-Galileo-2021-02-15.pdf

Nel caso sia già presente un allegato quello precedente viene sovrascritto.

Da notare che nell'esempio sopra si è utilizzata l'opzione **--form** ed il parametro 
**file@assenza-Galilei-Galileo-2021-02-15.pdf**, dove *file* indica il nome utilizzato nella POST
per passare allegato e *@assenza-Galilei-Galileo-2021-02-15.pdf* il riferimento al file locale da
caricare sul server tramite queste API.


Cancellazione di un allegato di un'assenza
------------------------------------------

Per eliminare l'allegato è possibile utilizzare una *HTTP DELETE* all'indirizzo **/rest/absences/addAttachment**.

Per individuare l'assenza di cui rimuovere l'allegato si utilizza il parametro *id* dell'assenza.

Esempio:

.. code-block:: bash

  $ http -a istituto_iit_absence_manager 
      DELETE https://epas-demo.devel.iit.cnr.it/rest/absences/addAttachment
      id==107122

Nel caso non fosse presente un'allegato viene restituito con codice *HTTP 404*, altrimenti un codice *HTTP 200* se
la cancellazione va a buon fine.
