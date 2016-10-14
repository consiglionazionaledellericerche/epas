# ---!Ups

--- 1) takable_absence_behaviours
CREATE TABLE takable_absence_behaviours (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  amount_type TEXT NOT NULL,
  --- takable_count_behaviour TEXT NOT NULL, (per ora inutile
  --- takaen_count_behaviour TEXT NOT NULL,   sempre period)
  fixed_limit INT NOT NULL,
  takable_amount_adjust TEXT
);;

CREATE TABLE takable_absence_behaviours_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT,
  amount_type TEXT,
  --- takable_count_behaviour TEXT,
  --- takaen_count_behaviour TEXT,
  fixed_limit INT,
  takable_amount_adjust TEXT
);

--- 2) taken_codes_group
CREATE TABLE taken_codes_group (
  absence_types_id BIGINT NOT NULL,
  takable_behaviour_id BIGINT NOT NULL,
  FOREIGN KEY (absence_types_id) REFERENCES absence_types (id),
  FOREIGN KEY (takable_behaviour_id) REFERENCES takable_absence_behaviours (id),
  CONSTRAINT taken_codes_group_pk PRIMARY KEY(absence_types_id, takable_behaviour_id)
);

CREATE TABLE taken_codes_group_history (
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT NOT NULL,
  takable_behaviour_id BIGINT NOT NULL
);

--- 3) takable_codes_group
CREATE TABLE takable_codes_group (
  absence_types_id BIGINT NOT NULL,
  takable_behaviour_id BIGINT NOT NULL,
  FOREIGN KEY (absence_types_id) REFERENCES absence_types (id),
  FOREIGN KEY (takable_behaviour_id) REFERENCES takable_absence_behaviours (id),
  CONSTRAINT takable_codes_group_pk PRIMARY KEY(absence_types_id, takable_behaviour_id)
);

CREATE TABLE takable_codes_group_history (
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT NOT NULL,
  takable_behaviour_id BIGINT NOT NULL
);

--- 4) complation_absence_behaviours
CREATE TABLE complation_absence_behaviours (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  amount_type TEXT NOT NULL
);

CREATE TABLE complation_absence_behaviours_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT,
  amount_type TEXT
);

--- 5) complation_codes_group
CREATE TABLE complation_codes_group (
  absence_types_id BIGINT NOT NULL,
  complation_behaviour_id BIGINT NOT NULL,
  FOREIGN KEY (absence_types_id) REFERENCES absence_types (id),
  FOREIGN KEY (complation_behaviour_id) REFERENCES complation_absence_behaviours (id),
  CONSTRAINT complation_codes_group_pk PRIMARY KEY(absence_types_id, complation_behaviour_id)
);

CREATE TABLE complation_codes_group_history (
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT NOT NULL,
  complation_behaviour_id BIGINT NOT NULL
);


--- 6) replacing_codes_group
CREATE TABLE replacing_codes_group (
  absence_types_id BIGINT NOT NULL,
  complation_behaviour_id BIGINT NOT NULL,
  FOREIGN KEY (absence_types_id) REFERENCES absence_types (id),
  FOREIGN KEY (complation_behaviour_id) REFERENCES complation_absence_behaviours (id),
  CONSTRAINT replacing_codes_group_pk PRIMARY KEY(absence_types_id, complation_behaviour_id)
);

CREATE TABLE replacing_codes_group_history (
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT NOT NULL,
  complation_behaviour_id BIGINT NOT NULL
);

--- 7) group_absence_types

CREATE TABLE category_group_absence_types (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  priority INTEGER NOT NULL
);

CREATE TABLE category_group_absence_types_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT,
  description TEXT,
  priority INTEGER
);

CREATE TABLE group_absence_types (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  description TEXT,
  chain_description TEXT,
  category_type_id BIGINT NOT NULL,
  pattern TEXT NOT NULL,
  period_type TEXT,
  takable_behaviour_id BIGINT,
  complation_behaviour_id BIGINT,
  next_group_to_check_id BIGINT,
  FOREIGN KEY (category_type_id) REFERENCES category_group_absence_types (id),
  FOREIGN KEY (takable_behaviour_id) REFERENCES takable_absence_behaviours (id),
  FOREIGN KEY (complation_behaviour_id) REFERENCES complation_absence_behaviours (id),
  FOREIGN KEY (next_group_to_check_id) REFERENCES group_absence_types (id)
);

