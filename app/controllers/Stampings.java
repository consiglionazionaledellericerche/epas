package controllers;

import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import manager.PersonDayManager;
import manager.PersonManager;
import manager.StampingManager;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonTags;
import models.StampModificationType;
import models.StampType;
import models.Stamping;
import models.rendering.PersonStampingDayRecap;
import models.rendering.PersonTroublesInMonthRecap;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dto.PersonStampingDto;

@With( {RequestInit.class, Resecure.class} )

public class Stampings extends Controller {

	@Inject
	static SecurityRules rules;
	
	
	public static void stampings(Integer year, Integer month) {

		Person person = Security.getUser().get().person;

		if(!PersonManager.isActiveInMonth(person, month, year, false))
		{
			flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
					"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			
			//Un dipendente fuori contratto non può accedere a ePAS ??
			try {
				Secure.login();
			} catch(Throwable e) {
				Application.index();
			}
			
		}
		
		PersonStampingDto psDto = PersonStampingDto.build(year, month, person);
		
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
		
		PersonStampingDto psDto = PersonStampingDto.build(year, month, person);
		
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
		
		PersonDay personDay = null;
		Optional<PersonDay> pd = PersonDayDao.getSinglePersonDay(person, date);

		if(!pd.isPresent()){
			personDay = new PersonDay(person, date);
			personDay.create();
		}
		else{
			personDay = pd.get();
		}
		
		StampingManager.addStamping(personDay, time, note, service, type, true);
				
		flash.success("Inserita timbratura per %s %s in data %s", person.name, person.surname, date);

		Stampings.personStamping(personId, year, month);


	}
	

	public static void edit(@Required Long stampingId) {
		
		Stamping stamping = StampingDao.getStampingById(stampingId);
		
		if (stamping == null) {
			notFound();
		}

		rules.checkIfPermitted(stamping.personDay.person.office);
		
		LocalDate date = stamping.date.toLocalDate();

		List<String> hourMinute = timeDivided(stamping);
		
		render(stamping, hourMinute, date);				
	}

