package controllers;

import models.MonthRecap;
import models.Person;

import org.joda.time.LocalDate;

import play.mvc.Controller;

public class Stampings extends Controller {

    public static void show(Long id) {
    	Person person = Person.findById(id);
    	
    	LocalDate now = new LocalDate();
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
        render(monthRecap);
    }

}