CREATE TABLE group_absence_types_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT,
  description TEXT,
  chain_description TEXT,
  category_type_id BIGINT,
  pattern TEXT,
  period_type TEXT,
  takable_behaviour_id BIGINT,
  complation_behaviour_id BIGINT,
  next_group_to_check_id BIGINT
);

--- 8) Assenze / Tipi Assenze

CREATE TABLE justified_types (
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE justified_types_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  name TEXT
);

CREATE TABLE absence_types_justified_types (
  absence_types_id BIGINT NOT NULL REFERENCES absence_types (id),
  justified_types_id BIGINT NOT NULL REFERENCES justified_types (id),
  CONSTRAINT absence_type_justified_type_pk PRIMARY KEY(absence_types_id,justified_types_id)
);

CREATE TABLE absence_types_justified_types_history (
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  absence_types_id BIGINT NOT NULL,
  justified_types_id BIGINT NOT NULL
);

--- 9) Assenze / Tipi Assenze Errori

CREATE TABLE absence_troubles (
  id BIGSERIAL PRIMARY KEY,
  trouble TEXT NOT NULL,
  absence_id BIGINT NOT NULL REFERENCES absences (id)
);

CREATE TABLE absence_troubles_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  trouble TEXT,
  absence_id BIGINT
);

ALTER TABLE absence_types ADD COLUMN time_for_mealticket BOOLEAN default false;
ALTER TABLE absence_types ADD COLUMN justified_time INT;
ALTER TABLE absence_types ADD COLUMN replacing_time INT;
ALTER TABLE absence_types ADD COLUMN replacing_type_id BIGINT;

ALTER TABLE absence_types_history ADD COLUMN time_for_mealticket BOOLEAN;
ALTER TABLE absence_types_history ADD COLUMN justified_time INT;
ALTER TABLE absence_types_history ADD COLUMN replacing_time INT;
ALTER TABLE absence_types_history ADD COLUMN replacing_type_id BIGINT;

ALTER TABLE absences ADD COLUMN justified_type_id BIGINT REFERENCES justified_types(id);
ALTER TABLE absences_history ADD COLUMN justified_type_id BIGINT REFERENCES justified_types(id);

CREATE TABLE initialization_groups (
  id BIGSERIAL PRIMARY KEY,

  person_id BIGINT NOT NULL REFERENCES persons(id),
  group_absence_type_id BIGINT NOT NULL REFERENCES group_absence_types(id),
  initialization_date DATE NOT NULL,

  forced_begin DATE,
  forced_end DATE,
  takable_total INT,
  takable_used INT,
  complation_used INT,
  vacation_year INT,
  residual_minutes_last_year INT,
  residual_minutes_current_year INT,
  UNIQUE (person_id, group_absence_type_id, initialization_date)
);

CREATE TABLE initialization_groups_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,

  person_id BIGINT,
  group_absence_type_id BIGINT,
  initialization_date DATE,

  forced_begin DATE,
  forced_end DATE,
  takable_total INT,
  takable_used INT,
  complation_used INT,
  vacation_year INT,
  residual_minutes_last_year INT,
  residual_minutes_current_year INT
);

ALTER TABLE person_reperibility_types ALTER id SET DEFAULT nextval('seq_person_reperibility_types'::regclass);
ALTER TABLE persons_competence_codes ADD COLUMN enabling_date DATE;

UPDATE persons_competence_codes
SET enabling_date = o.begin_date FROM office o LEFT JOIN persons p ON (o.id = p.office_id)
WHERE p.id = persons_id;

CREATE TABLE persons_competence_codes_temp (
  id BIGSERIAL PRIMARY KEY,
  person_id BIGINT,
  competence_code_id BIGINT,
  begin_date DATE NOT NULL,
  end_date DATE
);
INSERT INTO persons_competence_codes_temp(person_id, competence_code_id, begin_date)
SELECT persons_id, competencecode_id, enabling_date
FROM persons_competence_codes;

DROP TABLE persons_competence_codes;
ALTER TABLE persons_competence_codes_temp RENAME TO persons_competence_codes;
ALTER TABLE persons_competence_codes ADD FOREIGN KEY (competence_code_id) REFERENCES competence_codes (id);
ALTER TABLE persons_competence_codes ADD FOREIGN KEY (person_id) REFERENCES persons (id);


