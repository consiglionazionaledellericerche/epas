# ---!Ups

ALTER TABLE persons DROP COLUMN cnr_email;
ALTER TABLE persons_history DROP COLUMN cnr_email;

# ---!Downs

ALTER TABLE persons ADD COLUMN cnr_email varchar(255);
ALTER TABLE persons_history ADD COLUMN cnr_email varchar(255);
