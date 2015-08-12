# ---!Ups

ALTER TABLE absence_types DROP COLUMN meal_ticket_calculation;
ALTER TABLE absence_types DROP COLUMN multiple_use;
ALTER TABLE absence_types DROP COLUMN replacing_absence;

ALTER TABLE absence_types_history DROP COLUMN meal_ticket_calculation;
ALTER TABLE absence_types_history DROP COLUMN multiple_use;
ALTER TABLE absence_types_history DROP COLUMN replacing_absence;

UPDATE person_days_in_trouble SET cause = 'UNCOUPLED_FIXED' WHERE cause = 'timbratura disaccoppiata persona fixed';
UPDATE person_days_in_trouble SET cause = 'NO_ABS_NO_STAMP' WHERE cause = 'no assenze giornaliere e no timbrature';
UPDATE person_days_in_trouble SET cause = 'UNCOUPLED_WORKING' WHERE cause = 'timbratura disaccoppiata giorno feriale';
UPDATE person_days_in_trouble SET cause = 'UNCOUPLED_HOLIDAY' WHERE cause = 'timbratura disaccoppiata giorno festivo';

DELETE FROM person_days_in_trouble WHERE cause NOT IN ('UNCOUPLED_FIXED','NO_ABS_NO_STAMP','UNCOUPLED_WORKING','UNCOUPLED_HOLIDAY');

DROP TABLE IF EXISTS person_days_in_trouble_history;

CREATE TABLE person_days_in_trouble_history (
    id bigint NOT NULL,
    _revision integer NOT NULL REFERENCES revinfo (rev), 
    _revision_type smallint,
    cause text,
    emailsent boolean,
    personday_id bigint,

    CONSTRAINT person_days_in_trouble_history_pkey PRIMARY KEY (id, _revision)    
);

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO person_days_in_trouble_history (id, _revision, _revision_type, cause, emailsent, personday_id)
	SELECT p.id, r.rev, 0, p.cause,false,p.personday_id FROM person_days_in_trouble p,
	(SELECT MAX(rev) AS rev FROM revinfo) AS r;

INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO person_days_in_trouble_history (id, _revision, _revision_type, cause, emailsent, personday_id)
	SELECT p.id, r.rev, 1, p.cause,true,p.personday_id FROM (SELECT * FROM person_days_in_trouble WHERE emailsent = true) as p,
	(SELECT MAX(rev) AS rev FROM revinfo) AS r;

ALTER TABLE person_days_in_trouble DROP COLUMN fixed;

# ---!Downs

ALTER TABLE absence_types ADD COLUMN meal_ticket_calculation boolean;
ALTER TABLE absence_types ADD COLUMN multiple_use boolean;
ALTER TABLE absence_types ADD COLUMN replacing_absence boolean;

ALTER TABLE absence_types_history ADD COLUMN meal_ticket_calculation boolean;
ALTER TABLE absence_types_history ADD COLUMN multiple_use boolean;
ALTER TABLE absence_types_history ADD COLUMN replacing_absence boolean;

UPDATE person_days_in_trouble SET cause = 'timbratura disaccoppiata persona fixed' WHERE cause = 'UNCOUPLED_FIXED';
UPDATE person_days_in_trouble SET cause = 'no assenze giornaliere e no timbrature' WHERE cause = 'NO_ABS_NO_STAMP';
UPDATE person_days_in_trouble SET cause = 'timbratura disaccoppiata giorno feriale' WHERE cause = 'UNCOUPLED_WORKING';
UPDATE person_days_in_trouble SET cause = 'timbratura disaccoppiata giorno festivo' WHERE cause = 'UNCOUPLED_HOLIDAY';

DROP TABLE IF EXISTS person_days_in_trouble_history;

ALTER TABLE person_days_in_trouble ADD COLUMN fixed boolean;