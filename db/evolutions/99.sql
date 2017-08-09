# ---!Ups

CREATE TABLE reperibility_type_month (
	id BIGSERIAL PRIMARY KEY,
	version INTEGER,
	created_at timestamp without time zone,
  updated_at timestamp without time zone,
	year_month TEXT NOT NULL,
	person_reperibility_type_id BIGINT NOT NULL REFERENCES person_reperibility_types(id),
	approved boolean,
	CONSTRAINT rep_unique_in_a_month UNIQUE(year_month, person_reperibility_type_id)
);

CREATE TABLE reperibility_type_month_history (
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT NOT NULL,
  version INTEGER,
	year_month TEXT,
	person_reperibility_type_id BIGINT,
	approved boolean,
  PRIMARY KEY (id, _revision, _revision_type)
);

INSERT INTO person_reperibility_days_history (id, _revision, _revision_type, "date", holiday_day, person_reperibility_id, reperibility_type)
  SELECT id, r.rev, 0, "date", holiday_day, person_reperibility_id, reperibility_type FROM person_reperibility_days,
    (SELECT MAX(rev) AS rev FROM revinfo) AS r;
    
# ---!Downs

DROP TABLE reperibility_type_month;
DROP TABLE reperibility_type_month_history;