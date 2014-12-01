Gestione e inserimento Personale
================================
La prima funzionalità che si incontra analizzando il sistema in modalità amministratore riguarda la possibilità di inserire, modificare e, più in generale, gestire il personale.
Dal menu :menuselection:`Amministrazione --> Lista Persone` è possibile aprire l'interfaccia di consultazione delle persone presenti in anagrafica.

.. figure:: _static/images/listPersone.png
   :scale: 40
   :align: center

   Schermata visualizzazione lista personale
   
In primo luogo si può notare come esistano due colorazioni distine per il personale:
   * quelli colorati di blu sono i dipendenti effettivi, ovvero coloro i quali sono strutturati attraverso un contratto a tempo determinato o indeterminato e che sono abilitati alle timbrature.
   * quelli colorati di grigio sono o ex dipendenti per i quali è terminato il rapporto di lavoro con l'ente, o personale non strutturato (co.co,co., co.co.pro., assegnisti ecc...) che non sono abilitati alle timbrature.   

Da questa schermata è possibile evincere quali siano i tipi di dato supportati per ciascun dipendente.
Ogni persona è attualmente modificabile cliccando sul nominativo. 


Modifica persona
----------------

In caso si vogliano modificare i dati di una certa persona la schermata che potremo visualizzare sarà di questo tipo

.. figure:: _static/images/modificaPersona.png
   :scale: 40
   :align: center

   Schermata modifica persona (top)
   
.. figure:: _static/images/modificaPersona2.png
   :scale: 40
   :align: center

   Schermata modifica persona (bottom)

In ogni pannello (titolato) sono contenute informazioni referenti ad esso, rendendo così più intuitivo dove andare a cercare l'informazione specifica che si richiede o che si intende modificare.
Nel caso ad esempio del pannello relativo alle informazioni sulla "presenza default", è possibile specificare se il dipendente può godere dell'autocertificazione per la presenza a lavoro (orario giornaliero fissato al quantitativo orario giornaliero che per contratto il dipendente deve effettuare) oppure no.
Cliccando sul tasto "Modifica", sarà possibile andare a modificare tale informazione seguendo i semplici passi che vengono spiegati nella finestra che si aprirà e che sarà di questo tipo:

.. figure:: _static/images/modificaTipologiaTimbratura.png
   :scale: 40
   :align: center
   
   Schermata di modifica tipologia timbratura
   
Selezionando la tipologia di timbratura dal menu a tendina (timbratura manuale / timbratura automatica) e specificando il periodo per cui si vuole applicare tale modifica (leggendo accuratamente le istruzioni riportate nella finestra), il sistema calcolerà le nuove impostazioni che saranno da subito visibili dal menu Timbrature.

Nel caso del pannello relativo alle informazioni contrattuali, la logica rimane più o meno la stessa, ma le informazioni evidenziate sono maggiori.
In tale pannello si possono trovare:

   * informazioni sul tipo di orario di lavoro associato alla persona;
   * informazioni sui contratti associati alla persona nel corso degli anni;
   * se per il contratto in questione, la persona era presente negli attestati di presenza (v. :menuselection:`Amministrazione --> Invio attestati`);
   * la possibilità di inserire dati di inizializzazione nel caso la tale persona arrivi ad anno in corso in istituto proveniente da altri istituti CNR

Per quanto concerne le informazioni relative all'orario di lavoro, cliccando su "modifica" nella colonna relativa appunto all'orario di lavoro, si aprirà un pannello simile al precedente per la modifica della timbratura manuale/automatica:

.. figure:: _static/images/modificaOrarioLavoro.png
   :scale: 40
   :align: center
   
   Schermata di modifica orario di lavoro
   
La finestra che si apre, permette all'amministratore di:
   * suddividere il periodo specificato nelle colonne, in due periodi distinti specificando la data in cui far terminare il primo e da cui far partire il secondo;
   * cambiare la tipologia di orario di lavoro per il periodo specificaro nelle colonne
   * eliminare il periodo specificato nelle colonne
   
Ciascuna delle precedenti opzioni comporta un ricalcolo delle informazioni personali del dipendente da parte di ePAS sulla base della selezione effettuata.
Come evidente in figura, è presente una breve guida nella finestra che accompagna l'amministratore nei passi da effettuare a seconda dell'operazione richiesta.

Per quanto riguarda invece le informazioni relative al contratto, cliccando nella colonna "modifica date" in corrispondenza della riga relativa al contratto che si intende modificare, si aprirà una finestra del tutto analoga a quella vista in precedenza:

.. figure:: _static/images/modificaContratto.png
   :scale: 40
   :align: center
   
   Schermata di modifica contratto
   
In questo caso è possibile andare a modificare le date del contratto selezionato, inserire un'eventuale data di terminazione del contratto in essere (i casi vengono specificati nell'informativa presente nella finestra).

Appena sotto questo box contenente informazioni contrattuali, è presente il link per poter creare un nuovo contratto da zero. Cliccandolo, si aprirà una finestra nella quale sarà possibile inserire le date del nuovo contratto associato alla persona (lasciando in bianco la data di fine, si avrà un contratto a tempo indeterminato), l'orario di lavoro da associare a quel contratto da scegliere tra quelli proposti nel menu a tendina e il checkbox per sapere se la persona deve essere inserita nella lista del personale da considerare per l'invio delle informazioni a Roma negli attestati di presenza.



Inserimento figli dipendente
----------------------------

Nella schermata top della modifica del personale si può notare anche il link ad un'altra schermata: la possibilità di inserire per il dipendente in questione, dei figli in anagrafica. Di modo da poter far verificare al sistema la possibilità per quel dipendente di usufruire di particolari permessi per l'astensione dal lavoro.
Il link si chiama "Inserisci figlio per...", cliccandoci verrà proposta una form di inserimento per l'eventuale figlio del dipendente contenente nome, cognome e data di nascita e, sopra, un link per la visualizzazione di eventuali figli già inseriti.










   

   
