# ---!Ups

CREATE INDEX competence_year_idx ON competences(year);
CREATE INDEX competence_month_idx ON competences(month);
CREATE INDEX competence_value_approved_idx ON competences(value_approved);

# ---!Downs

DROP INDEX competence_value_approved_idx;
DROP INDEX competence_month_idx;
DROP INDEX competence_year_idx;