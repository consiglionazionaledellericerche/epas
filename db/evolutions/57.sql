# ---!Ups

 -- imposta a null tutti i badgenumber che hanno piu' di un'occorrenza uguale 
UPDATE persons
SET badgenumber= default
WHERE id IN
(
    SELECT id
    FROM persons S
    INNER JOIN
    (
        SELECT badgenumber
        FROM persons
        GROUP BY badgenumber
        HAVING COUNT(*) > 1 
    ) T
    ON S.badgenumber=T.badgenumber
 );

ALTER TABLE persons ADD CONSTRAINT badge_office_key UNIQUE (badgenumber, office_id);

# ---!Downs

ALTER TABLE persons DROP CONSTRAINT badge_office_key;
