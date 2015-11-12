package controllers;

import helpers.PersonTags;
import helpers.Web;
import helpers.validators.StringIsTime;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.NullStringBinder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.SecureManager;
import manager.StampingManager;
import manager.recaps.personStamping.PersonStampingDayRecap;
import manager.recaps.personStamping.PersonStampingRecap;
import manager.recaps.personStamping.PersonStampingRecapFactory;
import manager.recaps.troubles.PersonTroublesInMonthRecap;
import manager.recaps.troubles.PersonTroublesInMonthRecapFactory;
import models.Absence;
import models.Institute;
import models.Person;
import models.PersonDay;
import models.StampType;
import models.Stamping;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;

import play.data.binding.As;
import play.data.validation.CheckWith;
import play.data.validation.InPast;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.history.HistoryValue;
import dao.history.StampingHistoryDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

@Slf4j
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
	private static SecureManager secureManager;
	@Inject
	private static PersonTroublesInMonthRecapFactory personTroubleRecapFactory;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static ConsistencyManager consistencyManager;
	@Inject 
	private static StampingHistoryDao stampingsHistoryDao;

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

	public static void blank(@Required Long personId, @Required LocalDate date) {

		Person person = personDao.getPersonById(personId);

		Preconditions.checkState(!date.isAfter(LocalDate.now()));
		
		rules.checkIfPermitted(person.office);

		render(person, date);
	}
	
	public static void edit(Long stampingId) {

		Stamping stamping = stampingDao.getStampingById(stampingId);
		
		if (stamping == null) {
			notFound();
		}
		
		List<HistoryValue<Stamping>> historyStamping = stampingsHistoryDao
				.stampings(stampingId);
	
		rules.checkIfPermitted(stamping.personDay.person.office);
		
		render(stamping, historyStamping);
	}
	
	public static void save(@Required Person person, @Required LocalDate date,
			Stamping stamping, @Required @NotNull Boolean way, @CheckWith (StringIsTime.class) String time,
			String note, StampType stampType) {
		
		Preconditions.checkState(!date.isAfter(LocalDate.now()));
		
		PersonDay personDay = personDayDao.getOrBuildPersonDay(person, date);
		
		rules.checkIfPermitted(person.office);

		if (!stamping.isPersistent()) {
			stamping = new Stamping(personDay, null);
		} 
		
		if (Validation.hasErrors()) {
			
			response.status = 400;
			flash.error(Web.msgHasErrors());
			
			List<HistoryValue<Stamping>> historyStamping = Lists.newArrayList();
			if (stamping.isPersistent()) {
				historyStamping = stampingsHistoryDao.stampings(stamping.id);
			}
			
			render("@edit", stamping, time, way, stampType, historyStamping);
			//log.warn("validation errors for {}: {}", institute,
			//		validation.errorsMap());
		} 

		personDay.save();
		stamping.date = stampingManager.deparseStampingDateTime(date, time);
		stamping.stampType = stampType.isPersistent() ? stampType : null;
		stamping.markedByAdmin = true;
		stamping.way = way ? Stamping.WayType.in : Stamping.WayType.out;
		stamping.note = note;
		stamping.save();

		personDay.stampings.add(stamping);
		personDay.save();
		//stamping.save();

		consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

		flash.success(Web.msgSaved(Institute.class));

		Stampings.personStamping(person.id,
				date.getYear(), date.getMonthOfYear());

	}
	
	public static void delete(Long id){

		final Stamping stamping = stampingDao.getStampingById(id);

		Preconditions.checkState(stamping != null);

		rules.checkIfPermitted(stamping.personDay.person);

		final PersonDay personDay = stamping.personDay;
		stamping.delete();

		consistencyManager.updatePersonSituation(personDay.person.id, personDay.date);

		flash.success("Timbratura rimossa correttamente.");

		personStamping(personDay.person.id, personDay.date.getYear(), 
				personDay.date.getMonthOfYear());
	}
	
	
	public static void updateEmployee(Long stampingId, StampType stampType, @As(binder=NullStringBinder.class) String note) {

		Stamping stamp = stampingDao.getStampingById(stampingId);
		if (stamp == null) {
			notFound();
		}

		rules.checkIfPermitted(stamp.personDay.person);

		if(stampType.code == null){
			stamp.stampType = null;
		}
		else{
			stamp.stampType = stampType;
		}

		stamp.note = note;

		stamp.markedByEmployee = true;

		stamp.save();

		consistencyManager.updatePersonSituation(stamp.personDay.person.id, stamp.personDay.date);

		flash.success("Timbratura per il giorno %s per %s aggiornata.", PersonTags.toDateTime(stamp.date.toLocalDate()), stamp.personDay.person.fullName());

		Stampings.stampings(stamp.personDay.date.getYear(), stamp.personDay.date.getMonthOfYear());
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

		List<Person> activePersons = personDao.list(
				Optional.<String>absent(),
				secureManager.officesReadAllowed(Security.getUser().get()), 
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

		LocalDate date = new LocalDate(year, month, day);

		List<Person> activePersonsInDay = personDao.list(
				Optional.<String>absent(), 
				secureManager.officesReadAllowed(Security.getUser().get()),
				false, date, date, true).list();

		int numberOfInOut = stampingManager
				.maxNumberOfStampingsInMonth(date, activePersonsInDay);

		List<PersonStampingDayRecap> daysRecap = Lists.newArrayList();

		daysRecap = stampingManager
				.populatePersonStampingDayRecapList(activePersonsInDay, date, numberOfInOut);

		String month_capitalized = DateUtility.fromIntToStringMonth(month);

		render(daysRecap, year, month, day, numberOfInOut, month_capitalized);
	}

	public static void holidaySituation(int year) {

		List<Person> simplePersonList = personDao.list(
				Optional.<String>absent(),
				secureManager.officesReadAllowed(Security.getUser().get()),
				false, new LocalDate(year, 1, 1), new LocalDate(year, 12, 31), false).list();

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

		consistencyManager.updatePersonSituation(pd.person.id, pd.date);

		flash.success("Operazione completata. Per concludere l'operazione di ricalcolo "
				+ "sui mesi successivi o sui riepiloghi mensili potrebbero occorrere alcuni secondi. "
				+ "Ricaricare la pagina.");

		Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());

	}


	public static void dailyPresenceForPersonInCharge(Integer year, Integer month, Integer day){

		if(!Security.getUser().get().person.isPersonInCharge) {
			forbidden();
		}
		
		LocalDate date = new LocalDate(year, month, day);

		User user = Security.getUser().get();

		List<Person> people = user.person.people;
		int numberOfInOut = stampingManager.maxNumberOfStampingsInMonth(date, people);

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();

		daysRecap = stampingManager
				.populatePersonStampingDayRecapList(people, date, numberOfInOut);

		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(daysRecap, year, month, day, numberOfInOut, month_capitalized);


	}
		
		
}

