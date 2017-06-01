# ---!Ups

INSERT INTO roles (name) values ('seatSupervisor');
INSERT INTO roles_history (id, _revision, _revision_type, name) SELECT id, (SELECT MAX(rev) AS rev FROM revinfo), 0, name  FROM roles WHERE name = 'seatSupervisor';

select * from roles;

# ---!Downs

delete from users_roles_offices where role_id in (select id from roles where name = 'seatSupervisor');
delete from roles where name = 'seatSupervisor';
delete from roles_history where name = 'seatSupervisor';