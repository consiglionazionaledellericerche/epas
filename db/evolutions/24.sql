# ---!Ups

CREATE SEQUENCE seq_conf_general_tmp
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE conf_general_tmp(
id bigint not null DEFAULT nextval('seq_conf_general_tmp'::regclass),
field text,
field_value text,
office_id bigint not null,
CONSTRAINT conf_general_tmp_pkey PRIMARY KEY (id),
CONSTRAINT conf_general_tmp_fkey FOREIGN KEY (office_id)
      REFERENCES office (id)
);

CREATE TABLE conf_general_tmp_history(
id bigint not null,
_revision integer NOT NULL,
_revision_type smallint,
field text,
field_value text,
office_id bigint not null,
CONSTRAINT conf_general_tmp_history_pkey PRIMARY KEY (id, _revision),
CONSTRAINT revinfo_conf_general_tmp_history_fkey FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);
insert into conf_general_tmp (field, office_id) values('init_use_program', 1);
insert into conf_general_tmp (field, office_id) values('institute_name', 1);
insert into conf_general_tmp (field, office_id) values('email_to_contact', 1);
insert into conf_general_tmp (field, office_id) values('seat_code', 1);
insert into conf_general_tmp (field, office_id) values('url_to_presence', 1);
insert into conf_general_tmp (field, office_id) values('user_to_presence', 1);
insert into conf_general_tmp (field, office_id) values('password_to_presence', 1);
insert into conf_general_tmp (field, office_id) values('number_of_viewing_couple', 1);
insert into conf_general_tmp (field, office_id) values('month_of_patron', 1);
insert into conf_general_tmp (field, office_id) values('day_of_patron', 1);
insert into conf_general_tmp (field, office_id) values('web_stamping_allowed', 1);
insert into conf_general_tmp (field, office_id) values('meal_time_start_hour', 1);
insert into conf_general_tmp (field, office_id) values('meal_time_start_minute', 1);
insert into conf_general_tmp (field, office_id) values('meal_time_end_hour', 1);
insert into conf_general_tmp (field, office_id) values('meal_time_end_minute', 1);

UPDATE conf_general_tmp SET field_value = conf_general.init_use_program from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'init_use_program';
UPDATE conf_general_tmp SET field_value = conf_general.institute_name from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'institute_name';
UPDATE conf_general_tmp SET field_value = conf_general.email_to_contact from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'email_to_contact';
UPDATE conf_general_tmp SET field_value = conf_general.seat_code from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'seat_code';
UPDATE conf_general_tmp SET field_value = conf_general.url_to_presence from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'url_to_presence';
UPDATE conf_general_tmp SET field_value = conf_general.user_to_presence from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'user_to_presence';
UPDATE conf_general_tmp SET field_value = conf_general.password_to_presence from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'password_to_presence';
UPDATE conf_general_tmp SET field_value = conf_general.number_of_viewing_couple from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'number_of_viewing_couple';
UPDATE conf_general_tmp SET field_value = conf_general.month_of_patron from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'month_of_patron';
UPDATE conf_general_tmp SET field_value = conf_general.day_of_patron from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'day_of_patron';
UPDATE conf_general_tmp SET field_value = conf_general.web_stamping_allowed from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'web_stamping_allowed';
UPDATE conf_general_tmp SET field_value = conf_general.meal_time_start_hour from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'meal_time_start_hour';
UPDATE conf_general_tmp SET field_value = conf_general.meal_time_start_minute from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'meal_time_start_minute';
UPDATE conf_general_tmp SET field_value = conf_general.meal_time_end_hour from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'meal_time_end_hour';
UPDATE conf_general_tmp SET field_value = conf_general.meal_time_end_minute from conf_general where conf_general.id = 1 and conf_general_tmp.field = 'meal_time_end_minute';


DROP TABLE conf_general_history;
DROP TABLE conf_general;
DROP SEQUENCE seq_conf_general;
ALTER TABLE conf_general_tmp RENAME TO conf_general;
ALTER TABLE conf_general_tmp_history RENAME TO conf_general_history;
ALTER SEQUENCE seq_conf_general_tmp RENAME TO seq_conf_general;



CREATE SEQUENCE seq_conf_year_tmp
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE conf_year_tmp(
id bigint not null DEFAULT nextval('seq_conf_year_tmp'::regclass),
field text,
field_value text,
year integer,
office_id bigint not null,
CONSTRAINT conf_year_tmp_pkey PRIMARY KEY (id),
CONSTRAINT conf_year_tmp_fkey FOREIGN KEY (office_id)
      REFERENCES office (id)
);

