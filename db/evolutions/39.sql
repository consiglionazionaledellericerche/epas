# --- !Ups
alter table absence_types_history drop column compensatory_rest;
alter table absence_types_history drop column ignore_stamping;
alter table absence_types drop column compensatory_rest;
alter table absence_types drop column ignore_stamping;
alter table competence_codes drop column inactive;

alter table permissions_groups drop constraint fk7abb85ef522ebd41;
alter table web_stamping_address drop constraint fk84df567fac97433e;
alter table persons_groups drop constraint fkdda67575522ebd41;
drop table auth_users;
drop table web_stamping_address_history;
drop table web_stamping_address;
drop table configurations_history;
drop table configurations;
drop table groups_history;
drop table groups;
drop table options;
drop table valuable_competences;

drop table year_recaps;

# --- !Downs

alter table absence_types_history add column compensatory_rest integer;
alter table absence_types_history add column ignore_stamping boolean;
alter table absence_types add column compensatory_rest integer;
alter table absence_types add column ignore stamping boolean;

add table auth_users;
add table configurations_history;
add table configurations;
add table groups_history;
add table groups;
add table options;
add table valuable_competences;
add table web_stamping_address_history;
add table web_stamping_address;
add table year_recaps;


