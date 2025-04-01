Calendario turni
================

In questa sezione vediamo come si presenta un calendario di turno e quali siano le operazioni in esso consentite per poter gestire al meglio
gli slot di turno all'interno di ogni mese.

Per prima cosa, occorre definire il servizio per il quale i calendari devono essere utilizzati. Vi rimandiamo alla sezione v. :doc:`Gestione servizi <../admin/services>`.
per le informazioni necessarie.

Dal menu :menuselection:`Calendari --> Calendario turni` possiamo entrare nella pagina relativa al calendario dei turni:

Sulla parte sinistra della pagina troviamo la lista dei turnisti definiti nella sezione v. :doc:`Gestione servizi <../admin/services>`.

Sotto la lista dei turnisti è invece presente la selezione dello slot di turno (mattina, pomeriggio).

Nella parte centrale è invece presente il calendario del mese. 

.. figure:: _static/images/calendarioTurno.png
   :scale: 100
   :align: center
   
   Calendario turno

Sopra il calendario compare un menu a tendina per scegliere l'attività di turno di cui 
visualizzare il calendario. Questa cosa può capitare se chi sta visitando la pagina ha ruoli su più servizi di turno (gestore del servizio o 
responsabile del servizio).
Nella parte in alto a destra del calendario, invece, compaiono le frecce per spostarsi avanti e indietro nei calendari dei mesi.

Nella parte destra della pagina, infine, troviamo i riepiloghi. 

In questa sezione si trova il conteggio delle ore di turno accumulate da ciascun turnista nel corso del mese.
Infine, se a visualizzare il calendario è il responsabile del servizio, comparirà sotto ai riepiloghi il pulsante per l'approvazione del calendario e quindi delle
ore di turno che saranno inviate alle competenze e saranno rese disponibili per l'invio dell'attestato di presenza.

Inserire un turnista
--------------------

Per inserire un turnista sul calendario è sufficiente selezionare lo slot in cui inserirlo, cliccare col mouse nello spazio a questo dedicato sulla parte 
sinistra della pagina e trascinarlo, letteralmente, sul giorno in cui si intende inserire.

.. figure:: _static/images/inserimentoTurnista.png
   :scale: 100
   :align: center
   
   Inserimento turnista
   
L'inserimento di un turnista comporta contestualmente sia l'inserimento dell'evento con il nome del turnista sul calendario, sia l'inizio del conteggio del 
riepilogo nell'apposita sezione. Ciò può capitare se il calendario viene ad esempio definito in un giorno a metà del mese e il turnista inserito in uno dei giorni
appena trascorsi. In questo caso, come si può vedere anche dalla figura sopra, il conteggio dei riepiloghi si aggiornerà.

Eliminazione di un turnista
---------------------------

Così come si può inserire un reperibile sul calendario, è altrettanto possibile rimuoverlo dal calendario in caso di modifiche dello stesso.
Per farlo, è sufficiente cliccare sulla "*x*" presente in corrispondenza dell'evento col nome del turnista da eliminare (si trova nella parte destra dell'evento 
sul giorno). Si aprirà un popup nel quale si chiede di confermare l'eliminazione dell'evento e, in caso di conferma, l'evento verrà rimosso dal calendario.

Definizione calendario
----------------------

Quando il calendario sarà stato completato tra mattine e pomeriggi la situazione sarà la seguente:

.. figure:: _static/images/calendarioTurnoCompleto.png
   :scale: 90
   :align: center
   
   Calendario turno completo
   
All'interno dei giorni del calendario vengono anche riportate le eventuali assenze (a sfondo rosso) dei turnisti che possono essere inseriti nel calendario. 
Per quei giorni ovviamente i turnisti assenti non possono essere inseriti negli slot di turno. 
Inoltre nel calendario sono visibili anche gli eventuali giorni di Lavoro Agile dei turnisti.

A differenza del calendario della reperibilità, su ciascuno degli slot di ogni giorno può comparire in alto a sinistra un triangolino con al centro un punto
esclamativo. Ciò sta a significare che sullo slot, o sull'intero turno, ci può essere un problema. Passando il mouse sopra quel triangolino compare un messaggio
in cui viene spiegata la causa del problema. 
   
A questo punto il calendario è completo, i conteggi nei riepiloghi laterali si aggiornano giorno dopo giorno fino allo scavallamento del mese, momento in cui 
il responsabile del servizio può procedere con l'approvazione del calendario.

Approvazione del calendario
---------------------------

Una volta scavallato il mese e quindi calcolati tutti i giorni di turno, sarà possibile per il responsabile approvare il calendario.
Come accennato nel primo paragrafo il responsabile del servizio, entrando nella pagina del calendario, troverà nella parte destra riservata ai riepiloghi il pulsante
di approvazione *Approva competenze*. Cliccando sul pulsante si aprirà la finestra riepilogativa di approvazione di seguito riportata:

.. figure:: _static/images/approvazioneTurno.png
   :scale: 90
   :align: center
   
   Approvazione turno
   
In questa pagina si trova il riepilogo di tutti i giorni di turno effettuati dai turnisti nel mese da approvare.
Cliccando sul bottone *Approva competenze*, il responsabile approva le quantità di turno. L'approvazione comporta l'impossibilità, sul calendario,
di fare modifiche sui giorni di turno. Inoltre, i riepiloghi laterali con i giorni di turno diventano colorati con fondo **giallo**.
E il bottone di approvazione del calendario per il responsabile diventa anch'esso di colore **giallo**.

