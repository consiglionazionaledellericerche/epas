# ---!Ups

CREATE TABLE person_shift_day_in_trouble (
	person_shift_day_in_trouble_id BIGINT NOT NULL,
	cause TEXT,
	email_sent boolean,
	person_shift_day_id BIGINT,
	FOREIGN KEY (person_shift_day_id) REFERENCES person_shift_days(id)
);


CREATE TABLE person_shift_day_in_trouble_history (

    id BIGINT NOT NULL,
    _revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT,

    person_shift_day_in_trouble_id BIGINT,
    cause TEXT,
	email_sent boolean,
	person_shift_day_id BIGINT,
    PRIMARY KEY (id, _revision)
);


# ---!Downs

DROP TABLE person_shift_day_in_trouble_history;
DROP TABLE person_shift_day_in_trouble;