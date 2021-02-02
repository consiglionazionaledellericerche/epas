Consultazione situazione giornaliera dipendenti via REST
========================================================

Di seguito una breve spiegazione dell'API REST relativa alla consultazione della situazione 
giornaliera delle presenze e assenze dei dipendenti di una sede. 
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

Cartellino byPerson (giornaliero e/o mensile)
=============================================
Le informazioni di un singolo giorno sulle timbrature e assenze di una persona è fruibile tramite 
una HTTP GET all'indirizzo **/rest/v3/personDays/getDaySituation**.

La persona può essere individuata passando i parametri identificativi delle persone: 
*id, email, eppn, perseoPersonId, fiscalCode*, la data tramite il campo *date*.
Negli esempi successivi viene utilizzato il parametro email=galileo.galilei@cnr.it, 
cambiatelo con un utente appropriato per la vostra sede.

::
  $ http -a istituto_xxx_person_day_reader GET https://epas-demo.devel.iit.cnr.it/rest/v3/persondays/getdaysituation email==galileo.galilei@cnr.it date==2020-10-28

Le informazioni mensili di una persona si ottengono in modo simile al precedente passando al posto 
del parametro *date* i parametri *year* e *month* con un GET all'indirizzo 
**/rest/v3/personDays/getMonthSituationByPerson**.

::
  $ http -a istituto_xxx_person_day_reader GET https://epas-demo.devel.iit.cnr.it/rest/v3/persondays/getMonthSituationByPerson email==galileo.galilei@cnr.it year==2020 month==10

Cartellino byOffice (giornaliero e/o mensile)
=============================================

Analogamente ai metodi precedenti è possibile avere le informazioni giornaliere o mensili di tutti 
i dipendenti di una sede. 
La sede è individuata tramite il parametro *sedeId*, per esempio per l'IIT corrisponde a *233400*.
Negli esempio successivi sostituite *233400* con il *sedeId* della vostra sede.

::
  $ http -a istituto_xxx_person_day_reader GET https://epas-demo.devel.iit.cnr.it/rest/v3/persondays/getdaysituationbyoffice sedeId==233400 date==2020-10-28

::
  $ http -a istituto_xxx_person_day_reader GET https://epas-demo.devel.iit.cnr.it/rest/v3/persondays/getmonthsituationbyoffice sedeId==233400 year==2020 month==10


Verifica validazione attestati
==============================

Sono disponibili anche due metodi relativi ai servizi REST della parte _certifications_, 
questi metodi sono solamente un _proxy_ rispetto ad *Attestati* nel senso che ePAS effettua 
le chiamate REST ad attestati per sapere se i cartellini sono stati validati o meno.

*QUESTI DUE METODI HANNO SENSO SOLO PER IL CNR DOVE E' PRESENTE IL SERVIZIO ATTESTATI*.

I due metodi sono:

::
  $ http -a istituto_xxx_person_day_reader GET https://epas-demo.devel.iit.cnr.it/rest/v2/certifications/getMonthValidationStatusByPerson email==galileo.galilei@cnr.it year==2020 month==10

::
  $ http -a istituto_xxx_person_day_reader GET https://epas-demo.devel.iit.cnr.it/rest/v2/certifications/getMonthValidationStatusByOffice sedeId==233400 year==2020 month==10
