package it.cnr.iit.epas;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.joda.time.LocalDate;

import models.Person;
import models.PersonDay;
import models.Stamping;

import play.Logger;
import play.db.jpa.JPA;

public class PopulatePersonDay {
	
	public static void PopulatePersonDayForOne(){
		fillPersonDay();
	}
	
	
	public static void fillPersonDay(){
		Long id = new Long(139);
		Person p = Person.findById(id);
		if(p != null){
			LocalDate date = new LocalDate(2010,12,31);			
			LocalDate now = new LocalDate(2012,5,7);
			while(!date.equals(now)){
				PersonDay pd = new PersonDay(p,date);
				Logger.warn("Person: "+p );
				Logger.warn("Date: "+date);
				pd.populatePersonDay();
				pd.save();
				date = date.plusDays(1);
			}
		
			
		}
		
	}
}
