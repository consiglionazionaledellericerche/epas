# --- !Ups

ALTER TABLE contracts_working_time_types ADD COLUMN external_id TEXT;

CREATE TABLE contracts_working_time_types_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT NOT NULL,
  begin_date DATE,
  end_date DATE,
  contract_id BIGINT,
  working_time_type_id BIGINT,
  external_id TEXT,
  PRIMARY KEY (id, _revision, _revision_type)
);

-- Creo una nuova revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

INSERT INTO contracts_working_time_types_history (
    id, _revision, _revision_type, begin_date, end_date, contract_id, working_time_type_id, external_id)
  SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, begin_date, end_date, contract_id, working_time_type_id, external_id
    FROM contracts_working_time_types;

# --- !Downs

ALTER TABLE contracts_working_time_types DROP COLUMN external_id;
DROP TABLE contracts_working_time_types_history;
