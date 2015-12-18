# ---!Ups
ALTER TABLE stampings ADD COLUMN stamp_type text;
ALTER TABLE stampings_history ADD COLUMN stamp_type text;

UPDATE stampings SET stamp_type = 'MOTIVI_DI_SERVIZIO' WHERE stamp_type_id = 1;
UPDATE stampings SET stamp_type = 'VISITA_MEDICA' WHERE stamp_type_id = 2;
UPDATE stampings SET stamp_type = 'PERMESSO_SINDACALE' WHERE stamp_type_id = 3;
UPDATE stampings SET stamp_type = 'INCARICO_DI_INSEGNAMENTO' WHERE stamp_type_id = 4;
UPDATE stampings SET stamp_type = 'DIRITTO_ALLO_STUDIO' WHERE stamp_type_id = 5;
UPDATE stampings SET stamp_type = 'MOTIVI_PERSONALI' WHERE stamp_type_id = 6;
UPDATE stampings SET stamp_type = 'REPERIBILITA' WHERE stamp_type_id = 7;
UPDATE stampings SET stamp_type = 'INTRAMOENIA' WHERE stamp_type_id = 8;
UPDATE stampings SET stamp_type = 'GUARDIA_MEDICA' WHERE stamp_type_id = 9;
UPDATE stampings SET stamp_type = 'PAUSA_PRANZO' WHERE stamp_type_id = 10;

UPDATE stampings_history SET stamp_type = 'MOTIVI_DI_SERVIZIO' WHERE stamp_type_id = 1;
UPDATE stampings_history SET stamp_type = 'VISITA_MEDICA' WHERE stamp_type_id = 2;
UPDATE stampings_history SET stamp_type = 'PERMESSO_SINDACALE' WHERE stamp_type_id = 3;
UPDATE stampings_history SET stamp_type = 'INCARICO_DI_INSEGNAMENTO' WHERE stamp_type_id = 4;
UPDATE stampings_history SET stamp_type = 'DIRITTO_ALLO_STUDIO' WHERE stamp_type_id = 5;
UPDATE stampings_history SET stamp_type = 'MOTIVI_PERSONALI' WHERE stamp_type_id = 6;
UPDATE stampings_history SET stamp_type = 'REPERIBILITA' WHERE stamp_type_id = 7;
UPDATE stampings_history SET stamp_type = 'INTRAMOENIA' WHERE stamp_type_id = 8;
UPDATE stampings_history SET stamp_type = 'GUARDIA_MEDICA' WHERE stamp_type_id = 9;
UPDATE stampings_history SET stamp_type = 'PAUSA_PRANZO' WHERE stamp_type_id = 10;

ALTER TABLE stampings DROP CONSTRAINT fk785e8f14932966bd;

ALTER TABLE stampings DROP COLUMN stamp_type_id;
ALTER TABLE stampings_history DROP COLUMN stamp_type_id;
ALTER TABLE stampings DROP COLUMN badge_reader_id;
ALTER TABLE stampings_history DROP COLUMN badge_reader_id;

DROP TABLE stamp_types_history;

DROP TABLE stamp_types;

# ---!Downs
