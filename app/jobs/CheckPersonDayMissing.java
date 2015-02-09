package jobs;

import java.util.List;

import manager.PersonDayManager;
import manager.PersonManager;
import models.Person;
import models.PersonDay;

import org.joda.time.LocalDate;

import play.Logger;
import play.jobs.Job;

//@On("0 /5 * * * ?")
//@On("0 0 2 * * ?")
@SuppressWarnings("rawtypes")
public class CheckPersonDayMissing extends Job{

	/**
	 * controlla che nel giorno trascorso non ci siano persone senza timbrature e assenze (quindi non sia stato creato il personday corrispondente).
	 * In tal caso lo crea e valorizza il tempo di lavoro a -workingTime e la differenza a -difference aggiornando di conseguenza il progressivo 
	 */
	
	public void doJob(){
		Logger.debug("Chiamata della funzione check person day missing");
		LocalDate date = new LocalDate();
	//	List<Person> personList = Person.find("Select p from Person p where p.surname = ? or p.surname = ?", "Vasarelli", "Lucchesi").fetch();
	//	List<Person> personList = Person.getActivePersons(date);
		List<Person> personList = PersonManager.getPeopleForTest();
		Logger.debug("La lista di personale attivo è composta da %d persone", personList.size());
		
		for(Person p : personList){
			LocalDate dateBegin = date.dayOfMonth().withMinimumValue();
			Logger.debug("Analizzo %s %s", p.name, p.surname);
			while(dateBegin.isBefore(date)){
				PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", p, dateBegin).first();
				if(pd == null && !PersonManager.isHoliday(p,dateBegin)){
					Logger.debug("Non c'è personDay e non è festa per %s %s nel giorno %s", p.name, p.surname, dateBegin);
					pd = new PersonDay(p, dateBegin);
					pd.create();
					PersonDayManager.populatePersonDay(pd);
					pd.save();
					Logger.debug("Creato person day per %s %s per il giorno %s in cui non risultavano nè timbrature nè codici di assenza", 
							p.name, p.surname, dateBegin);
					
				}
				dateBegin = dateBegin.plusDays(1);
				
			}
		}
		
		
	}
}
