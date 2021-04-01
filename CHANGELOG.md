# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.2] - UNRELEASED
### Fixed
- Corretta gestione campi non obbligatori id_ordine, anno, numero nell'endpoint per inserimento
missioni via REST - /rest/missions/amqpreceiver.
- Corretti controlli di validazione errati sui metodi rest/checkAbsence e rest/insertAbsence.

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