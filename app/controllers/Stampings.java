package controllers;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.joda.time.LocalDate;

import models.Person;
import models.Stamping;
import play.mvc.*;

public class Stampings extends Controller {

    public static void show(Long personId) {
    	Person person = Person.findById(personId);
    	LocalDate now = new LocalDate();

    	Calendar firstOfMonth = new GregorianCalendar();
    	firstOfMonth.set(Calendar.DATE, 1);
    	List<Stamping> stampings = 
    		Stamping.find("person_id = ? and date between ? and ?", person.id, new LocalDate(firstOfMonth), now).fetch();
    	
        render(person, stampings);
    }

}
