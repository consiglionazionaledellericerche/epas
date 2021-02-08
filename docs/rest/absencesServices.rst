Consultazione ed inserimento assenze via REST
=============================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione ed inserimento 
delle assenze dei dipendenti di una sede. 
Ogni sede potrà utilizzare l'API accedendo con un utente apposito opportunamente configurato ai 
soli dati della propria sede. 

Gli esempi sono per semplicità basati sulla `httpie https://httpie.org/`_ ed utilizzano la demo 
disponibile all'indirizzo *https://epas-demo.devel.iit.cnr.it*.

Naturalmente gli stessi comandi che trovate di seguito potete farli anche nella istanza in 
produzione del vostro ente.

Permessi
========

Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo
di *Rest Client*, oppure di *Lettore Informazioni* per le operazioni di sola lettura.
I nuovi utenti possono essere definiti dagli utenti che hanno il ruolo di *amministratore tecnico*.

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla
vostra sede tipo *istituto_xxx_rest_client* oppure *istituto_xxx_personday_reader*
(cambiate il nome o in futuro andrà in conflitto con quello di altri istituti) ed una volta creato
l'utente assegnateli il ruolo *Rest Client*.

Inoltre è possibile utilizzare un utente di sistema con ruolo di *Gestore Assenze* per accedere 
alle informazioni sulle assenze di tutte le sedi. Questo utente è utiizzato per l'eventuale 
integrazione con sistemi esterni (per esempio di rendicontazione) a livello di tutte le sedi. 
L'utente di sistema con ruolo di *Gestore Assenze* non può essere creato dalle singole sedi ma
può essere creato tra un utente con ruolo di *Amministratore* di ePAS.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Assenze di un dipendente in un periodo
======================================

Le informazioni relative al assenze di un singolo dipendente in uno spefico intervallo temporatale
anno sono disponibili tramite una HTTP GET all'indirizzo
**/rest/absences/absencesInPeriod**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode*. 
Il periodo può essere specificato tramite le variabili *begin* ed *end* con data nel formato
*YYYY-MM-dd*.

Negli esempi successivi viene utilizzato il parametro email=galileo.galilei@cnr.it,
cambiatelo con un utente appropriato per la vostra sede.

::
  $ http -a istituto_xxx_rest_client GET https://epas-demo.devel.iit.cnr.it/rest/absences/absencesInPeriod email==galileo.galilei@cnr.it begin==2020-12-01 end==2021-12-31

La risposta sarà del tipo:

::
      {
        "absenceCode": "37",
        "date": "2020-12-21",
        "description": "ferie anno precedente (dopo il 31/8)",
        "name": "Galileo",
        "surname": "Galilei"
    },
    {
        "absenceCode": "94",
        "date": "2020-12-28",
        "description": "festività soppresse (ex legge 937/77)",
        "name": "Galileo",
        "surname": "Galilei"
    },

Verifica della possibilità di inserire un'assenza
=================================================

È possibile verificare se è possibile inserire un'assenza per un dipendente senza effettuare
l'effettivo inserimento. Questa operazione è fruibile utilizzando l'endpoint
**/rest/absences/checkAbsence**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode*. 
Il periodo può essere specificato tramite le variabili *begin* ed *end* con data nel formato
*YYYY-MM-dd*.
Il codice dell'assenza deve essere indicato con il parametro *absenceCode*.

::
  $ http -a istituto_xxx_rest_client GET https://epas-demo.devel.iit.cnr.it/rest/absences/checkAbsence email==galileo.galilei@cnr.it begin==2021-02-02 end==2021-02-03 absenceCode==31

Il risultato conterrà i giorni in cui sarebbe possibile inserire l'assenza, con un formato
tipo il seguente.

::
  [
     {
        "absenceCode": "31",
        "date": "2021-02-02",
        "isOk": true,
        "reason": ""
     },
     {
        "absenceCode": "31",
        "date": "2021-02-03",
        "isOk": true,
        "reason": ""
     }
  ]


Inserimento nuova assenza
=========================

Analagamente al metodo precedente per controllare un'assenza è possibile effettuare l'operazione di 
inserimento di una assenza tramite una *HTTP PUT* all'endpoint **/rest/absences/insertAbsence**.

La persona può essere individuata passando i parametri identificativi delle persone:
*id, email, eppn, perseoPersonId, fiscalCode*. 
Il periodo può essere specificato tramite le variabili *begin* ed *end* con data nel formato
*YYYY-MM-dd*.
Il codice dell'assenza deve essere indicato con il parametro *absenceCode*.
Nel caso di tratti di un'assenza oraria è possibile indicare i campi *hours* and *minutes*.

::
    $ http -a istituto_xxx_rest_client GET https://epas-demo.devel.iit.cnr.it/rest/absences/insertAbsence email==galileo.galilei@cnr.it begin==2021-02-02 end==2021-02-03 absenceCode==31

Il risultato sarà un json contenente i codici effettivamente inseriti nel sistema nei vari giorni.
Con un risultato tipo il seguente.

::
  [
     {
        "absenceCode": "31",
        "date": "2021-02-02",
        "isOk": true,
        "reason": ""
     },
     {
        "absenceCode": "31",
        "date": "2021-02-03",
        "isOk": true,
        "reason": ""
     }
  ]

Per esempio nel caso di inserimento di giorni di ferie in un periodo che comprende giorni festivi
il sistema inserirà i codice relativi alle ferie solo nei giorni feriali.
