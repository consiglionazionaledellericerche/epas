package controllers;

import models.MonthRecap;
import models.Person;

import org.joda.time.LocalDate;

import com.sun.xml.internal.txw2.Document;

import play.mvc.Before;
import play.mvc.Controller;

public class Stampings extends Controller {

    @Before
    static void checkPerson() {
        if(session.get(Application.PERSON_ID_SESSION_KEY) == null) {
            flash.error("Please log in first");
            Application.index();
        }
    }

    private static void show(Long id) {

    	Person person = Person.findById(id);
    	
    	LocalDate now = new LocalDate();
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
        render(monthRecap);
    }

    public static void show() {
    	show(Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY)));
    }
    
    public static void recharge(String recharge){
    	if(recharge != null){
			Integer year = new Integer(session.get("anno"));
			Integer month = new Integer(session.get("mese"));
			long id = Long.parseLong(session.get(Application.PERSON_ID_SESSION_KEY));	
			Person person = Person.findById(id);
			MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year.intValue(), month.intValue());
			render(monthRecap);
    	}
    } 
}
