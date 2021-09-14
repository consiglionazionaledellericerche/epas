# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.5] - UNRELEASED
### Added
 - Aggiunto controllo di concorrenza su inserimento multiplo via REST della stessa missione
### Changed
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