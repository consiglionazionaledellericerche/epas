# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.27.1] - Unreleased
### Added
  - Aggiunto il permesso per l'utente per vedere quando, nel mese, sono state maturate le ore per gli straordinari

### Changed
  - Corretto il calcolo delle ore di straordinario disponibile nel gruppo nel caso in cui la configurazione della sede dica che
    deve essere presente la doppia approvazione.
  - Resa modificabile la motivazione alla base della richiesta di straordinario da parte dei soggetti approvatori.
  - Modificato PDF del calendario mensile reperibilità per non mostrare le persone non più assegnati al
    servizio di reperibilità nel mese selezionato.
  

## [2.27.0] - 2025-10-16
### Added
  - Aggiunta differenziazione nel conteggio del residuo per straordinari tra personale turnista e non turnista.
  - Aggiunto parametro di sede visibile solo ai ruoli di sistema per abilitare la gestione delle informazioni per i flussi
    degli straordinari
  
### Changed
  - Rimossa la presenza del codice 24PROV dal gruppo di codici che decurtano le ferie. 
  - Inserita limitazione a 1 ora per i codici 18M e 19M.
  - Migliorato il controllo per le ore di turno in caso un dipendente abbia avuto in passato assegnato il turno festivo
    ma che nel mese di riferimento non ce l'abbia più.

## [2.26.0] - 2025-08-20
### Added
  - Aggiunto parametro forceInsert nel metodo rest /rest/absences/insertAbsence
  - Aggiunto il codice 62s75v alla lista di quelli che decurtano le ferie.
  - Aggiunto codice 243PROV che permette di inserire il codice di congedo parentale all'80% per il terzo figlio.


## [2.25.2] - 2025-07-09
### Added
  - Aggiunto il controllo di integrity in alcune risorse js importate via CDN

### Changed
  - Rimosso tutorial sul parametro per l'abilitazione del flusso per le richieste ferie dopo il 31/8.
  - Cambiato il meccanismo di controllo della presenza dei file allegati a un'assenza (nel tabellone
    timbrature)

## [2.25.1] - 2025-06-25
### Added
  - Aggiunta schermata di monitoraggio per verificare le casistiche di persone con lavoro agile e timbrature nella stessa giornata.
  - Aggiunto metodo che modifica la data di fine delle ferie anno passato per tutte le sedi.
  
### Changed
  - Rimossa classe che conteneva il job per il controllo delle ore settimanali lavorate.
  - Permessa l'aggiunta delle note nell'inserimento via REST di un'assenza e anche come metodo
    separato per le assenze già esistenti.
  - Rimossa l'intera gestione delle richieste ferie anno passato dopo deadline.
  - Cambiato il comportamento del codice a ore e minuti per assemblea nella maturazione del buono pasto.


## [2.25.0] - 2025-06-09
### Added
  - Aggiunta gestione piani ferie dei dipendenti via REST

### Changed
  - Limitato il nome delle schede Excel nell'esportazione periodica in Excel
  - Aggiornamento delle librerie micrometer alla 1.15
  - Corretta procedura per cessione giorni di reperibilità senza cambio giorni
  
## [2.24.2] - 2025-05-30
### Added
  - Aggiunto job schedulato a cadenza settimanale (il lunedì) che controlla quanti dipendenti, nella settimana appena conclusa, hanno
    superato il limite di 48 ore di lavoro settimanale previsto dalla circolare 12/2004. Per quei dipendenti viene inviata una mail
    al responsabile della sede e all'amministratore del personale della sede di afferenza del dipendente.
  - Aggiunti codici interni 212P e 213P che consentono di inviare ad Attestati il codice 21P per congedo parentale per il padre per il 
    secondo e terzo figlio così da evitare il limite di 10 giorni previsto per il codice 21P del primo figlio.
  - Aggiunto storico delle modifiche sul buono pasto nel giorno.

### Changed
  - Modificata la visibilità dei parametri relativi al mese di fruizione dei residui per i livelli 4-8 e al numero di riposi compensativi
    prendibili in un anno per i livelli 4-8: ora non possono più essere modificati.
  - Modificato il comportamento nei festivi dei codici 24PROV, 242PROV e 39
  - Modificato l'algoritmo che assegna il buono pasto durante le missioni orarie: eliminata la clausola che limitava ai soli livelli 1-3
    di vedersi non assegnato il buono pasto se la missione oraria supera le 4 ore.
  - Risolto il problema della visualizzazione e conteggio dei nuovi piani ferie 25+4 e 27+4

## [2.24.1] - 2025-04-24
### Added
  - Gestiti timezone diversi da Europe/Rome nella creazione delle missioni via REST.
  - Aggiunto il campo ContractType nel Contract che specifica se un dipendente è strutturato, interinale o non strutturato.
  - Aggiunto controllo in fase di modifica della configurazione di una persona che sospende l'operazione se la data di attivazione del
    dipendente e la data di inizio del parametro di configurazione sono diverse.
  
### Changed
  - Modificata interfaccia per specificare quale debba essere la discriminante se far finire un dipendente nella lista del personale che
    va su Attestati. Modificato il comportamento del setter del ContractType garantendo la contestuale modifica del campo "onCertificate"
    che determina la presenza o meno su attestati di quel contratto.
  - Modificato il comportamento del codice 7XR portandolo a ore e minuti come i 7M e 7DM
  - Rimosso codice 65 e modificato comportamento del codice 61.

### Changed
  - Modificata la documentazione aggiornandola alle novità rilasciate nel tempo

## [2.24.0] - 2025-03-10
### Added
  - Aggiunto parametro di configurazione CLEAN_ABSENCE_TYPES_CONFIGURATIONS nel docker-compose.yml per
    disattivare la cancellazione delle configurazioni delle assenze non presenti nel codice sorgente.
  - Aggiunto parametro di configurazione REPORT_ALWAYS_TO_PERSONNEL_ADMINS per inviare le segnalazioni 
    via email sempre agli amministratori del personale (anche quelle dei responsabili di sede e amministratori tecnici).
  - Aggiunti piani ferie 9+1, 25+4, 27+4
  - Abilitate in visualizzazione tutte le funzionalità del menu "amministrazione" per i responsabili di sede

