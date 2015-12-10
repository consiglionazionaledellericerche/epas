# ---!Ups

CREATE SEQUENCE seq_users_permissions_offices
  START WITH 1
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;
  
CREATE TABLE users_permissions_offices(
id bigint not null DEFAULT nextval('seq_users_permissions_offices'::regclass),
user_id bigint not null,
permission_id bigint not null,
office_id bigint,
CONSTRAINT users_permissions_offices_pkey PRIMARY KEY (id),
CONSTRAINT user_id_fk FOREIGN KEY (user_id) REFERENCES users (id),
CONSTRAINT permission_id_fk FOREIGN KEY (permission_id) REFERENCES permissions (id)
);

INSERT INTO users_permissions_offices (user_id, permission_id) SELECT users_id, permissions_id from users_permissions; 
 
UPDATE users_permissions_offices set office_id = office.id from office where joining_date is null;
ALTER TABLE users_permissions_offices ALTER COLUMN office_id SET NOT NULL;
ALTER TABLE users_permissions_offices ADD CONSTRAINT office_id_fk FOREIGN KEY (office_id) REFERENCES office (id);

# ---!Downs

DROP TABLE users_permissions_offices;
DROP SEQUENCE seq_users_permissions_offices;