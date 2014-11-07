# --- !Ups

DELETE FROM conf_year WHERE EXISTS (select * from office where name = 'NOME-DA-DEFINIRE');
DELETE FROM conf_general WHERE EXISTS (select * from office where name = 'NOME-DA-DEFINIRE');
DELETE FROM office WHERE EXISTS (select * from office where name = 'NOME-DA-DEFINIRE');

select setval('seq_office', (select max(id) FROM office));
select setval('seq_absence_type_groups', max(id)) FROM absence_type_groups;
select setval('seq_absence_types', max(id)) FROM absence_types;
select setval('seq_qualifications', max(id)) FROM qualifications;
select setval('seq_competence_codes', max(id)) FROM competence_codes;
select setval('seq_stamp_modification_types', max(id)) FROM stamp_modification_types;
select setval('seq_stamp_types', max(id)) FROM stamp_types;
select setval('seq_vacation_codes', max(id)) FROM vacation_codes;
select setval('seq_working_time_types', max(id)) FROM working_time_types;
select setval('seq_working_time_type_days', max(id)) FROM working_time_type_days;


INSERT INTO conf_general (field, field_value, office_id) 
	SELECT 'date_start_meal_ticket', null, office_id 
	FROM conf_general 
	GROUP BY office_id; 

# ---!Downs

DELETE FROM conf_general WHERE field = 'date_start_meal_ticket';