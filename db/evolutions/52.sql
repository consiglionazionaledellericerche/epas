# ---!Ups

ALTER TABLE office ALTER COLUMN code SET DATA TYPE varchar(255);
ALTER TABLE office RENAME COLUMN code TO codeId;
ALTER TABLE office ADD COLUMN code varchar(255);
ALTER TABLE office ADD COLUMN cds varchar(255);


ALTER TABLE office_history ALTER COLUMN code SET DATA TYPE varchar(255);
ALTER TABLE office_history RENAME COLUMN code TO codeId;
ALTER TABLE office_history ADD COLUMN code varchar(255);
ALTER TABLE office_history ADD COLUMN cds varchar(255);

# ---!Downs

ALTER TABLE office ALTER COLUMN codeId SET DATA TYPE integer;
ALTER TABLE office RENAME COLUMN codeId TO code;
ALTER TABLE office DROP COLUMN code;
ALTER TABLE office DROP COLUMN cds;

ALTER TABLE office_history ALTER COLUMN codeId SET DATA TYPE integer;
ALTER TABLE office_history RENAME COLUMN codeId TO code;
ALTER TABLE office_history DROP COLUMN code;
ALTER TABLE office_history DROP COLUMN cds;