# ---!Ups

ALTER TABLE meal_ticket_history ADD COLUMN office_id BIGINT;

ALTER TABLE meal_ticket ADD COLUMN office_id BIGINT;
ALTER TABLE meal_ticket ADD CONSTRAINT "mealticket_officefk" FOREIGN KEY (office_id) REFERENCES office(id);
ALTER TABLE meal_ticket ADD CONSTRAINT code_office_id UNIQUE (code, office_id);

UPDATE meal_ticket 
SET office_id = persons.office_id
FROM persons JOIN contracts ON contracts.person_id = persons.id
WHERE contracts.id = meal_ticket.contract_id;


ALTER TABLE meal_ticket ALTER COLUMN office_id SET NOT NULL;


# ---!Downs


ALTER TABLE meal_ticket DROP CONSTRAINT code_office_id;
ALTER TABLE meal_ticket DROP CONSTRAINT "mealticket_officefk";
ALTER TABLE meal_ticket DROP COLUMN office_id;