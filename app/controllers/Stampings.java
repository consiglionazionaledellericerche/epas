package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Absence;
import models.AbsenceType;
import models.ConfGeneral;
import models.Contract;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;
import models.PersonMonthRecap;
import models.PersonTags;
import models.StampModificationType;
import models.StampProfile;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.exports.AbsenceReperibilityPeriod;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.PersonTroublesInMonthRecap;
import models.rendering.PersonStampingDayRecap;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;
import com.ning.http.util.DateUtil.DateParseException;



@With( {Secure.class, NavigationMenu.class} )
public class Stampings extends Controller {

	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 */
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void stampings(Integer year, Integer month){

		if (Security.getPerson().username.equals("admin")) {
			Application.indexAdmin();
		}
		Person person = Security.getPerson();
		if(!person.isActiveInMonth(month, year))
		{
			flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
					"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			render("@redirectToIndex");
		}
		
		LocalDate today = new LocalDate();
		if(today.getYear()==year && month>today.getMonthOfYear())
		{
			flash.error("Impossibile accedere a situazione futura, redirect automatico a mese attuale");
			month = today.getMonthOfYear();
		}
		
		
	
		
		//Configuration conf = Configuration.getCurrentConfiguration();
		ConfGeneral conf = ConfGeneral.getConfGeneral();
		int minInOutColumn = conf.numberOfViewingCoupleColumn;
		int numberOfInOut = Math.max(minInOutColumn, PersonUtility.getMaximumCoupleOfStampings(person, year, month));

		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = PersonUtility.getTotalPersonDayInMonth(person, year, month);
		
		//Costruzione dati da renderizzare
		for(PersonDay pd : totalPersonDays)
		{
			pd.computeValidStampings(); //calcolo del valore valid per le stamping del mese (persistere??)
		}
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(PersonDay pd : totalPersonDays )
		{
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);
			daysRecap.add(dayRecap);
		}
		List<StampModificationType> stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;
		List<StampType> stampTypeList = PersonStampingDayRecap.stampTypeList;
		
