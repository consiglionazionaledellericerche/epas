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

-- aggiunge il campo di configurazione di inizio utilizzo dei buoni pasto
INSERT INTO conf_general (field, field_value, office_id) 
	SELECT 'date_start_meal_ticket', null, office_id 
	FROM conf_general 
	GROUP BY office_id; 

-- aggiunge il campo dei meal ticket residui al riepilogo annuale
ALTER TABLE contract_year_recap ADD COLUMN remaining_meal_tickets INTEGER;
UPDATE contract_year_recap set remaining_meal_tickets = 0;

-- sposta la relazione del meal_ticket dalla persona al contratto attivo alla data di consegna del buono pasto
ALTER TABLE meal_ticket ADD COLUMN contract_id bigint REFERENCES contracts(id);
UPDATE meal_ticket AS mt 
    SET contract_id = t.contract_id 
    
    FROM (
		SELECT 
			c.id AS contract_id, 
			p.id AS person_id, 
			c.end_contract AS cend,
			c.begin_contract AS cbeg,
			c.expire_contract AS cexp
		FROM contracts c LEFT JOIN persons p ON (c.person_id = p.id)
	) AS t 
	
	WHERE mt.person_id = t.person_id and 
		( t.cend is null 
		  and 
		  ( (t.cexp is null and t.cbeg <= mt.date ) or (t.cexp is not null and t.cbeg <= mt.date and t.cexp >= mt.date ) ) 
		  or 
		  t.cend is not null and t.cbeg <= mt.date and t.cend >= mt.date 
		);
	
ALTER TABLE meal_ticket ALTER COLUMN contract_id SET NOT NULL;
ALTER TABLE meal_ticket_history ADD COLUMN contract_id bigint;
ALTER TABLE meal_ticket DROP COLUMN person_id;
ALTER TABLE meal_ticket_history DROP COLUMN person_id;

UPDATE meal_ticket SET expire_date = '2015-12-31' WHERE expire_date IS null;
ALTER TABLE meal_ticket ALTER COLUMN expire_date SET NOT NULL;


# ---!Downs

--DOWN sposta la relazione del meal_ticket dalla persona al contratto attivo alla data di consegna del buono pasto
ALTER TABLE meal_ticket ADD COLUMN person_id bigint REFERENCES persons(id);
UPDATE meal_ticket AS mt 
    SET person_id = t.person_id 
    
    FROM (
		SELECT 
			c.id AS contract_id, 
			p.id AS person_id, 
			c.end_contract AS cend,
			c.begin_contract AS cbeg,
			c.expire_contract AS cexp
		FROM persons p LEFT JOIN contracts c ON (c.person_id = p.id)
	) AS t 
	
	WHERE mt.contract_id = t.contract_id and 
		( t.cend is null 
		  and 
		  ( (t.cexp is null and t.cbeg <= mt.date ) or (t.cexp is not null and t.cbeg <= mt.date and t.cexp >= mt.date ) ) 
		  or 
		  t.cend is not null and t.cbeg <= mt.date and t.cend >= mt.date 
		);
	
ALTER TABLE meal_ticket ALTER COLUMN person_id SET NOT NULL;
ALTER TABLE meal_ticket_history ADD COLUMN person_id bigint;
ALTER TABLE meal_ticket DROP COLUMN contract_id;
ALTER TABLE meal_ticket_history DROP COLUMN contract_id;



DELETE FROM conf_general WHERE field = 'date_start_meal_ticket';

ALTER TABLE contract_year_recap DROP COLUMN remaining_meal_tickets;
