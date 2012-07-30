package controllers;

import java.util.Date;
import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.Absence;
import models.AbsenceType;
import models.MonthRecap;
import models.Person;
import models.PersonDay;
import models.PersonTags;
import models.Stamping;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.binding.As;
import play.data.binding.types.DateTimeBinder;
import play.data.validation.Required;
import play.db.jpa.GenericModel.JPAQuery;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Absences extends Controller{
	
	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.absences;
	
	private static List<AbsenceType> getFrequentAbsenceTypes(){
		return AbsenceType.find("Select abt from AbsenceType abt, Absence abs " +
    			"where abs.absenceType = abt group by abt order by sum(abt.id) desc limit 20").fetch();
		
	}
	
	private static List<AbsenceType> getAllAbsenceTypes(){
		
		return AbsenceType.find("Select abt from AbsenceType abt where abt.validTo > ? order by code", new LocalDate().toDate()).fetch();
	}
		 
	@Check(Security.VIEW_PERSON_LIST)
	public static void show(Person person) {
		String menuItem = actionMenuItem.toString();
		
    	String anno = params.get("year");
    	Logger.info("Anno: "+anno);
    	String mese= params.get("month");
    	Logger.info("Mese: "+mese);
    	if(anno==null || mese==null){
    		        	
        	LocalDate now = new LocalDate();
        	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
            render(monthRecap, menuItem);
    	}
    	else{
    		Logger.debug("Sono dentro il ramo else della creazione del month recap");
    		Integer year = new Integer(params.get("year"));
			Integer month = new Integer(params.get("month"));
    		MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year.intValue(), month.intValue());
    		Logger.debug("Il month recap è formato da: " +person.id+ ", " +year.intValue()+ ", " +month.intValue());
    		
            render(monthRecap, menuItem);
    	}
    	
    }
	
	public static void show() {
		show(Security.getPerson());
    }
	
	/**
	 * questa è una funzione solo per admin, quindi va messa con il check administrator
	 */
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void manageAbsenceCode(){
		List<AbsenceType> absenceList = AbsenceType.findAll();
		
		render(absenceList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void absenceCodeList(){
		List<AbsenceType> absenceList = AbsenceType.findAll();
		render(absenceList);
	}
	

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void create(@Required Long personId, @Required Integer year, @Required Integer month, @Required Integer day, String absenceCode) {
    	Logger.debug("Insert absence called for personId=%d, year=%d, month=%d, day=%d", personId, year, month, day);
    	List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
    	
    	List<AbsenceType> allCodes = getAllAbsenceTypes();
		Person person = Person.em().getReference(Person.class, personId);
		LocalDate date = new LocalDate(year, month, day);
		PersonDay personDay = new PersonDay(person, date);
		render(personDay, frequentAbsenceTypeList, allCodes, absenceCode);
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insert(@Required Long personId, @Required Integer yearFrom, @Required Integer monthFrom, @Required Integer dayFrom, @Required String absenceCode){
		/**
		 * TODO: implementare il corpo della insertAbsence di una nuova assenza con la logica 
		 */
		Person person = Person.em().getReference(Person.class, personId);
		LocalDate dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);
		
		AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
		if (absenceType == null) {
			validation.keep();
			params.flash();
			flash.error("Il codice di assenza %s non esiste", params.get("absenceCode"));
			create(personId, yearFrom, monthFrom, dayFrom, absenceCode);
			render("@create");
		}
		
		
		Logger.debug("Richiesto inserimento della assenza codice = %s della persona %s, dataInizio = %s", absenceCode, person, dateFrom);
		
		Absence existingAbsence = Absence.find("person = ? and date = ? and absenceType = ?", person, dateFrom, absenceType).first();
		if(existingAbsence != null){
			validation.keep();
			params.flash();
			flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(dateFrom));
			create(personId, yearFrom, monthFrom, dayFrom, absenceCode);
			render("@create");
		}
		Absence absence = new Absence();
		absence.person = person;
		absence.date = dateFrom;
		absence.absenceType = absenceType;
		
		absence.save();
		
		if (absence.id != null) {
			LocalDateTime ldtBegin = new LocalDateTime(dateFrom.getYear(), dateFrom.getMonthOfYear(), dateFrom.getDayOfMonth(), 0, 0);
			LocalDateTime ldtEnd = new LocalDateTime(dateFrom.getYear(), dateFrom.getMonthOfYear(), dateFrom.getDayOfMonth(), 23, 59);
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, dateFrom).first();
			
			if(absenceType.isHourlyAbsence == true && absenceType.mealTicketCalculation == true){
				/**
				 * è un'assenza oraria e il calcolo del buono mensa deve essere fatto lo stesso: devo vedere se il tempo di lavoro
				 */
				if(person.workingTimeType.getMinimalTimeForLunch(dateFrom.getDayOfWeek(), person.workingTimeType) < pd.timeAtWork){
					/**
					 * tolgo dal tempo di lavoro la quantità di ore che il codice di assenza toglie 
					 */
					pd.timeAtWork = pd.timeAtWork-absenceType.justifiedWorkTime;
					pd.populatePersonDay();
					pd.save();
				}
				else{
					/**
					 * in questo caso il tempo di lavoro è superiore almeno al minimo tempo per ottenere il buono mensa
					 */
				}
			}
			if(absenceType.ignoreStamping == true){
				/**
				 * deve ignorare le timbrature, quindi per quel giorno vale l'assenza e della timbratura che fare? vanno cancellate? e il personday?
				 */
				
				int timbrature = Stamping.delete("Select st from Stamping st " +
						"where st.person = ? and st.date between ? and ? ", person, ldtBegin, ldtEnd);
				
				pd.populatePersonDay();
				pd.save();
			}
			
			flash.success(
				String.format("Assenza di tipo %s inserita per il giorno %s per %s %s", absenceCode, PersonTags.toDateTime(dateFrom), person.surname, person.name));
			render("@save");	
		}
		
		
	}

	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void edit(@Required Long absenceId) {
    	Logger.debug("Edit absence called for absenceId=%d", absenceId);
    	
    	Absence absence = Absence.findById(absenceId);
    	if (absence == null) {
    		notFound();
    	}
    	List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
    	
    	List<AbsenceType> allCodes = getAllAbsenceTypes();
		render(absence, frequentAbsenceTypeList, allCodes);				
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void update() {
		Absence absence = Absence.findById(params.get("absenceId", Long.class));
		if (absence == null) {
			notFound();
		}
		String oldAbsenceCode = absence.absenceType.code;
		String absenceCode = params.get("absenceCode");
		if (absenceCode == null || absenceCode.isEmpty()) {
			absence.delete();
			flash.success("Timbratura di tipo %s per il giorno %s rimossa", oldAbsenceCode, PersonTags.toDateTime(absence.date));			
		} else {
			
			AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
			
			Absence existingAbsence = Absence.find("person = ? and date = ? and absenceType = ? and id <> ?", absence.person, absence.date, absenceType, absence.id).first();
			if(existingAbsence != null){
				validation.keep();
				params.flash();
				flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(absence.date));
				edit(absence.id);
				render("@edit");
			}
			absence.absenceType = absenceType;
			absence.save();
			flash.success(
				String.format("Assenza per il giorno %s per %s %s aggiornata con codice %s", PersonTags.toDateTime(absence.date), absence.person.surname, absence.person.name, absenceCode));
		}
		render("@save");
	}

}
