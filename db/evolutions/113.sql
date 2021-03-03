# ---!Ups

ALTER TABLE contractual_clauses RENAME COLUMN description TO fruition_time;
ALTER TABLE contractual_clauses ADD COLUMN how_to_request TEXT;
ALTER TABLE contractual_clauses ADD COLUMN supporting_documentation TEXT;
ALTER TABLE contractual_clauses ADD COLUMN legal_and_economic TEXT;
ALTER TABLE contractual_clauses ADD COLUMN other_infos TEXT;

ALTER TABLE contractual_clauses_history RENAME COLUMN description TO fruition_time;
ALTER TABLE contractual_clauses_history ADD COLUMN how_to_request TEXT;
ALTER TABLE contractual_clauses_history ADD COLUMN supporting_documentation TEXT;
ALTER TABLE contractual_clauses_history ADD COLUMN legal_and_economic TEXT;
ALTER TABLE contractual_clauses_history ADD COLUMN other_infos TEXT;

# ---!Downs

ALTER TABLE contractual_clauses_history DROP COLUMN other_infos;
ALTER TABLE contractual_clauses_history DROP COLUMN legal_and_economic;
ALTER TABLE contractual_clauses_history DROP COLUMN supporting_documentation;
ALTER TABLE contractual_clauses_history DROP COLUMN how_to_request;
ALTER TABLE contractual_clauses_history RENAME COLUMN fruition_time TO description;

ALTER TABLE contractual_clauses DROP COLUMN other_infos;
ALTER TABLE contractual_clauses DROP COLUMN legal_and_economic;
ALTER TABLE contractual_clauses DROP COLUMN supporting_documentation;
ALTER TABLE contractual_clauses DROP COLUMN how_to_request;
ALTER TABLE contractual_clauses RENAME COLUMN fruition_time TO description;