### Changed
  - Modificato il binder per l'integrazione del componente missioni con epas. Consentito di specificare un nuovo nome per il campo
    delle missioni nel comune di residenza. 
  - Modificato algoritmo che calcola i buoni pasto maturati da inviare ad attestati.
  - Rimossa riduzione percentuale al DefaultTakable del gruppo relativo ai codici per 104 a ore e minuti per il primo parente disabile.
  - Modificato il codice orario che deve essere ritornato per il completamento dell'intera giornata del codice 252AM.
  - Nel Pdf mensile delle reperibilità mostrare come direttore tutte le persone con ruolo responsabile di sede

## [2.23.2] - 2025-02-26
### Added
  - Aggiunto algoritmo che controlla le ore di straordinario festivo per assegnare le giuste quantità nell'interfaccia di inserimento
    da parte dell'amministratore del personale
    
### Changed
  - Risolto problema nella vista del returnedMealTicket
  - Modificata la fruibilità dei codici per legge 104: ora i codici giornalieri sono separati da quelli orari che hanno una limitazione in minuti
    e che non supera le 18 ore mensili. A discrezione dell'amministratore usare gli uni o gli altri.

## [2.23.1] - 2025-02-18
### Added
  - Aggiunto nuovo parametro generale che permette di inviare il report giornaliero sui problemi sui giorni che già viene inviato 
    agli amministratori del personale anche ai responsabili di sede.
  - Aggiunto codice 125 per malattia quinto figlio.

### Changed
  - Modificato l'algoritmo che invia il report giornaliero sui problemi sui giorni consentendo di considerare anche di inviarlo 
    ai responsabili di sede nel caso nella loro sede sia stato messo a SI il parametro descritto nella sezione precedente.
  - Modificato il limite di prendibilità del codice 24 a 60 giorni.

## [2.23.0] - 2025-02-07
### Added
  - Introdotta funzionalità per svuoltare la cache del Token OAuth
  - Aggiunta possibilità di doppia approvazione del flusso di richiesta straordinari da parte del responsabile
    di gruppo e del responsabile di sede.
    
### Changed
  - Cambiati defaut scadenza cache token OAuth
  - Corretto salvataggio ExternalId dei gruppi

## [2.22.3] - 2025-02-04
### Added
  - Aggiunta possibilità per l'amministratore tecnico di una sede di modificare l'externalId di una relazione
    persona-gruppo.
  - Aggiunta generazione parametri di configurazione della persona al metodo REST di creazione della persona

### Changed
  - Rimosso il link al nuovo attestati da ogni parte sulla pagina di invio dati ad attestati.

## [2.22.2] - 2025-01-30
### Added
  - Aggiunto parametro di configurazione personale che consente di bypassare il limite delle 200 ore annue di straordinario.
  - Aggiunti codici 25S e 252S relativi ai congedi parentali per primo e secondo figlio per genitore unico.

### Changed
  - Modificato l'algoritmo che assegna le ore di straordinario in funzione del parametro di cui alla sezione Added.
  - Modificati i limiti per i congedi parentali al 30%

## [2.22.1] - 2025-01-23
### Added
  - Aggiunto parametro di configurazione generale per determinare se sia possibile modificare la quantità oraria per la
    maturazione dei buoni pasto durante la creazione degli orari di lavoro
  - Aggiunti i nuovi codici per le ferie e i riposi compensativi per missione in antartide.
    
### Changed
  - Modificato l'algoritmo che crea gli orari di lavoro in funzione del nuovo parametro generale creato.
  - Nel metodo che aggiorna gli orari di lavoro, inserito controllo che disabilita gli orari riscontrati "non corretti" 
    relativamente alla maturazione del buono pasto

## [2.22.0] - 2025-01-20
### Changed
  - Modificato l'algoritmo che carica la lista dei gruppi di codici per i dipendenti
  - Modificato il novero dei permessi per un amministratore tecnico che ora può anche vedere le info generiche sulle persone 
    della lista persone
  

## [2.21.3] - 2025-01-02
### Changed
  - Modificata procedura per impostare la maturazione del buono pasto a 6 ore per gli orari di 7:12

## [2.21.2] - 2024-12-17
### Added
  - Aggiunta procedura per impostare la maturazione del buono pasto a 6 ore per gli orari di 7:12
  - Aggiunto codice di assenza 31_2023 per la fruzione delle ferie 2023 oltre il 31/12/2024
  - Aggiunti metodi per importare/esportare dati da un'istanza di epas ad un'altra

### Changed
  - Rimossa dicitura "assenza" sul tabellone delle timbrature e nelle modali di inserimento/modifica assenza
    oltre che nello storico del giorno. Rimossa anche da viste che riepilogano i codici.
  - Modificato l'algoritmo che calcola il buono pasto per le persone con presenza automatica: ora il buono viene
    assegnato solo se esiste abbastanza tempo a lavoro da timbrature rispetto all'orario di lavoro associato.
  - Rimosso contenuto delle celle della colonna tempo a lavoro per i giorni in cui il dipendente ha la presenza
    automatica abilitata
  - Evitate rimozioni delle associazioni con gruppi, tab, takable, delle tipologie di assenze impostate
    come da non aggiornare

## [2.21.1] - 2024-12-09
### Added
  - Possibilità di configurare il db.pool e il play.pool all'avvio del container docker

## [2.21.0] - 2024-12-05
### Changed
  - Accorparti e resi più efficienti i controlli di sicurezza sul tabellone delle timbrature
  - Semplificati i controlli di sicurezza sulle voci del menu flussi di lavoro
  - Rimossi i pulsanti di approva e rifiuta dai flussi informativi terminati
  - Corretto il permesso per visualizzare la voce di menu per i permessi genitorali
  - Corretta grafica voce di menu Presenza Giornaliera

## [2.20.4] - 2024-12-03
### Changed
  - Semplificati i controlli di sicurezza sulle timbrature modificabili dagli amministratori del personale

## [2.20.3] - 2024-11-22
### Added
   - Aggiunti metodi per l'esportazione dei dati di un'istanza
   
## [2.20.2] - 2024-11-18
### Added
   - Aggiunto codice 54TD - Aspettativa Legge Gelmini
   - Aggiunto flusso di richiesta straordinari
   
### Changed
   - Rimossa validazione dati della persona nell'inserimento di un nuovo badge
   - Aggiunta la possibilità di inserire un codice di assenza al momento scaduto ma non attivo
     alla data dell'assenza.
   - Modificata descrizione del codice 39LA
   - Modificate le form di inserimento e modifica assenza, togliendo la parola assenza dal titolo

