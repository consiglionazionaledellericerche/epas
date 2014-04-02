# ---!Ups

CREATE SEQUENCE seq_users
  START WITH 10000
  INCREMENT BY 1
  NO MINVALUE
  NO MAXVALUE;

CREATE TABLE users
(
  id bigint NOT NULL DEFAULT nextval('seq_users'::regclass),
  expire_recovery_token date,
  password character varying(255),
  recovery_token character varying(255),
  username character varying(255),
  CONSTRAINT users_pkey PRIMARY KEY (id)
);


CREATE TABLE users_history
(
  id bigint NOT NULL,
  _revision integer NOT NULL,
  _revision_type smallint,
  expire_recovery_token date,
  password character varying(255),
  recovery_token character varying(255),
  username character varying(255),
  CONSTRAINT users_history_pkey PRIMARY KEY (id, _revision),
  CONSTRAINT fk5c2b915dd54d10ea FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

CREATE TABLE users_permissions
(
  users_id bigint NOT NULL,
  permissions_id bigint NOT NULL,
  CONSTRAINT fkf0f758334428ea9 FOREIGN KEY (permissions_id)
      REFERENCES permissions (id),
  CONSTRAINT fkf0f7583a4f8de2b FOREIGN KEY (users_id)
      REFERENCES users (id)
);

CREATE TABLE users_permissions_history
(
  _revision integer NOT NULL,
  users_id bigint NOT NULL,
  permissions_id bigint NOT NULL,
  _revision_type smallint,
  CONSTRAINT users_permissions_history_pkey PRIMARY KEY (_revision, users_id, permissions_id),
  CONSTRAINT fk799ecdd8d54d10ea FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);


ALTER TABLE persons
  ADD COLUMN user_id bigint,
  ADD CONSTRAINT fkd78fcfbe47140efe FOREIGN KEY (user_id) REFERENCES users (id);
  

ALTER TABLE persons_history
  ADD COLUMN user_id bigint;

INSERT INTO users(id, username, password) SELECT  id, username, password FROM persons;

ALTER TABLE persons
  DROP COLUMN username,
  DROP COLUMN password;
  
ALTER TABLE persons_history
  DROP COLUMN username,
  DROP COLUMN password;

INSERT INTO users_permissions(users_id, permissions_id) SELECT users_id, permissions_id FROM persons_permissions;

DROP TABLE persons_permissions;
DROP TABLE persons_permissions_history;

UPDATE persons set user_id = id;

DELETE FROM persons_working_time_types where person_id = 1;
DELETE FROM persons where id = 1;


# ---!Downs

CREATE TABLE persons_permissions
(
  users_id bigint NOT NULL,
  permissions_id bigint NOT NULL,
  CONSTRAINT fkf0f758334428ea9 FOREIGN KEY (permissions_id)
      REFERENCES permissions (id),
  CONSTRAINT fkf0f7583a4f8de2b FOREIGN KEY (users_id)
      REFERENCES persons (id)
);

CREATE TABLE persons_permissions_history
(
  _revision integer NOT NULL,
  users_id bigint NOT NULL,
  permissions_id bigint NOT NULL,
  _revision_type smallint,
  CONSTRAINT persons_permissions_history_pkey PRIMARY KEY (_revision, users_id, permissions_id),
  CONSTRAINT fk799ecdd8d54d10ea FOREIGN KEY (_revision)
      REFERENCES revinfo (rev)
);

ALTER TABLE persons 
  ADD COLUMN username character varying(255),
  ADD COLUMN password character varying(255),
  DROP CONSTRAINT fkd78fcfbe47140efe,
  DROP COLUMN user_id;
  
ALTER TABLE persons_history 
  ADD COLUMN username character varying(255),
  ADD COLUMN password character varying(255),
  DROP COLUMN user_id;
  
DROP TABLE users_permissions_history;

DROP TABLE users_permissions;

DROP TABLE users_history;

DROP TABLE users;

DROP SEQUENCE seq_users;

