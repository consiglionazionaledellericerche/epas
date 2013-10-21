package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.MainMenu;

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
import models.Configuration;
import models.Contract;
import models.InitializationAbsence;
import models.InitializationTime;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.PersonTags;
import models.StampProfile;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;
import models.WorkingTimeTypeDay;
import models.efficiency.EfficientPersonDay;
import models.exports.AbsenceReperibilityPeriod;

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

		Configuration confParameters = Configuration.getCurrentConfiguration();
		if(confParameters == null)
			confParameters = Configuration.find("Select c from Configuration c order by c.id desc").first();
		
		Person person = Security.getPerson();
		Logger.debug("La persona presa dal security è: %s %s", person.name, person.surname);
		LocalDate date = new LocalDate();
		Logger.trace("Anno: "+year);    	
		Logger.trace("Mese: "+month);
		if(year == null || month == null){
			year = date.getYear();
			month = date.getMonthOfYear();
		}
		person = person.findById(person.id);
		//person.refresh();
		PersonMonth personMonth = PersonMonth.byPersonAndYearAndMonth(person, year, month);
		if(personMonth == null)
			personMonth = new PersonMonth(person, year, month);
		int situazioneParziale = 0;
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				Security.getPerson(), new LocalDate(year, month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
		if(pdList.size() == 0 && year > new LocalDate().getYear())
			situazioneParziale = 0;
		else{
			if(personMonth.month == 1){
				situazioneParziale = personMonth.residuoAnnoCorrenteDaInizializzazione();
			}
			else{
				PersonMonth count = personMonth;
				while(count.month > 1){
					situazioneParziale = situazioneParziale + count.mesePrecedente().residuoDelMese() - count.mesePrecedente().straordinari;
					count = count.mesePrecedente();
				}
			}
		}		
		
		int numberOfCompensatoryRest = personMonth.getCompensatoryRestInYear();
		int numberOfInOut = Math.max(confParameters.numberOfViewingCoupleColumn, (int)personMonth.getMaximumCoupleOfStampings());

		int totaleResiduo = situazioneParziale+personMonth.residuoDelMese();
		//calcolo del valore valid per le stamping del mese
		personMonth.getDays();
		for(PersonDay pd : personMonth.days)
		{
			pd.computeValidStampings();
		}
		
		render(personMonth, numberOfInOut, numberOfCompensatoryRest, situazioneParziale, totaleResiduo);
	}


	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void personStamping(Long personId, int year, int month) {

		if (personId == null) {
			personStamping();
		}
		
		if (year == 0 || month == 0) {
			personStamping(personId);
		}

//		PersonMonth previousPersonMonth = null;
		Logger.debug("Called personStamping of personId=%s, year=%s, month=%s", personId, year, month);

		Person person = Person.findById(personId);
		/**
		 * il conf parameters serve per recuperare il parametro di quante colonne entrata/uscita far visualizzare.
		 * Deve essere popolata una riga di quella tabella prima però....
		 */
		long id = 1;
		Configuration confParameters = Configuration.findById(id);

		PersonMonth personMonth =
				PersonMonth.find(
						"Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
						person, month, year).first();

		if (personMonth == null) {
			/**
			 * se il personMonth che viene richiesto, è situato nel tempo prima dell'inizio del contratto della persona oppure successivamente 
			 * ad esso, se quest'ultimo è a tempo determinato (expireContract != null), si rimanda alla pagina iniziale perchè si tenta di accedere
			 * a un periodo fuori dall'intervallo temporale in cui questa persona ha un contratto attivo
			 */
			if(new LocalDate(year, month, 1).dayOfMonth().withMaximumValue().isBefore(person.getCurrentContract().beginContract)
					 || (person.getCurrentContract().expireContract != null && new LocalDate(year, month, 1).isAfter(person.getCurrentContract().expireContract))){
				flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
						"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
				render("@redirectToIndex");
			}
			personMonth = new PersonMonth(person, year, month);
			personMonth.create();

		}
		
		int situazioneParziale = 0;
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				Security.getPerson(), new LocalDate(year, month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
		
		if(pdList.size() == 0 && year > new LocalDate().getYear())
			situazioneParziale = 0;
		else{
			if(personMonth.month == 1){
				situazioneParziale = personMonth.residuoAnnoCorrenteDaInizializzazione();
			}
			else{
				PersonMonth count = personMonth;
				while(count.month > 1){
					Logger.debug("Mese precedente: %s", count.mesePrecedente());
					if(count.mesePrecedente() != null)
						situazioneParziale = situazioneParziale + count.mesePrecedente().residuoDelMese() - count.mesePrecedente().straordinari;
					
					count = count.mesePrecedente();
					if(count == null)
						break;
				}
			}
		}
		
		
//		int overtimeHour = personMonth.getOvertimeHourInYear(new LocalDate(year,month,1).dayOfMonth().withMaximumValue());
		int numberOfCompensatoryRestUntilToday = personMonth.numberOfCompensatoryRestUntilToday();

		int numberOfCompensatoryRest = personMonth.getCompensatoryRestInYear();
		int numberOfInOut = Math.max(confParameters.numberOfViewingCoupleColumn, (int)personMonth.getMaximumCoupleOfStampings());
//		Logger.debug("NumberOfInOut: %d, NumberOfCompensatoryRest: %d, OvertimeHour: %d", numberOfInOut, numberOfCompensatoryRest, overtimeHour);
		
		//calcolo del valore valid per le stamping del mese
		personMonth.getDays();
		for(PersonDay pd : personMonth.days)
		{
			pd.computeValidStampings();
		}
		int totaleResiduo = situazioneParziale+personMonth.residuoDelMese();
		render(personMonth, numberOfInOut, numberOfCompensatoryRestUntilToday, numberOfCompensatoryRest,situazioneParziale, totaleResiduo);

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
		
		if(pd.stampings.size() == 0 && pd.isHoliday()){
			flash.error("Si sta inserendo una timbratura in un giorno di festa. Errore");
			render("@save");
		}
		
		if(date.isAfter(new LocalDate())){
			flash.error("Non si può inserire una timbratura futura!!!");
			render("@save");
		}
		
		
		
		/**
		 * controllo che il radio button sulla timbratura forzata all'orario di lavoro sia checkato
		 */
		
		if(params.get("timeAtWork", Boolean.class) == true){
			pd.timeAtWork = person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(new LocalDate(year, month, day).getDayOfWeek()).workingTime;
			pd.save();
			pd.populatePersonDay();
			pd.save();
			flash.success("Inserita timbratura forzata all'orario di lavoro per %s %s", person.name, person.surname);
			render("@save");
		}
		Integer hour = params.get("hourStamping", Integer.class);
		Integer minute = params.get("minuteStamping", Integer.class);
		Logger.debug("I parametri per costruire la data sono: anno: %s, mese: %s, giorno: %s, ora: %s, minuti: %s", year, month, day, hour, minute);

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
		pd.save();
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ?", 
				pd.person, pd.date).fetch();
		for(PersonDay p : pdList){
			if(p.date.getMonthOfYear() == stamp.date.getMonthOfYear()){
				p.populatePersonDay();
				p.save();
			}
			
		}
		flash.success("Inserita timbratura per %s %s in data %s", person.name, person.surname, date);
		render("@save");
		

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
		if(hour != null && minute == null || hour == null && minute != null){
			flash.error("Attribuire valore a ciascun campo se si intende modificare la timbratura o togliere valore a entrambi i campi" +
					" se si intende cancellarla");
			render("@save");
		}
		if (hour == null && minute == null) {
			
			stamping.delete();
			pd.stampings.remove(stamping);
//			stamping.considerForCounting = true;
//			stamping.save();
			pd.populatePersonDay();
			pd.save();
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ?", 
					pd.person, pd.date).fetch();
			for(PersonDay p : pdList){
				if(p.date.getMonthOfYear() == stamping.date.getMonthOfYear()){
					p.populatePersonDay();
					p.save();
				}
				
			}
			flash.success("Timbratura per il giorno %s rimossa", PersonTags.toDateTime(stamping.date.toLocalDate()));	
		
			render("@save");

		} else {
			if (hour == null || minute == null) {
				flash.error("E' necessario specificare sia il campo ore che minuti, oppure nessuno dei due per rimuovere la timbratura.");
				render("@edit");
				return;
			}
			Logger.debug("Ore: %s Minuti: %s", hour, minute);
			
			stamping.date = stamping.date.withHourOfDay(hour);
			stamping.date = stamping.date.withMinuteOfHour(minute);
			
			stamping.markedByAdmin = true;
			stamping.note = "timbratura modificata dall'amministratore";
			stamping.save();
			pd.populatePersonDay();
			pd.save();
			//stamping.personDay.populatePersonDay();
			//stamping.personDay.save();
			Logger.debug("Aggiornata ora della timbratura alle ore: %s", stamping.date);
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ?", 
					stamping.personDay.person, stamping.personDay.date).fetch();
			for(PersonDay p : pdList){
				if(p.date.getMonthOfYear() == stamping.date.getMonthOfYear()){
					p.populatePersonDay();
					p.save();
				}
				
			}
			flash.success("Timbratura per il giorno %s per %s %s aggiornata.", PersonTags.toDateTime(stamping.date.toLocalDate()), stamping.personDay.person.surname, stamping.personDay.person.name);

		}
		render("@save");

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
		
		//Immutable table per rendering
		ImmutableTable.Builder<Person, String, List<Integer>>  builder = ImmutableTable
				.<Person, String, List<Integer>>builder()
				.orderRowsBy(new Comparator<Person>(){
			public int compare(Person p1, Person p2) {

				return p1.surname.compareTo(p2.surname);
			}

		});
		Table<Person, String, List<Integer>> tableMissingStampings = null;
		
	
		LocalDate monthBegin = new LocalDate().withYear(year).withMonthOfYear(month).withDayOfMonth(1);
		LocalDate monthEnd = new LocalDate().withYear(year).withMonthOfYear(month).dayOfMonth().withMaximumValue();
		LocalDate today = new LocalDate();
		
		//lista delle persone che sono state attive nel mese
		List<Person> activePersons = Person.getActivePersonsInMonth(month, year);
				
		//diagnosi
		boolean dbConsistentControl = PersonDay.diagnosticPersonDay(year, month, activePersons);
				
		
		//Se mese attuale considero i person day fino a ieri
		if(today.getMonthOfYear()==month)
		{
			monthEnd = today.minusDays(1);
		}
		//Se oggi e' il primo giorno del mese stampo la tabella vuota 
		if(today.getDayOfMonth()==1)
		{
			for(Person person : activePersons)
			{
				List<Integer> pdMissingStampingList = new ArrayList<Integer>();
				builder.put(person, "Giorni del mese da controllare", pdMissingStampingList);
			}
			tableMissingStampings = builder.build();
			render(tableMissingStampings, month, year, dbConsistentControl);
		}
	
		for(Person person : activePersons)
		{
			List<EfficientPersonDay> personDayRsList = EfficientPersonDay.getEfficientPersonDays(person, monthBegin, monthEnd);
			//personDayRsList di norma non e' vuoto, capita per esempio per Cresci/DiPietro/Conti etc che non dovrebbero esistere
			if(personDayRsList.size()!=0)
			{
				checkPersonMissingStampings(person, personDayRsList, builder);
			}
			
		}
		tableMissingStampings = builder.build();
		render(tableMissingStampings, month, year, dbConsistentControl);
		
		
	
	
	}
	
	/**
	 * Metodo privato che verifica per ogni giorno lavorativo del mese se la persona ha timbrature mancanti
	 * In caso affermativo il giorno individuato viene inserito in una lista nella tabella ImmutableTable
	 * @param currentPerson
	 * @param personDayRsList
	 * @param builder
	 */
	private static void checkPersonMissingStampings(
			Person currentPerson, 
			List<EfficientPersonDay> personDayRsList, 
			ImmutableTable.Builder<Person, String, List<Integer>> builder)
	{
		
		List<Integer> pdMissingStampingList = new ArrayList<Integer>();
		for(EfficientPersonDay currentPersonDayRs : personDayRsList)
		{
			//controllo 
			if(currentPersonDayRs.fixed==true)
				continue;
			if(currentPersonDayRs.justified!=null && currentPersonDayRs.justified.equals("AllDay"))
				continue;
			if(currentPersonDayRs.way.size()==0 || currentPersonDayRs.way.size()%2==1)		//zero timbrature o timbrature dispari
			{
				pdMissingStampingList.add(currentPersonDayRs.date.getDayOfMonth());
			}
			else 																			//timbrature pari ma due consecutive uguali
			{
				String lastWay = "out";						
				for(String way : currentPersonDayRs.way)
				{
					if(way.equals(lastWay))
					{
						pdMissingStampingList.add(currentPersonDayRs.date.getDayOfMonth());
						break;
					}
					lastWay = way;
				}
			}
		}
		builder.put(currentPerson, "Giorni del mese da controllare", pdMissingStampingList);
		
	}
	
	

	private static int maxNumberOfStampingsInMonth(Integer year, Integer month, Integer day){
		LocalDate date = new LocalDate(year, month,1);
		int max = 0;
		List<Person> activePersons = null;
		if(day != null)
			activePersons = Person.getActivePersons(new LocalDate(year, month, day));
		else
			activePersons = Person.getActivePersons(date);
		for(Person person : activePersons){
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.date between ? and ?  and pd.person = ?", 
					date, date.dayOfMonth().withMaximumValue(), person).fetch();
			for(PersonDay pd : pdList){
				if(max < pd.stampings.size())
					max = pd.stampings.size();
			}
		}
		return max;

	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void dailyPresence(Integer year, Integer month, Integer day) {

		int maxNumberOfInOut = maxNumberOfStampingsInMonth(year, month, day);
		Logger.debug("Il numero massimo di timbrature tra tutti i dipendenti per questo mese è: %d", maxNumberOfInOut);
		ImmutableTable.Builder<Person, String, String> builder = ImmutableTable.builder();
		Table<Person, String, String> tablePersonDailyPresence = null;
		//Table<Person, String, String> tablePersonDailyPresence =  HashBasedTable.create();
		//List<Person> persons = Person.findAll();
		//
		//List<Person> activePersons = Person.getActivePersons(new LocalDate(year, month, giorno));
		LocalDate today = new LocalDate(year, month, day);
		List<Person> persons = new ArrayList<Person>();
			List<Person> genericPerson = Person.find("Select p from Person p order by p.surname").fetch();
			for(Person p : genericPerson){
				Contract c = Contract.find("Select c from Contract c where c.person = ? and ((c.beginContract != null and c.expireContract = null) or " +
						"(c.expireContract > ?) or (c.beginContract = null and c.expireContract = null)) order by c.beginContract desc limit 1", 
						p, today).first();
				if(c != null && c.onCertificate == true)
					persons.add(p);
			}
		Logger.trace("Gli utenti attivi in questo giorno sono: %d", persons.size());

		
		Person per = new Person();
		builder.put(per, "Assenza", "");
		for(int i = 1; i <= maxNumberOfInOut; i++){
			if(i % 2 != 0){
				builder.put(per, (i+1)/2+"^ Ingresso", "");    			
			}
			else{
				builder.put(per, (i/2)+"^ Uscita", "");
			}
		}

		for(Person p : persons){
			//Logger.trace("Inizio le operazioni di inserimento in tabella per %s %s ",p.name, p.surname);
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.date = ? and pd.person = ?", today, p).first();
			//Logger.trace("Cerco il person day in data %s per %s %s", today, p.name, p.surname);
			if(pd != null){
				if(pd.absences.size() > 0)
					builder.put(p, "Assenza", pd.absences.get(0).absenceType.code);
				else
					builder.put(p, "Assenza", " ");
				int size = pd.stampings.size();
				/**
				 * TODO: si verificano casi in cui una persona fa due timbratue di uscita (Pinizzotto il 17 gennaio 2013), come lo gestisco?
				 * 
				 */
				for(int i = 0; i < size; i++){
					if(pd.stampings.get(i).way == WayType.in){
						builder.put(p, 1+(i+1)/2+"^ Ingresso", PersonTags.toCalendarTime(pd.stampings.get(i).date));
						//Logger.trace("inserisco in tabella l'ingresso per %s %s", p.name, p.surname);
					}
					else{
						builder.put(p, 1+(i/2)+"^ Uscita", PersonTags.toCalendarTime(pd.stampings.get(i).date));
						//Logger.trace("inserisco in tabella l'uscita per %s %s", p.name, p.surname);
					}
				}

				builder.put(p, "Tempo Lavoro", PersonTags.toHourTime(pd.timeAtWork));
			}

		}
		tablePersonDailyPresence = builder.build();
		render(tablePersonDailyPresence, year, month, day, maxNumberOfInOut);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void mealTicketSituation(Integer year, Integer month){

		LocalDate beginMonth = new LocalDate(year, month, 1);

		List<Person> activePersons = Person.getActivePersons(new LocalDate(year, month, 1));
		Builder<Person, LocalDate, String> builder = ImmutableTable.<Person, LocalDate, String>builder().orderColumnsBy(new Comparator<LocalDate>() {

			public int compare(LocalDate date1, LocalDate date2) {
				return date1.compareTo(date2);
			}
		}).orderRowsBy(new Comparator<Person>(){
			public int compare(Person p1, Person p2) {

				return p1.surname.compareTo(p2.surname);
			}

		});
		for(Person p : activePersons){
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
					p, beginMonth, beginMonth.dayOfMonth().withMaximumValue()).fetch();
			Logger.debug("La lista dei personDay: ", pdList);
			for(PersonDay pd : pdList){

				Logger.debug("Per la persona %s nel personDay %s il buono mensa è: %s", p, pd, pd.isTicketAvailable);
				if(pd.isTicketAvailable == true){
					Logger.debug("Per il giorno %s il valore del ticket è: ", pd.date, "si");
					builder.put(p, pd.date, "si");
				}
				else{
					Logger.debug("Per il giorno %s il valore del ticket è: ", pd.date, "");
					builder.put(p, pd.date, "");
				}    			

			}

		}
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		int numberOfDays = endMonth.getDayOfMonth();
		Table<Person, LocalDate, String> tablePersonTicket = builder.build();
		render(year, month, tablePersonTicket, numberOfDays);
	}
	
	


}
