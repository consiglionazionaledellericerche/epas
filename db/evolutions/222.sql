# --- !Ups

-- Creo una nouva revisione
INSERT INTO revinfo (revtstmp) VALUES (EXTRACT(EPOCH FROM NOW())::BIGINT*1000);

CREATE TABLE persons_offices (
  id BIGSERIAL PRIMARY KEY,
  person_id BIGINT,
  office_id BIGINT,
  begin_date DATE NOT NULL,
  end_date DATE,
  version INT DEFAULT 0
);

CREATE TABLE persons_offices_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT NOT NULL,
  person_id BIGINT,
  office_id BIGINT,
  begin_date DATE,
  end_date DATE
);

INSERT INTO persons_offices (person_id, office_id, begin_date, end_date)
SELECT p.id, p.office_id, p.begin_date, p.end_date
FROM persons p
;

INSERT INTO persons_offices_history (id, _revision, _revision_type, person_id, office_id, begin_date, end_date)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, person_id, office_id, begin_date, end_date
FROM persons_offices;


ALTER TABLE persons DROP COLUMN office_id;

# ---!Downs

--Non è necessaria una down
