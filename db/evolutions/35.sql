# ---!Ups

ALTER TABLE shift_time_table DROP COLUMN startshift;
ALTER TABLE shift_time_table DROP COLUMN endshift;
ALTER TABLE shift_time_table DROP COLUMN description;

ALTER TABLE shift_time_table ADD COLUMN start_morning VARCHAR(64);
ALTER TABLE shift_time_table ADD COLUMN end_morning VARCHAR(64);
ALTER TABLE shift_time_table ADD COLUMN start_afternoon VARCHAR(64);
ALTER TABLE shift_time_table ADD COLUMN end_afternoon VARCHAR(64);

ALTER TABLE shift_time_table ADD COLUMN start_morning_lunch_time VARCHAR(64);
ALTER TABLE shift_time_table ADD COLUMN end_morning_lunch_time VARCHAR(64);
ALTER TABLE shift_time_table ADD COLUMN start_afternoon_lunch_time VARCHAR(64);
ALTER TABLE shift_time_table ADD COLUMN end_afternoon_lunch_time VARCHAR(64);

ALTER TABLE shift_time_table ADD COLUMN total_working_minutes INT;
ALTER TABLE shift_time_table ADD COLUMN paid_minutes INT;

ALTER TABLE person_shift_days ADD COLUMN shift_slot VARCHAR(64);

ALTER TABLE shift_type ADD COLUMN shift_time_table_id bigint REFERENCES shift_time_table(id);
ALTER TABLE shift_type ADD COLUMN supervisor bigint REFERENCES persons(id);

UPDATE person_shift_days SET shift_slot = 'MORNING' WHERE shift_time_table_id = 82;
UPDATE person_shift_days SET shift_slot = 'AFTERNOON' WHERE shift_time_table_id = 83;

ALTER TABLE person_shift_days DROP COLUMN shift_time_table_id;

DELETE FROM shift_time_table;

ALTER TABLE shift_time_table ALTER COLUMN id SET DEFAULT nextval('seq_shift_time_table');
SELECT setval('seq_shift_time_table', 1);
 
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('07:00:00', '14:00:00', '12:30:00', '19:00:00', 
			'12:30:00','13:00:00', '13:30:00', '14:00:00', 720, 390);
			
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('06:00:00', '13:00:00', '11:30:00', '18:00:00', 
			'12:00:00','12:30:00', '12:30:00', '13:00:00', 720, 390);
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('06:30:00', '13:30:00', '12:00:00', '18:30:00', 
			'12:30:00','13:00:00', '13:00:00', '13:30:00', 720, 390);
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('07:50:00', '14:50:00', '13:20:00', '19:50:00', 
			'13:50:00','14:20:00', '14:20:00', '14:50:00', 720, 390);
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('08:20:00', '14:20:00', '13:50:00', '20:20:00', 
			'14:20:00','', '', '13:50:00', 720, 390);
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('07:00:00', '13:35:00', '13:05:00', '18:00:00', 
			'13:35:00','', '', '13:05:00', 660, 345); 
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('07:50:00', '13:35:00', '13:05:00', '18:50:00', 
			'13:35:00','', '', '13:05:00', 660, 345);
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('08:20:00', '14:05:00', '13:35:00', '19:20:00', 
			'14:05:00','', '', '13:35:00', 660, 345);
			
CREATE TABLE shift_type_history (
    id bigint NOT NULL,
    _revision integer NOT NULL,
  	_revision_type smallint,
    description character varying(255),
    type character varying(255),
    supervisor bigint,
    
    CONSTRAINT revinfo_shift_type_history_fkey FOREIGN KEY (_revision)
    REFERENCES revinfo (rev)
);

ALTER TABLE competences RENAME COLUMN valuerequest TO valuerequested;
ALTER TABLE competences ALTER COLUMN valuerequested type numeric(4,1);

# ---!Downs
  
ALTER TABLE shift_time_table ADD COLUMN startshift TIMESTAMP without time zone;
ALTER TABLE shift_time_table ADD COLUMN endshift TIMESTAMP without time zone;
ALTER TABLE shift_time_table ADD COLUMN description VARCHAR(64);

ALTER TABLE shift_type DROP COLUMN shift_time_table_id;
ALTER TABLE shift_type DROP COLUMN supervisor;

drop table if exists shift_type_history; 

ALTER TABLE shift_time_table DROP COLUMN start_morning;
ALTER TABLE shift_time_table DROP COLUMN end_morning;
ALTER TABLE shift_time_table DROP COLUMN start_afternoon;
ALTER TABLE shift_time_table DROP COLUMN end_afternoon;

ALTER TABLE shift_time_table DROP COLUMN start_morning_lunch_time;
ALTER TABLE shift_time_table DROP COLUMN end_morning_lunch_time;
ALTER TABLE shift_time_table DROP COLUMN start_afternoon_lunch_time;
ALTER TABLE shift_time_table DROP COLUMN end_afternoon_lunch_time;

ALTER TABLE shift_time_table DROP COLUMN total_working_minutes;
ALTER TABLE shift_time_table DROP COLUMN paid_minutes;

ALTER TABLE person_shift_days ADD COLUMN shift_time_table_id bigint REFERENCES shift_time_table(id);

INSERT INTO shift_time_table (id, startshift, endshift, description) VALUES (82, '01-01-12 07:00:00 AM', '01-01-12 01:30:00 PM', 'turno mattina');
INSERT INTO shift_time_table (id, startshift, endshift, description) VALUES (83, '01-01-12 01:30:00 PM', '01-01-12 07:00:00 PM', 'turno pomeriggio');

UPDATE person_shift_days SET shift_time_table_id = 82 WHERE shift_slot = 'MORNING' ;
UPDATE person_shift_days SET shift_time_table_id = 83 WHERE shift_slot = 'AFTERNOON';

ALTER TABLE person_shift_days DROP COLUMN shift_slot;

ALTER TABLE competences RENAME COLUMN valuerequested TO valuerequest;
ALTER TABLE competences ALTER COLUMN valuerequest type integer;