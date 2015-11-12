# ---!Ups

ALTER TABLE absence_types ADD CONSTRAINT absence_code_unique UNIQUE(code);


# ---!Downs

ALTER TABLE absence_types DROP CONSTRAINT absence_code_unique;
