# ---!Ups

DELETE FROM persons_permissions where permissions_id = 14;

DELETE FROM permissions where id = 14 and description = 'insertAndUpdateOffices';

INSERT INTO permissions (description) values ('insertAndUpdateOffices');


# ---!Downs

DELETE FROM permissions where description = 'insertAndUpdateOffices';
INSERT INTO permissions (id, description) values (14, 'insertAndUpdateOffices');