package controllers;

import java.util.ArrayList;
import java.util.List;

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

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Stampings extends Controller {
		
	//private final static ActionMenuItem actionMenuItem = ActionMenuItem.stampings;
    /**
     * 
     * @param person
     * @param year
     * @param month
     */
    public static void show(Long personId, int year, int month){
    	
    	//String menuItem = actionMenuItem.toString();
    	if (personId == null) {
    		show();
    	}
    	
    	if (year == 0 || month == 0) {
    		show(personId);
    	}
    	
    	Logger.trace("Called show of personId=%s, year=%s, month=%s", personId, year, month);
    	
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
    	
    	Logger.debug("Month recap of person.id %s, year=%s, month=%s", person.id, year, month);
    	    	
        render(monthRecap, personMonth/*, menuItem*/);
    }
    
	private static void show() {
		LocalDate now = new LocalDate();
		show(Security.getPerson().getId(), now.getYear(), now.getMonthOfYear());
	}
	
	private static void show(Long personId) {
		LocalDate now = new LocalDate();
		show(personId, now.getMonthOfYear(), now.getYear());
	}
	
	
	
	
	
    public static void personStamping(Long personId, int year, int month) {
		
    	if (personId == null) {
    		personStamping();
    	}
    	
    	if (year == 0 || month == 0) {
    		personStamping(personId);
    	}
    	
    	Logger.trace("Called show of personId=%s, year=%s, month=%s", personId, year, month);
    	
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
					stamp.person = person;
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
				LocalDate datePd = new LocalDate(stamp1.date);
				PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person,datePd).first();
				pd.setStampings();
				pd.setTimeAtWork();
				pd.setDifference();
				pd.setProgressive();
				pd.setTicketAvailable();
				pd.save();
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
			
			Absence existingAbsence = Absence.find("person = ? and date = ? and absenceType = ? and id <> ?", stamping.person, stamping.date, absenceType, stamping.id).first();
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
				String.format("Assenza per il giorno %s per %s %s aggiornata con codice %s", PersonTags.toDateTime(stamping.date.toLocalDate()), stamping.person.surname, stamping.person.name, absenceCode));
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
}
