# ---!Ups

UPDATE roles SET name = 'technicalAdmin' where name = 'tecnicalAdmin';

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles text NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_roles_history (
    _revision INTEGER NOT NULL,
    _revision_type smallint,
    user_id BIGINT NOT NULL,
    roles text,

    PRIMARY KEY (_revision, user_id, roles),
    FOREIGN KEY (_revision) REFERENCES revinfo(rev)
);

INSERT INTO user_roles select id,'DEVELOPER' from users where username = 'developer';
INSERT INTO user_roles select id,'ADMIN' from users where username = 'admin';

DELETE FROM users_roles_offices WHERE user_id in (SELECT id FROM users WHERE username = 'developer');
DELETE FROM users_roles_offices WHERE user_id in (SELECT id FROM users WHERE username = 'admin');

DELETE FROM roles WHERE name = 'admin';
DELETE FROM roles WHERE name = 'developer';

# ---!Downs

UPDATE roles SET name = 'tecnicalAdmin' where name = 'technicalAdmin';

DROP TABLE user_roles;
DROP TABLE user_roles_history;

INSERT INTO roles (name) values ('admin');
INSERT INTO roles (name) values ('developer');