package controllers;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.cnr.iit.epas.ActionMenuItem;
import it.cnr.iit.epas.MainMenu;
import models.Absence;
import models.AbsenceType;
import models.ContactData;
import models.Contract;
import models.Location;
import models.MonthRecap;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.PersonTags;
import models.StampType;
import models.Stamping;
import models.Stamping.WayType;
import models.Configuration;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ImmutableTable.Builder;
import com.google.common.collect.Table;
import com.ning.http.util.DateUtil.DateParseException;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

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
    	    	
    	if (personId == null) {
    	
    		show();
    	}
    	
    	if (year == 0 || month == 0) {
    		show(personId);
    	}
    	
    	Logger.trace("Called show of personId=%s, year=%s, month=%s", personId, year, month);
    	long id = 1;
    	Configuration confParameters = Configuration.findById(id);
    	Person person = Person.findById(personId);
    	
    	//TODO: Se il mese è gestito vecchio... usare il monthRecap, altrimenti utilizzare il personMonth
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year, month);
    	PersonMonth personMonth =
    			PersonMonth.find(
    				"Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", 
    				person, year, month).first();
    	
    	if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
    	int numberOfInOut = Math.min(confParameters.numberOfViewingCoupleColumn, (int)personMonth.getMaximumCoupleOfStampings());
    	
    	Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
    	    	
        render(monthRecap, personMonth, numberOfInOut);
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
    	
    	Logger.debug("Called personStamping of personId=%s, year=%s, month=%s", personId, year, month);
    	
    	Person person = Person.findById(personId);
    	/**
    	 * il conf parameters serve per recuperare il parametro di quante colonne entrata/uscita far visualizzare.
    	 * Deve essere popolata una riga di quella tabella prima però....
    	 */
    	long id = 1;
    	Configuration confParameters = Configuration.findById(id);
    	//TODO: Se il mese è gestito vecchio... usare il monthRecap, altrimenti utilizzare il personMonth
    	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year, month);
    	PersonMonth personMonth =
    			PersonMonth.find(
    				"Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
    				person, month, year).first();
    	
    	if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
    	
    	Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
    	Logger.debug("PersonMonth of person.id %s, year=%s, month=%s", person.id, year, month);
    	
    	
    	int numberOfInOut = Math.min(confParameters.numberOfViewingCoupleColumn, (int)personMonth.getMaximumCoupleOfStampings());
    	    	
        render(monthRecap, personMonth, numberOfInOut);
    	
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
    	Person person = Person.em().getReference(Person.class, personId);
    	    	      	
    	LocalDate date = new LocalDate(year,month,day);
    	    	
    	PersonDay personDay = new PersonDay(person, date);
    	
    	render(personDay);
    }
    
    @Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void insert(@Valid @Required Long personId, @Required Integer year, @Required Integer month, @Required Integer day, String s) {
		if(validation.hasErrors()) {
			
			render("@create", personId, year, month, day);
		}
		Person person = Person.em().getReference(Person.class, personId);
		
		/**
		 * guardo quante e quali timbrature sono state modificate e per queste genero i nuovi stamping o aggiorno i già esistenti 
		 * TODO: completare la select per il recupero dell'eventuale già presente timbratura da aggiornare
		 */
		LocalDate date = new LocalDate(year,month,day);
		LocalDateTime startOfDay = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),0,0);
		LocalDateTime endOfDay = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),23,59);
		List<Stamping> stamping = Stamping.find("Select st from Stamping where st.person = ? " +
				"and st.date between ? and ? ", person, startOfDay, endOfDay).fetch();
		Stamping stamp = null;
		int count = 0;
		
		List<Stamping> stamps = Stamping.find("Select st from Stamping st where st.person = ? " +
				"and st.date between ? and ? order by st.date", person,startOfDay,endOfDay).fetch();
		while(count <= stamping.size()){
			if(stamping.get(count) != null){
				stamp = stamps.get(count);				
				if(stamp == null){
					stamp = new Stamping();
					stamp.date = stamping.get(count).date;
					stamp.personDay.person = person;
					stamp.markedByAdmin = true;
					if(count == 0 || count == 2 || count == 4 || count == 6){
						stamp.way = WayType.in;
					}
					else
						stamp.way = WayType.out;
					stamp.save();
				}
				else{
					stamp.date = stamping.get(count).date;
					stamp.markedByAdmin = true;
					stamp.save();
				}				
														
			}
			count++;
		}		
		/**
		 * a questo punto devono essere ricalcolati tutti i valori del timeAtWork, del progressive e della difference oltre che 
		 * dell'assegnazione del buono pasto sul personDay comprendente la nuova timbratura
		 */
		boolean flag = false;
		int i = 0;
		Stamping stamp1 = null;
		while(flag == false && i<stamping.size()){
			stamp1 = stamping.get(i);
			if(stamp1 != null && stamp1.date != null){
				stamp1.personDay.populatePersonDay();
				
				stamp1.personDay.save();
				
				/**
				 * TODO: applicare la logica del ricalcolo mensile e annuale se mi trovo nel primo giorno del nuovo mese o del nuovo anno
				 */
				flag = true;
			}
			else{
				i++;
			}
			
		}		
		
		render("@save");
	}
    
    @Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void edit(@Required Long stampingId) {
    	Logger.debug("Edit absence called for absenceId=%d", stampingId);
    	
    	Stamping stamping = Stamping.findById(stampingId);
    	if (stamping == null) {
    		notFound();
    	}
    	LocalDate date = stamping.date.toLocalDate();
    	List<String> hourMinute = timeDivided(stamping);
		render(stamping, hourMinute, date);				
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void update() {
		Stamping stamping = Stamping.findById(params.get("stampingId", Long.class));
		if (stamping == null) {
			notFound();
		}
		//String oldAbsenceCode = stamping.absenceType.code;
		String absenceCode = params.get("absenceCode");
		if (absenceCode == null || absenceCode.isEmpty()) {
			stamping.delete();
			//flash.success("Timbratura di tipo %s per il giorno %s rimossa", oldAbsenceCode, PersonTags.toDateTime(stamping.date.toLocalDate()));			
		} else {
			
			AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
			
			Absence existingAbsence = Absence.find("person = ? and date = ? and absenceType = ? and id <> ?", stamping.personDay.person, stamping.date, absenceType, stamping.id).first();
			if(existingAbsence != null){
				validation.keep();
				params.flash();
				flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(stamping.date.toLocalDate()));
				edit(stamping.id);
				render("@edit");
			}
			//stamping.absenceType = absenceType;
			stamping.save();
			flash.success(
				String.format("Assenza per il giorno %s per %s %s aggiornata con codice %s", PersonTags.toDateTime(stamping.date.toLocalDate()), stamping.personDay.person.surname, stamping.personDay.person.name, absenceCode));
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
    public static void dailyPresence(Integer year, Integer month, Integer day) throws DateParseException{

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
    	/**
    	 * TODO: nuovo tipo di permesso per la visualizzazione della situazione mensile dei buoni pasto
    	 */
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
