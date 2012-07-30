package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Person;
import models.PersonMonth;

import org.joda.time.LocalDate;

import play.data.binding.As;
import play.mvc.Controller;

public class MonthRecap extends Controller{
	
	public static void show(@As("yyyy-MM-dd") Date date) {
		LocalDate localDate = new LocalDate(date);
		List<Person> persons = Person.findAll();
		List<PersonMonth> personMonths = new ArrayList<PersonMonth>();
		for( Person p : persons) {
			personMonths.add(new PersonMonth(p, localDate.getYear(), localDate.getMonthOfYear()));
    	
	    	//MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
	       
		}
		render(date, personMonths);
	}

}
