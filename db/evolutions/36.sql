# --- !Ups
UPDATE persons set want_email = true;

# ---!Downs

update persons set want_email = null;