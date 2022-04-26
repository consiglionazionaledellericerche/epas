# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.5.1] - UNRELEASED
### Added
 - Aggiunto controllo su API rest contracts/byPerson per fornire un messaggio di errore
   quando si tenta di leggere un contratto con previousContract non coerente
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