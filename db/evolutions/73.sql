# ---!Ups

-- Aggiungere alla tabella personDay 
--  il tempo timbrature 
--  il tempo giustificato buono pasto
--  il tempo giustificato non buono pasto (si potrebbe dedurre dagli altri due)

ALTER TABLE person_days ADD COLUMN stamping_time INTEGER;
ALTER TABLE person_days ADD COLUMN justified_time_no_meal INTEGER;
ALTER TABLE person_days ADD COLUMN justified_time_meal INTEGER;

ALTER TABLE person_days_history ADD COLUMN stamping_time INTEGER;
ALTER TABLE person_days_history ADD COLUMN justified_time_no_meal INTEGER;
ALTER TABLE person_days_history ADD COLUMN justified_time_meal INTEGER;

# ---!Downs

ALTER TABLE person_days DROP COLUMN stamping_time INTEGER;
ALTER TABLE person_days DROP COLUMN justified_time_no_meal INTEGER;
ALTER TABLE person_days DROP COLUMN justified_time_meal INTEGER;

ALTER TABLE person_days_history DROP COLUMN stamping_time INTEGER;
ALTER TABLE person_days_history DROP COLUMN justified_time_no_meal INTEGER;
ALTER TABLE person_days_history DROP COLUMN justified_time_meal INTEGER;





