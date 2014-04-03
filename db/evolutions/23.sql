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

ALTER TABLE shift_type ADD COLUMN shift_time_table_id INT REFERENCES shift_time_table(id);

UPDATE person_shift_days SET shift_slot = 'MORNING' WHERE shift_time_table_id = 82;
UPDATE person_shift_days SET shift_slot = 'AFTERNOON' WHERE shift_time_table_id = 83;

ALTER TABLE person_shift_days DROP COLUMN shift_time_table_id;

DELETE FROM shift_time_table;

CREATE SEQUENCE seq_shift_time_table;
ALTER TABLE shift_time_table ALTER COLUMN id SET DEFAULT nextval('seq_shift_time_table');
 
INSERT INTO shift_time_table (start_morning, end_morning, start_afternoon, end_afternoon,
	start_morning_lunch_time, end_morning_lunch_time, start_afternoon_lunch_time, end_afternoon_lunch_time,
	total_working_minutes, paid_minutes)
	VALUES ('07:00:00', '14:00:00', '12:30:00', '19:00:00', 
			'12:30:00','13:00:00', '13:30:00', '14:00:00', 720, 390);

UPDATE shift_type SET shift_time_table_id = 1;


# ---!Downs
  
ALTER TABLE shift_time_table ADD COLUMN startshift TIMESTAMP without time zone;
ALTER TABLE shift_time_table ADD COLUMN endshift TIMESTAMP without time zone;
ALTER TABLE shift_time_table ADD COLUMN description VARCHAR(64);

ALTER TABLE shift_type DROP COLUMN shift_time_table_id;

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

ALTER TABLE person_shift_days ADD COLUMN shift_time_table_id INT REFERENCES shift_time_table(id);

INSERT INTO shift_time_table (id, startshift, endshift, description) VALUES (82, '01-01-12 07:00:00 AM', '01-01-12 01:30:00 PM', 'turno mattina');
INSERT INTO shift_time_table (id, startshift, endshift, description) VALUES (83, '01-01-12 01:30:00 PM', '01-01-12 07:00:00 PM', 'turno pomeriggio');

UPDATE person_shift_days SET shift_time_table_id = 82 WHERE shift_slot = 'MORNING';
UPDATE person_shift_days SET shift_time_table_id = 83 WHERE shift_slot = 'AFTERNOON';

ALTER TABLE person_shift_days DROP COLUMN shift_slot;

DROP SEQUENCE seq_shift_time_table CASCADE;