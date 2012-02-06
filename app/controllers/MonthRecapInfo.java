package controllers;

import java.util.List;

import models.MonthRecap;
import models.Person;
import models.PersonMonth;

import org.joda.time.LocalDate;

import play.mvc.Controller;

public class MonthRecapInfo extends Controller{
	
	private static void show(LocalDate data) {

		List<Person> personList = Person.findAll();
    	PersonMonth personMonth = new PersonMonth(data);
    	
    	LocalDate now = new LocalDate();
	    for(Person p : personList){
	    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(p, now.getYear(), now.getMonthOfYear());
	        render(monthRecap);
    	}
    }

}
