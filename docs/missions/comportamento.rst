Comportamento del servizio
==========================
Al completamento del flusso per l'autorizzazione della missione, le informazioni sui giorni e gli
orari di inizio e fine missione verranno inviati ad ePAS che inserirà, sul cartellino mensile del
dipendente, i giorni di assenza per missione con l’opportuno codice di assenza:
92, 92M, 92NG (v. :doc:`Regole del servizio <regole>` per info per il dettaglio del comportamento
dei codici).
Allo stesso modo, quando il dipendente inserirà la richiesta di rimborso missione, Missioni invierà
ad ePAS i dati relativi ai giorni e agli orari di inizio e fine missione.
ePAS verificherà eventuali cambiamenti nei giorni rispetto a quanto salvato in fase di ordine di
missione ed effettuerà gli opportuni aggiornamenti dei codici di assenza sul cartellino del dipendente.

Casistica
---------

Qui di seguito è definito il comportamento che ePAS segue nell'inserire i codici di assenza sulla
base delle informazioni che Missioni invia:

Supponiamo di avere un intervallo (v. :doc:`Configurazione del servizio <configurazione>` per info)
impostato in questo modo: **Inizio: 08.30 / Fine: 19:30**
In base agli estremi impostati in questo intervallo, il sistema opererà il seguente controllo in
fase di inserimento dei codici di assenza:

* se **l'orario di inizio missione** è *precedente* all'inizio dell'attività lavorativa, ePAS
  inserirà un codice **92** a giustificazione dell'intera giornata
* se **l'orario di inizio missione** è *compreso* tra l'inizio e la fine dell'attività
  lavorativa, ePAS inserirà:

  * un **92** se la differenza tra l'orario di inizio missione e la fine della giornata
    lavorativa è superiore o uguale all'orario di lavoro del dipendente in quel giorno (es.: se
    in quel giorno il dipendente parte alle 9 di mattina e ha un orario di lavoro normale a 7 ore
    e 12 minuti, la differenza con la fine dell'orario lavorativo è superiore alle 7 ore e 12
    minuti quindi inserisco un 92).
  * un **92M** se la differenza tra l'orario di inizio missione e la fine della giornata 
    lavorativa è inferiore all'orario di lavoro del dipendente in quel giorno (es.: se in quel
    giorno il dipendente parte alle 17 e ha un orario di lavoro normale, la differenza con la
    fine dell'orario lavorativo è di 2 ore e 30 minuti, quindi inserisco un 92M).

* se l’orario di inizio missione è successivo alla fine dell’attività lavorativa, ePAS inserirà
  un codice 92NG non utile ai fini del riconoscimento di orario lavorativo
* se **l'orario di fine missione** è *successivo* alla fine dell'attività lavorativa, ePAS
  inserirà un codice **92** a giustificazione dell'intera giornata
* se **l'orario di fine missione** è *compreso* tra l'inizio e la fine dell'attività lavorativa,
  ePAS inserirà:

  * un **92** se la differenza tra l'orario di fine missione e l'inizio dell'attività lavorativa
    è superiore o uguale all'orario di lavoro del dipendente in quel giorno (es.: se in quel
    giorno il dipendente arriva alle 18 e ha un orario di lavoro normale a 7 ore e 12 minuti,
    la differenza con l'inizio dell'orario lavorativo è superiore alle 7 ore e e 12 minuti quindi
    inserisco un 92).
  * un **92M** se la differenza tra l'orario di fine missione e l'inizio della giornata
    lavorativa è inferiore all'orario di lavoro del dipendente in quel giorno (es.: se in quel
    giorno il dipendente arriva alle 11 e ha un orario di lavoro normale, la differenza con
    l'inizio dell'orario lavorativo è di 2 ore e 30 minuti, quindi inserisco un 92M).

* se **l'orario di fine missione** è *precedente* all'inizio dell'attività lavorativa, ePAS
  inserirà un codice 92NG non utile ai fini del riconoscimento di orario lavorativo

Nei casi in cui viene inserito un 92M ePAS, in fase di invio dati ad Attestati, invierà:

  * un 92HX dove X è la quantità oraria più prossima alla quantità giustificata dal codice 92M.
    Es.: se 92M giustifica 2 ore e 20 minuti, ePAS invierà ad Attestati un codice 92H2

Nei casi in cui viene inserito un 92NG, ePAS, in fase di invio dati ad Attestati, invierà:

  * un 92. In questo caso ad Attestati interessa che venga inviato un codice che corrisponda
    all’inizio della missione del dipendente, tuttavia ai fini del calcolo dell’orario di lavoro,
    il sistema invia il 92 solo per far corrispondere i dati di Missioni con quelli di Attestati.

Info aggiuntive per amministratori del personale
------------------------------------------------

L’amministratore del personale potrà effettuare eventuali modifiche del cartellino del dipendente,
aggiornando i codici di assenza inseriti automaticamente per i singoli giorni di missione a seconda
di eventuali informazioni aggiuntive che il sistema non può conoscere quali 
ad esempio:

  * la durata del trasferimento
  * l'aggiunta di ore di lavoro in missione
  * la destinazione della missione (nei casi, ad esempio, di missioni all’estero in altro
    continente, il volo di ritorno potrebbe far terminare la missione la mattina presto. 
    In questo caso, in accordo con l’USGTEP, potrebbe essere possibile considerare l'inserimento
    del codice missione 92 anche per la giornata di arrivo.
