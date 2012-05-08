package controllers;

import it.cnr.iit.epas.ActionMenuItem;
import models.MonthRecap;
import models.Person;
import models.PersonDay;
import models.PersonMonth;

import org.bouncycastle.asn1.x509.sigi.PersonalData;
import org.joda.time.LocalDate;

import com.sun.xml.internal.txw2.Document;

import play.Logger;
import play.mvc.Before;
import play.mvc.Controller;

public class Stampings extends Controller {

	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.stampings;
	
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
    	
    	LocalDate now = new LocalDate();
    	Integer year = params.get("year") != null ? Integer.parseInt(params.get("year")) : now.getYear();
    	Integer month = params.get("month") != null ? Integer.parseInt(params.get("month")) : now.getMonthOfYear();
    	
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year, month);
    	PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and " +
    			"pm.month = ? and pm.year = ?", person, month, year).first();
    	if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
    	Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
        render(monthRecap, personMonth, menuItem);
    	
    	
    }

    public static void show() {
    	show(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
    }
    
    public static void dailyStampings() {
    	Person person = Person.findById(Long.parseLong(params.get("id")));
    	PersonDay personDay = 
    			new PersonDay(
    				person, 
    				new LocalDate(
    					Integer.parseInt(params.get("year")),
    					Integer.parseInt(params.get("month")), 
    					Integer.parseInt(params.get("day"))),
    					0,
    					0,
    					0);
    	render(personDay);
    }
}
