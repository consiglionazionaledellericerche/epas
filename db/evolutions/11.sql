# ---!Ups

CREATE sequence seq_certificated_data
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;
    
CREATE TABLE certificated_data
(
  id bigint NOT NULL DEFAULT nextval('seq_certificated_data'::regclass),
  absences_sent character varying(255),
  cognome_nome character varying(255),
  competences_sent character varying(255),
  is_ok boolean,
  matricola character varying(255),
  mealticket_sent integer,
  month integer NOT NULL,
  ok boolean NOT NULL,
  problems character varying(255),
  year integer NOT NULL,
  person_id bigint NOT NULL,
  CONSTRAINT certificated_data_pkey PRIMARY KEY (id),
  CONSTRAINT fkca05bafce7a7b1be FOREIGN KEY (person_id)
      REFERENCES persons (id)
);   


CREATE TABLE certificated_data_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL,
  _revision_type smallint,
  absences_sent character varying(255),
  cognome_nome character varying(255),
  competences_sent character varying(255),
  is_ok boolean,
  matricola character varying(255),
  mealticket_sent integer,
  month integer,
  ok boolean,
  problems character varying(255),
  year integer,
  person_id bigint,
  CONSTRAINT certificated_data_history_pkey PRIMARY KEY (id, _revision),
  CONSTRAINT fk64d88a51d54d10ea FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

CREATE sequence seq_conf_general
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;

CREATE TABLE conf_general
(
  id bigint NOT NULL DEFAULT nextval('seq_conf_general'::regclass),
  email_to_contact character varying(255),
  init_use_program date,
  institute_name character varying(255),
  number_of_viewing_couple integer,
  password_to_presence character varying(255),
  seat_code integer,
  url_to_presence character varying(255),
  user_to_presence character varying(255),
  day_of_patron integer,
  month_of_patron integer,
  web_stamping_allowed boolean,
  meal_time_end_hour integer,
  meal_time_end_minute integer,
  meal_time_start_hour integer,
  meal_time_start_minute integer,
  CONSTRAINT conf_general_pkey PRIMARY KEY (id)
);

CREATE TABLE conf_general_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL,
  _revision_type smallint,
  email_to_contact character varying(255),
  init_use_program date,
  institute_name character varying(255),
  number_of_viewing_couple integer,
  password_to_presence character varying(255),
  seat_code integer,
  url_to_presence character varying(255),
  user_to_presence character varying(255),
  day_of_patron integer,
  month_of_patron integer,
  web_stamping_allowed boolean,
  meal_time_end_hour integer,
  meal_time_end_minute integer,
  meal_time_start_hour integer,
  meal_time_start_minute integer,
  CONSTRAINT conf_general_history_pkey PRIMARY KEY (id, _revision),
  CONSTRAINT fk77a6922d54d10ea FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

CREATE sequence seq_conf_year
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE;

CREATE TABLE conf_year
(
  id bigint NOT NULL DEFAULT nextval('seq_conf_year'::regclass),
  day_expiry_vacation_past_year integer,
  hour_max_to_calculate_worktime integer,
  max_recovery_days_49 integer,
  max_recovery_days_13 integer,
  month_expire_recovery_days_49 integer,
  month_expire_recovery_days_13 integer,
  month_expiry_vacation_past_year integer,
  year integer,
  CONSTRAINT conf_year_pkey PRIMARY KEY (id)
);

CREATE TABLE conf_year_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL,
  _revision_type smallint,
  day_expiry_vacation_past_year integer,
  hour_max_to_calculate_worktime integer,
  max_recovery_days_49 integer,
  max_recovery_days_13 integer,
  month_expire_recovery_days_49 integer,
  month_expire_recovery_days_13 integer,
  month_expiry_vacation_past_year integer,
  year integer,
  CONSTRAINT conf_year_history_pkey PRIMARY KEY (id, _revision),
  CONSTRAINT fkd883fbcdd54d10ea FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

INSERT INTO conf_general(
            email_to_contact, init_use_program, institute_name, number_of_viewing_couple, 
            password_to_presence, seat_code, url_to_presence, user_to_presence, 
            day_of_patron, month_of_patron, web_stamping_allowed, meal_time_end_hour, 
            meal_time_end_minute, meal_time_start_hour, meal_time_start_minute)
            SELECT  email_to_contact, init_use_program, institute_name, numberofviewingcouplecolumn,
            password_to_presence, seat_code, url_to_presence, user_to_presence, day_of_patron, month_of_patron, false, 
            mealtimeendhour, mealtimeendminute, mealtimestarthour, mealtimestartminute  
            FROM configurations;
            
INSERT INTO conf_year(
            day_expiry_vacation_past_year, hour_max_to_calculate_worktime, max_recovery_days_49, max_recovery_days_13, 
            month_expire_recovery_days_49, month_expire_recovery_days_13, 
            month_expiry_vacation_past_year, year)
    		SELECT  dayexpiryvacationpastyear, hourmaxtocalculateworktime, maxrecoverydaysfournine, maxrecoverydaysonethree, 
    		monthexpirerecoverydaysfournine, monthexpirerecoverydaysonethree, monthexpiryvacationpastyear, 2013
            FROM configurations;            
            
INSERT INTO conf_year(
            day_expiry_vacation_past_year, hour_max_to_calculate_worktime, max_recovery_days_49, max_recovery_days_13, 
            month_expire_recovery_days_49, month_expire_recovery_days_13, 
            month_expiry_vacation_past_year, year)
    		SELECT  dayexpiryvacationpastyear, hourmaxtocalculateworktime, maxrecoverydaysfournine, maxrecoverydaysonethree, 
    		monthexpirerecoverydaysfournine, monthexpirerecoverydaysonethree, monthexpiryvacationpastyear, 2014
            FROM configurations;

    
    


