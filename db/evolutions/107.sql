# ---!Ups

CREATE TABLE users_roles_offices_history (
	id BIGINT NOT NULL,
  	_revision INTEGER NOT NULL REFERENCES revinfo(rev),
  	_revision_type SMALLINT NOT NULL,
	office_id BIGINT,
	role_id BIGINT,
	user_id BIGINT,
  	PRIMARY KEY (id, _revision, _revision_type)
	);

INSERT INTO users_roles_offices_history (id, _revision, _revision_type, office_id, role_id, user_id)
SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, office_id, role_id, user_id FROM users_roles_offices;

# ---!Downs

DROP TABLE users_roles_offices_history;

