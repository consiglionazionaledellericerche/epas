# --- !Ups

-- Parametri della sede nascosti con valore fisso (#742):
-- - l'integrazione con la piattaforma Missioni vale per tutto il CNR;
-- - il numero di colonne di entrata/uscita da visualizzare è sempre 2;
-- - il flusso di richiesta assenza e i flussi informativi sono abilitati per tutte le sedi.
-- Le modifiche vengono registrate nello storico con una nuova revisione.

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

CREATE TEMP TABLE configurations_232 AS
SELECT id, CASE WHEN epas_param = 'NUMBER_OF_VIEWING_COUPLE' THEN '2' ELSE 'true' END AS new_value
FROM configurations
WHERE (epas_param IN ('ENABLE_MISSIONS_INTEGRATION', 'ENABLE_FLOWS', 'ENABLE_INFORMATION_FLOWS')
    AND field_value <> 'true')
  OR (epas_param = 'NUMBER_OF_VIEWING_COUPLE' AND field_value <> '2');

UPDATE configurations c SET field_value = t.new_value
FROM configurations_232 t
WHERE c.id = t.id;

INSERT INTO configurations_history
  (id, _revision, _revision_type, office_id, epas_param, field_value, begin_date, end_date)
SELECT c.id, (SELECT MAX(rev) FROM revinfo), 1, c.office_id, c.epas_param, c.field_value,
  c.begin_date, c.end_date
FROM configurations c JOIN configurations_232 t ON t.id = c.id;

DROP TABLE configurations_232;

# --- !Downs

-- Non è necessaria una down: i valori precedenti restano nello storico delle configurazioni
