# ---!Ups

-- aggiungere alla tabella office i campi begin_date e end_date ...

ALTER TABLE office ADD COLUMN begin_date DATE;
ALTER TABLE office ADD COLUMN end_date DATE;

ALTER TABLE office_history ADD COLUMN begin_date DATE;
ALTER TABLE office_history ADD COLUMN end_date DATE;

-- popolare il valore begin_date con la creazione dell'office nello storico
-- (e copiarlo anche nelle occorrenze dello storico) ...

UPDATE office 
SET begin_date = to_date(I.tempo, 'YYYY-MM-DD')
FROM (
  SELECT o.id AS oid, to_char(to_timestamp(r.revtstmp/1000), 'YYYY-MM-DD') AS tempo
  FROM office o 
    LEFT OUTER JOIN office_history oh ON o.id = oh.id 
    LEFT OUTER JOIN revinfo r ON oh._revision = r.rev
  WHERE _revision_type = 0) AS I 
WHERE id = I.oid;

UPDATE office_history 
SET begin_date = I.tempo 
FROM (
  SELECT oh.id AS oid, o.begin_date AS tempo
  FROM office_history oh LEFT OUTER JOIN office o ON o.id = oh.id)
AS I 
WHERE id = I.oid;  

-- (i valori che continuano ad essere null li popopolo tramite la configurazone) ...

UPDATE office
SET begin_date = to_date(T.initUse, 'YYYY-MM-DD')
FROM ( SELECT office_id AS oid, field_value AS initUse FROM conf_general WHERE office_id = 1 AND field = 'init_use_program') AS T
WHERE id = T.oid AND begin_date IS null;

UPDATE office_history
SET begin_date = to_date(T.initUse, 'YYYY-MM-DD')
FROM ( SELECT office_id AS oid, field_value AS initUse FROM conf_general WHERE office_id = 1 AND field = 'init_use_program') AS T
WHERE id = T.oid AND begin_date IS null;

-- vincolo not null al campo begin_date ...

ALTER TABLE office ALTER COLUMN begin_date SET NOT NULL;

-- creare la tabella della nuova configurazione (con storico) ...

CREATE TABLE configurations (
  id BIGSERIAL PRIMARY KEY,
  office_id BIGINT NOT NULL,
  epas_param TEXT NOT NULL,
  field_value TEXT NOT NULL,
  begin_date DATE NOT NULL,
  end_date DATE,
  FOREIGN KEY (office_id) REFERENCES office (id)
);

CREATE TABLE configurations_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  office_id BIGINT,
  epas_param TEXT,
  field_value TEXT,
  begin_date DATE,
  end_date DATE
);

# ---!Downs

DROP TABLE configurations;
DROP TABLE configurations_history;

ALTER TABLE office DROP COLUMN begin_date;
ALTER TABLE office DROP COLUMN end_date;

ALTER TABLE office_history DROP COLUMN begin_date;
ALTER TABLE office_history DROP COLUMN end_date;

