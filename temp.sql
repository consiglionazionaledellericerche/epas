select competence0_.*, personshif2_.*, shifttype4_.*
from competences competence0_ 
inner join persons person1_ on competence0_.person_id=person1_.id 
inner join person_shift personshif2_ on person1_.id=personshif2_.person_id 
inner join person_shift_shift_type personshif3_ on personshif2_.id=personshif3_.personshifts_id 
inner join shift_type shifttype4_ on personshif3_.shifttypes_id=shifttype4_.id 
where shifttype4_.type='A' and competence0_.year=2014 and competence0_.month=5 and competence0_.competence_code_id=5 and personshif3_.end_date is null 
order by person1_.surname;

select competence0_.*, personshif2_.*, shifttype4_.*
from competences competence0_ 
inner join persons person1_ on competence0_.person_id=person1_.id 
inner join person_shift personshif2_ on person1_.id=personshif2_.person_id 
inner join person_shift_shift_type personshif3_ on personshif2_.id=personshif3_.personshifts_id 
inner join shift_type shifttype4_ on personshif3_.shifttypes_id=shifttype4_.id 
where  competence0_.year=2014 and competence0_.month=5 and competence0_.competence_code_id=5 and
(now() >= personshif3_.begin_date and (now() <= personshif3_.end_date or  personshif3_.end_date is null))
order by person1_.surname;


select competence0_.* from competences competence0_ 
inner join persons person1_ on competence0_.person_id=person1_.id 
inner join person_shift personshif2_ on person1_.id=personshif2_.person_id 
inner join person_shift_shift_type personshif3_ on personshif2_.id=personshif3_.personshifts_id 
inner join shift_type shifttype4_ on personshif3_.shifttypes_id=shifttype4_.id 
where shifttype4_.type='A'
	and competence0_.year=2014
	and competence0_.month=5
	and competence0_.competence_code_id=5
	and personshif3_.begin_date<='2014-05-01'
	order by person1_.surname asc

	and (personshif3_.end_date<='2014-05-31' or personshif3_.iend is null)
ant build -Dplay.path=/Applications/play-1.2.7/


select competence0_.*, personshif2_.*, personshif3_.* from competences competence0_ 
inner join persons person1_ on competence0_.person_id=person1_.id 
inner join person_shift personshif2_ on person1_.id=personshif2_.person_id 
inner join person_shift_shift_type personshif3_ on personshif2_.id=personshif3_.personshifts_id 
inner join shift_type shifttype4_ on personshif3_.shifttypes_id=shifttype4_.id 
where shifttype4_.type='A'
	and competence0_.year=2014 
	and competence0_.month=5
	and competence0_.competence_code_id=5 
	and personshif3_.begin_date<='2014-05-01' 
	and (personshif3_.end_date>='2014-05-31' or personshif3_.end_date is null) 
	order by person1_.surname asc;

	
	resto = (giorniTot%2 == 0) ? ':00' : ':30'; 
					ore = giorniTot*6 + (int)(giorniTot/2) + resto;	