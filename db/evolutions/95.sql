# ---!Ups

CREATE TABLE person_shift_days_history(
  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  "date" DATE,
  person_shift_id BIGINT,
  shift_type_id BIGINT,
  shift_slot TEXT
);

CREATE TABLE person_shift_day_in_trouble (
	id BIGSERIAL PRIMARY KEY,
	cause TEXT NOT NULL,
	person_shift_day_id BIGINT NOT NULL REFERENCES person_shift_days(id),
	email_sent boolean,
	CONSTRAINT only_one_type_in_a_day UNIQUE(cause, person_shift_day_id)
);

CREATE TABLE person_shift_day_in_trouble_history (
  id BIGINT,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
  cause TEXT,
	email_sent boolean,
	person_shift_day_id BIGINT,
  PRIMARY KEY (id, _revision)
);

# ---!Downs

DROP TABLE person_shift_days_history;
DROP TABLE person_shift_day_in_trouble_history;
DROP TABLE person_shift_day_in_trouble;