CREATE TABLE conf_year_tmp_history(
id bigint not null,
_revision integer NOT NULL,
_revision_type smallint,
field text,
field_value text,
year integer,
office_id bigint not null,
CONSTRAINT conf_year_tmp_history_pkey PRIMARY KEY (id, _revision),
CONSTRAINT revinfo_conf_year_tmp_history_fkey FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

insert into conf_year_tmp (field, year, office_id) values('month_expiry_vacation_past_year', 2014, 1);
insert into conf_year_tmp (field, year, office_id) values('day_expiry_vacation_past_year', 2014, 1);
insert into conf_year_tmp (field, year, office_id) values('month_expire_recovery_days_13', 2014, 1);
insert into conf_year_tmp (field, year, office_id) values('month_expire_recovery_days_49', 2014, 1);
insert into conf_year_tmp (field, year, office_id) values('max_recovery_days_13', 2014, 1);
insert into conf_year_tmp (field, year, office_id) values('max_recovery_days_49', 2014, 1);
insert into conf_year_tmp (field, year, office_id) values('hour_max_to_calculate_worktime', 2014, 1);

insert into conf_year_tmp (field, year, office_id) values('month_expiry_vacation_past_year', 2013, 1);
insert into conf_year_tmp (field, year, office_id) values('day_expiry_vacation_past_year', 2013, 1);
insert into conf_year_tmp (field, year, office_id) values('month_expire_recovery_days_13', 2013, 1);
insert into conf_year_tmp (field, year, office_id) values('month_expire_recovery_days_49', 2013, 1);
insert into conf_year_tmp (field, year, office_id) values('max_recovery_days_13', 2013, 1);
insert into conf_year_tmp (field, year, office_id) values('max_recovery_days_49', 2013, 1);
insert into conf_year_tmp (field, year, office_id) values('hour_max_to_calculate_worktime', 2013, 1);

UPDATE conf_year_tmp SET field_value = conf_year.month_expiry_vacation_past_year from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'month_expiry_vacation_past_year';
UPDATE conf_year_tmp SET field_value = conf_year.day_expiry_vacation_past_year from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'day_expiry_vacation_past_year';
UPDATE conf_year_tmp SET field_value = conf_year.month_expire_recovery_days_13 from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'month_expire_recovery_days_13';
UPDATE conf_year_tmp SET field_value = conf_year.month_expire_recovery_days_49 from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'month_expire_recovery_days_49';
UPDATE conf_year_tmp SET field_value = conf_year.max_recovery_days_13 from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'max_recovery_days_13';
UPDATE conf_year_tmp SET field_value = conf_year.max_recovery_days_49 from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'max_recovery_days_49';
UPDATE conf_year_tmp SET field_value = conf_year.hour_max_to_calculate_worktime from conf_year where conf_year.year = 2014 and conf_year_tmp.year = 2014 and conf_year_tmp.field = 'hour_max_to_calculate_worktime';

UPDATE conf_year_tmp SET field_value = conf_year.month_expiry_vacation_past_year from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'month_expiry_vacation_past_year';
UPDATE conf_year_tmp SET field_value = conf_year.day_expiry_vacation_past_year from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'day_expiry_vacation_past_year';
UPDATE conf_year_tmp SET field_value = conf_year.month_expire_recovery_days_13 from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'month_expire_recovery_days_13';
UPDATE conf_year_tmp SET field_value = conf_year.month_expire_recovery_days_49 from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'month_expire_recovery_days_49';
UPDATE conf_year_tmp SET field_value = conf_year.max_recovery_days_13 from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'max_recovery_days_13';
UPDATE conf_year_tmp SET field_value = conf_year.max_recovery_days_49 from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'max_recovery_days_49';
UPDATE conf_year_tmp SET field_value = conf_year.hour_max_to_calculate_worktime from conf_year where conf_year.year = 2013 and conf_year_tmp.year = 2013 and conf_year_tmp.field = 'hour_max_to_calculate_worktime';


DROP TABLE conf_year_history;
DROP TABLE conf_year;
DROP SEQUENCE seq_conf_year;
ALTER TABLE conf_year_tmp RENAME TO conf_year;
ALTER TABLE conf_year_tmp_history RENAME TO conf_year_history;
ALTER SEQUENCE seq_conf_year_tmp RENAME TO seq_conf_year;

ALTER TABLE conf_general ADD CONSTRAINT unique_conf_general_integrity_key UNIQUE (field, office_id);
ALTER TABLE conf_year ADD CONSTRAINT unique_conf_year_integrity_key UNIQUE (field, year, office_id);

# ---!Downs


CREATE SEQUENCE seq_conf_general_tmp
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE conf_general_tmp(
id bigint not null DEFAULT nextval('seq_conf_general_tmp'::regclass),
init_use_program date,	
institute_name text,
email_to_contact text,
seat_code integer,
url_to_presence text,
user_to_presence text,
password_to_presence text,
number_of_viewing_couple integer,
month_of_patron integer,
day_of_patron integer,
web_stamping_allowed boolean,
meal_time_start_hour integer, 
meal_time_start_minute integer,
meal_time_end_hour integer,
meal_time_end_minute integer,
CONSTRAINT conf_general_tmp_pkey PRIMARY KEY (id)
);

CREATE TABLE conf_general_tmp_history(
id bigint not null,
_revision integer NOT NULL,
_revision_type smallint,
init_use_program date,	
institute_name text,
email_to_contact text,
seat_code integer,
url_to_presence text,
user_to_presence text,
password_to_presence text,
number_of_viewing_couple integer,
month_of_patron integer,
day_of_patron integer,
web_stamping_allowed boolean,
meal_time_start_hour integer, 
meal_time_start_minute integer,
meal_time_end_hour integer,
meal_time_end_minute integer,
CONSTRAINT revinfo_conf_general_tmp_history_fkey FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);


insert into conf_general_tmp (id) values (1);

UPDATE conf_general_tmp SET init_use_program = conf_general.field_value from conf_general where conf_general.field = 'init_use_program' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET institute_name = conf_general.field_value from conf_general where conf_general.field = 'institute_name' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET email_to_contact = conf_general.field_value from conf_general where conf_general.field = 'email_to_contact' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET seat_code = conf_general.field_value from conf_general where conf_general.field = 'seat_code' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET url_to_presence = conf_general.field_value from conf_general where conf_general.field = 'url_to_presence' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET user_to_presence = conf_general.field_value from conf_general where conf_general.field = 'user_to_presence' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET password_to_presence = conf_general.field_value from conf_general where conf_general.field = 'password_to_presence' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET number_of_viewing_couple = conf_general.field_value from conf_general where conf_general.field = 'number_of_viewing_couple' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET month_of_patron = conf_general.field_value from conf_general where conf_general.field = 'month_of_patron' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET day_of_patron = conf_general.field_value from conf_general where conf_general.field = 'day_of_patron' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET web_stamping_allowed = conf_general.field_value from conf_general where conf_general.field = 'web_stamping_allowed' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET meal_time_start_hour = conf_general.field_value from conf_general where conf_general.field = 'meal_time_start_hour' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET meal_time_start_minute = conf_general.field_value from conf_general where conf_general.field = 'meal_time_start_minute' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET meal_time_end_hour = conf_general.field_value from conf_general where conf_general.field = 'meal_time_end_hour' and conf_general_tmp.id = 1;
UPDATE conf_general_tmp SET meal_time_end_minute = conf_general.field_value from conf_general where conf_general.field = 'meal_time_end_minute' and conf_general_tmp.id = 1;

DROP TABLE conf_general_history;
DROP TABLE conf_general;
DROP SEQUENCE seq_conf_general;
ALTER TABLE conf_general_tmp RENAME TO conf_general;
ALTER TABLE conf_general_tmp_history RENAME TO conf_general_history;
ALTER SEQUENCE seq_conf_general_tmp RENAME TO seq_conf_general;

CREATE SEQUENCE seq_conf_year_tmp
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE conf_year_tmp(
id bigint not null DEFAULT nextval('seq_conf_year_tmp'::regclass),
month_expiry_vacation_past_year integer,
day_expiry_vacation_past_year integer, 
month_expire_recovery_days_13 integer, 
month_expire_recovery_days_49 integer,
max_recovery_days_13 integer,
max_recovery_days_49 integer,
hour_max_to_calculate_worktime integer,
year integer,
CONSTRAINT conf_year_tmp_pkey PRIMARY KEY (id)
);

CREATE TABLE conf_year_tmp_history(
id bigint not null,
_revision integer NOT NULL,
_revision_type smallint,
month_expiry_vacation_past_year integer,
day_expiry_vacation_past_year integer, 
month_expire_recovery_days_13 integer, 
month_expire_recovery_days_49 integer,
max_recovery_days_13 integer,
max_recovery_days_49 integer,
hour_max_to_calculate_worktime integer,
year integer,
CONSTRAINT conf_year_tmp_history_pkey PRIMARY KEY (id, _revision),
CONSTRAINT revinfo_conf_year_tmp_history_fkey FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

insert into conf_year_tmp (id, year) values (1, 2014);
insert into conf_year_tmp (id, year) values (2, 2013);

UPDATE conf_year_tmp SET month_expiry_vacation_past_year = conf_year.field_value from conf_year where conf_year.field = 'month_expiry_vacation_past_year' and conf_year.year = 2014 and conf_year_tmp.year = 2014;
UPDATE conf_year_tmp SET day_expiry_vacation_past_year = conf_year.field_value from conf_year where conf_year.field = 'day_expiry_vacation_past_year' and conf_year.year = 2014 and conf_year_tmp.year = 2014;
UPDATE conf_year_tmp SET month_expire_recovery_days_13 = conf_year.field_value from conf_year where conf_year.field = 'month_expire_recovery_days_13' and conf_year.year = 2014 and conf_year_tmp.year = 2014;
UPDATE conf_year_tmp SET month_expire_recovery_days_49 = conf_year.field_value from conf_year where conf_year.field = 'month_expire_recovery_days_49' and conf_year.year = 2014 and conf_year_tmp.year = 2014;
UPDATE conf_year_tmp SET max_recovery_days_13 = conf_year.field_value from conf_year where conf_year.year = 'max_recovery_days_13' and conf_year.year = 2014 and conf_year_tmp.year = 2014;
UPDATE conf_year_tmp SET max_recovery_days_49 = conf_year.field_value from conf_year where conf_year.year = 'max_recovery_days_49' and conf_year.year = 2014 and conf_year_tmp.year = 2014;
UPDATE conf_year_tmp SET hour_max_to_calculate_worktime = conf_year.field_value from conf_year where conf_year.field = 'hour_max_to_calculate_worktime' and conf_year.year = 2014 and conf_year_tmp.year = 2014;

UPDATE conf_year_tmp SET month_expiry_vacation_past_year = conf_year.field_value from conf_year where conf_year.field = 'month_expiry_vacation_past_year' and conf_year.year = 2013 and conf_year_tmp.year = 2013;
UPDATE conf_year_tmp SET day_expiry_vacation_past_year = conf_year.field_value from conf_year where conf_year.field = 'day_expiry_vacation_past_year' and conf_year.year = 2013 and conf_year_tmp.year = 2013;
UPDATE conf_year_tmp SET month_expire_recovery_days_13 = conf_year.field_value from conf_year where conf_year.field = 'month_expire_recovery_days_13' and conf_year.year = 2013 and conf_year_tmp.year = 2013;
UPDATE conf_year_tmp SET month_expire_recovery_days_49 = conf_year.field_value from conf_year where conf_year.field = 'month_expire_recovery_days_49' and conf_year.year = 2013 and conf_year_tmp.year = 2013;
UPDATE conf_year_tmp SET max_recovery_days_13 = conf_year.field_value from conf_year where conf_year.year = 'max_recovery_days_13' and conf_year.year = 2013 and conf_year_tmp.year = 2013;
UPDATE conf_year_tmp SET max_recovery_days_49 = conf_year.field_value from conf_year where conf_year.year = 'max_recovery_days_49' and conf_year.year = 2013 and conf_year_tmp.year = 2013;
UPDATE conf_year_tmp SET hour_max_to_calculate_worktime = conf_year.field_value from conf_year where conf_year.field = 'hour_max_to_calculate_worktime' and conf_year.year = 2013 and conf_year_tmp.year = 2013;


DROP TABLE conf_year_history;
DROP TABLE conf_year;
DROP SEQUENCE seq_conf_year;
ALTER TABLE conf_year_tmp RENAME TO conf_year;
ALTER TABLE conf_year_tmp_history RENAME TO conf_year_history;
ALTER SEQUENCE seq_conf_year_tmp RENAME TO seq_conf_year;