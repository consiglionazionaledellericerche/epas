# --- !Ups

ALTER TABLE persons DROP COLUMN badgenumber;
ALTER TABLE persons_history DROP COLUMN badgenumber;

# --- !Downs

ALTER TABLE persons_history ADD COLUMN badgenumber VARCHAR (255);
ALTER TABLE persons ADD COLUMN badgenumber VARCHAR (255);