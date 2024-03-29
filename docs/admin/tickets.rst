Gestione buoni pasto
====================

Il sistema ePAS permette all'amministratore di poter gestire i buoni pasto da attribuire a ciascun
dipendente.
Dal menu :menuselection:`Amministrazione --> Gestione buoni pasto` si arriva in questa schermata:

.. figure:: _static/images/riepilogoBuoni.png
   :scale: 40
   :align: center
   
   Schermata di riepilogo buoni pasto


Come si può notare, la strutturazione della tabella ricalca quella della lista persone.
Vengono, di default, visualizzate 10 persone ma dalla form in alto a sinistra è possibile
specificare la visualizzazione di 25, 50 o di tutte le persone.
E' possibile la ricerca di una specifica persona usufruendo del riquadro di ricerca a destra in
alto, scrivendo il nome o il cognome della persona che si sta cercando.
E' possibile l'ordinamento degli elementi su ciascuno dei campi della tabella, è sufficiente
cliccare sull'intestazione della colonna per ordinare in modo discendente o ascendente su quello
specifico campo.

Per ogni persona vengono visualizzati i buoni rimanenti dal mese precedente, quelli consegnati nel
mese, quelli usati nel mese e il totale dei rimanenti.

La colonna interessante di questa tabella è quella relativa ai *buoni usati nel mese*. In questa colonna vengono riportati
i buoni pasto che il dipendente ha maturato nel corso dell'anno/mese di riferimento. 
Il calcolo dei buoni maturati viene effettuato sulla base dei giorni di presenza a lavoro in cui il dipendente
ha raggiunto il tempo di lavoro necessario alla maturazione del buono pasto.

E' importante ricordare come, da qualche mese, al CNR siano in uso oltre ai vecchi buoni cartacei anche quelli 
elettronici, per prima cosa quindi occorre andare nella configurazione della propria sede e specificare con quale tipologia di 
buoni pasto occorre coprire le maturazioni dei buoni dei propri dipendenti (v. :doc:`Gestione parametri <parameters>` ).

Questa operazione è importante poichè i buoni pasto adesso servono a "coprire" le maturazioni che mensilmente ciascun dipendente fa 
grazie al tempo a lavoro trascorso ed è proprio questo dato che verrà inviato ad Attestati a fine mese ( v. :doc:`Invio dati ad attestati <sendStampings>`). 

Per fare un esempio:
se un dipendente nel mese di settembre matura 16 buoni pasto, dal mese precedente (agosto) gli avanzavano 5 buoni cartacei e 
sempre nel mese di settembre gli vengono anche rilasciati 30 buoni elettronici, il sistema calcolerà naturalmente 16 come 
numero di buoni pasto maturati ma, a fine mese nell'invio dei dati ad Attestati, quel quantitativo sarà suddiviso in 
5 buoni cartacei e 11 buoni elettronici con cui verrà "coperto" il numero dei buoni maturati.

Inserire buoni pasto elettronici
--------------------------------

La possibilità di utilizzare i buoni pasto elettronici ha portato anche ePAS a differenziare le modalità di inserimento dei buoni
pasto.
In particolare, se si è selezionata la tipologia di buoni pasto elettronici, cliccando sul dipendente per cui si vuole effettuare
l'inserimento, si aprirà questa nuova schermata di inserimento buoni:

.. figure:: _static/images/assegnaBuoniElettronici.png
   :scale: 40
   :align: center
   
   Schermata di assegnamento buoni pasto elettronici
   
E' facilmente intuibile che la schermata sia del tutto analoga a quella prevista per i buoni cartacei (di cui si parla di seguito).
In questo caso occorre specificare la tessera (che deve essere stata preventivamente assegnata) su cui si vogliono caricare i
buoni pasto, quanti se ne vogliono caricare, la data di consegna, e la data di scadenza.
Premendo sul bottone "Salva" posizionato sotto la form si procederà all'assegnazione dei buoni elettronici al dipendente.

In questa pagina è altresì possibile passare all'inserimento di un blocchetto cartaceo (spiegazione presente nell'alert azzurro in alto), 
qualora ci fosse la necessità di inserire dei buoni nei mesi passati e questi fossero di tipo cartaceo.

Inoltre, nel caso in cui nel corso dei mesi precedenti siano stati assegnati dei buoni elettronici usando la form di inserimento
standard (selezionando l'etichetta *elettronico* nella form di inserimento) nella parte bassa della pagina è presente un ulteriore alert, 
di colore giallo, in cui è possibile procedere con l'associazione di quei buoni inseriti alla card attualmente in uso dal dipendente.
Premendo il bottone giallo, il sistema provvederà in automatico all'associazione. 

Cliccando sulla scheda *Modifica buoni elettronici consegnati*, si aprirà una pagina in cui sarà possibile eliminare l'inserimento
fatto nella sua totalità o solo parte di esso. 

.. figure:: _static/images/gestisciBuoniElettronici.png
   :scale: 40
   :align: center
   
   Schermata di gestione degli inserimenti dei buoni pasto elettronici


Cliccando infatti sul bottone *Rimuovi*, si aprirà una finestra in cui specificare quanti buoni eliminare dell'inserimento in 
questione e, una volta approvata la rimozione, il conteggio dei buoni del dipendente si aggiornerà in automatico.
   

Inserire buoni pasto cartacei
-----------------------------

Cliccando sulla persona, si aprirà la form di inserimento dei ticket:

.. figure:: _static/images/assegnaBuoniTop.png
   :scale: 40
   :align: center
   
   Schermata di assegnamento buoni pasto (1)
   

.. figure:: _static/images/assegnaBuoniBottom.png
   :scale: 40
   :align: center
   
   Schermata di assegnamento buoni pasto (2)

In questa schermata vengono visualizzati:
   * il codice blocco (campo obbligatorio): in cui inserire il codice del blocchetto (nel caso di buoni elettronici,
     provvisoriamente, inserire una stringa alfanumerica univoca del tipo "matricola+anno+mese"
   * la tipologia di blocco (campo obbligatorio) da selezionare tra cartaceo e elettronico
   * numero iniziale (obbligatorio): da quale buono iniziare a conteggiare i buoni assegnati (campo che ha naturalmente più valenza nel
     caso dei buoni cartacei e da utilizzare, provvisoriamente, anche nel caso di inserimento di buoni elettronici 
   * numero finale (obbligatorio): fino a quale buono conteggiare i buoni assegnati (campo che ha naturalmente più valenza nel caso dei 
     buoni cartacei e da utilizzare, provvisoriamente, anche nel caso di inserimento di buoni elettronici
   * la data di consegna (obbligatorio): la data di consegna dei buoni pasto
   * la data di scadenza (obbligatorio): la data di scadenza di quei buoni consegnati
   
Nella seconda immagine, è invece possibile vedere un riepilogo degli ultimi tre inserimenti di buoni pasto.


Dalle schede presenti in pagina, è possibile anche andare a modificare i buoni consegnati al dipendente cliccando sulla
scheda **Modifica buoni consegnati**:

.. figure:: _static/images/modificaBuoni.png
   :scale: 40
   :align: center
   
   Schermata di modifica buoni pasto consegnati
   
Nella schermata è possibile convertire da cartaceo ad elettronico i blocchetti consegnati cliccando sull'apposito bottone
giallo nella colonna "Converti" oppure riconsegnare o eliminare i blocchetti cliccando sui rispettivi bottoni presenti a 
fine tabella.

Nell'ultima scheda, quella relativa al riepilogo dei buoni consegnati, è ovviamente presente lo storico di tutti i buoni
consegnati a quel dipendente.
