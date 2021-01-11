Consultazione situazione giornaliera dipendenti via REST
========================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione dei periodi di 
aspettativa del personale di una sede. 
Ogni sede potrà utilizzare l'API accedendo con un utente apposito opportunamente configurato ai 
soli dati della propria sede. 

Gli esempi sono per semplicità basati sulla `httpie https://httpie.org/`_ ed utilizzano la demo 
disponibile all'indirizzo *https://epas-demo.devel.iit.cnr.it*.

Naturalmente gli stessi comandi che trovate di seguito potete farli anche nella istanza in 
produzione del vostro ente.

Permessi
========
Per poter accedere a queste interfaccie REST è necessario utilizzare un utente che abbia il ruolo 
di *Lettore Informazioni* per la sede su cui si vuole effettuare le operazioni. 
I nuovi utenti possono essere definiti dagli utenti che hanno il ruolo di *amministratore tecnico*. 

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare un nuovo utente associato alla 
vostra sede tipo *istituto_xxx_person_day_reader* (cambiate il nome o in futuro andrà in 
conflitto con quello di altri istituti) ed una volta creato l'utente assegnateli il 
ruolo *Lettore informazioni*.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Congedi per persona e anno (byPersonAndYer)
=============================================
Le informazioni relative alle aspettative di un singolo dipendente in uno spefico anno sono 
disponibili tramite una HTTP GET all'indirizzo */rest/v2/leaves/byPersonAndYear*.

La persona può essere individuata passando i parametri identificativi delle persone: 
*id, email, eppn, perseoPersonId, fiscalCode*, l'anno tramite il campo *year*.
Negli esempi successivi viene utilizzato il parametro email=galileo.galilei@cnr.it, 
cambiatelo con un utente appropriato per la vostra sede.

::
  $ http -a istituto_xxx_person_day_reader GET https://epas-demo.devel.iit.cnr.it/rest/v2/leaves/byPersonAndYear email==galileo.galilei@cnr.it year==2020

Il JSON restituto contiene una lista con le eventuali aspettative del dipendente per 
l'anno selezionato

:: 
  [
    {
      "person":
        {"id":9999,"fullname":"Galileo Galilei","fiscalCode":null,"email":"galileo.galilei@.cnr.it","number":"99999","eppn":null,"updatedAt":"2021-01-07T16:38:54.223763"},
      "code":"54A17",
      "start":"2020-01-01",
      "end":"2020-06-02"}
  ]

Congedi per ufficio e anno (byOfficeAndYear)
=============================================

Analogamente ai metodi precedenti è possibile avere le informazioni annuali di tutte le aspettative 
dei dipendenti di una sede tramite una HTTP GET all'indirizzo */rest/v2/leaves/byOfficeAndYear*.

La sede è individuata tramite il parametro *sedeId*, per esempio per l'IIT corrisponde a *233400*.
Negli esempio successivi sostituite *233400* con il *sedeId* della vostra sede.

::
  $ http -a istituto_xxx_person_day_reader GET https://epas-demo.devel.iit.cnr.it/rest/v2/leaves/byOfficeAndYear sedeId==233400 year==2020

Il JSON restituto contiene una lista con le eventuali aspettative di tutti i dipendenti della
sede per l'anno selezionato.

:: 
  [
    {
      "person":
        {"id":9999, "fullname":"Galileo Galilei", "fiscalCode":null, "email":"galileo.galilei@cnr.it", "number":"99999", "eppn":null, "updatedAt":"2021-01-07T16:38:54.223763"},
      "code":"54A17",
      "start":"2020-01-01",
      "end":"2020-06-02"
    },
    {
      "person":
        {"id":9999, "fullname":"Leonardo Fibonacci","fiscalCode":null,"email":"leonardo.fibonacci@cnr.it","number":"99998","eppn":null,"updatedAt":"2021-01-07T16:38:54.223763"},
      "code":"54A17",
      "start":"2020-03-06",
      "end":"2020-09-23"
    }
  ]
