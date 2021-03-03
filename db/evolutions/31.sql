# --- !Ups

ALTER TABLE office 
  DROP COLUMN discriminator;

ALTER TABLE office_history
  DROP COLUMN discriminator; 
# ---!Downs

ALTER TABLE office 
  ADD COLUMN discriminator character varying(31);
  
ALTER TABLE office_history 
  ADD COLUMN discriminator character varying(31);
