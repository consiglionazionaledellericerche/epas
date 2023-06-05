Regole ed esempi del servizio
=============================

Per poter permettere la corretta integrazione tra i due sistemi è stato necessario aggiungere un
parametro di configurazione che permettesse al sistema ePAS di avere un riferimento orario in fase
di inserimento dei codici di assenza per missione oraria o giornaliera
 
**Intervallo di attività lavorativa in sede per giorni missione** 
(v. :doc:`Configurazione del servizio <configurazione>` per info).
Per prima cosa sono stati rivisti, di concerto con l’Ufficio Stato Giuridico e Trattamento
Economico del Personale, i codici di assenza da utilizzare:

    * 92: codice di missione a completamento giornaliero (non è cambiato)
    * 92M: codice di missione a giustificazione di ore e minuti. In fase di invio dati ad Attestati
      verrà inviato il codice orario più prossimo alla quantità giustificata in ore e minuti
    * 92NG: codice di missione per info circa il fatto che sia un giorno di missione ma con partenza
      successiva alla fine dell'intervallo della giornata lavorativa o con arrivo precedente 
      l'inizio della giornata lavorativa

Per poter spiegare nel modo migliore come funziona questa integrazione e cosa gli amministratori
del personale (e i dipendenti) troveranno sul cartellino mensile, abbiamo pensato che fosse più
appropriato fornire una serie di esempi pratici che rispecchiano situazioni che si possono
verificare nella compilazione di ordini di missione o rimborsi.

Partiamo con lo stabilire che, per la nostra sede immaginaria, l'intervallo di attività lavorativa
in sede per giorni di missione
(v. :doc:`Configurazione del servizio <configurazione>` per info) sarà: **08.30 - 19.30**.

Ordine di missione
~~~~~~~~~~~~~~~~~~

.. _ordine-primocaso-label:

Primo caso ordine di missione
-----------------------------

Supponiamo che il dipendente in esempio crei un ordine di missione che, una volta giunto a fine
flusso di approvazione, sia definito: **dal 21 gennaio 2019 ore 15.45 al 24 gennaio 2019 ore 11.30**.

Con la nuova procedura di sincronizzazione con Missioni, ePAS riceverà quindi queste informazioni e,
confrontandole con il dato relativo all'intervallo di attività lavorativa in sede per giorni di
missione, inserirà automaticamente sul cartellino del dipendente queste assenze:

* il 21 gennaio un codice 92M che giustifica 3 ore e 45 minuti (in fase di invio dati ad
  Attestati verrà inviato un 92H3). In questo modo il dipendente dovrà completare la giornata
  lavorativa con le 3 ore e 27 minuti mancanti al raggiungimento della totalità del suo orario
  di lavoro (se lavora con un orario di lavoro normale)
* il 22 gennaio un codie 92 che giustifica l'intera giornata
* il 23 gennaio un codice 92 che giustifica l'intera giornata
* il 24 gennaio un codice 92M che giustifica 3 ore  (in fase di invio dati ad Attestati verrà
  inviato un 92H3). In questo modo il dipendente dovrà completare la giornata lavorativa con 4 
  ore e 12 minuti mancanti al raggiungimento della totalità dell'orario di lavoro (se lavora con
  un orario di lavoro normale).

.. _ordine-secondocaso-label:

Secondo caso ordine di missione
-------------------------------

Supponiamo che il dipendente in esempio crei un ordine di missione che, una volta giunto a fine
flusso di approvazione, sia così definito: 
**dal 21 gennaio 2019 ore 10.45 al 21 gennaio 2019 ore 16.30**.

Con la nuova procedura di sincronizzazione con Missioni, ePAS riceverà quindi queste informazioni
e, confrontandole con il dato relativo all'intervallo di attività lavorativa in sede per giorni di
missione, inserirà automaticamente sul cartellino del dipendente queste assenze:

* il 21 gennaio un codice 92M che giustifica 5 ore e 45 minuti (in fase di invio dati ad Attestati
  verrà inviato un 92H5). In questo caso la quantità oraria da giustificare viene dedotta dagli
  orari di inizio e fine missione che sono entrambi contenuti in un unico giorno (caso di missione
  oraria).

.. _ordine-terzocaso-label:

Terzo caso ordine di missione
-----------------------------

Supponiamo che il dipendente in esempio crei un ordine di missione che, una volta giunto a fine
flusso di approvazione, sia così definito: 
**dal 21 gennaio 2019 ore 19.45 al 23 gennaio 2019 ore 06.30**.

Con la nuova procedura di sincronizzazione con Missioni, ePAS riceverà quindi queste informazioni
e, confrontandole con il dato relativo all'intervallo di attività lavorativa in sede per giorni di
missione, inserirà automaticamente sul cartellino del dipendente queste assenze:

