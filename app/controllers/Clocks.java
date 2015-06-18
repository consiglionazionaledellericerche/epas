package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.ConsistencyManager;
import manager.PersonDayManager;
import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingDayRecapFactory;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.Stamping.WayType;
import models.User;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.hash.Hashing;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.UserDao;

@With( RequestInit.class )
public class Clocks extends Controller{

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static UserDao userDao;
	@Inject
	private static PersonDayDao personDayDao;
	@Inject
	private static ConfGeneralManager confGeneralManager;
	@Inject
	private static PersonDayManager personDayManager;
	@Inject
	private static PersonStampingDayRecapFactory stampingDayRecapFactory;
	@Inject
	private static ConsistencyManager consistencyManager;


	public static void show(){

		LocalDate data = new LocalDate();

		String remoteAddress = Http.Request.current().remoteAddress;

		Set<Office> offices = officeDao.getOfficesWithAllowedIp(remoteAddress);

		if(offices.isEmpty()){
			flash.error("Le timbrature web non sono permesse da questo terminale! "
					+ "Inserire l'indirizzo ip nella configurazione della propria sede per abilitarlo");

			try {
				Secure.login();
			} catch (Throwable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		List<Person> personList = personDao.list(Optional.<String>absent(),offices, false, data, data, true).list();
		render(data, personList);
	}


	public static void clockLogin(Long userId, String password) {
		LocalDate today = new LocalDate();
		if(userId == null || userId == 0){

			flash.error("Utente non selezionato");
			Clocks.show();
		}

		User user = userDao.getUserById(userId, Optional.fromNullable(Hashing.md5().hashString(password,  Charsets.UTF_8).toString()));

		if(user == null){

			flash.error("Password non corretta");
			Clocks.show();
		}

		String addressesAllowed = confGeneralManager.getFieldValue(Parameter.ADDRESSES_ALLOWED, user.person.office);

		if(!addressesAllowed.contains(Http.Request.current().remoteAddress)){

			flash.error("Le timbrature web per la persona indicata non sono abilitate da questo terminale!" +
					"Inserire l'indirizzo ip nella configurazione della propria sede per abilitarlo");
			try {
				Secure.login();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		PersonDay personDay = null;			
		Optional<PersonDay> pd = personDayDao.getPersonDay(user.person, today);

		if(!pd.isPresent()){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", user.person.name, user.person.surname);
			personDay = new PersonDay(user.person, today);
			personDay.create();
		}
		else{
			personDay = pd.get();
		}				
		int minInOutColumn = confGeneralManager.getIntegerFieldValue(Parameter.NUMBER_OF_VIEWING_COUPLE, user.person.office);
		int numberOfInOut = Math.max(minInOutColumn, personDayManager.numberOfInOutInPersonDay(personDay));

		PersonStampingDayRecap dayRecap = stampingDayRecapFactory
				.create(personDay, numberOfInOut, Optional.<List<Contract>>absent());

		render(user, dayRecap, numberOfInOut);
	}



	/**
	 * 
	 * @param personId. Con questo metodo si permette l'inserimento della timbratura per la persona contrassegnata da id personId.
	 */
	public static void insertStamping(Long personId){
		Person person = personDao.getPersonById(personId);
		if(person == null){
			throw new IllegalArgumentException("Persona non trovata!!!! Controllare l'id!");
		}
		
		LocalDateTime ldt = LocalDateTime.now();
		LocalDateTime time = new LocalDateTime(ldt.getYear(),ldt.getMonthOfYear(),ldt.getDayOfMonth(),ldt.getHourOfDay(),ldt.getMinuteOfHour(),0);
		PersonDay personDay = null;
		Optional<PersonDay> pd = personDayDao.getPersonDay(person, ldt.toLocalDate());

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

		consistencyManager.updatePersonSituation(person, day.date);

		flash.success("Aggiunta timbratura per %s %s", person.name, person.surname);

		Clocks.showRecap(personId);
	}

	public static void showRecap(Long personId)
	{
		Person person = personDao.getPersonById(personId);

		if(person == null)
			throw new IllegalArgumentException("Persona non trovata!!!! Controllare l'id!");

		LocalDate today = new LocalDate();
		PersonDay personDay = null;
		Optional<PersonDay> pd = personDayDao.getPersonDay(person, today);
		if(!pd.isPresent()){
			Logger.debug("Prima timbratura per %s %s non c'è il personday quindi va creato.", person.name, person.surname);
			personDay = new PersonDay(person, today);
			personDay.create();
		}
		else{
			personDay = pd.get();
		}

		int minInOutColumn = confGeneralManager.getIntegerFieldValue(Parameter.NUMBER_OF_VIEWING_COUPLE, person.office);
		int numberOfInOut = Math.max(minInOutColumn,  personDayManager.numberOfInOutInPersonDay(personDay));

		PersonStampingDayRecap dayRecap = stampingDayRecapFactory
				.create(personDay, numberOfInOut, Optional.<List<Contract>>absent());

		render(person, dayRecap, numberOfInOut);

	}

}
