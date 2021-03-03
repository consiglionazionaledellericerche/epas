# --- !Ups

ALTER TABLE persons 
  ADD COLUMN birthday date;
 
ALTER TABLE persons_history 
  ADD COLUMN birthday date;

# ---!Downs

ALTER TABLE persons 
  DROP COLUMN birthday;
  
ALTER TABLE persons_history 
  DROP COLUMN birthday;
