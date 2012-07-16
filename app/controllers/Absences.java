package controllers;

import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.AbsenceType;
import models.MonthRecap;
import models.Person;
import models.PersonDay;

import org.joda.time.LocalDate;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

public class Absences extends Controller{
	
	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.absences;
	
	@Before
    static void checkPerson() {
        if(session.get(Application.PERSON_ID_SESSION_KEY) == null) {
            flash.error("Please log in first");
            Application.index();
        }
    }
	
	private static void show(Long id) {
		String menuItem = actionMenuItem.toString();
		
    	Person person = Person.findById(id);
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
    		Logger.info("Sono dentro il ramo else della creazione del month recap");
    		Integer year = new Integer(params.get("year"));
			Integer month = new Integer(params.get("month"));
    		MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year.intValue(), month.intValue());
    		Logger.info("Il month recap è formato da: " +person.id+ ", " +year.intValue()+ ", " +month.intValue());
    		
            render(monthRecap, menuItem);
    	}
    	
    }
	
	public static void show() {
    	show(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
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
	public static void insertAbsence(){
		
		Person person = Person.findById(Long.parseLong(session.get("person_id")));
    	LocalDate day = 
    			new LocalDate(
    				Integer.parseInt(session.get("year")),
    				Integer.parseInt(session.get("month")), 
    				Integer.parseInt(session.get("day")));
    	
    	Logger.trace("Insert absence called for %s %s", person, day);
    	
    	PersonDay personDay = new PersonDay(person, day);
		render(personDay);		
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void save(){
		/**
		 * TODO: implementare il corpo della save di una nuova assenza con la logica 
		 */
	}

}