## [2.20.1] - 2024-11-13
### Added
   - Inserito job per far scadere le richieste di uscite di servizio non approvate oltre i 3 mesi
   - Introdotto meccanismo per impostare come lette le notifiche non lette più vecchie di 3 mesi

## [2.20.0] - 2024-11-12
### Added
  - Aggiunto un parametro JAVA_OPTIONS per controllare i parametri passati in fase di avvio
    della JVM Java. Impostato di default il parametro -XX:MaxRAMPercentage=50 che assegna il
    50% della memoria disponibile al container docker.
  

### Changed
  - Corretta visualizzazione dei flussi di richiesta uscita di servizio da parte del responsabile
    di sede se è anche responsabile di gruppo

## [2.19.1] - 2024-10-30
### Added
  - Aggiunto codice 183 giornaliero e ore e minuti per terzo parente disabile
  - Aggiunto job che invia agli amministrativi i problemi sui giorni dei dipendenti della sede che
    gestiscono
    
### Changed
  - Il codice 98 è ora inseribile anche nei festivi
  - Corretto typo nella visualizzazione della pagina Categorie gruppi assenze


## [2.19.0] - 2024-10-15
### Added
  - Aggiunti i codici a completamento "H7" per i codici 24PROV e 242PROV in caso di fruizione oraria.
  - Aggiunto codice 105 per convenzione cnr-università che non matura buono pasto
  - Aggiunta procedura per modificare i codici 37 in codici 31 come da disposizione di UGRU per il 2024

### Changed
  - Rimosso vincolo obbligatorietà id missione nell'annullamento missione via REST 
  - Modificato il comportamento in caso di richiesta di approvazione telelavoro rifiutata
  - Rimossi codici non più usati (vac19, 103P, 111FR)

## [2.18.1] - 2024-09-05
### Added
  - Aggiunta possibilità per l'utente che si autoinserisce le assenze per congedo parentale di aggiungere anche l'allegato

### Changed
  - Aggiornata libreria Guava alla 33.3.0
  - Corretto il comportamento del codice 7M e 7DM che non devono maturare buono pasto qualsiasi quantità oraria venga
    inserita

## [2.18.0] - 2024-08-23
### Changed
  - Corretto il redirect dopo l'inserimento di assenze nel proprio cartellino da parte dell'amministratore del
    personale.
  - Modificata interfaccia di inserimento timbrature rimuovendo la causale del lavoro fuori sede da quelle
    inseribili da parte dell'amministratore del personale. Ora rimane la sola form di inserimento fuori sede.
  - Modificato il controllo della sede di appartenenza del dipendente di cui arriva l'ordine/annullamento/rimborso
    di missione per consentire l'integrazione con il nuovo sistema di missioni Cineca

## [2.17.2] - 2024-07-29
### Changed
  - Aggiunta all'evoluzione 215 l'impostazione della sequenza seq_working_time_types per evitare gli errori 
    nel passaggio dalla versione 2.15.0 a quelle successive in alcuni casi

## [2.17.1] - 2024-07-15
### Changed
  - Nel job all'avvio dell'applicazione evitato di chiudere l'ufficio se ne è presente uno solo anche se vuoto.
  - Scartato l'inserimento delle missioni con data di inizio o fine più lontana di 6 mesi.
  - Corretta evoluzione 217 che aggiornava l'orario Allattamento ma aveva problemi in caso di più orari Allattamento presenti

## [2.17.0] - 2024-07-09
### Added
  - Aggiunto orario di lavoro Allattamento che sostituisce il precedente: non si matura mai il buono pasto.

### Changed
  - Modificato il precedente orario Allattamento in 'Allattamento fino al 31-06-2024' e disabilitato. Questo orario 
    non è più da usarsi a causa dell'inserimento del nuovo orario Allattamento.

  - Corretta una vecchia evoluzione del db (la 69.sql) che inseriva i working_time_types senza
    usare la sequenza postgres corretta. Questo rompeva l'evoluzione 215.sql nel caso di nuova
    installazione a partire dalla versione 2.16.0 o di aggiornamento di una installazione esistente
    pre 2.16.0 senza inserimenti effettuati via web di nuovi working_time_types

## [2.16.1] - 2024-06-18
### Added
  - Aggiunto codice 24PROV che manda ad Attestati il codice 24 per recepire la direttiva di consentire l'utilizzo di 30 giorni di congedo 
    all'80%.

### Changed
  - Modificato il comportamento dei codici 62S...per distacchi sindacali part time: adesso non decurtano più le ferie

## [2.16.0] - 2024-05-24
### Changed
  - Modificato il nome dell'orario Maternità CNR in Allattamento per renderlo più generico e usabile in 
    tutte le installazioni di ePAS
  
## [2.15.2] - 2024-05-23
### Added
  - Aggiunto nuovo orario Maternità CNR da utilizzare come orario di lavoro maternità generico per tutti i dipendenti CNR. 

### Changed
  - Disabilitati gli orari Maternità e Maternità gemellare che erano incompleti o sbagliati

## [2.15.1] - 2024-04-23
### Added
  - Aggiunto campo externalId all'enumerato che modella gli absenceTypes e reso disponibile anche 
    nella form di modifica degli absence_types
  - Aggiunta possibilità per il responsabile di sede di verificare lo stato di avanzamento delle assenze
    soggette a limitazione temporale/quantitativa sui cartellini dei dipendenti delle sedi che gestisce
    
### Changed
  - Modificato il valore del campo isRealAbsence per le assenze che sono in realtà specifiche modalità
    di lavoro
  - Aggiornata la procedura di allineamento tra enumerato e absence_types
  - Modificato il valore del campo externalId per assenze per malattia (F) e rimosso dalle missioni orarie
    "H"

## [2.15.0] - 2024-04-04
### Added
  - Aggiunti ruoli di sistema Gestore assenze e Gestore anagrafica a quelli che possono chiamare
    il servizio REST con la lista delle sedi
  - Modificato il calcolo delle ore di turno del pomeriggio per chi utilizza la fascia oraria
    07-14/13.30-19: ora a chi fa il pomeriggio viene conteggiata una mezz'ora in meno

