# --- !Ups

-- Creo una nouva revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

ALTER TABLE absence_types ADD COLUMN reperibility_compatible BOOLEAN DEFAULT FALSE;
ALTER TABLE absence_types_history ADD COLUMN reperibility_compatible BOOLEAN ;


UPDATE absence_types SET reperibility_compatible = false WHERE code not like '91%';
UPDATE absence_types SET reperibility_compatible = true WHERE code like '91%';

INSERT INTO absence_types_history (id, _revision, _revision_type, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation,
reperibility_compatible) 
     SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 1, certification_code, code, considered_week_end, description, internal_use,
justified_time_at_work, valid_from, valid_to, time_for_mealticket, justified_time, replacing_time, replacing_type_id, documentation,
reperibility_compatible FROM absence_types;

-- Non è necessaria una down