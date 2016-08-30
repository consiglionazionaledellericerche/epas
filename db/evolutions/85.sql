# ---!Ups

UPDATE roles SET name = 'technicalAdmin' where name = 'tecnicalAdmin';

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    roles text NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_roles_history (
    _revision INTEGER NOT NULL,
    user_id BIGINT NOT NULL,
    roles text,
    _revision_type smallint,

    PRIMARY KEY (_revision, user_id, roles)
    FOREIGN KEY (_revision) REFERENCES revinfo(rev)
);

# ---!Downs

UPDATE roles SET name = 'tecnicalAdmin' where name = 'technicalAdmin';

DROP TABLE user_roles;
DROP TABLE user_roles_history;