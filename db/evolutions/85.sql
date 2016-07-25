# ---!Ups

CREATE TABLE competence_code_groups (
  id BIGSERIAL PRIMARY KEY,
  label TEXT NOT NULL,
  limit_type TEXT NOT NULL,
  limit_value INTEGER
);

INSERT INTO competence_code_groups(label, limit_type, limit_value) values('Gruppo reperibilità', 'monthly', 16);
INSERT INTO competence_code_groups(label, limit_type, limit_value) values('Gruppo straordinari', 'yearly', 200);
INSERT INTO competence_code_groups(label, limit_type, limit_value) values('Gruppo turni', 'monthly', 165);

ALTER TABLE competence_codes ADD COLUMN limit_value INTEGER;
ALTER TABLE competence_codes ADD COLUMN limit_type TEXT;
ALTER TABLE competence_codes ADD COLUMN competence_code_group_id BIGINT;
ALTER TABLE competence_codes ADD FOREIGN KEY (competence_code_group_id) REFERENCES competence_code_groups(id);

UPDATE competence_codes SET limit_type = 'monthly' WHERE code in ('207', '208', 'T1', 'T2', 'T3');
UPDATE competence_codes SET limit_type = 'yearly' WHERE code in ('S1', 'S2', 'S3');
UPDATE competence_codes SET limit_type = 'noLimit' WHERE code not in ('207', '208', 'S1', 'S2', 'S3', 'T1', 'T2', 'T3');
UPDATE competence_codes SET limit_value = 12 where code = '207';
UPDATE competence_codes SET limit_value = 4 where code = '208';
UPDATE competence_codes SET limit_value = 150 where code = 'T1';
UPDATE competence_codes SET limit_value = 15 where code = 'T2';
UPDATE competence_codes SET limit_value = 200 where code = 'S1';

UPDATE competence_codes SET competence_code_group_id = competence_code_groups.id
FROM competence_code_groups
WHERE competence_codes.code in ('207', '208') and competence_code_groups.label = 'Gruppo reperibilità';
    
UPDATE competence_codes SET competence_code_group_id = competence_code_groups.id
FROM competence_code_groups
WHERE competence_codes.code in ('S1', 'S2', 'S3') and competence_code_groups.label = 'Gruppo straordinari';
    
UPDATE competence_codes SET competence_code_group_id = competence_code_groups.id
FROM competence_code_groups    
WHERE competence_codes.code in ('T1', 'T2') and competence_code_groups.label = 'Gruppo turni';
    

# ---!Downs

ALTER TABLE competence_codes DROP COLUMN limit_value;
ALTER TABLE competence_codes DROP COLUMN limit_type;
ALTER TABLE competence_codes DROP CONSTRAINT competence_codes_competence_code_group_id_fkey;
ALTER TABLE competence_codes DROP COLUMN competence_code_group_id;

DROP TABLE competence_code_groups;