		int numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month);
		int numberOfMealTicketToUse = PersonUtility.numberOfMealTicketToUse(person, year, month);
		int numberOfMealTicketToRender = PersonUtility.numberOfMealTicketToRender(person, year, month);
		int basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);
		Map<AbsenceType,Integer> absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);

		List<Contract> monthContracts = person.getMonthContracts(month, year);
		List<Mese> contractMonths = new ArrayList<Mese>();
		for(Contract contract : monthContracts)
		{
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, null);
			if(c.getMese(year, month)!=null)
				contractMonths.add(c.getMese(year, month));
		}
		if(contractMonths.size()==0)
		{
			flash.error("Impossibile visualizzare la situazione mensile per %s %s per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			render("@redirectToIndex");
		}
		
		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		
		//Render
		render(person, year, month, numberOfInOut, numberOfCompensatoryRestUntilToday,numberOfMealTicketToUse,numberOfMealTicketToRender,
				daysRecap, stampModificationTypeList, stampTypeList, basedWorkingDays, absenceCodeMap, contractMonths, month_capitalized);

	}


	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void personStamping(Long personId, int year, int month) {
		
		if (personId == null){
			personStamping();
		}
		if (year == 0 || month == 0) {
			personStamping(personId);
		}
		Person person = Person.findById(personId);
		if(!person.isActiveInMonth(month, year))
		{
			flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
					"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			render("@redirectToIndex");
		}
		
//		LocalDate today = new LocalDate();
//		if(today.getYear()==year && month>today.getMonthOfYear())
//		{
//			flash.error("Impossibile accedere a situazione futura, redirect automatico a mese attuale");
//			month = today.getMonthOfYear();
//		}
		
		//Configuration conf = Configuration.getCurrentConfiguration();													//0 sql (se già in cache)
		ConfGeneral conf = ConfGeneral.getConfGeneral();
		int minInOutColumn = conf.numberOfViewingCoupleColumn;
		int numberOfInOut = Math.max(minInOutColumn, PersonUtility.getMaximumCoupleOfStampings(person, year, month));	//30 sql

		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = PersonUtility.getTotalPersonDayInMonth(person, year, month);					//1 sql
		
		//Costruzione dati da renderizzare
		for(PersonDay pd : totalPersonDays)
		{
			pd.computeValidStampings(); //calcolo del valore valid per le stamping del mese								//0 sql
		}
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(PersonDay pd : totalPersonDays )
		{
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);								//1 quando non e' festa (absence)						
			daysRecap.add(dayRecap);
		}
		List<StampModificationType> stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;		//0 sql
		List<StampType> stampTypeList = PersonStampingDayRecap.stampTypeList;											//0 sql
		
		int numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month); //1 sql
		int numberOfMealTicketToUse = PersonUtility.numberOfMealTicketToUse(person, year, month);						//1 sql
		int numberOfMealTicketToRender = PersonUtility.numberOfMealTicketToRender(person, year, month);					//1 sql
		int basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);											//0 sql
		Map<AbsenceType,Integer> absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);				//1 sql

		List<Contract> monthContracts = person.getMonthContracts(month, year);
		List<Mese> contractMonths = new ArrayList<Mese>();
		for(Contract contract : monthContracts)
		{
			CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, null);
			if(c.getMese(year, month)!=null)
				contractMonths.add(c.getMese(year, month));
		}
		String month_capitalized = DateUtility.fromIntToStringMonth(month);

		//Render	//0 sql
		render(person, year, month, numberOfInOut, numberOfCompensatoryRestUntilToday,numberOfMealTicketToUse,numberOfMealTicketToRender,
				daysRecap, stampModificationTypeList, stampTypeList, basedWorkingDays, absenceCodeMap, contractMonths, month_capitalized);

		 
	}

	private static void personStamping() {
		LocalDate now = new LocalDate();
		personStamping(Security.getPerson().getId(), now.getYear(),now.getMonthOfYear());
	}

	private static void personStamping(Long personId) {
		LocalDate now = new LocalDate();
		personStamping(personId, now.getYear(), now.getMonthOfYear());
	}



	public static void dailyStampings() {
		Person person = Person.findById(params.get("id", Long.class));
		LocalDate day = 
				new LocalDate(
						params.get("year", Integer.class),
						params.get("month", Integer.class), 
						params.get("day", Integer.class));

		Logger.trace("dailyStampings called for %s %s", person, day);

		PersonDay personDay = new PersonDay(person, day);
		render(personDay);
	}


	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void create(@Required Long personId, @Required Integer year, @Required Integer month, @Required Integer day){
		Logger.debug("Insert stamping called for personId=%d, year=%d, month=%d, day=%d", personId, year, month, day);
		Person person = Person.findById(personId);

		Logger.debug("La person caricata è: %s", person);   	      	
		LocalDate date = new LocalDate(year,month,day);
		Logger.debug("La data è: %s", date);
		PersonDay personDay = new PersonDay(person, date);

		render(person, personDay);
	}

	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void insert(@Valid @Required Long personId, @Required Integer year, @Required Integer month, @Required Integer day) {

		Person person = Person.em().getReference(Person.class, personId);

		LocalDate date = new LocalDate(year,month,day);
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		if(pd == null){
			pd = new PersonDay(person, date);
			pd.create();
		}

//		if(pd.stampings.size() == 0 && pd.isHoliday()){
//			flash.error("Si sta inserendo una timbratura in un giorno di festa. Errore");
//			Stampings.personStamping(personId, year, month);
//		}

		if(date.isAfter(new LocalDate())){
			flash.error("Non si può inserire una timbratura futura!!!");
			Stampings.personStamping(personId, year, month);
		}

		if(params.get("timeAtWork", Boolean.class) == true){
			//pd.timeAtWork = person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(new LocalDate(year, month, day).getDayOfWeek()).workingTime;
			pd.timeAtWork = person.getWorkingTimeType(new LocalDate(year,month,day)).getWorkingTimeTypeDayFromDayOfWeek(new LocalDate(year, month, day).getDayOfWeek()).workingTime;
			pd.save();
			pd.populatePersonDay();
			pd.save();
			flash.success("Inserita timbratura forzata all'orario di lavoro per %s %s", person.name, person.surname);
			Stampings.personStamping(personId, year, month);
		}
		Integer hour = params.get("hourStamping", Integer.class);
		Integer minute = params.get("minuteStamping", Integer.class);
		//Logger.debug("I parametri per costruire la data sono: anno: %s, mese: %s, giorno: %s, ora: %s, minuti: %s", year, month, day, hour, minute);

		String type = params.get("type");
		String service = params.get("service");
		Stamping stamp = new Stamping();
		stamp.date = new LocalDateTime(year, month, day, hour, minute, 0);
		stamp.markedByAdmin = true;		

		if(service.equals("true")){
			stamp.note = "timbratura di servizio";
			stamp.stampType = StampType.find("Select st from StampType st where st.code = ?", "motiviDiServizio").first();
		}
		else{
			String note = params.get("note");
			if(!note.equals(""))
				stamp.note = note;
			else
				stamp.note = "timbratura inserita dall'amministratore";
		}
		if(type.equals("true")){
			stamp.way = Stamping.WayType.in;
		}
		else{
			stamp.way = Stamping.WayType.out;
		}
		stamp.personDay = pd;
		stamp.save();
		pd.stampings.add(stamp);
		pd.save();
			
		pd.populatePersonDay();
		pd.updatePersonDaysInMonth();
		
		flash.success("Inserita timbratura per %s %s in data %s", person.name, person.surname, date);

		Stampings.personStamping(personId, year, month);


	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void edit(@Required Long stampingId) {
		Logger.debug("Edit stamping called for stampingId=%d", stampingId);

		Stamping stamping = Stamping.findById(stampingId);
		if (stamping == null) {
			notFound();
		}

		LocalDate date = stamping.date.toLocalDate();
		List<String> hourMinute = timeDivided(stamping);
		render(stamping, hourMinute, date);				
	}

	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void update() {
		Stamping stamping = Stamping.findById(params.get("stampingId", Long.class));
		if (stamping == null) {
			notFound();
		}
		PersonDay pd = stamping.personDay;
		Integer hour = params.get("stampingHour", Integer.class);
		Integer minute = params.get("stampingMinute", Integer.class);
		
		if(hour != null && minute == null || hour == null && minute != null)
		{
			flash.error("Attribuire valore a ciascun campo se si intende modificare la timbratura o togliere valore a entrambi i campi" +
					" se si intende cancellarla");
			Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());
		}
		if (hour == null && minute == null) 
		{

			stamping.delete();
			pd.stampings.remove(stamping);

			pd.populatePersonDay();
			pd.updatePersonDaysInMonth();
	
			flash.success("Timbratura per il giorno %s rimossa", PersonTags.toDateTime(stamping.date.toLocalDate()));	

			Stampings.personStamping(pd.person.id, pd.date.getYear(), pd.date.getMonthOfYear());

		} 
		else 
		{
			if (hour == null || minute == null) {
				flash.error("E' necessario specificare sia il campo ore che minuti, oppure nessuno dei due per rimuovere la timbratura.");
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
				stamping.stampType = StampType.find("Select st from StampType st where st.code = ?", "motiviDiServizio").first();
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
		
			pd.populatePersonDay();
			pd.updatePersonDaysInMonth();
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



	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void discard(){
		personStamping();
	}




	/**
	 * Controller che attua la verifica di timbrature mancanti per il mese selezionato per tutte quelle persone
	 * che avevano almeno un contratto attivo in tale mese
	 * @param year
	 * @param month
	 * @throws ClassNotFoundException 
	 * @throws SQLException 
	 */
	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void missingStamping(int year, int month) throws ClassNotFoundException, SQLException{

		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();

		//lista delle persone che sono state attive nel mese
		List<Person> activePersons = Person.getActivePersonsInMonth(month, year, Security.getPerson().getOfficeAllowed(), false);

		List<PersonTroublesInMonthRecap> missingStampings = new ArrayList<PersonTroublesInMonthRecap>();
		
		for(Person person : activePersons)
		{
			PersonTroublesInMonthRecap pt = new PersonTroublesInMonthRecap(person, monthBegin, monthEnd);
			missingStampings.add(pt);
		}
		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(month, year, missingStampings, month_capitalized);




	}



	/**
	 * Calcola il numero massimo di coppie ingresso/uscita nel personday di un giorno specifico per tutte le persone presenti nella lista
	 * di persone attive a quella data
	 * @param year
	 * @param month
	 * @param day
	 * @param activePersonsInDay
	 * @return 
	 */
	private static int maxNumberOfStampingsInMonth(Integer year, Integer month, Integer day, List<Person> activePersonsInDay){
		
		LocalDate date = new LocalDate(year, month, day);
		int max = 0;
			
		for(Person person : activePersonsInDay){
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.date = ? and pd.person = ?", 
					date, person).first();

			if(max < PersonUtility.numberOfInOutInPersonDay(pd))
				max = PersonUtility.numberOfInOutInPersonDay(pd);

		}
		return max;

	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void dailyPresence(Integer year, Integer month, Integer day) {

		LocalDate dayPresence = new LocalDate(year, month, day);
		List<Person> activePersonsInDay = Person.getActivePersonsInDay(day, month, year, Security.getPerson().getOfficeAllowed(), false);
		int numberOfInOut = maxNumberOfStampingsInMonth(year, month, day, activePersonsInDay);
		
		
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();						
		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(Person person : activePersonsInDay){
			if(!person.username.equals("epas.clocks")){
				PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.date = ? and pd.person = ?", dayPresence, person).first();
				if(pd==null)
					pd = new PersonDay(person, dayPresence);

				pd.computeValidStampings();
				daysRecap.add(new PersonStampingDayRecap(pd, numberOfInOut));

			}
						
		}

		
		
		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(daysRecap, year, month, day, numberOfInOut, month_capitalized);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void mealTicketSituation(Integer year, Integer month){

		LocalDate beginMonth = new LocalDate(year, month, 1);

		List<Person> activePersons = Person.getActivePersonsInMonth(month, year, Security.getPerson().getOfficeAllowed(), false);
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
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
					p, beginMonth, beginMonth.dayOfMonth().withMaximumValue()).fetch();
			Logger.debug("La lista dei personDay: ", pdList);
			for(PersonDay pd : pdList){
				if(pd.isTicketForcedByAdmin)
				{
					if(pd.isTicketAvailable)
					{
						builder.put(p, pd.date, "siAd");
					}
					else
					{
						builder.put(p, pd.date, "noAd");
					}
				}
				else
				{
					if(pd.isTicketAvailable)
					{
						builder.put(p, pd.date, "si");
					}
					else
					{
						builder.put(p, pd.date, "");
					}
				}    			

			}

		}
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		int numberOfDays = endMonth.getDayOfMonth();
		Table<Person, LocalDate, String> tablePersonTicket = builder.build();
		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(year, month, tablePersonTicket, numberOfDays, month_capitalized);
	}




}
