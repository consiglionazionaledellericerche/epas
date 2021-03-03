# ---!Ups

ALTER TABLE contract_month_recap ADD CONSTRAINT contract_month_recap_unique_key UNIQUE (year, month, contract_id);

# ---!Downs

ALTER TABLE contract_month_recap DROP CONSTRAINT contract_month_recap_unique_key;