### Changed
  - Nel metodo REST /rest/v2/certifications/getMonthSituationByOffice valorizzate correttamente
    le competenze
  - Nel metodo REST /rest/v3/persondays/getMonthSituationByPerson per il personDay esportati i campi
    stampingsTime e decurtedMeal e per le assenze esportato isRealAbsence
  
## [2.14.1] - 2024-03-15
### Changed
  - Corretto metodo che preleva la lista delle persone attive che aveva un problema
    introdotto nella versione 2.14.0

## [2.14.0] - 2024-03-12
### Added
  - Aggiunto piano ferie 15+2
  - Aggiunto servizio REST per esportazione lista delle sedi
  - Aggiunto campo externalId ai tipi di assenza
  - Aggiunta limitazione di 3 giorni annuali al codice 662

### Changed
  - Corretta lista persone in Straordinario mensili gruppo, filtrando le persone non più affiliate
  - Cambiata la gestione dei codici 71D e seguenti con la stessa logica del 7M
  - Corretta possibilità di inserire più richieste di cambio reperibilità non ancora confermate nello stesso mese
  - Nell'esportazione via REST del riepilogo mensile dei dipendenti aggiunto nuovo campo externalId del tipo di assenza
  - Modificata durata cache del javascript per l'invio delle segnalazioni
  - Rimossi codici 402 e 413

## [2.13.0] - 2024-02-13
### Added
  - Aggiunta possibilità di inserire residenza ed id anagrafica esterna nella creazione di 
    una nuova persona.
  - Inserita in configurazione generale la possibilità di disabilitare l'inserimento del personale
    da parte dei responsabili del personale degli uffici

### Changed
  - Corretta possibilità di azzerare via REST la causale di una timbratura
  - Correzione trattamento OIL categories selected, riduzione logo predefinito CNR
  - Rimossi vecchi file in /public/images/old
  - Limitata la visualizzazione delle ferie residue negli anni futuri

## [2.12.2] - 2024-02-01
### Changed
  - Ripristinato vincolo degli 8 giorni al Lavoro Agile per il mese di febbraio.

## [2.12.1] - 2024-01-30
### Changed
  - Rimosso vincolo degli 8 giorni al Lavoro Agile per il mese di febbraio.
  - Rimossa redirect in caso di cancellazione di un orario di lavoro particolare che generava loop infinito
  - Resi opzionali parametri type e year nel ical dei turni

## [2.12.0] - 2024-01-15
### Added
  - Aggiunto calendario ICS con le assenze di un dipendente
  - Aggiunti codici di ferie e riposo compensativo per missione antartide
  - Aggiunti codici 62S25V e  62S75V
### Changed
  - Rimosso campo personId da calendari ICS per reperibilità e turni
  - Rivisto metodo approvazione ferie con rimozione doppio invio email al dipendente

## [2.11.0] - 2023-12-14
### Added
  - Aggiunto codice di assenza COMANDO
### Changed
  - Permesso l'inserimento delle missioni orarie nei festivi
  - Corretto invio email a responsabile di gruppo per richiesta ferie quando afferenza al gruppo
    è scaduta.
  - Modificata descrizione del codice 442
  - Modificata descrizione del codice 105BP

## [2.10.1] - 2023-11-17
### Changed
  - Nel init docker configurati i parametri relativi al keycloak anche se l'OAuth2 per 
    gli utenti è disabilitato

## [2.10.0] - 2023-11-17
### Added
  - Aggiunta possibilità di comunicazione tra ePAS ed Attestati (CNR) tramite token JWT
    rilascio dall'SSO del CNR.
  - Reso configurabile il timeout alle chiamate REST al servizio Attestati (CNR)

## [2.9.2] - 2023-11-14
### Added
  - Aggiunto il codice 31_2022 per la gestione delle assenze del 2022 nell'anno 2024

## [2.9.1] - 2023-11-07
### Added
  - Fornito permesso di ignorare il calcolo permesso breve ai responsabili del personale


## [2.9.0] - 2023-11-06
### Added
  - Gestione delle richieste di straordinario da parte del personale IV-VIII, con possibilità 
    di configurare richieste preventive e consuntive di straordinario ed approvazione da parte
    del responsabile di gruppo e/o di sede.
  - Aggiornata immagine docker con tag stable su ghcr.io per ogni nuova release
  
### Changed
  - Evitati di caricare tutti i contratti presenti nel sistema nella visualizzazione delle sedi senza personale
  - Permesso al ruolo "ADMIN" di effettuare le operazioni sulle assenze
  - Permesso al ruolo "RO_ADMIN" di scaricare gli allegati delle assenze


## [2.8.0] - 2023-09-15
### Added
 - Gestito invio segnalazioni anonime ad epas-helpdesk-service.
 - Aggiunto campo residenza all'anagrafica delle persone
 - Inserita possibilità di disattivare il calcolo automatico del permesso breve in un 
   giorno
 - Reso configurabile il numero di mesi nel passato per cui è possibile inserire assenze

### Changed
 - Mostrato un messaggio di avvertimento in caso di segnalazione non inviata correttamente
 - Corretta gestione caso JWT necessario ma non presente.
 - Inviata la sessione play al epas-helpdesk-service (se configurato)
 - Risolto bug nello scaricamento dell'allegato per richieste congedo parentale per il padre
 - Gestiti campi data di nascita e residenza nell'interfacce REST di gestione delle persone
 - Corretta gestione di missioni ricevute via REST con date sovrapposte


## [2.7.0] - 2023-08-10
### Added
 - Aggiunta la CRUD per la gestione delle qualifiche del personale
 - Inserita possibilità di configurare l'abilitazione di servizi REST esterni
   per epas-service e epas-helpdesk-service.
 - Aggiunto il piano di maturazione ferie per la tipologia 20+3
 - Possibilità di azzerare una competenza assegnata in mese nel caso si sia 
   superato il limite delle competenze assegnabili
 - Aggiunta funzionalità di rimozione delle quantità di reperibilità feriale e festiva tramite calendario

