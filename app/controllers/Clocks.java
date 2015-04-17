package controllers;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.PersonDayManager;
import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingDayRecapFactory;
import models.ConfGeneral;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.ConfigurationFields;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.jobs.Job;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.UserDao;

@With( RequestInit.class )
public class Clocks extends Controller{
	
	@Inject
	static OfficeDao officeDao;
	
	@Inject
	static PersonStampingDayRecapFactory stampingDayRecapFactory;
	
	@Inject
	static PersonDayManager personDayManager;
	
	@Inject
	static PersonDayDao personDayDao;

	public static void show(){
		
		LocalDate data = new LocalDate();
		
		//TODO Capire quali office saranno visibili a questo livello
		List<Office> officeAllowed = officeDao.getAllOffices();

		List<Person> personList = PersonDao.list(Optional.<String>absent(), new HashSet<Office>(officeAllowed), false, data, data, true).list();
		render(data, personList);
	}
	
	
	public static void clockLogin(Long userId, String password)
	{
		LocalDate today = new LocalDate();
		if(userId==0)
		{
			flash.error("Utente non selezionato");
			Clocks.show();
		}
		
		User user = UserDao.getUserById(userId, Optional.fromNullable(Hashing.md5().hashString(password,  Charsets.UTF_8).toString()));
		//User user = User.find("select u from User u where id = ? and password = md5(?)", userId, password).first();

		if(user == null)
		{
			flash.error("Password non corretta");
			Clocks.show();
		}	
		PersonDay personDay = null;			
		Optional<PersonDay> pd = personDayDao.getSinglePersonDay(user.person, today);
		
		if(!pd.isPresent()){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", user.person.name, user.person.surname);
			personDay = new PersonDay(user.person, today);
			personDay.create();
		}
		else{
			personDay = pd.get();
		}				
		int minInOutColumn = ConfGeneralManager.getIntegerFieldValue(Parameter.NUMBER_OF_VIEWING_COUPLE, user.person.office);
		int numberOfInOut = Math.max(minInOutColumn,  PersonUtility.numberOfInOutInPersonDay(personDay));
		
		PersonStampingDayRecap.stampModificationTypeSet = Sets.newHashSet();	
		PersonStampingDayRecap.stampTypeSet = Sets.newHashSet();				
		PersonStampingDayRecap dayRecap = stampingDayRecapFactory.create(personDay,numberOfInOut);
		
		render(user, dayRecap, numberOfInOut);
	}
	
	

	/**
	 * 
	 * @param personId. Con questo metodo si permette l'inserimento della timbratura per la persona contrassegnata da id personId.
	 */
	public static void insertStamping(Long personId){
		Person person = PersonDao.getPersonById(personId);
		if(person == null)
			throw new IllegalArgumentException("Persona non trovata!!!! Controllare l'id!");
		LocalDateTime ldt = new LocalDateTime();
		LocalDateTime time = new LocalDateTime(ldt.getYear(),ldt.getMonthOfYear(),ldt.getDayOfMonth(),ldt.getHourOfDay(),ldt.getMinuteOfHour(),0);
		PersonDay personDay = null;
		Optional<PersonDay> pd = personDayDao.getSinglePersonDay(person, ldt.toLocalDate());
		
		if(!pd.isPresent()){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", person.name, person.surname);
			personDay = new PersonDay(person, ldt.toLocalDate());
			personDay.create();
		}
		else{
			personDay = pd.get();
		}		
		//Se la stamping esiste già mostro il riepilogo
		int minNew = time.getMinuteOfHour();
		int hourNew = time.getHourOfDay();
		for(Stamping s : personDay.stampings){
			int hour = s.date.getHourOfDay();
			int min = s.date.getMinuteOfHour();
			int minMinusOne = s.date.plusMinutes(1).getMinuteOfHour();
			if( hour==hourNew && (minNew==min || minNew==minMinusOne) )
			{
				
				flash.error("Timbratura ore %s gia' inserita, prossima timbratura accettata a partire da %s",
						DateUtility.fromLocalDateTimeHourTime(s.date),
						DateUtility.fromLocalDateTimeHourTime(s.date.plusMinutes(2)));
				
				Clocks.showRecap(personId);
			}
		}

		
		//Altrimenti la inserisco
		Stamping stamp = new Stamping();
		stamp.date = time;
		if(params.get("type").equals("true")){
			stamp.way = WayType.in;
		}
		else
			stamp.way = WayType.out;
		stamp.personDay = personDay;
		stamp.markedByAdmin = false;
		stamp.save();
		personDay.stampings.add(stamp);
		personDay.save();
		
		final PersonDay day = personDay;
		
		new Job() {
			@Override
			public void doJob() {
				personDayManager.updatePersonDaysFromDate(day.person, day.date);

			}
		}.afterRequest();

		flash.success("Aggiunta timbratura per %s %s", person.name, person.surname);
		
		Clocks.showRecap(personId);
	}
	
	public static void showRecap(Long personId)
	{
		Person person = PersonDao.getPersonById(personId);
		
		if(person == null)
			throw new IllegalArgumentException("Persona non trovata!!!! Controllare l'id!");
		
		LocalDate today = new LocalDate();
		PersonDay personDay = null;
		Optional<PersonDay> pd = personDayDao.getSinglePersonDay(person, today);
		if(!pd.isPresent()){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", person.name, person.surname);
			personDay = new PersonDay(person, today);
			personDay.create();
		}
		else{
			personDay = pd.get();
		}
				
		int minInOutColumn = ConfGeneralManager.getIntegerFieldValue(Parameter.NUMBER_OF_VIEWING_COUPLE, person.office);
		int numberOfInOut = Math.max(minInOutColumn,  PersonUtility.numberOfInOutInPersonDay(personDay));
		
		PersonStampingDayRecap.stampModificationTypeSet = Sets.newHashSet();	
		PersonStampingDayRecap.stampTypeSet = Sets.newHashSet();				
		PersonStampingDayRecap dayRecap = stampingDayRecapFactory.create(personDay,numberOfInOut);
		
		render(person, dayRecap, numberOfInOut);
		
	}

}
