# ---!Ups

-- Aggiungere alla tabella mealticket l'informazione di buono pasto restituito

ALTER TABLE meal_ticket ADD COLUMN returned BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE meal_ticket_history ADD COLUMN returned BOOLEAN DEFAULT false;

ALTER TABLE conf_year_history ALTER COLUMN office_id DROP not null ;

# ---!Downs

ALTER TABLE meal_ticket DROP COLUMN returned;
ALTER TABLE meal_ticket_history DROP COLUMN returned;





