# --- !Ups

ALTER TABLE absence_types ADD COLUMN shift_compatible BOOLEAN DEFAULT false; 
ALTER TABLE absence_types_history ADD COLUMN shift_compatible BOOLEAN DEFAULT false;
UPDATE absence_types SET shift_compatible = TRUE, reperibility_compatible = TRUE where code = 'PB';

# --- !Downs

ALTER TABLE absence_types DROP COLUMN shift_compatible; 
ALTER TABLE absence_types_history DROP COLUMN shift_compatible;