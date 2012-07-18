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
import play.mvc.With;

@With(Secure.class)
public class Absences extends Controller{
	
	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.absences;
	
	@Before
    static void checkPerson() {
		if (!Security.isConnected()) {
            flash.error("Please log in first");
            Application.index();
        }
    }
	
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
	public static void insertAbsence(LocalDate date){			
    	Logger.debug("Insert absence called for %s %s", Security.getPerson(), date);

    	PersonDay personDay = new PersonDay(Security.getPerson(), date);
		render(personDay);		
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void save(){
		/**
		 * TODO: implementare il corpo della save di una nuova assenza con la logica 
		 */
	}

}
