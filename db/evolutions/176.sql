# --- !Ups

ALTER TABLE contracts_history 
  DROP constraint contracts_history_previous_contract_id_fkey;

# --- !Downs