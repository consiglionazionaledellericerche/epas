Inserimento ordini e rimborsi di missione REST
==============================================

Di seguito una breve spiegazione dell'API REST utilizzata per l'inserimento via REST delle missioni
da parte di un applicazione esterna che gestisce in autonomia le missioni e comunica ad ePAS gli
ordini di missioni, i rimborsi di missione e gli eventuali annullamenti di un ordine di missione.

Le funzionalità di questi servizi REST sono pensati per essere utilizzati per integrare le
informazioni sulle missioni di tutte le sedi non per una singola sede.

Se una sede singola volesse inserire via REST le missioni in autonomia dovrebbe utilizzare le
funzionalità di inserimento delle assenze via REST descritte nella pagina
:doc:`Consultazione ed inserimento assenze via REST <absencesServices>`.

Nel caso del CNR le missioni vengono le informazioni sugli ordini e rimborsi di missione vengono
inseriti dalla piattaforma *Missioni* all'interno di un topic *AMQP* e le informazioni vengono
lette dal topic *AMQP* da parte di un componente aggiuntivo che si occupa di leggere dalla coda ed
inviare le informazione via REST ad ePAS tramite questo endpoint REST.

Permessi
--------

Per poter accedere a queste interfaccie REST è possibile utilizzare un utente di sistema con ruolo
di sistema **Gestore Missioni**.
Questo utente sarà utiizzato per l'integrazione con sistemi esterni di gestione delle missioni
a livello di tutte le sedi.
L'utente di sistema con ruolo di *Gestore Missioni* non può essere creato dalle singole sedi ma
può essere creato tra un utente con ruolo di *Amministratore* di ePAS.
 
L'autenticazione da utilizzare è come per gli altri servizi REST quella *Basic Auth*.


Inserimento ordine/rimborso/annullamento missione
--------------------------------------------------

Per effettuare l'inserimento di un ordine/rimborso/annullamento missione effettuare una *HTTP POST*
all'endpoint **/rest/missions/amqpreceiver**. 

La *POST* deve contenere nel body un json con i campi della dell'ordine/rimborso/annullamento.

  - *dest_missione*: valore obbligatorio. I possibili valori sono *ITALIA* e *ESTERA*.
    Questo campo è utilizzato per decidere se inserire un codice tipo *92* o *92E*.
  - *tipo_missione*: valore obbligatorio. I valori attualmente gestiti sono *ORDINE*, 
    *RIMBORSO* e *ANNULLAMENTO*. Questo è il campo che differenzia gli ordini di missione dai
    rimborsi e dagli annullamenti.
  - *codice_sede*: valore obbligatorio. Corrisponde al codice della sede a cui a cui afferisce
    il dipendente di questo ordine/rimborso/annullamento. La sede deve aver attivato il parametro.
    *Abilitazione integrazione con piattaforma Missioni*.
  - *id*: valore obbligatorio. Id univoco nel sistema esterno per individuare questo
    ordine/rimborso/annullamento 
  - *matricola*: una stringa obbligatoria. Indica il numero di matricola del dipendente
  - *data_inizio*: valore obbligatorio. Contiene la data di inizio dell'ordine/rimborso comprensiva
    dell'orario.
  - *data_fine*: valore obbligatorio. Contiene la data di fine dell'ordine/rimborso comprensiva
    dell'orario.
  - *id_ordine*: valore opzionale. Utilizzato nei rimborsi e negli annullamenti per riferire l'id
    dell'ordine di missione a cui si riferisce questo rimborso/annullamento.
  - *anno*: valore opzionale. Se indicato viene inserite nelle note dell'assenza per missione.
  - *numero*: valore opzionale. Se indicato viene inserite nelle note dell'assenza per missione.

Un esempio dei campi JSON per un *ordine di missione* sono i seguenti:

.. code-block:: json

  {
    "dest_missione":"ITALIA", "tipo_missione":"ORDINE",
     "codice_sede":"222300", "id":30064974, "matricola":"9802",
     "data_inizio":"2021-04-16T08:00:56.698+0200",
     "data_fine":"2021-04-16T18:00:56.699+0200",
     "id_ordine":null, "anno":2019, "numero":396
  }

Un esempio dei campi JSON per un *rimborso di missione* sono i seguenti:

.. code-block:: json

  {
    "dest_missione":"ITALIA", "tipo_missione":"RIMBORSO",
    "codice_sede":"222300", "id":46512, "matricola":"9802",
    "data_inizio":"2021-04-16T08:00:56.698+0200",
    "data_fine":"2021-04-16T18:00:56.699+0200",
    "id_ordine":30064974, "anno":2019, "numero":396
  }

Durante l'inserimento dell'ordine/rimborso di missione se il sistema trova che esiste già
un ordine/rimborso in ePAS con l'id passato allora il sistema non fa niente e restituisce
un codice *HTTP 409*.
Il codice *HTTP 409* viene restituito anche nel caso siano già presenti dei codici di tipo
*92* nei giorni della missione e che questi stiano stati inseriti manualmente dall'amministratore
del personale.

In caso di inserimento della missione/rimborso/annullamento con successo il comportamento del
sistema con i codici inseriti è descritto nella documentazione 
:doc:`Integrazione ePAS - Missioni <../missions/index>`.

