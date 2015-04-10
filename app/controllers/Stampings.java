package controllers;

import helpers.ModelQuery.SimpleResults;
import helpers.PersonTags;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.PersonDayManager;
import manager.PersonManager;
import manager.StampingManager;
import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingRecap;
import manager.recaps.personStamping.PersonStampingRecapFactory;
import manager.recaps.troubles.PersonTroublesInMonthRecap;
import manager.recaps.troubles.PersonTroublesInMonthRecapFactory;
import models.Person;
import models.PersonDay;
import models.Stamping;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.jobs.Job;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;

@With( {RequestInit.class, Resecure.class} )

public class Stampings extends Controller {

	@Inject
	static SecurityRules rules;
	
	@Inject
	static OfficeDao officeDao;
	
	@Inject
	static StampingManager stampingManager;
	
	@Inject 
	static PersonStampingRecapFactory stampingsRecapFactory;
	
	@Inject
	static PersonDayManager personDayManager;
	
	@Inject
	static PersonDayDao personDayDao;
	
	@Inject
	static StampingDao stampingDao;
	
	@Inject
	static PersonTroublesInMonthRecapFactory personTroubleRecapFactory;
	
	public static void stampings(Integer year, Integer month) {

		Person person = Security.getUser().get().person;

		if(!PersonManager.isActiveInMonth(person, month, year, false)) {
			
			flash.error("Non esiste situazione mensile per il mese di %s %s", 
					DateUtility.fromIntToStringMonth(month), year);
			
			Stampings.stampings(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
			return;
		}
		
		PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month);
		
		render(psDto) ;

	}

	
	public static void personStamping(Long personId, int year, int month) {
		
		if (personId == null) {
			
			personId = Security.getUser().get().person.getId();
			year = LocalDate.now().getYear();
			month = LocalDate.now().getMonthOfYear();
		}
		
		if (year == 0 || month == 0) {
			
			year = LocalDate.now().getYear();
			month = LocalDate.now().getMonthOfYear();
		}
		
		Person person = PersonDao.getPersonById(personId);

		if(person == null){
			flash.error("Persona inesistente in anagrafica");
			Application.indexAdmin();
		}
		
		rules.checkIfPermitted(person.office);

		if(!PersonManager.isActiveInMonth(person, month, year, false))
		{
			flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
					"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			render("@redirectToIndex");
		}
		
		PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month);
		
