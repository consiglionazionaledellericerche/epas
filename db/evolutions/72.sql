# ---!Ups

-- Issues #160 elimina le doppie occorrenze nella tabella meal tickets
-- e aggiunge il vincolo di unicità nella tabella.

-- creo tabella di supporto con gli id da cancellare.
create table mealaux ( id bigint, code text, block integer, number integer, 
                       date date, expire_date date, admin_id bigint, contract_id bigint);

-- cancellare tutte le occorrenze doppie, lasciandone solo una (comando un pò lento).
-- http://stackoverflow.com/questions/6583916/ --
insert into mealaux 
	select a.id as id, a.code as code, a.block as block, 
	       a.number as number, a.date as date, a.expire_date as expire_date, 
               a.admin_id as admin_id, a.contract_id as contract_id
	from meal_ticket a 
	where a.id <> (select min(b.id) from meal_ticket b where a.code = b.code);

-- eliminazione delle occorrenze dalla tabella e nello storico
delete from meal_ticket using mealaux where meal_ticket.id = mealaux.id;
delete from meal_ticket_history using mealaux where meal_ticket_history.id = mealaux.id;

-- eliminazione tabella di supporto.
-- (disattivata, probabilmente per sicurezza potrebbe fare comodo controllarla dopo l'evoluzione)
-- drop table mealaux;

-- inserimento del vincolo di unicità
alter table meal_ticket add CONSTRAINT code_unique_key unique (code);

# ---!Downs

drop table mealaux;

alter table meal_ticket drop constraint code_unique_key;





