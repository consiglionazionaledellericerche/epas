# ---!Ups

UPDATE stampings SET way = 'out' WHERE way IS NULL;

ALTER TABLE stampings ALTER COLUMN way SET NOT NULL;
ALTER TABLE stampings ALTER COLUMN "date" SET NOT NULL;

# ---!Downs