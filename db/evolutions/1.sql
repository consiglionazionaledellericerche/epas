# epas-devel schema
 
# --- !Ups
 
ALTER TABLE absence_types ADD COLUMN considered_week_end boolean default false;
UPDATE absence_types SET considered_week_end = TRUE WHERE code in ('21','38','11','111','111U','115','116','117','118','119','11C','11R','11R5','11R9','11S');

# --- !Downs
 
ALTER TABLE absence_types DROP COLUMN considered_week_end ; 
 