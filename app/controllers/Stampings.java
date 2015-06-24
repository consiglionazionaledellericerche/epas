package controllers;

import helpers.ModelQuery.SimpleResults;
import helpers.PersonTags;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.ConsistencyManager;
import manager.StampingManager;
import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingRecap;
import manager.recaps.personStamping.PersonStampingRecapFactory;
import manager.recaps.troubles.PersonTroublesInMonthRecap;
import manager.recaps.troubles.PersonTroublesInMonthRecapFactory;
import models.Person;
import models.PersonDay;
import models.Stamping;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Table;

import dao.OfficeDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {RequestInit.class, Resecure.class} )

public class Stampings extends Controller {

	@Inject
	private static PersonStampingRecapFactory stampingsRecapFactory;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static SecurityRules rules;
	@Inject
	private static StampingManager stampingManager;
	@Inject
	private static StampingDao stampingDao;
	@Inject
	private static PersonDayDao personDayDao;
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonTroublesInMonthRecapFactory personTroubleRecapFactory;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static ConsistencyManager consistencyManager;

	public static void stampings(Integer year, Integer month) {

		
		IWrapperPerson person = wrapperFactory
				.create(Security.getUser().get().person);

		if(! person.isActiveInMonth(new YearMonth(year, month))) {
			flash.error("Non esiste situazione mensile per il mese di %s %s", 
					DateUtility.fromIntToStringMonth(month), year);

			YearMonth last = person.getLastActiveMonth();
			stampings(last.getYear(), last.getMonthOfYear());
		}

		PersonStampingRecap psDto = stampingsRecapFactory
				.create(person.getValue(), year, month);

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
		
		Person person = personDao.getPersonById(personId);
		Preconditions.checkNotNull(person); 
		
		rules.checkIfPermitted(person.office);
		
		IWrapperPerson wPerson = wrapperFactory.create(person);
		
		if(! wPerson.isActiveInMonth(new YearMonth(year,month) )) {
			
			flash.error("Non esiste situazione mensile per il mese di %s", 
					person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			
			YearMonth last = wrapperFactory.create(person).getLastActiveMonth();
			personStamping(personId, last.getYear(), last.getMonthOfYear());
		}

		PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month);

		render(psDto) ;


	}

	public static void createStamp(@Required Long personId, 
			@Required Integer year, @Required Integer month, @Required Integer day){

		Person person = personDao.getPersonById(personId);

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

		LocalDateTime time = stampingManager.buildStampingDateTime(year, month, day, hourStamping);

		if(time == null) {
			flash.error("Inserire un valore valido per l'ora timbratura. Operazione annullata");
			Stampings.personStamping(personId, year, month);
		}

		PersonDay personDay = 	personDayDao.getPersonDay(person, date).orNull();

		if(personDay == null){
			personDay = new PersonDay(person, date);
			personDay.save();
		}

		stampingManager.addStamping(personDay, time, note, service, type, true);

		consistencyManager.updatePersonSituation(personDay.person, personDay.date);
		

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

			consistencyManager.updatePersonSituation(pd.person, pd.date);

			flash.success("Timbratura per il giorno %s rimossa", PersonTags.toDateTime(stamping.date.toLocalDate()));	

			Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
		}

		if (stampingHour == null || stampingMinute == null) {

			flash.error("E' necessario specificare sia il campo ore che minuti. Operazione annullata.");
			Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
		}

		stampingManager.persistStampingForUpdate(stamping, note, stampingHour, stampingMinute, service);

		consistencyManager.updatePersonSituation(pd.person, pd.date);

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

		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();

		List<Person> activePersons = 
				personDao.list(Optional.<String>absent(), officeDao.getOfficeAllowed(Security.getUser().get()), 
						false, monthBegin, monthEnd, true).list();

		List<PersonTroublesInMonthRecap> missingStampings = new ArrayList<PersonTroublesInMonthRecap>();

		for(Person person : activePersons) {
			
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

		LocalDate dayPresence = new LocalDate(year, month, day);

		List<Person> activePersonsInDay = personDao.list(Optional.<String>absent(), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, dayPresence, dayPresence, true).list();

		int numberOfInOut = stampingManager.maxNumberOfStampingsInMonth(year, month, day, activePersonsInDay);

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();

		daysRecap = stampingManager.populatePersonStampingDayRecapList(activePersonsInDay, dayPresence, numberOfInOut);

		String month_capitalized = DateUtility.fromIntToStringMonth(month);

		render(daysRecap, year, month, day, numberOfInOut, month_capitalized);
	}


	public static void mealTicketSituation(Integer year, Integer month, String name, Integer page){

		if(page == null)
			page = 0;

		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();

		SimpleResults<Person> simpleResults = personDao.list(Optional.fromNullable(name), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, beginMonth, endMonth, true);

		List<Person> activePersons = simpleResults.paginated(page).getResults();		

		int numberOfDays = endMonth.getDayOfMonth();
		Table<Person, LocalDate, String> tablePersonTicket = 
				stampingManager.populatePersonTicketTable(activePersons, beginMonth);
		render(year, month, tablePersonTicket, numberOfDays, simpleResults, name);
	}

	public static void holidaySituation(int year) {

		List<Person> simplePersonList = personDao.list(Optional.<String>absent(),
				officeDao.getOfficeAllowed(Security.getUser().get()), false, 
				new LocalDate(year, 1, 1), new LocalDate(year, 12, 31), false).list();

		List<IWrapperPerson> personList = FluentIterable
				.from(simplePersonList)
				.transform(wrapperFunctionFactory.person()).toList();
		render(personList, year);
	}

	public static void personHolidaySituation(Long personId, int year) {

		Person p = personDao.getPersonById(personId);
		Preconditions.checkNotNull(p);

		rules.checkIfPermitted(p.office);

		IWrapperPerson person = wrapperFactory.create(p);

		render(person, year);
	}

	public static void toggleWorkingHoliday(Long personDayId) {

		PersonDay pd = personDayDao.getPersonDayById(personDayId);
		Preconditions.checkNotNull(pd);
		Preconditions.checkNotNull(pd.isPersistent());
		Preconditions.checkState(pd.isHoliday == true && pd.timeAtWork > 0);

		rules.checkIfPermitted(pd.person.office);

		pd.acceptedHolidayWorkingTime = !pd.acceptedHolidayWorkingTime;
		pd.save();

		consistencyManager.updatePersonSituation(pd.person, pd.date);

		flash.success("Operazione completata. Per concludere l'operazione di ricalcolo "
				+ "sui mesi successivi o sui riepiloghi mensili potrebbero occorrere alcuni secondi. "
				+ "Ricaricare la pagina.");

		Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());

	}


	public static void dailyPresenceForPersonInCharge(Integer year, Integer month, Integer day){

		if(!Security.getUser().get().person.isPersonInCharge)
			forbidden();
		LocalDate dayPresence = new LocalDate(year, month, day);

		User user = Security.getUser().get();

		List<Person> people = user.person.people;
		int numberOfInOut = stampingManager.maxNumberOfStampingsInMonth(year, month, day, people);

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();

		daysRecap = stampingManager.populatePersonStampingDayRecapList(people, dayPresence, numberOfInOut);

		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(daysRecap, year, month, day, numberOfInOut, month_capitalized);


	}

}

