package it.cnr.iit.epas;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

import org.joda.time.LocalDate;

import models.Group;
import models.Permission;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;

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
	
	public static void fillWorkingTimeTypeDays(){
		Long id = new Long(1);
		WorkingTimeType wtt = WorkingTimeType.findById(id);
		//WorkingTimeType wtt = WorkingTimeType.find("Select wtt from WorkingTimeType wtt where wtt.description = ?", "normale-mod").first();
		WorkingTimeTypeDay wttd = null;
		for(int i=1; i<=5; i++){
			wttd = new WorkingTimeTypeDay();
			wttd.workingTimeType = wtt;
			wttd.breakTicketTime = 0;
			wttd.dayOfWeek = i;
			wttd.holiday = false;
			wttd.mealTicketTime = 360;
			wttd.timeMealFrom = 0;
			wttd.timeMealTo = 0;
			wttd.timeSlotEntranceFrom = 0;
			wttd.timeSlotEntranceTo = 0;
			wttd.timeSlotExitFrom = 0;
			wttd.timeSlotExitTo = 0;
			wttd.willBeSaved = false;
			wttd.workingTime = 432;
			wttd.save();

		}
		for(int i=6; i<8; i++){
			wttd = new WorkingTimeTypeDay();
			wttd.workingTimeType = wtt;
			wttd.breakTicketTime = 0;
			wttd.dayOfWeek = i;
			wttd.holiday = true;
			wttd.mealTicketTime = 360;
			wttd.timeMealFrom = 0;
			wttd.timeMealTo = 0;
			wttd.timeSlotEntranceFrom = 0;
			wttd.timeSlotEntranceTo = 0;
			wttd.timeSlotExitFrom = 0;
			wttd.timeSlotExitTo = 0;
			wttd.willBeSaved = false;
			wttd.workingTime = 432;
			wttd.save();
		}
				
	}
	
	//TODO: solo per prova, da cancellare
	public static void fillPersonDay(){
		Long id = new Long(139);
		fillPersonDay((Person) Person.findById(id));	
	}
	

}
