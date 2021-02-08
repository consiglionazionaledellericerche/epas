Inserimento delle timbrature via REST
=====================================

Di seguito una breve spiegazione dell'API REST relativa all'inserimento delle timbrature dei
dipendenti di una sede. 
Ogni sede potrà utilizzare l'API accedendo con un utente apposito opportunamente configurato ai 
soli dati della propria sede. 
**Questa è l'interfaccia WEB utilizzata dagli epas-client**, se possibile è consigliato utilizzare 
uno degli ePAS client opensource disponibili.

Gli esempi sono per semplicità basati sulla `httpie https://httpie.org/`_ ed utilizzano la demo 
disponibile all'indirizzo *https://epas-demo.devel.iit.cnr.it*.

Naturalmente gli stessi comandi che trovate di seguito potete farli anche nella istanza in 
produzione del vostro ente.

Permessi
========

Per poter accedere a questa interfaccia REST è necessario utilizzare le credenziali associate
ad una **Sorgente Timbratura** della propria sede. Eventualmente verificare nella documentazione
di ePAS come creare le Sorgenti Timbrature.

Su https://epas-demo.devel.iit.cnr.it potete per esempio creare una nuova Sorgente Timbratura
utente associato alla vostra sede, con uno username tipo *client_timbrature_sede_xxx*.

L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.

Inserimento di una timbratura
=============================

Per effettuare l'inserimento di una timbratura è necessario effettuare una *HTTP PUT* all'endpoint
**/stampingsfromclient/create**. La *PUT* deve contenere nel body un json con i campi della
timbratura che sono:

  - *operazione*: è un numero obbligatorio. 0 indica un'entrata, 1 un uscita.
  - *causale*: è una stringa opzionale. che contiene la causale della timbraura 
    I due valori attualmente gestiti sono *pausaPranzo* e *motiviDiServizio*.
  - *anno*, *mese*, *giorno*, *ora*, *minuti*: sono tutti campi numerici obbligatori
  - *matricolaFirma*: è una stringa obbligatoria. Indica il numero di badge
  - *terminale* o *lettore*: è una stringa opzionale. Può contenere la *zona di timbratura*, utilizzata
    in alcuni rari casi per gestire le timbrature continuative tra due zone definite
  - *note*: è una stringa opzionale.

Il metodo restituisce uno dei seguenti codici HTTP:

 - *200*: timbratura inserita correttamente
 - *400*: body delle richiesta HTTP non presente
 - *404*: numero badge non trovato per la sorgente timbratura utilizzare per l'inserimento
 - *409*: in caso timbratura già presente (il sistema non ne inserisce un'altra uguale)

