# ---!Ups

ALTER TABLE person_shift_days ALTER id SET DEFAULT nextval('seq_person_shift_days'::regclass);

# ---!Downs

