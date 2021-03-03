# ---!Ups

ALTER TABLE certificated_data ADD COLUMN traininghours_sent varchar(255);
ALTER TABLE certificated_data_history ADD COLUMN traininghours_sent varchar(255);
ALTER TABLE certificated_data DROP COLUMN ok;
ALTER TABLE certificated_data_history DROP COLUMN ok;

# ---!Downs

ALTER TABLE certificated_data DROP COLUMN traininghours_sent;
ALTER TABLE certificated_data_history DROP COLUMN traininghours_sent;
ALTER TABLE certificated_data ADD COLUMN ok boolean;
ALTER TABLE certificated_data_history ADD COLUMN ok boolean;