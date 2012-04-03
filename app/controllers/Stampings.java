package controllers;

import models.MonthRecap;
import models.Person;
import models.PersonDay;

import org.bouncycastle.asn1.x509.sigi.PersonalData;
import org.joda.time.LocalDate;

import com.sun.xml.internal.txw2.Document;

import play.Logger;
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
    	String year = params.get("year");
    	String month = params.get("month");
    	
    	if(year==null || month==null){   		        	
        	LocalDate now = new LocalDate();
        	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
            render(monthRecap);
    	}
    	else{
    		MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, Integer.parseInt(year), Integer.parseInt(month));
    		Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
            render(monthRecap);
    	}
    	
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
    					Integer.parseInt(params.get("day"))));
    	render(personDay);
    }
}
