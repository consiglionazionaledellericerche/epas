# --- !Ups

UPDATE absence_types SET reperibility_compatible = true WHERE code = 'COVID19';
UPDATE absence_types_history SET reperibility_compatible = true WHERE code = 'COVID19';

# --- !Downs
-- non è necessaria una down