CREATE TABLE competence_code_groups (
  id BIGSERIAL PRIMARY KEY,
  label TEXT NOT NULL,
  limit_type TEXT NOT NULL,
  limit_value INTEGER,
  limit_unit TEXT NOT NULL
);

ALTER TABLE competence_code_groups ADD CONSTRAINT competence_code_groups_label_key UNIQUE (label);
INSERT INTO competence_code_groups (label, limit_type, limit_value, limit_unit) values('Gruppo reperibilità', 'monthly', 16, 'days');
INSERT INTO competence_code_groups (label, limit_type, limit_value, limit_unit) values('Gruppo straordinari', 'yearly', 200, 'hours');
INSERT INTO competence_code_groups (label, limit_type, limit_value, limit_unit) values('Gruppo turni', 'monthly', 165, 'hours');
INSERT INTO competence_code_groups (label, limit_type, limit_value, limit_unit) values('Gruppo Ind.tà rischio', 'monthly', NULL, 'days');

ALTER TABLE competence_codes ADD COLUMN disabled BOOLEAN;
ALTER TABLE competence_codes ADD COLUMN limit_value INTEGER;
ALTER TABLE competence_codes ADD COLUMN limit_type TEXT;
ALTER TABLE competence_codes ADD COLUMN competence_code_group_id BIGINT REFERENCES competence_code_groups(id);
ALTER TABLE competence_codes ADD COLUMN limit_unit TEXT;

ALTER TABLE person_reperibility_types_history ADD COLUMN office_id BIGINT;
ALTER TABLE person_reperibility_types_history ADD COLUMN disabled BOOLEAN;
ALTER TABLE person_reperibility_types ADD COLUMN office_id BIGINT;
ALTER TABLE person_reperibility_types ADD COLUMN disabled BOOLEAN;
ALTER TABLE person_reperibility_types ADD FOREIGN KEY (office_id) REFERENCES office(id);

UPDATE competence_codes SET limit_type = 'monthly' WHERE code in ('207', '208', 'T1', 'T2', 'T3', '351', '352', '353', '354', '355');
UPDATE competence_codes SET limit_type = 'yearly' WHERE code in ('S1', 'S2', 'S3');
UPDATE competence_codes SET limit_type = 'noLimit' WHERE code not in ('207', '208', 'S1', 'S2', 'S3', 'T1', 'T2', 'T3', '351', '352', '353', '354', '355');
UPDATE competence_codes SET limit_type = 'onMonthlyPresence' WHERE code in ('351', '352', '353', '354', '355');
UPDATE competence_codes SET limit_value = 16 WHERE code = '207';
UPDATE competence_codes SET limit_value = 4 WHERE code = '208';
UPDATE competence_codes SET limit_value = 150 WHERE code = 'T1';
UPDATE competence_codes SET limit_value = 15 WHERE code = 'T2';
UPDATE competence_codes SET limit_unit = 'days' WHERE code in ('207', '208', '351', '352', '353', '354', '355');
UPDATE competence_codes SET limit_unit = 'hours' WHERE code in ('T1', 'T2', 'T3', 'S1', 'S2', 'S3');


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
  limit_unit TEXT
);

CREATE TABLE competence_code_groups_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  label TEXT,
  limit_type TEXT,
  limit_value INTEGER,
  limit_unit TEXT
);

INSERT INTO competence_codes_history
  SELECT id, (SELECT MIN(rev) FROM revinfo), 0, code,  codetopresence, description, disabled,
    limit_value, limit_type, competence_code_group_id, limit_unit FROM competence_codes;

INSERT INTO competence_code_groups_history
  SELECT id, (SELECT MIN(rev) FROM revinfo), 0, label, limit_type, limit_value,
    limit_unit FROM competence_code_groups;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles text NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_roles_history (
    _revision INTEGER NOT NULL,
    _revision_type smallint,
    user_id BIGINT NOT NULL,
    roles text,

    PRIMARY KEY (_revision, user_id, roles),
    FOREIGN KEY (_revision) REFERENCES revinfo(rev)
);

INSERT INTO user_roles select id,'DEVELOPER' from users WHERE username = 'developer';
INSERT INTO user_roles select id,'ADMIN' from users WHERE username = 'admin';

DELETE FROM users_roles_offices WHERE user_id in (SELECT id FROM users WHERE username = 'developer');
DELETE FROM users_roles_offices WHERE user_id in (SELECT id FROM users WHERE username = 'admin');

