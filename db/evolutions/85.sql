# ---!Ups

CREATE TABLE competence_code_groups (
  id BIGSERIAL PRIMARY KEY,
  label TEXT NOT NULL,
  limit_type TEXT NOT NULL,
  limit_value INTEGER,
  limit_description TEXT,
  limit_unit TEXT NOT NULL
);

INSERT INTO competence_code_groups (label, limit_type, limit_value, limit_description, limit_unit) values('Gruppo reperibilità', 'monthly', 16, null, 'days');
INSERT INTO competence_code_groups (label, limit_type, limit_value, limit_description, limit_unit) values('Gruppo straordinari', 'yearly', 200, null, 'hours');
INSERT INTO competence_code_groups (label, limit_type, limit_value, limit_description, limit_unit) values('Gruppo turni', 'monthly', 165, null, 'hours');
INSERT INTO competence_code_groups (label, limit_type, limit_value, limit_description, limit_unit) values('Gruppo Ind.tà rischio', 'monthly', null, 'daysOfMonth', 'days');

ALTER TABLE competence_codes ADD COLUMN disabled BOOLEAN;
ALTER TABLE competence_codes ADD COLUMN limit_value INTEGER;
ALTER TABLE competence_codes ADD COLUMN limit_type TEXT;
ALTER TABLE competence_codes ADD COLUMN competence_code_group_id BIGINT REFERENCES competence_code_groups(id);
ALTER TABLE competence_codes ADD COLUMN limit_unit TEXT;
ALTER TABLE competence_codes ADD COLUMN limit_description TEXT;

ALTER TABLE person_reperibility_types_history ADD COLUMN office_id BIGINT;
ALTER TABLE person_reperibility_types_history ADD COLUMN disabled BOOLEAN;
ALTER TABLE person_reperibility_types ADD COLUMN office_id BIGINT;
ALTER TABLE person_reperibility_types ADD COLUMN disabled BOOLEAN;
ALTER TABLE person_reperibility_types ADD FOREIGN KEY (office_id) REFERENCES office(id);

UPDATE competence_codes SET limit_type = 'monthly' WHERE code in ('207', '208', 'T1', 'T2', 'T3', '351', '352', '353', '354', '355');
UPDATE competence_codes SET limit_type = 'yearly' WHERE code in ('S1', 'S2', 'S3');
UPDATE competence_codes SET limit_type = 'noLimit' WHERE code not in ('207', '208', 'S1', 'S2', 'S3', 'T1', 'T2', 'T3', '351', '352', '353', '354', '355');
UPDATE competence_codes SET limit_value = 12 where code = '207';
UPDATE competence_codes SET limit_value = 4 where code = '208';
UPDATE competence_codes SET limit_value = 150 where code = 'T1';
UPDATE competence_codes SET limit_value = 15 where code = 'T2';
UPDATE competence_codes SET limit_value = 200 where code = 'S1';
UPDATE competence_codes SET limit_value = null where code in('351', '352', '353', '354', '355');
UPDATE competence_codes SET limit_unit = 'days' where code in ('207', '208', '351', '352', '353', '354', '355');
UPDATE competence_codes SET limit_unit = 'hours' where code in ('T1', 'T2', 'T3', 'S1', 'S2', 'S3');
UPDATE competence_codes SET limit_description = 'daysOfMonth' where code in ('351', '352', '353', '354', '355');

UPDATE competence_codes SET disabled = 'true' WHERE code = '050';
UPDATE competence_codes SET disabled = 'false' WHERE code <> '050';

UPDATE competence_codes SET competence_code_group_id = competence_code_groups.id
FROM competence_code_groups
WHERE competence_codes.code in ('207', '208') and competence_code_groups.label = 'Gruppo reperibilità';
    
UPDATE competence_codes SET competence_code_group_id = competence_code_groups.id
FROM competence_code_groups
WHERE competence_codes.code in ('S1', 'S2', 'S3') and competence_code_groups.label = 'Gruppo straordinari';
    
UPDATE competence_codes SET competence_code_group_id = competence_code_groups.id
FROM competence_code_groups    
WHERE competence_codes.code in ('T1', 'T2') and competence_code_groups.label = 'Gruppo turni';

UPDATE competence_codes SET competence_code_group_id = competence_code_groups.id
FROM competence_code_groups
WHERE competence_codes.code in ('351', '352', '353', '354', '355') and competence_code_groups.label = 'Gruppo Ind.tà rischio';

UPDATE person_reperibility_types SET disabled = false;

CREATE TABLE competence_codes_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  code TEXT,
  codetopresence TEXT,
  description TEXT,
  disabled BOOLEAN,
  limit_value INTEGER,
  limit_type TEXT,
  competence_code_group_id BIGINT,
  limit_unit TEXT,
  limit_description TEXT
);

CREATE TABLE competence_code_groups_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,  
  label TEXT,
  limit_type TEXT,
  limit_value INTEGER,
  limit_description TEXT,
  limit_unit TEXT
);

INSERT INTO competence_codes_history
  SELECT id, (SELECT MIN(rev) FROM revinfo), 0, code,  codetopresence, description, disabled, 
    limit_value, limit_type, competence_code_group_id, limit_unit, limit_description FROM competence_codes;
    
INSERT INTO competence_code_groups_history
  SELECT id, (SELECT MIN(rev) FROM revinfo), 0, label, limit_type, limit_value, limit_description, 
    limit_unit FROM competence_code_groups;

# ---!Downs

ALTER TABLE competence_codes DROP COLUMN limit_value;
ALTER TABLE competence_codes DROP COLUMN limit_type;
ALTER TABLE competence_codes DROP COLUMN limit_unit;
ALTER TABLE competence_codes DROP COLUMN limit_description;
ALTER TABLE competence_codes DROP CONSTRAINT competence_codes_competence_code_group_id_fkey;
ALTER TABLE competence_codes DROP COLUMN competence_code_group_id;

ALTER TABLE person_reperibility_types DROP CONSTRAINT person_reperibility_types_office_id_fkey;
ALTER TABLE person_reperibility_types DROP COLUMN office_id;

DROP TABLE competence_code_groups;
DROP TABLE competence_code_groups_history;
DROP TABLE competence_codes_history;
