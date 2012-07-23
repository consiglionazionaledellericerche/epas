package controllers;

import java.util.Date;
import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.Absence;
import models.AbsenceType;
import models.MonthRecap;
import models.Person;
import models.PersonDay;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.binding.As;
import play.data.binding.types.DateTimeBinder;
import play.db.jpa.GenericModel.JPAQuery;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Absences extends Controller{
	
	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.absences;
		
	@Check(Security.VIEW_PERSON_LIST)
	public static void show(Person person) {
		String menuItem = actionMenuItem.toString();
		
    	String anno = params.get("year");
    	Logger.info("Anno: "+anno);
    	String mese= params.get("month");
    	Logger.info("Mese: "+mese);
    	if(anno==null || mese==null){
    		        	
        	LocalDate now = new LocalDate();
        	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
            render(monthRecap, menuItem);
    	}
    	else{
    		Logger.debug("Sono dentro il ramo else della creazione del month recap");
    		Integer year = new Integer(params.get("year"));
			Integer month = new Integer(params.get("month"));
    		MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year.intValue(), month.intValue());
    		Logger.debug("Il month recap è formato da: " +person.id+ ", " +year.intValue()+ ", " +month.intValue());
    		
            render(monthRecap, menuItem);
    	}
    	
    }
	
	public static void show() {
		show(Security.getPerson());
    }
	
	/**
	 * questa è una funzione solo per admin, quindi va messa con il check administrator
	 */
	public static void manageAbsenceCode(){
		List<AbsenceType> absenceList = AbsenceType.findAll();
		
		render(absenceList);
	}
	
	public static void absenceCodeList(){
		List<AbsenceType> absenceList = AbsenceType.findAll();
		render(absenceList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insertAbsence(Long personId, Integer year, Integer month, Integer day){
    	Logger.debug("Insert absence called for personId=%s, year=%s, month=%s, day=%s", personId, year, month, day);
    	
		Person person = Person.em().getReference(Person.class, personId);
		LocalDate date = new LocalDate(year, month, day);

    	PersonDay personDay = new PersonDay(person, date);
		render(personDay);		
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void save(){
		
		/**
		 * TODO: implementare il corpo della save di una nuova assenza con la logica 
		 */
		Person person = Person.em().getReference(Person.class, params.get("personId", Long.class));
		LocalDate dateFrom = new LocalDate(params.get("year.from", Integer.class), params.get("month.from", Integer.class), params.get("day.from", Integer.class));
		
		AbsenceType absenceType = AbsenceType.find("byCode", params.get("absenceCode")).first();
		if (absenceType == null) {
			notFound();
		}
		
		Logger.debug("Richiesto inserimento della assenza codice = %s della persona %s, dataInizio = %s", absenceType.code, person, dateFrom);
		
		Absence absence = new Absence();
		absence.person = person;
		absence.date = dateFrom;
		
		absence.absenceType = absenceType;
		
		absence.save();
		if (absence.id != null) {
			//Aggiornare il personDay? controllare le eventuali timbrature? etc.....
			flash.success(
					String.format("Assenza di tipo %s inserita per il giorno %s per %s %s", absenceType.code, dateFrom, person.surname, person.name));
			
		}
		
		render();
	}

}
