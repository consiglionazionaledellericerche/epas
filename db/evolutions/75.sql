# ---!Ups

-- Aggiungere alla tabella mealticket l'informazione di buono pasto restituito

ALTER TABLE meal_ticket ADD COLUMN returned BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE meal_ticket_history ADD COLUMN returned BOOLEAN DEFAULT false;

# ---!Downs

ALTER TABLE meal_ticket DROP COLUMN returned;
ALTER TABLE meal_ticket_history DROP COLUMN returned;





