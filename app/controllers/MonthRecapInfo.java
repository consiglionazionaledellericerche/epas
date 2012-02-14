package controllers;

import java.util.List;

import models.MonthRecap;
import models.Person;
import models.PersonMonth;

import org.joda.time.LocalDate;

import play.mvc.Controller;

public class MonthRecapInfo extends Controller{
	
	private static void show(LocalDate data) {

		List<Person> person = Person.findAll();
		for(Person p : person){
    	PersonMonth personMonth = new PersonMonth(p, data);
    	
    	//LocalDate now = new LocalDate();
	    
	    	//MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
	        render(personMonth);
    	
		}
	}
	
	public static void show() {
    	show(new LocalDate(session.get(Application.PERSON_ID_SESSION_KEY)));
    }

}
