# ---!Ups

alter table contracts add column source_date_vacation date;
update contracts set source_date_vacation = source_date_residual;

# ---!Downs

alter table contracts drop column source_date_vacation;