	public static void update() {
		Stamping stamping = StampingDao.getStampingById(params.get("stampingId", Long.class));
		//Stamping stamping = Stamping.findById(params.get("stampingId", Long.class));
		if (stamping == null) {
			notFound();
		}
		
		rules.checkIfPermitted(stamping.personDay.person.office);
		
		PersonDay pd = stamping.personDay;
		
		//elimina
		if( params.get("elimina") != null) {
			stamping.delete();
			pd.stampings.remove(stamping);

			PersonDayManager.updatePersonDaysFromDate(pd.person, pd.date);
	
			flash.success("Timbratura per il giorno %s rimossa", PersonTags.toDateTime(stamping.date.toLocalDate()));	

			Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
		}
		
		
		Integer hour = params.get("stampingHour", Integer.class);
		Integer minute = params.get("stampingMinute", Integer.class);
		
		if(hour != null && minute == null || hour == null && minute != null)
		{
			flash.error("Attribuire valore a ciascun campo se si intende modificare la timbratura");
			Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
		}
		else 
		{
			if (hour == null || minute == null) {
				flash.error("E' necessario specificare sia il campo ore che minuti.");
				Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
				
			}
			Logger.debug("Ore: %s Minuti: %s", hour, minute);

			stamping.date = stamping.date.withHourOfDay(hour);
			stamping.date = stamping.date.withMinuteOfHour(minute);
			String service = params.get("service");
			if(service.equals("false") && (stamping.stampType == null || !stamping.stampType.identifier.equals("s"))){
				String note = params.get("note");
				stamping.note = note;
			}
			if(service.equals("true") && (stamping.stampType == null || !stamping.stampType.identifier.equals("s"))){
				stamping.note = "timbratura di servizio";
				stamping.stampType = StampingDao.getStampTypeByCode("motiviDiServizio");
				//stamping.stampType = StampType.find("Select st from StampType st where st.code = ?", "motiviDiServizio").first();
			}
			if(service.equals("false") && (stamping.stampType != null)){
				stamping.stampType = null;
				stamping.note = "timbratura inserita dall'amministratore";
			}
			
			if(service.equals("true") && (stamping.stampType != null || stamping.stampType.identifier.equals("s"))){
				String note = params.get("note");
				stamping.note = note;
			}
			
			stamping.markedByAdmin = true;
			
			stamping.save();
		
			PersonDayManager.updatePersonDaysFromDate(pd.person, pd.date);
			
			flash.success("Timbratura per il giorno %s per %s %s aggiornata.", PersonTags.toDateTime(stamping.date.toLocalDate()), stamping.personDay.person.surname, stamping.personDay.person.name);

		}
		Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());

	}

	/**
	 * 
	 * @return una lista con due elementi: nella prima posizione c'è l'ora della timbratura in forma di Stringa, nella seconda posizione
	 * troviamo invece i minuti della timbratura sempre in forma di stringa
	 */
	public static List<String> timeDivided (Stamping s){
		List<String> td = new ArrayList<String>();
		Integer hour = s.date.getHourOfDay();
		Integer minute = s.date.getMinuteOfHour();
		String hours = Integer.toString(hour);
		String minutes = Integer.toString(minute);
		td.add(0, hours);
		td.add(1, minutes);

		return td;
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
				PersonDao.list(Optional.<String>absent(), new HashSet<Office>(Security.getOfficeAllowed()), 
						false, monthBegin, monthEnd, true).list();

		List<PersonTroublesInMonthRecap> missingStampings = new ArrayList<PersonTroublesInMonthRecap>();
		
		for(Person person : activePersons)
		{
			PersonTroublesInMonthRecap pt = new PersonTroublesInMonthRecap(person, monthBegin, monthEnd);
			missingStampings.add(pt);
		}
		
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

		List<Person> activePersonsInDay = PersonDao.list(Optional.<String>absent(), new HashSet<Office>(Security.getOfficeAllowed()), false, dayPresence, dayPresence, true).list();

		int numberOfInOut = StampingManager.maxNumberOfStampingsInMonth(year, month, day, activePersonsInDay);
				
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();						
		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		
		for(Person person : activePersonsInDay){
		
			PersonDay personDay = null;
			person = PersonDao.getPersonById(person.id);
			Optional<PersonDay> pd = PersonDayDao.getSinglePersonDay(person, dayPresence); 
			
			if(!pd.isPresent()){
				personDay = new PersonDay(person, dayPresence);
				personDay.create();
			}
			else{
				personDay = pd.get();
			}

			PersonDayManager.computeValidStampings(personDay);
			daysRecap.add(new PersonStampingDayRecap(personDay, numberOfInOut));
			
		}

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
				Sets.newHashSet(Security.getOfficeAllowed()), false, beginMonth, endMonth, true);

		List<Person> activePersons = simpleResults.paginated(page).getResults();
		
		Builder<Person, LocalDate, String> builder = ImmutableTable.<Person, LocalDate, String>builder().orderColumnsBy(new Comparator<LocalDate>() {

			public int compare(LocalDate date1, LocalDate date2) {
				return date1.compareTo(date2);
			}
		}).orderRowsBy(new Comparator<Person>(){
			public int compare(Person p1, Person p2) {

				return p1.surname.compareTo(p2.surname);
			}

		});
		
		for(Person p : activePersons)
		{
			List<PersonDay> pdList = PersonDayDao.getPersonDayInPeriod(p, beginMonth, Optional.fromNullable(beginMonth.dayOfMonth().withMaximumValue()), true);

			for(PersonDay pd : pdList){
				if(pd.isTicketForcedByAdmin) {
					
					if(pd.isTicketAvailable) {
						builder.put(p, pd.date, "siAd");
					}
					else {
						builder.put(p, pd.date, "noAd");
					}
				}
				else {
					
					if(pd.isTicketAvailable) {
						builder.put(p, pd.date, "si");
					}
					else {
						builder.put(p, pd.date, "");
					}
				}    			

			}

		}

		int numberOfDays = endMonth.getDayOfMonth();
		Table<Person, LocalDate, String> tablePersonTicket = builder.build();
		render(year, month, tablePersonTicket, numberOfDays, simpleResults, name);
	}




}
