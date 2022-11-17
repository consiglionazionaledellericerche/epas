# --- !Ups

CREATE TABLE parental_leave_requests(
	information_request_id BIGINT NOT NULL REFERENCES information_requests (id),
	begin_date DATE,
	end_date DATE,
	born_certificate TEXT,
	expected_date_of_birth TEXT,
	version INT DEFAULT 0);
	
CREATE TABLE parental_leave_requests_history(
	information_request_id BIGINT NOT NULL,
	id BIGINT NOT NULL,
	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
    _revision_type SMALLINT NOT NULL,
    begin_date DATE,
	end_date DATE,
	born_certificate TEXT,
	expected_date_of_birth TEXT,
	PRIMARY KEY (information_request_id, _revision, _revision_type)
);

# --- !Downs

DROP TABLE parental_leave_requests_history;
DROP TABLE parental_leave_requests;