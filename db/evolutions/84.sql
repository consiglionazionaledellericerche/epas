# ---!Ups

ALTER TABLE persons RENAME COLUMN created_at TO begin_date;
ALTER TABLE persons RENAME COLUMN updated_at TO end_date;

ALTER TABLE persons ALTER COLUMN begin_date SET DATA TYPE date;
ALTER TABLE persons ALTER COLUMN end_date SET DATA TYPE date;

UPDATE persons SET end_date = NULL;

CREATE TABLE person_configurations (
  id BIGSERIAL PRIMARY KEY,
  person_id BIGINT NOT NULL,
  epas_param TEXT NOT NULL,
  field_value TEXT NOT NULL,
  begin_date DATE NOT NULL,
  end_date DATE,
  FOREIGN KEY (person_id) REFERENCES persons (id)
);

CREATE TABLE person_configurations_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  person_id BIGINT,
  epas_param TEXT,
  field_value TEXT,
  begin_date DATE,
  end_date DATE
);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    version integer,
    message TEXT,
    reference TEXT,
    subject_id INT,
    subject TEXT,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at timestamp without time zone,
    updated_at timestamp without time zone

);

ALTER TABLE absence_type_groups ADD COLUMN description TEXT;
ALTER TABLE absence_type_groups_history ADD COLUMN description TEXT;

# ---!Downs


DROP TABLE person_configurations;
DROP TABLE person_configurations_history;

ALTER TABLE persons ALTER COLUMN begin_date SET DATA TYPE timestamp with time zone;
ALTER TABLE persons ALTER COLUMN end_date SET DATA TYPE timestamp with time zone;

ALTER TABLE persons RENAME COLUMN begin_date TO created_at;
ALTER TABLE persons RENAME COLUMN end_date TO update_at;

DROP TABLE notifications;

ALTER TABLE absence_type_groups DROP COLUMN description;
ALTER TABLE absence_type_groups_history DROP COLUMN description;
