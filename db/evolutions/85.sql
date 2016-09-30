# ---!Ups

UPDATE roles SET name = 'technicalAdmin' where name = 'tecnicalAdmin';

# ---!Downs

UPDATE roles SET name = 'tecnicalAdmin' where name = 'technicalAdmin';