### Changed
 - Nei metodi rest /rest/v3/persondays/* valorizzato il campo justifiedTime anche per
   le assenze/presenze di tipo giornaliero (come i codici LAGILE, SMART, 32, etc)

## [2.6.8] - 2023-07-06
### Added
 - Aggiunto metodo REST per la configurazione delle decisioni sul buono pasto in un giorno
   specifico
 - Aggiunto codice 54B
 - Aggiunto controllo in fase di invio attestati che verifica la presenza o meno di allegato per i giorni con codice
   appartenente al gruppo dei congedi parentali e malattia figlio
 - Aggiunta gestione delle missioni nel comune di residenza

### Changed
 - corretta gestione di periodi ferie anno precedente non presenti nel metodo rest /rest/v3/vacations/byPersonAndYear
 - ignorata la gestione di turni con slot dispari nel caso di timetable esterna, così da non fornire errore in questo
   specifico caso errore nella gestione dei calendari
 - cambiata regola drools di approvazione delle richieste ferie che non funzionava in alcuni casi per i responsabili di gruppo
 - corretta visualizzazione Presenza giornaliera gruppo e Straordinari mensili gruppi che restituivano errore per gli utenti di servizio (senza persona associata)
 - cambiato comportamento nell'attribuzione dei giorni di reperibilità se i reperibili stanno su più calendari (sede centrale)
 - rimossi i codici 31_2020 e 31_2021 dalla scheda dei codici di ferie inseribili dall'amministratore del personale
 - corretta gestione permessi brevi per personale in turno

## [2.6.7] - 2023-06-05
### Added
 - Aggiunta la possibilità di inserire la zona di timbratura in fase di inserimento timbratura da interfaccia da parte
   dell'amministratore del personale.
 - Aggiunto controllo che impedisce la fruizione del turno in caso di turno con disparità tra slot e eccessiva disparità
   tra questi (2 mattine e 0 pomeriggi o viceversa).
   
### Changed
  - Modificata la creazione del gruppo per correggere bug in caso di externalId nullo
  - Dopo la login utilizzando LDAP effettuato il redirect alla configurazione http.path la quale
    può essere diversa da /
  - Sostituite nei template le stringe CNR con la variabile company.code
  - Sostituito l'utilizzo dell'attributo html popover con custom-popover per compatibilità 
    con nuova specifica html

## [2.6.6] - 2023-05-18
### Added
 - Aggiunto controllo per l'attribuzione del buono pasto nel caso dei dipendenti I-III livello
   che fanno missioni orarie superiori alle 4 ore: in quel caso il buono non deve essere    attribuito.  Negli altri casi sì.
 - Documentazione per clusterizzazione servizio. 
 
 
### Changed
 - Cambiata la modalità di fruizione dei permessi personali tramite flusso: rimosso codice selezionato
   in automatico da ePAS.
 - Cambiata la modellazione del codice LAGILE in "assegna giornata lavorativa"
 - Risolto errore in fase di redirect dopo l'assegnamento dei buoni elettronici sulle tessere
 - Rimosso l'invio della notifica delle information requests agli altri responsabili di gruppo quando una notifica
   deve arrivare ad uno specifico responsabile di gruppo

## [2.6.5] - 2023-04-06
### Added
 - Aggiunto controllo sui giorni di congedo matrimoniale massimi prendibili
 - Aggiunta gestione delle password sha512 per futura rimozione attuale algoritmo hash password
 - Aggiunta l'importazione dei codici di lavoro agile nell'importazione delle assenze

### Changed
 - Rimossa @As sulle entity, utilizzato per i binder NullStringBinder e LocalTimeBinder
 - Corretta creazione dei TimeSlot che utilizzavano la @As nell'entity
 - Corretto cambio di mese nelle richieste di cambio e reperibilità
 - Corretta creazione gruppi quando l'externalId è vuoto ed esiste già un gruppo con externalId
   vuoto per la relativa sede
 - Introdotto ordine alfabetico per cognome al risultato del metodo rest
   rest/v2/certifications/getmonthsituationbyoffice
 - Rimossi i parametri di configurazione che permettevano ai I-III livelli di auto inserirsi le ferie e
   i riposi compensativi. Condizionata la visibilità del parametro di auto inserimento delle timbrature
   per i I-III livelli al fatto che il valore di quel parametro sia "sì"
 - Aggiunto alert configurabile da generalSetting per informare il personale CNR che l'inserimento di una nuova
   persona può comportare rischi di malfunzionamenti e di contattare l'helpdesk CNR
 - Corretto bug nel calcolo dei periodi delle entità collegate al contratto in caso di modifica a date dei 
   contratti scaduti
 - Corretto bug nell'inserimento di tessere per i buoni pasto per dipendenti non facenti parte della sede
   di afferenza del meal ticket manager
 

## [2.6.4] - 2023-02-02
### Added
 - Aggiunto controllo su correttezza codice fiscale
 - Aggiunto campo absenceTypeId ai principali metodi REST che restituiscono informazioni
   sulle assenze.
 - Aggiunta possibilità di visualizzare le tipologie di assenza a partire dal code (oltre che l'id)
 - Aggiunta regola drools per permettere all'amministratore in sola lettura di esportare i timesheet
 - Aggiunto page break nel pdf per separare in nuova pagina i cartellini di tutti i dipendenti 
   nella stampa cartellino
 - Aggiunto controllo che verifica l'owner di una timbratura nella stampa cartellino nel caso di timbrature
   inserite dal sistema a cavallo della mezzanotte
 - Aggiunto controllo che limita a 10 giorni il lavoro agile nel caso di più di 22 giorni lavorativi in un mese
 
 

### Changed
 - Corretto controllo su univocità codice fiscale e eppn
 - Corretto metodo REST /rest/v2/certifications/getMonthSituationByOffice che non restituiva
   tutte le persone della sede
 - Sostituito il logo CNR nel pulsante per attivare autenticazione SSO
 - Corretto funzionamento respingimento richieste di assenza da parte dei responsabili
   di sede per i responsabili di gruppo.
 - Corretto funzionamento della visualizzazione delle tessere per i buoni elettronici e risolto bug
   di visualizzazione per amministratori associati a sedi ormai chiuse
 - Modificata la chiamata della schermata di inserimento buoni pasto usando il contratto al posto della persona

## [2.6.3] - 2023-01-12
### Added
 - Aggiunta modellazione per la gestione completa dei buoni pasto elettronici
 - Aggiunto nuovo codice 31_2021 per la gestione dei codici di ferie del 2021 nell'anno 2023
 - Aggiunto flusso informativo per la comunicazione delle date di congedo parentale per il padre
 - Aggiunta possibilità auto inserimento codici congedo parentale per il padre
 - Aggiunti codici per missione antartica per l'anno 2023
 
### Changed
 - Corretta gestione sincronizzazione personale afferente in una sede nel caso non
   sia presente l'id anagrafica esterno
 - Aggiornamento del logo del CNR
 - Modificato comportamento del codice 98CV che deve decurtare le ferie
 - Corretta richiesta cambio di reperibilità quando non si danno giorni in cambio
 - Modificate le schermate riepilogative dei flussi di congedo parentale conclusi
 - Corretta visualizzazione privacy policy
 - Migliorate le prestazioni del metodo /rest/v2/certifications/getmonthsituationbyoffice
 - Modificato il calcolo della quantità di ore da togliere per i 661G ai part time orizzontali
 - Rimosso codice 26BP

## [2.6.2] - 2022-11-17

### Added
 - Aggiunto metodo REST /rest/v3/personDays/serviceExitByPersonAndMonth
 - Aggiunto campo mealTicketsPreviousMonth e remainingMealTickets alla risposta del metodo
   REST /rest/v2/certifications/getMonthSituation
 - Modificato il modello per una più facile migrazione a Spring. Private tutti i campi del modello 
 - Aggiunta la possibilità di aggiungere motivazione, luogo e note alle timbrature per motivi di servizio

### Changed
 - Corretta associazione contratto precedente per i contratti continuativi

## [2.6.1] - 2022-10-13
### Added
 - Aggiunta la possibilità per i responsabili di gruppo di approvare le richieste di uscite per servizio
 
### Changed
 - Rivista la procedura di merge di contratto con spostamento delle assenze senza effettuare il re-inserimento
 - Cambiato il tipo a text nelle input per la modifica della matricola del personale
 - Aggiunta possibilità di specificare un URL per il recovery della password LDAP

## [2.6.0] - 2022-09-27
### Added
 - Aggiunta possibilità di servire l'applicazione non come root path (per esempio come
   https://mioentediricerca.it/epas).
 - Completata la gestione delle comunicazioni ferie/riposi compensativi per i livelli I-III
   nel caso il parametro generale sia configurato per non permettere approvazioni per i
   livelli I-III 
 - Aggiunti nuovi codici di assenza per congedo parentale per il padre.
 - Modificati i codici d congedo parentale al 30% secondo le linee guida dettate dalle modifiche al 
   regolamento del CNR.

## [2.5.5] - 2022-09-19
### Changed
 - Corretta la rimozione dai gruppi, servizi di reperibità e turno delle persone
   con contratto scaduto. 

## [2.5.4] - 2022-09-16
### Added
 - Aggiunta la possibilità di disabilitare la configurabilità delle approvazioni delle ferie
   e riposi compensati dei livelli I-III. Con l'apposita configurazione generale abilitata 
   i livelli I-III hanno dei flussi solo per comunicare le asssenze, senza autorizzazioni e 
   con le etichette dei flussi modificati da "richiesta" a "comunicazione"
 - Aggiunta la possibilità di disattivare e cancellare gli orari di lavoro predefiniti
   non utilizzati.
 - Aggiunta la possibilità di rinominare gli orari di lavoro predefiniti.
 - Inviata email al dipendente ed all'ufficio opportuno quando ci sono problemi
   nell'inserimento di una richiesta di missione o di rimborso
 - Aggiunto codice 35R ai codici che riducono le ferie
 
### Changed
 - Reso più robusto il cambio di menu in caso di giorno/mese/anno corrente mancante
 - Corretto orrdinamento delle datatable con data e ora in italiano
 - Aggiunti codici LAGILE e LAGILE in esportazione situazione mensile relativo a presenza a
   lavoro e in smart working
 - Corretta generazione form richiesta ferie quando richieste nel passato
 - Cambiato comportamento del codice 26 che non deve far maturare residuo orario
 - Aggiunto organigramma della sede di appartenenza del dipendente
 - Modificata la visualizzazione sui dati contrattuali del tipo di incongruenza sulle inizializzazioni
 - Aggiunto controllo per impedire la visualizzazione del flusso ferie anno passato dopo deadline se il 
   corrispondente parametro in configurazione è a NO
 - Aggiunto controllo che visualizza l'username nel caso un utente che ha ruoli su una sede sia un utente 
   orfano
 - Risolto bug che faceva visualizzare un'erronea lista di attività su cui il turnista aveva diritti 
   di visualizzazione
 - Corrette tabelle flussi competenze e assenze per init datatable e aggiornate le datatable alla 
   ultima versione 1.12.1.

## [2.5.3] - 2022-08-16
### Added
 - Aggiunta email di warning per assenze per missione non inserite a causa di assenze preesistenti
 - Aggiunta possibilità per il responsabile di sede di vedere i flussi di richiesta ferie anche
   di personale non del proprio istituto ma di cui si è responsabile di sede (utile per le Aree di Ricerca)
 - Aggiunte alcune label per gli screenreader per migliorare l'accessibilità
 - Aggiunta possibilità di vedere nel menù la gestione dei telework se questi sono abilitati per
   l'utente
 - Aggiunta possibilità di vedere il menu con i flussi di lavoro al personale che è responsabile di
   sede, anche sulle sedi diversa dalla propria di assegnazione

### Changed 
 - Corretto controllo dei buoni pasto inviati ad Attestati (funzionalità solo per CNR)
 - La cancellazione dei servizi di reperibilità adesso è possibile anche se il servizio
   ha associato delle persone ma non ha nessun giorno di reperibilità assegnato
 - Modificato il comportamento del codice 39LA per l'assegnazione del buono pasto (non lo assegna)
 - Rimosso codice 40LA dalla lista dei codici prendibili
 - Modificata lista dei flussi attivi evitando di mostrare quelli relativi alla propria sede se non
   si hanno gli opportuni permessi
 - Rimossa scritta SSO nella form di login tramite OAuth
 - Modificato messaggio di errore in caso di richiesta riposo compensativo senza ore sufficienti

## [2.5.2] - 2022-07-06

### Added
 - Aggiunta possibilità di vedere il menu con i flussi informativi al personale che è 
   amministratori del personale o responsabile di sede, anche sulle sedi diversa dalla propria
   di assegnazione
 - Aggiunto il campo markedByTelework nella response delle api REST rest/v3/stampings/create e 
   /rest/v3/stampings/show
 - Aggiunta possibilità di cedere un giorno di reperibilità
 - Aggiunto controllo sulla visibilità della presenza giornaliera dei dipendenti appartenenti ad un 
   gruppo da parte del responsabile di gruppo

### Changed
 - Corretto messaggio di errore in caso di attestato di fine mese non calcolato correttamente
 - Modificato il conteggio dei giorni di presenza in sede quando un dipendente è in telelavoro e 
   le sue timbrature finiscono anche sul proprio cartellino mensile.
 - Modificato il permesso da controllare da InformationRequests.teleworksToApprove a
   InformationRequests.revokeValidation per visualizzare la colonna per revocare le approvazioni di
   telework
 - Fix del recap del calendario delle reperibilità che non mostrava i reperibili che avevano iniziato
   la loro presenza nel servizio durante il mese

## [2.5.1] - 2022-04-29
### Added
 - Aggiunto controllo su API rest contracts/byPerson per fornire un messaggio di errore
   quando si tenta di leggere un contratto con previousContract non coerente
 - Aggiunto campo enumerato per la gestione del buono pasto sul modello di absenceType
   Risolve anche la problematica del 103RT che non deve permettere l'attribuzione del buono
   pasto per coloro i quali fanno telelavoro che finisce sul cartellino come orario di lavoro.

### Changed
 - Migliorato messaggio di errore in caso di inserimento via REST di buoni pasto già esistenti
 - Fix bug del permesso breve che non veniva eliminato quando si completava la giornata
 - Modificata procedura di allineamento dei codici di assenza con gli enumerati per risolvere 
   il problema CNR/INAF su alcuni codici.
 - Reinserito il codice 31_2020 per le esigenze INAF

## [2.5.0] - 2022-04-14
### Added
 - Aggiunta possiblità di trattare gli orari in telelavoro inseriti dai livelli I-III
   come timbrature da conteggiare nel monte orario del dipendente
 - Aggiunto controllo della presenza della configurazione dell'orario di lavoro nei giorni
   calcolati dal metodo REST /rest/v2/certifications/getMonthSituationByOffice
 - Inviata email al responsabile per richieste di cambio turno/reperibilità
 - Inviata notifica al collega in caso di revoca di una richiesta di cambio turno/reperibilità
 - Aggiunta verifica nella richiesta di ferie/permesso che il giorno ricada in un contratto
   del dipendente
 - Aggiunta documentazione su parametri di sede e del dipendente

### Changed
 - Condizionate alcune funzioni per l'admin legate ad "Attestati" del CNR
   alla presenza della configurazione specifica di attestati (la password di accesso)
 - Semplificata l'email inviata agli amministratori del personale per le segnalazioni dei 
   dipendenti: rimosse le info sulla sessione utente
 - Rimosso campo quarter da MealTicket perché non più usato.
 - Modificato il calcolo della quantità di giorni di lavoro agile che possono essere usati nel mese.

## [2.4.1] - 2022-04-06 
### Added
 - Aggiunta configurabilità campo del JWT da dove prelevare il campo eppn dell'utente
 - Aggiunti metodi REST per la visualizzazione e gestione dei buoni pasto

### Changed
 - Corretta attivazione pulsante inserimento richieste assenza nel passato quando
   compilato il campo note.
 - Corretta gestione null del campo externalId dei gruppi

## [2.4.0] - 2022-03-09
### Added
 - Aggiunto il supporto all'utenticazione tramite OAuth, test effettuati solo con keycloak.

### Changed
 - Corretta visualizzazione assenze annuali, codice VAC19 non era incolonnato correttamente.
 - Aggiornata la versione del fullcalendar alla 3.10.2 e della query-ui.
 - Inseriti title e aria-label in link per inserimento assenze e ferie in tabellone timbrature
 - Aggiornato tag f.edit per problema quando l'attributo label non è presente 
 - Aggiornate dipendenze jquery e bootstrap-datapicker
 - Rivisti metodi di aggiornamento automatico delle pagine con selezioni di date
   per migliorare l'accessibilità
 - Migliorati i tag per la generazione delle input in modo da aggiungere title e aria-label
   utili per l'accessibiità
 
## [2.3.0] - 2022-01-21
### Added
 - Aggiunta possibilità di lanciare i ricalcoli per personale di una sede.
 - Aggiunti campi externalId e updatedAt nell'esportazione via REST delle assenze
 - Aggiunto campo externalId a crud Gruppi.
 - Inviata notifica a responsabili per eliminazione richiesta di assenza
 - Aggiunto controllo abilitazione Shibboleth per autenticazione tramite SAML
 - Aggiunta interfaccia per la definizione di un orario di lavoro personalizzato per il dipendente
 - Aggiunto controllo nel calcolo del tempo a lavoro che verifica se esiste un orario di lavoro
   personalizzato che sovrascrive quello della sede.

### Changed
 - Rimossa possibilità di lanciare i ricalcoli per tutto il personale presente nel sistema.
 - Aggiornata libreria org.graylog2 -> gelfj per il supporto al GELF 1.1
 - Corretta sostituzione credenziali attestati nel init docker
 - Corretto controllo per visualizzazione richieste di ferie da parte dei responsabili di sede
 - Corretta validazione dell'orario nell'inserimento lavoro fuori sede
 - Corretta validazione tipologia in inserimento orario telelavoro

## [2.2.1] - 2021-12-03
### Added
 - Aggiunta modellazione del codice 31_2020 per la gestione delle ferie 2020 prorogate oltre il 31/12/2021
 - Aggiunti Endpoint REST per la consultazione delle tipologie di codici di assenza

### Changed
 - Corretta visualizzazione delle tab nella form di inserimento delle assenze da parte dell'amministratore del personale
   parametrando la visibilità sulla base del fatto che i codici di quella tab siano expired o meno
 - Le timbrature fuori orario di presenza obbligatoria effettuate nel festivo non generano più permessi brevi
 - Corretto metodo REST personDays/getDaySituationByOffice in caso di riepiloghi giornalieri non presenti

## [2.2.0] - 2021-11-23
### Added
 - Aggiunta parametrizzazione della visibilità della configurazione personale per l'autoinserimento dei codici covid19
 - Aggiunta configurazione per l'auto inserimento dei codici 39LA

### Changed
 - Corretto calcolo buoni pasto maturati quando non sono presenti ne timbrature ne presenze nel mese 
 - Corretta gestione dei parametri luogo e motivazione per il metodo REST /stampingsfromclient/create
 - Corretta gestione del parametro reasonType per il metodo REST /rest/v3/stampings/create e /rest/v3/stampings/update
 - Corretto controllo dei parametri nel metodo REST /rest/absences/deleteAbsencesInPeriod
 - Corrette date riportate nell'email per informare sulla richiesta di cambio reperibilità 
 
## [2.1.5] - 2021-11-09
### Added
 - Aggiunto plugin per ordinamento corretto date in datatable
 - Aggiunto codice 98CV per le giornate senza giustificazione in caso di mancata mostra del green pass
 - Aggiunti codici COV50 giornaliero e a ore e minuti per il congedo parentale al 50% in periodo covid
 - Aggiunto parametro di sede contenente l'informazione su quale sia il tipo di blocchetto di buono pasto utilizzato
 - Aggiunta visualizzazione uscite di servizio per gli amministratori del personale
 - Estrazione random di personale per controllo greenpass (feature specifica IIT)
 - Aggiunti metodi REST per la visualizzazione delle informazioni relative alle competenze
 - Aggiunto controllo di concorrenza su inserimento multiplo via REST della stessa missione
 - Controllo sul parametro personId nella controller.Stampings::insert
 - Messaggio di errore per utente autenticato con Shibboleth ma eppn non presente in ePAS

### Changed
 - Cambiato l'algoritmo che determina con quale tipologia di buoni pasto vengono coperti i buoni maturati nel mese
   prima dell'invio dei dati ad Attestati
 - Corretta la visualizzazione degli orari in telelavoro per gli amministratori del personale
 - Corretta data per inizio secondo anno ferie per i contratti continuativi
 - Portato a 700ms il tempo minimo per log su metodi lenti
 - Corretti riferimenti a istituto e direttore nell'esportazione dei PDF delle reperibilità
 - Condizionato controllo sui permessi di editing delle assenze per funzionare anche quando 
   non è presente un utente in sessione (per esempio nei job)
 - Corretta generazione parametri di configurazione alla creazione di un nuova persona
   tramite il job di sincronizzazione degli uffici
 - La rimozione via REST di una singola assenza adesso rimuove solo quella indicata e non tutte
   quelle con lo stesso tipo nello stesso giorno.

## [2.1.4] - 2021-08-11
### Changed
 - Corretta la query per mostrare le assenza da approvare dei responsabili di più sedi
 - Nel pdf con la lista delle timbrature fuori sede aggiunto l'orario della timbrature
 - Nella lista dei badge di una persona mostrata anche la sorgente timbratura associata al
   numero di badge
 - reso più blando il controllo sulle stringhe valide nei testi inseriti dagli utenti
 - introdotto piano ferie 20+3

## [2.1.3] - 2021-08-05
### Changed
- Rivisti filtri per mostrare flussi terminati a responsabili di più sedi
- Modificata query per flussi da approvare da parte dei responsabili di sede per mostrare
solo quelli della propria sede e non anche di sedi dove si hanno altri ruoli
- Corretti controlli per invio email in reperibilità con codice di riposo compensativo.
- Corretta query per prelevare flussi completati nel caso di presenze gestite da 
personale di un'altra sede
- Inserito parametro e controllo per tempo massimo nel passato per inserimento timbrature
- Corretta query per prelevare lista assenza da approvare in modo da non mostrare duplicati
- Nella disattivazione dei gruppi redirect verso la lista dei gruppi dell'ufficio corrente
- Aggiunta evoluzione per correggere impostazione begin_date dei person_configurations
del parametro TELEWORK_STAMPINGS.
- Aggiunto controllo che le timbrature per motivi di servizi siano modificabili ed
  eliminabili dal dipendente abilitato solo se inserite dal dipendente

## [2.1.2] - 2021-07-01
### Changed
- Modifica query per prelevare le assenze di una persona in un periodo perché la precedente
impattiva in alcuni casi molto negativamente sulle prestazioni del sistema.

## [2.1.1] - 2021-06-23
### Changed
- Ritorno alla versione play 1.5.3 per problemi di compatibilità con le fastergt e la 
play precompile.

## [2.1.0] - 2021-06-04
### Changed
- Utilizzo della versione play 1.6.0 ed aggiornamento dipendenze varie.
- L'endpoint REST /rest/v2/persons/list adesso ritorna solo la lista del personale attivo 
al momento della chiamata, inoltre adesso supporta il parametro *atDate* con cui è possibile
passare una data con cui verificare i contratti attivi a quella data. Inoltre è possibile
utilizzare il parametro *terse* per avere solo informazioni principali del personale.
### Added
- Aggiunta possiblità di cercare le persone per matricola nei metodi REST.
- Aggiunta la configurabilità della visibilità del menu Normativa e migliorata la gestione dei
permessi di questa parte

## [2.0.2] - 2021-04-09
### Fixed
- Corretta gestione campi non obbligatori id_ordine, anno, numero nell'endpoint per inserimento
missioni via REST - /rest/missions/amqpreceiver.
- Corretti controlli di validazione errati sui metodi rest/checkAbsence e rest/insertAbsence.
### Added
- Aggiunta la possibilità di usufruire di flussi informativi relativamente a temi come telelavoro,
uscite di servizio e malattia (comunicazioni al responsabile di sede o all'amministratore del personale
secondo le stesse modalità delle richieste di ferie e riposo compensativo).

## [2.0.1] - 2021-03-16
### Changed
- Modificato comportamento form di richiesta ferie, adesso sposta la data finale impostandola
uguale alla data iniziale se si mette una data iniziale successiva alla data finale
### Fixed
- Corretta drools per l'accesso all'endpoint REST /rest/absences/insertVacation
### Added
- Aggiunto link alla privacy policy (se abilitata) nel footer
- Aggiunta gestione e visualizzazione delle privacy policy con possibilità di
attivare/disattivare la sua visualizzazione
- Aggiunti metodi Rest per la visualizzazione dell'associazione tra contratto
e tipologia di orario di lavoro e aggiornamento del corrispondente externalId

## [2.0.0] - 2021-02-25
### Added
- Prima relaase pubblica open source
