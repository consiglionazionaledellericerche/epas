# ---!Ups

ALTER TABLE office RENAME COLUMN code TO codeId;
ALTER TABLE office ADD COLUMN code varchar(255);
ALTER TABLE office ADD COLUMN cds varchar(255);


# ---!Downs

ALTER TABLE office RENAME COLUMN codeId TO code;
ALTER TABLE office DROP COLUMN code;
ALTER TABLE office DROP COLUMN cds;