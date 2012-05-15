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
	
	
	public static void fillPersonDay(Person person) {
		if(person != null){
			//TODO:le date vanno rese generiche
			LocalDate date = new LocalDate(2010,12,31);			
			LocalDate now = new LocalDate();
			while(!date.equals(now)){
				PersonDay pd = new PersonDay(person,date);
				Logger.warn("Person: "+person );
				Logger.warn("Date: "+date);
				pd.populatePersonDay();
				pd.save();
				date = date.plusDays(1);
			}
		
			
		}
	}
	
	//TODO: solo per prova, da cancellare
	public static void fillPersonDay(){
		Long id = new Long(139);
		fillPersonDay((Person) Person.findById(id));	
	}
}
