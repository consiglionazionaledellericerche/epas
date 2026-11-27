# --- !Ups

-- Parametri della persona nascosti (#742): COVID_19 e SMARTWORKING abilitano codici scaduti
-- (COVID19 e COVID19BP dal 15/04/2022, SMART dal 01/11/2021) e vengono disattivati per tutti.
-- Le modifiche vengono registrate nello storico con una nuova revisione.

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

CREATE TEMP TABLE person_configurations_233 AS
SELECT id FROM person_configurations
WHERE epas_param IN ('COVID_19', 'SMARTWORKING') AND field_value <> 'false';

UPDATE person_configurations pc SET field_value = 'false'
FROM person_configurations_233 t
WHERE pc.id = t.id;

INSERT INTO person_configurations_history
  (id, _revision, _revision_type, person_id, epas_param, field_value, begin_date, end_date)
SELECT pc.id, (SELECT MAX(rev) FROM revinfo), 1, pc.person_id, pc.epas_param, pc.field_value,
  pc.begin_date, pc.end_date
FROM person_configurations pc JOIN person_configurations_233 t ON t.id = pc.id;

DROP TABLE person_configurations_233;

-- Senza più i due parametri non servono le opzioni della configurazione generale che ne
-- abilitavano la visualizzazione.
ALTER TABLE general_setting DROP COLUMN enable_autoconfig_covid19;
ALTER TABLE general_setting_history DROP COLUMN enable_autoconfig_covid19;
ALTER TABLE general_setting DROP COLUMN enable_autoconfig_smartworking;
ALTER TABLE general_setting_history DROP COLUMN enable_autoconfig_smartworking;

# --- !Downs

ALTER TABLE general_setting ADD COLUMN enable_autoconfig_covid19 BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN enable_autoconfig_covid19 BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting ADD COLUMN enable_autoconfig_smartworking BOOLEAN DEFAULT FALSE;
ALTER TABLE general_setting_history ADD COLUMN enable_autoconfig_smartworking BOOLEAN DEFAULT FALSE;

-- I valori precedenti dei parametri restano nello storico delle configurazioni della persona