DELETE FROM roles WHERE name = 'admin';
DELETE FROM roles WHERE name = 'developer';

ALTER TABLE absence_types DROP CONSTRAINT if EXISTS fkfe65dbf7ca0a1c8a;
DROP TABLE if EXISTS absence_type_groups;
DROP TABLE if EXISTS absence_type_groups_history;
ALTER TABLE absence_types_history DROP COLUMN absence_type_group_id;
ALTER TABLE absence_types DROP COLUMN absence_type_group_id;


CREATE TABLE attachments (
  id BIGSERIAL PRIMARY KEY,
  filename TEXT NOT NULL,
  description TEXT,
  type TEXT NOT NULL,
  file TEXT NOT NULL,
  office_id BIGINT,
  created_at timestamp without time zone,
  UPDATEd_at timestamp without time zone,

  FOREIGN KEY (office_id) REFERENCES office(id)
);

CREATE TABLE attachments_history (
    _revision INTEGER NOT NULL,
    _revision_type smallint,
    id BIGINT NOT NULL,
    filename TEXT,
    description TEXT,
    type TEXT,
    file TEXT,
    office_id BIGINT,

    PRIMARY KEY (id, _revision),
    FOREIGN KEY (_revision) REFERENCES revinfo(rev)
);

UPDATE stampings SET marked_by_employee = false WHERE marked_by_employee is NULL;
UPDATE stampings SET marked_by_admin = false WHERE marked_by_admin is NULL;

# ---!Downs

DROP TABLE absence_troubles;
DROP TABLE absence_troubles_history;

DROP TABLE initialization_groups;
DROP TABLE initialization_groups_history;

DROP TABLE absence_types_justified_types;
DROP TABLE absence_types_justified_types_history;

ALTER TABLE absences DROP COLUMN justified_type_id;
ALTER TABLE absences_history DROP COLUMN justified_type_id;

ALTER TABLE absence_types DROP COLUMN time_for_mealticket;
ALTER TABLE absence_types DROP COLUMN justified_time;
ALTER TABLE absence_types DROP COLUMN replacing_time;
ALTER TABLE absence_types DROP COLUMN replacing_type_id;

ALTER TABLE absence_types_history DROP COLUMN time_for_mealticket;
ALTER TABLE absence_types_history DROP COLUMN justified_time;
ALTER TABLE absence_types_history DROP COLUMN replacing_time;
ALTER TABLE absence_types_history DROP COLUMN replacing_type_id;

DROP TABLE justified_types;
DROP TABLE justified_types_history;

DROP TABLE taken_codes_group;
DROP TABLE taken_codes_group_history;
DROP TABLE takable_codes_group;
DROP TABLE takable_codes_group_history;

DROP TABLE complation_codes_group;
DROP TABLE complation_codes_group_history;
DROP TABLE replacing_codes_group;
DROP TABLE replacing_codes_group_history;

DROP TABLE group_absence_types;
DROP TABLE group_absence_types_history;

DROP TABLE category_group_absence_types;
DROP TABLE category_group_absence_types_history;

DROP TABLE takable_absence_behaviours;
DROP TABLE takable_absence_behaviours_history;

DROP TABLE complation_absence_behaviours;
DROP TABLE complation_absence_behaviours_history;

ALTER TABLE persons_competence_codes DROP COLUMN begin_date;
ALTER TABLE persons_competence_codes DROP COLUMN end_date;
ALTER TABLE competence_codes DROP COLUMN limit_value;
ALTER TABLE competence_codes DROP COLUMN limit_type;
ALTER TABLE competence_codes DROP COLUMN limit_unit;
ALTER TABLE competence_codes DROP CONSTRAINT competence_codes_competence_code_group_id_fkey;
ALTER TABLE competence_codes DROP COLUMN competence_code_group_id;

ALTER TABLE person_reperibility_types DROP CONSTRAINT person_reperibility_types_office_id_fkey;
ALTER TABLE person_reperibility_types DROP COLUMN office_id;
ALTER TABLE competence_code_groups DROP CONSTRAINT competence_code_groups_label_key;
DROP TABLE competence_code_groups;
DROP TABLE competence_code_groups_history;
DROP TABLE competence_codes_history;

DROP TABLE user_roles;
DROP TABLE user_roles_history;

INSERT INTO roles (name) values ('admin');
INSERT INTO roles (name) values ('developer');

DROP TABLE attachments;
DROP TABLE attachments_history;
