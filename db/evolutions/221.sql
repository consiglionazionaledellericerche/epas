# --- !Ups

ALTER TABLE contracts ADD COLUMN contract_type TEXT;
ALTER TABLE contracts_history ADD COLUMN contract_type TEXT;

UPDATE contracts SET contract_type = 'structured_public_administration' WHERE on_certificate = true;
UPDATE contracts SET contract_type = 'unstructured' WHERE on_certificate = false;

ALTER TABLE contracts DROP COLUMN on_certificate;
ALTER TABLE contracts_history DROP COLUMN on_certificate;


# --- !Downs
# -- Non è necessaria una down