		render(psDto) ;

		 
	}

	public static void createStamp(@Required Long personId, 
			@Required Integer year, @Required Integer month, @Required Integer day){

		Person person = PersonDao.getPersonById(personId);
		
		rules.checkIfPermitted(person.office);
		
		LocalDate date = new LocalDate(year,month,day);

		PersonDay personDay = new PersonDay(person, date);

		render(person, personDay);
	}

	
	public static void insert(@Valid @Required Long personId, 
			@Required Integer year, @Required Integer month, @Required Integer day,
			@Required boolean type, @Required boolean service, @Required String hourStamping,
			String note) {

		Person person = Person.em().getReference(Person.class, personId);

		rules.checkIfPermitted(person.office);
		
		LocalDate date = new LocalDate(year,month,day);

		if( date.isAfter(new LocalDate()) ) 
		{
			flash.error("Non si può inserire una timbratura futura!!!");
			Stampings.personStamping(personId, year, month);
		}

		LocalDateTime time = StampingManager.buildStampingDateTime(year, month, day, hourStamping);
		
		if(time == null) {
			flash.error("Inserire un valore valido per l'ora timbratura. Operazione annullata");
			Stampings.personStamping(personId, year, month);
		}
		
		PersonDay personDay = 	personDayDao.getSinglePersonDay(person, date).orNull();
		
		if(personDay == null){
			personDay = new PersonDay(person, date);
			personDay.save();
		}
		
		stampingManager.addStamping(personDay, time, note, service, type, true);
		
		final PersonDay giorno = personDay;
		
		new Job() {
			@Override
			public void doJob() {
				personDayManager.updatePersonDaysFromDate(giorno.person, giorno.date);

			}
		}.afterRequest();
				
		flash.success("Inserita timbratura per %s %s in data %s", person.name, person.surname, date);

		Stampings.personStamping(personId, year, month);

	}
	

	public static void edit(@Required Long stampingId) {
		
		Stamping stamping = stampingDao.getStampingById(stampingId);
		
		if (stamping == null) {
			notFound();
		}

		rules.checkIfPermitted(stamping.personDay.person.office);
		
		LocalDate date = stamping.date.toLocalDate();

		Integer hour = stamping.date.getHourOfDay();
		Integer minute = stamping.date.getMinuteOfHour();
		
		render(stamping, hour, minute, date);				
	}

	public static void update(@Required Long stampingId, String elimina, 
			Integer stampingHour, Integer stampingMinute,
			@Required boolean service, String note) {
		
		Stamping stamping = stampingDao.getStampingById(stampingId);
		if (stamping == null) {
			notFound();
		}
		
		rules.checkIfPermitted(stamping.personDay.person.office);
		
		final PersonDay pd = stamping.personDay;
		
		//elimina
		if( elimina != null) {
			
			stamping.delete();
			pd.stampings.remove(stamping);
						
			new Job() {
				@Override
				public void doJob() {
					personDayManager.updatePersonDaysFromDate(pd.person, pd.date);

				}
			}.afterRequest();
	
			flash.success("Timbratura per il giorno %s rimossa", PersonTags.toDateTime(stamping.date.toLocalDate()));	

			Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
		}

		if (stampingHour == null || stampingMinute == null) {
		
			flash.error("E' necessario specificare sia il campo ore che minuti. Operazione annullata.");
			Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
		}

		StampingManager.persistStampingForUpdate(stamping, note, stampingHour, stampingMinute, service);
		
		new Job() {
			@Override
			public void doJob() {
				personDayManager.updatePersonDaysFromDate(pd.person, pd.date);

			}
		}.afterRequest();

		flash.success("Timbratura per il giorno %s per %s %s aggiornata.", PersonTags.toDateTime(stamping.date.toLocalDate()), stamping.personDay.person.surname, stamping.personDay.person.name);

		Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());

	}

	/**
	 * Controller che attua la verifica di timbrature mancanti per il mese selezionato per tutte quelle persone
	 * che avevano almeno un contratto attivo in tale mese
	 * @param year
	 * @param month
	 */
	public static void missingStamping(int year, int month) {

		rules.checkIfPermitted("");
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();

		//lista delle persone che sono state attive nel mese
		List<Person> activePersons = 
				PersonDao.list(Optional.<String>absent(), officeDao.getOfficeAllowed(Security.getUser().get()), 
						false, monthBegin, monthEnd, true).list();

		List<PersonTroublesInMonthRecap> missingStampings = new ArrayList<PersonTroublesInMonthRecap>();
		
		for(Person person : activePersons)
		{
			PersonTroublesInMonthRecap pt = personTroubleRecapFactory.create(person, monthBegin, monthEnd);
			missingStampings.add(pt);
		}
		render(month, year, missingStampings);
	}

	/**
	 * Controller che renderizza la presenza giornaliera dei dipendenti visibili all'amministratore.
	 * @param year
	 * @param month
	 * @param day
	 */
	public static void dailyPresence(Integer year, Integer month, Integer day) {

		rules.checkIfPermitted("");
		
		LocalDate dayPresence = new LocalDate(year, month, day);

		List<Person> activePersonsInDay = PersonDao.list(Optional.<String>absent(), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, dayPresence, dayPresence, true).list();

		int numberOfInOut = stampingManager.maxNumberOfStampingsInMonth(year, month, day, activePersonsInDay);
				
		PersonStampingDayRecap.stampModificationTypeSet = Sets.newHashSet();	
		PersonStampingDayRecap.stampTypeSet = Sets.newHashSet();						
		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		
		daysRecap = stampingManager.populatePersonStampingDayRecapList(activePersonsInDay, dayPresence, numberOfInOut);

		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		
		render(daysRecap, year, month, day, numberOfInOut, month_capitalized);
	}

	
	public static void mealTicketSituation(Integer year, Integer month, String name, Integer page){

		if(page == null)
			page = 0;
		
		rules.checkIfPermitted("");
		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		
		SimpleResults<Person> simpleResults = PersonDao.list(Optional.fromNullable(name), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, beginMonth, endMonth, true);

		List<Person> activePersons = simpleResults.paginated(page).getResults();		

		int numberOfDays = endMonth.getDayOfMonth();
		Table<Person, LocalDate, String> tablePersonTicket = 
				stampingManager.populatePersonTicketTable(activePersons, beginMonth);
		render(year, month, tablePersonTicket, numberOfDays, simpleResults, name);
	}
}
