# ---!Ups

update persons 
SET badgenumber = default 
where badgenumber = '0000000' or badgenumber = '';

ALTER TABLE persons ADD CONSTRAINT badge_office_key UNIQUE (badgenumber, office_id);

# ---!Downs

ALTER TABLE persons DROP CONSTRAINT badge_office_key;
