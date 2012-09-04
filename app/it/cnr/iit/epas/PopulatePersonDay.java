package it.cnr.iit.epas;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

import org.joda.time.LocalDate;

import models.Contract;
import models.Group;
import models.Permission;
import models.Person;
import models.PersonDay;
import models.StampType;
import models.Stamping;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;

import play.Logger;
import play.db.jpa.JPA;

public class PopulatePersonDay {
	
	public static void fillWorkingTimeTypeDays(){
//		Long id = new Long(2);
//		WorkingTimeType wtt = WorkingTimeType.findById(id);
		WorkingTimeType wtt = WorkingTimeType.find("Select wtt from WorkingTimeType wtt where wtt.description = ?", "normale-mod").first();
		WorkingTimeTypeDay wttd = null;
		for(int i=1; i<=5; i++){
			wttd = new WorkingTimeTypeDay();
			wttd.workingTimeType = wtt;
			wttd.breakTicketTime = 30;
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
			wttd.breakTicketTime = 30;
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
	

	
	/**
	 * cancella dalla tabella dei contratti tutti quelli che non sono i più recenti per ciascuna persona
	 */
	public static void manageContract(){
		List<Person> personList = Person.findAll();
		for(Person person : personList){
			List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ? " +
					"order by con.beginContract", person).fetch();
			if(contractList.size()>1){
				int size = contractList.size();
				//int count = 0;
				for(int count = 0; count<size-1; count++){
					contractList.get(count).delete();
					
				}
								
			}
			
		}
	}
	
	public static void manageStampType(){
		StampType st = new StampType();
		st.description = "Altra timbratura di ingresso";
		st.save();
		StampType st2 = new StampType();
		st2.description = "Altra timbratura di uscita";
		st2.save();
	}

	
	public static void personPermissions(){
		Permission permission1 = new Permission();
		permission1.description = "viewPersonList";
		permission1.save();
		Permission permission2 = new Permission();
		permission2.description = "deletePerson";
		permission2.save();
		Permission permission3 = new Permission();
		permission3.description = "insertAndUpdateStamping";
		permission3.save();
		Permission permission4 = new Permission();
		permission4.description = "insertAndUpdatePassword";
		permission4.save();
		Permission permission5 = new Permission();
		permission5.description = "insertAndUpdateWorkingTime";
		permission5.save();
		Permission permission6 = new Permission();
		permission6.description = "insertAndUpdateAbsence";
		permission6.save();
		Permission permission7 = new Permission();
		permission7.description = "insertAndUpdateConfiguration";
		permission7.save();
		long id = 139;
		Person person = Person.findById(id);
		person.permissions.add(permission1);
		person.permissions.add(permission2);
		person.permissions.add(permission3);
		person.permissions.add(permission4);
		person.permissions.add(permission5);
		person.permissions.add(permission6);
		person.permissions.add(permission7);
		person.save();
		
	}
}
