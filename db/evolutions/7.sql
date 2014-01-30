# ---!Ups

ALTER TABLE "person_children" ALTER COLUMN "borndate" TYPE date using ("borndate"::text::date);