# --- !Ups

ALTER TABLE contracts ADD COLUMN previous_contract_id BIGINT REFERENCES contracts(id);
ALTER TABLE contracts_history ADD COLUMN previous_contract_id BIGINT REFERENCES contracts(id);

# --- !Downs

ALTER TABLE contracts_history DROP COLUMN previous_contract_id;
ALTER TABLE contracts DROP COLUMN previous_contract_id;
