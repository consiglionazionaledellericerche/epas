# ---!Ups

INSERT INTO stamp_modification_types (code, description) VALUES('md','Timbratura modificata dal dipendente');
ALTER TABLE stampings ADD COLUMN marked_by_employee BOOLEAN;
UPDATE stampings SET marked_by_employee = FALSE;
ALTER TABLE stampings_history ADD COLUMN marked_by_employee BOOLEAN;
UPDATE stampings_history SET marked_by_employee = FALSE;

# ---!Downs

DELETE FROM stamp_modification_types where code = 'md';
ALTER TABLE stampings_history DROP COLUMN marked_by_employee;
ALTER TABLE stampings DROP COLUMN marked_by_employee;