* il 21 gennaio un codice 92NG che non giustifica niente (in fase di invio ad Attestati verrà
  inviato un 92). In questo caso la partenza è successiva alla fine dell'attività lavorativa in
  sede pertanto non giustifico niente.
* il 22 gennaio un codice 92 che giustifica l'intera giornata
* il 23 gennaio un codice 92NG che non giustifica niente (in fase di invio ad Attestati verrà
  inviato un 92). In questo caso l'arrivo è precedente all'inizio dell'attività lavorativa in sede
  pertanto non giustifico niente. E' sempre possibile contattare l'USGTEP per concordare
  l'inserimento di un codice 92 che giustifichi l'intera giornata nel caso ad esempio di voli di
  ritorno intercontinentali.

.. _ordine-quartocaso-label:

Quarto caso ordine di missione
------------------------------

Supponiamo che il dipendente in esempio crei un ordine di missione che, una volta giunto a fine
flusso di approvazione, sia così definito: 
**dal 21 gennaio 2019 ore 10.00 al 23 gennaio 2019 ore 19.00**.

Con la nuova procedura di sincronizzazione con Missioni, ePAS riceverà quindi queste informazioni
e, confrontandole con il dato relativo 
all'intervallo di attività lavorativa in sede per giorni di missione, inserirà automaticamente sul
cartellino del dipendente queste assenze:

  * il 21 gennaio un codice 92 che giustifica l'intera giornata
  * il 22 gennaio un codice 92 che giustifica l'intera giornata
  * il 23 gennaio un codice 92 che giustifica l'intera giornata

Rimborso di missione
~~~~~~~~~~~~~~~~~~~~

Primo caso rimborso missione
----------------------------

Con riferimento alla situazione descritta nella sezione :ref:`ordine-primocaso-label` supponiamo
che il dipendente crei un rimborso di missione che, una volta giunto a fine flusso di approvazione,
determini:

  * la partenza alle ore 14.45 invece che alle 15.45
  * l'arrivo alle ore 13.30

Con la nuova procedura di sincronizzazione con Missioni, ePAS riceverà quindi queste informazioni
e, confrontandole con il dato relativo all'intervallo di attività lavorativa in sede per giorni di
missione, inserirà automaticamente sul cartellino del dipendente queste assenze:

* il 21 gennaio un codice 92M che giustifica 4 ore e 45 minuti 
  * il 22 gennaio un codice 92 che giustifica l'intera giornata
  * il 23 gennaio un codice 92 che giustifica l'intera giornata
  * il 24 gennaio un codice 92M che giustifica 5 ore

Secondo caso rimborso missione
------------------------------

Con riferimento alla situazione descritta nella sezione :ref:`ordine-secondocaso-label` supponiamo
che il dipendente crei un rimborso di missione che, una volta giunto a fine flusso di approvazione,
determini:

  * la partenza alle ore 11.30

Con la nuova procedura di sincronizzazione con Missioni, ePAS riceverà quindi queste informazioni
e, confrontandole con il dato relativo all'intervallo di attività lavorativa in sede per giorni di
missione, inserirà automaticamente sul cartellino del dipendente queste assenze:

  * il 21 gennaio un codice 92M che giustifica 5 ore

Terzo caso rimborso missione
----------------------------

Con riferimento alla situazione descritta nella sezione :ref:`ordine-terzocaso-label` supponiamo
che il dipendente crei un rimborso di missione che, una volta giunto a fine flusso di approvazione,
determini:

  * la partenza alle ore 17.30
  * l'arrivo alle 9.30

Con la nuova procedura di sincronizzazione con Missioni, ePAS riceverà quindi queste informazioni
e, confrontandole con il dato relativo all'intervallo di attività lavorativa in sede per giorni di
missione, inserirà automaticamente sul cartellino del dipendente queste assenze:

  * il 21 gennaio un codice 92M che giustifica 2 ore
  * il 23 gennaio un codice 92M che giustifica 1 ora

Quarto caso rimborso missione
-----------------------------

Con riferimento alla situazione descritta nella sezione :ref:`ordine-quartocaso-label` supponiamo
che il dipendente crei un rimborso di missione che, una volta giunto a fine flusso di approvazione,
determini:

  * la partenza il giorno 20 alle ore 18.30
  * l'arrivo il giorno 22 alle ore 18.30

Con la nuova procedura di sincronizzazione con Missioni, ePAS riceverà quindi queste informazioni
e, confrontandole con il dato relativo all'intervallo di attività lavorativa in sede per giorni di
missione, inserirà automaticamente sul cartellino del dipendente queste assenze:

* il 20 gennaio un codice 92 che giustifica l'intera giornata poichè il 20 gennaio è domenica e
  quindi non verrebbero effettuati calcoli sull'orario di lavoro
* il 21 gennaio un codice 92 che giustifica l'intera giornata
* il 22 gennaio un codice 92 che giustifica l'intera giornata
  * il 23 gennaio verrebbe rimosso il codice di missione precedentemente inserito
