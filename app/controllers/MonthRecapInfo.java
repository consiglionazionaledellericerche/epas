package controllers;

import java.util.List;

import models.MonthRecap;
import models.Person;
import models.PersonMonth;

import org.joda.time.LocalDate;

import play.mvc.Controller;

public class MonthRecapInfo extends Controller{
	
	private static void show(LocalDate data) {

		long id = 1;
		Person person = Person.findById(id);
    	PersonMonth personMonth = new PersonMonth(person, data);
    	
    	LocalDate now = new LocalDate();
	    
	    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
	        render(monthRecap);
    	
    }

}
