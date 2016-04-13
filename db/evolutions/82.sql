# ---!Ups

# -- Nuova struttura dati per attestati

CREATE TABLE certifications (
  id BIGSERIAL PRIMARY KEY,
  person_id BIGINT REFERENCES persons(id),
  year INT NOT NULL,
  month INT NOT NULL,
  content TEXT NOT NULL,
  certification_type TEXT NOT NULL,
  problems TEXT
);

CREATE TABLE certifications_history (

  id BIGINT NOT NULL,
  _revision INTEGER NOT NULL REFERENCES revinfo(rev),
  _revision_type SMALLINT,
	
  person_id BIGINT,
  year INT,
  month INT,
  content TEXT,
  certification_type TEXT,
  problems TEXT,
  
  PRIMARY KEY (id, _revision)
);


# ---!Downs

DROP TABLE certifications;
DROP TABLE certifications_history;
