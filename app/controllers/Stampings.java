package controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Configuration;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.PersonTags;
import models.Stamping;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;

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
    public static void show(Long personId, int year, int month){

		if (Security.getPerson().username.equals("admin")) {
			Application.indexAdmin();
		}
		
    	if (personId == null) {
    	
    		show();
    	}
    	
    	if (year == 0 || month == 0) {
    		show(personId);
    	}
    	
    	int previousMonth = 0;
    	int previousYear = 0;
    	
    	PersonMonth previousPersonMonth = null;
    	
    	Logger.trace("Called show of personId=%s, year=%s, month=%s", personId, year, month);
    	long id = 1;
    	Configuration confParameters = Configuration.findById(id);
    	Person person = Person.findById(personId);
    	    	
    	PersonMonth personMonth =
    			PersonMonth.find(
    				"Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", 
    				person, year, month).first();
    	
    	if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
    	
    	Logger.debug("Controllo gli straordinari nel corso dell'anno fino ad oggi per %s %s...", person.name, person.surname);
    	int overtimeHour = personMonth.getOvertimeHourInYear();
    	Logger.debug("Le ore di straordinario da inizio anno sono: %s", overtimeHour);
    	
    	if(month == 1){    		
    		previousMonth = 12;
    		previousYear = year - 1;
    		Logger.debug("Prendo il personMonth relativo al dicembre dell'anno precedente: %s %s", previousMonth, previousYear);
    		previousPersonMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
    			person, previousMonth, previousYear).first();
    	}
    	else{
    		previousMonth = month - 1;
    		Logger.debug("Prendo il personMonth relativo al mese precedente: %s %s", previousMonth, year);
    		previousPersonMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?",
    				person, previousMonth, year).first();
    	}
    	
    	int numberOfCompensatoryRest = personMonth.getCompensatoryRestInYear();
    	int numberOfInOut = Math.min(confParameters.numberOfViewingCoupleColumn, (int)personMonth.getMaximumCoupleOfStampings());
    	
    	Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
    	    	
        render(personMonth, numberOfInOut, numberOfCompensatoryRest, previousPersonMonth, overtimeHour);
    }
    
	@Check(Security.VIEW_PERSONAL_SITUATION)
	private static void show() {
		LocalDate now = new LocalDate();
		show(Security.getPerson().getId(), now.getYear(), now.getMonthOfYear());
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	private static void show(Long personId) {
		LocalDate now = new LocalDate();
		show(personId, now.getMonthOfYear(), now.getYear());
	}
	
	
	
	
	@Check(Security.INSERT_AND_UPDATE_STAMPING)
    public static void personStamping(Long personId, int year, int month) {
		
    	if (personId == null) {
    		personStamping();
    	}
    	
    	if (year == 0 || month == 0) {
    		personStamping(personId);
    	}
    	int previousMonth = 0;
    	int previousYear = 0;
    	
    	PersonMonth previousPersonMonth = null;
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
			personMonth = new PersonMonth(person, year, month);
			personMonth.create();
			
		}
    	
    	Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
    	Logger.debug("PersonMonth of person.id %s, year=%s, month=%s", person.id, year, month);
    	if(month == 1){    		
    		previousMonth = 12;
    		previousYear = year - 1;
    		Logger.debug("Prendo il personMonth relativo al dicembre dell'anno precedente: %s %s", previousMonth, previousYear);
    		previousPersonMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
    			person, previousMonth, previousYear).first();
    	}
    	else{
    		previousMonth = month - 1;
    		Logger.debug("Prendo il personMonth relativo al mese precedente: %s %s", previousMonth, year);
    		previousPersonMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?",
    				person, previousMonth, year).first();
    	}
    	Logger.debug("Controllo gli straordinari nel corso dell'anno fino ad oggi per %s %s...", person.name, person.surname);
    	int overtimeHour = personMonth.getOvertimeHourInYear();
    	Logger.debug("Le ore di straordinario da inizio anno sono: %s", overtimeHour);
    	
    	int numberOfCompensatoryRest = personMonth.getCompensatoryRestInYear();
    	int numberOfInOut = Math.min(confParameters.numberOfViewingCoupleColumn, (int)personMonth.getMaximumCoupleOfStampings());
    	    	
        render(personMonth, numberOfInOut, previousPersonMonth, numberOfCompensatoryRest, overtimeHour);
    	
    }

	private static void personStamping() {
		LocalDate now = new LocalDate();
		personStamping(Security.getPerson().getId(), now.getMonthOfYear(), now.getYear());
	}
	
	private static void personStamping(Long personId) {
		LocalDate now = new LocalDate();
		personStamping(personId, now.getMonthOfYear(), now.getYear());
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
//		if(validation.hasErrors()) {
//			
//			render("@create", personId, year, month, day);
//		}
		Person person = Person.em().getReference(Person.class, personId);

		LocalDate date = new LocalDate(year,month,day);
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		
		if(pd.stampings.size() == 0 && pd.isHoliday()){
			flash.error("Si sta inserendo una timbratura in un giorno di festa. Errore");
			render("@create", personId, year, month, day);
		}
		Integer hour = params.get("hourStamping", Integer.class);
		Integer minute = params.get("minuteStamping", Integer.class);
		Logger.debug("I parametri per costruire la data sono: anno: %s, mese: %s, giorno: %s, ora: %s, minuti: %s", year, month, day, hour, minute);
		
		String type = params.get("type");
		Stamping stamp = new Stamping();
		stamp.date = new LocalDateTime(year, month, day, hour, minute, 0);
		stamp.markedByAdmin = true;
		stamp.note = "timbratura inserita dall'amministratore";
		if(type.equals("true")){
			stamp.way = Stamping.WayType.in;
		}
		else{
			stamp.way = Stamping.WayType.out;
		}
		stamp.personDay = pd;
		stamp.save();
		pd.stampings.add(stamp);
		pd.merge();
		pd.populatePersonDay();
		pd.save();
		flash.success("Inserita timbratura per %s %s in data %s", person.name, person.surname, date);
		Application.indexAdmin();

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
		
		Integer hour = params.get("stampingHour", Integer.class);
		Integer minute = params.get("stampingMinute", Integer.class);
		if(hour != null && minute == null || hour == null && minute != null){
			flash.error("Attribuire valore a ciascun campo se si intende modificare la timbratura o togliere valore a entrambi i campi" +
					" se si intende cancellarla");
			Stampings.personStamping();
		}
		if (hour == null && minute == null) {
			stamping.delete();
			stamping.personDay.populatePersonDay();
			stamping.personDay.save();
			flash.success("Timbratura per il giorno %s rimossa", PersonTags.toDateTime(stamping.date.toLocalDate()));	
			render("@create");
		} else {
			if (hour == null || minute == null) {
				flash.error("E' necessario specificare sia il campo ore che minuti, oppure nessuno dei due per rimuovere la timbratura.");
				render("@edit");
				return;
			}
			stamping.date.withHourOfDay(hour).withMinuteOfHour(minute);
			stamping.save();
			stamping.personDay.populatePersonDay();
			stamping.personDay.save();

			flash.success("Timbratura per il giorno %s per %s %s aggiornata.", PersonTags.toDateTime(stamping.date.toLocalDate()), stamping.personDay.person.surname, stamping.personDay.person.name);
			render("@create");
		}
		//render("@personStamping");
		Stampings.personStamping();
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
    
    @Check(Security.INSERT_AND_UPDATE_PERSON)
    public static void missingStamping(int year, int month){

    	Map<Person, List<PersonDay>> personPersonDayMap = new HashMap<Person,List<PersonDay>>();
    	List<Person> personList = Person.findAll();
    	for(Person p : personList){
    		List<PersonDay> pdMissingStampingList = new ArrayList<PersonDay>();
    		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date >= ? and pd.date <= ?", 
    				p, new LocalDate(year, month, 1), new LocalDate(year, month,1).dayOfMonth().withMaximumValue()).fetch();
    				
    		for(PersonDay pd : pdList){
    			if(pd.stampings.size() == 1 || 
    					(pd.stampings.size() == 0 && pd.absences.size() == 0 && pd.date.getDayOfWeek() != 7 && pd.date.getDayOfWeek() != 6)){
    				
    				if(!personPersonDayMap.containsKey(p)){
    					pdMissingStampingList.add(pd);
    					personPersonDayMap.put(p, pdMissingStampingList);
    				}
    				else{
    					pdMissingStampingList = personPersonDayMap.remove(p);
    					pdMissingStampingList.add(pd);
    					personPersonDayMap.put(p, pdMissingStampingList);
    				}    					
    				
    			}
    				
    		}
    	}
    	render(personPersonDayMap, month, year);
    	
    }
    
    @Check(Security.INSERT_AND_UPDATE_PERSON)
    public static void dailyPresence(Integer year, Integer month, Integer day) {

    	List<PersonDay> pdList = null;
    	LocalDate today = null;
    	if(day == null){
    		today = new LocalDate();
    		pdList = PersonDay.find("Select pd from PersonDay pd where pd.date = ?", today).fetch();
    	}    	
    	else{
    		try{
    			LocalDate date = new LocalDate(year,month,day);
				pdList = PersonDay.find("Select pd from PersonDay pd where pd.date = ?", date).fetch();
    		}
    		catch(IllegalArgumentException e){
    			flash.error(String.format("La data richiesta è errata"));
    			dailyPresence(year, month, day);
    		}
    		
    	}
    	List<Integer> days = new ArrayList<Integer>();
    	for(Integer i = 1; i < 32; i++){
    		days.add(i);
    	}
    	render(pdList, days, year, month, day);
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
