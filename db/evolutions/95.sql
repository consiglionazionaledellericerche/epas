# ---!Ups

ALTER TABLE meal_ticket DROP CONSTRAINT "code_unique_key";

# ---!Downs


ALTER TABLE meal_ticket ADD CONSTRAINT "code_unique_key" UNIQUE (code);