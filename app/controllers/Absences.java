package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import it.cnr.iit.epas.ActionMenuItem;
import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.MonthRecap;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.PersonTags;
import models.Qualification;
import models.Stamping;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;

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

@With( {Secure.class, NavigationMenu.class} )
public class Absences extends Controller{
	
	/* corrisponde alla voce di menu selezionata */
	private final static ActionMenuItem actionMenuItem = ActionMenuItem.absences;
	
	private static List<AbsenceType> getFrequentAbsenceTypes(){
		return AbsenceType.find("Select abt from AbsenceType abt, Absence abs " +
    			"where abs.absenceType = abt group by abt order by sum(abt.id) desc limit 20").fetch();
		
	}
	
	private static List<AbsenceType> getAllAbsenceTypes(LocalDate date){
		
		return AbsenceType.find("Select abt from AbsenceType abt where abt.validTo > ? order by code", date).fetch();
	}
		 
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void show(Long personId, Integer year, Integer month) {
		
		Person person = Person.findById(personId);
    	
    	Logger.info("Anno: "+year);    	
    	Logger.info("Mese: "+month);
    	PersonMonth personMonth =
    			PersonMonth.find(
    				"Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
    				person, month, year).first();
    	
    	if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
    	if(year==null || month==null){
    		        	
        	LocalDate now = new LocalDate();
        	MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, now.getYear(), now.getMonthOfYear());
            render(monthRecap);
    	}
    	else{
    		Logger.debug("Sono dentro il ramo else della creazione del month recap");
    		
    		MonthRecap monthRecap = MonthRecap.byPersonAndYearAndMonth(person, year.intValue(), month.intValue());
    		Logger.debug("Il month recap è formato da: " +person.id+ ", " +year.intValue()+ ", " +month.intValue());
    		
            render(monthRecap, personMonth);
    	}
    	
    }
//	
//	public static void show() {
//		show(Security.getPerson());
//    }
	
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
	public static void insertAbsenceCode(){
		AbsenceType abt = new AbsenceType();
		AbsenceTypeGroup  abtg = new AbsenceTypeGroup();
		List<Qualification> qualificationList = Qualification.findAll();
		List<AbsenceTypeGroup> abtList = AbsenceTypeGroup.findAll();
		List<JustifiedTimeAtWork> justifiedTimeAtWorkList = new ArrayList<JustifiedTimeAtWork>();
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.AllDay);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.EightHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.FiveHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.FourHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.HalfDay);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.Nothing);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.OneHour);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.ReduceWorkingTimeOfTwoHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.SevenHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.SixHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.ThreeHours);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.TimeToComplete);
		justifiedTimeAtWorkList.add(JustifiedTimeAtWork.TwoHours);
		List<AccumulationType> accumulationTypeList = new ArrayList<AccumulationType>();
		accumulationTypeList.add(AccumulationType.always);
		accumulationTypeList.add(AccumulationType.monthly);
		accumulationTypeList.add(AccumulationType.no);
		accumulationTypeList.add(AccumulationType.yearly);
		
		List<AccumulationBehaviour> accumulationBehaviourList = new ArrayList<AccumulationBehaviour>();
		accumulationBehaviourList.add(AccumulationBehaviour.noMoreAbsencesAccepted);
		accumulationBehaviourList.add(AccumulationBehaviour.nothing);
		accumulationBehaviourList.add(AccumulationBehaviour.replaceCodeAndDecreaseAccumulation);
		
		render(abt, abtg, qualificationList, abtList, justifiedTimeAtWorkList, accumulationTypeList, accumulationBehaviourList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void saveAbsenceCode(){
		AbsenceType abt = new AbsenceType();
		AbsenceTypeGroup abtg = null;
		abt.code = params.get("codice");
		if(params.get("codiceAttestati") != null)
			abt.certificateCode = params.get("codiceAttestati");
		else
			abt.certificateCode = null;
		abt.description = params.get("descrizione");
		abt.ignoreStamping = params.get("ignoraTimbrature", Boolean.class);
		abt.internalUse = params.get("usoInterno", Boolean.class);
		abt.mealTicketCalculation = params.get("calcoloBuono", Boolean.class);
		if(params.get("inizioValidita", Date.class) != null){
			Date validFrom = params.get("inizioValidita", Date.class);
			abt.validFrom = new LocalDate(validFrom);
		}
		else
			abt.validFrom = null;
		if(params.get("fineValidita", Date.class) != null){
			Date validTo = params.get("fineValidita", Date.class);
			abt.validTo = new LocalDate(validTo);
		}
		else
			abt.validTo = null;
		
		abt.justifiedTimeAtWork = params.get("jwt", JustifiedTimeAtWork.class);
		abt.multipleUse = params.get("usoMultiplo", Boolean.class);
		/**
		 * TODO: come fare per le qualifiche????
		 */
		if(params.get("livello1", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 1).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello2", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 2).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello3", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 3).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello4", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 4).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello5", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 5).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello6", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 6).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello7", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 7).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello8", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 8).first();
			abt.qualifications.add(qual);
		}
		if(params.get("livello9", Boolean.class) != null){
			Qualification qual = Qualification.find("Select q from Qualification q where q.qualification = ?", 9).first();
			abt.qualifications.add(qual);
		}
			
		if(!params.get("gruppo").equals("")){
			abtg = new AbsenceTypeGroup();
			abtg.accumulationBehaviour = params.get("accBehaviour", AccumulationBehaviour.class);
			abtg.accumulationType = params.get("accType", AccumulationType.class);
			abtg.label = params.get("gruppo");
			abtg.limitInMinute = params.get("limiteAccumulo", Integer.class);
			abtg.minutesExcess = params.get("minutiEccesso", Boolean.class);
			abtg.replacingAbsenceType = AbsenceType.find("Select abt from AbsenceType abt where abt.code = ?", params.get("codicePerSostituzione")).first();
			abtg.save();
		}
		abt.absenceTypeGroup = abtg;
		abt.save();
		flash.success(
				String.format("Inserita nuova assenza con codice %s", abt.code));
		Application.indexAdmin();
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void discard(){
		manageAbsenceCode();
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void create(@Required Long personId, @Required Integer year, @Required Integer month, @Required Integer day, String absenceCode) {
    	Logger.debug("Insert absence called for personId=%d, year=%d, month=%d, day=%d", personId, year, month, day);
    	List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
    	
    	List<AbsenceType> allCodes = getAllAbsenceTypes(new LocalDate(year,month,day));
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
		
		Absence existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date = ?" +
				" and a.absenceType = ?", person, dateFrom, absenceType).first();
		if(existingAbsence != null){
			validation.keep();
			params.flash();
			flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(dateFrom));
			create(personId, yearFrom, monthFrom, dayFrom, absenceCode);
			render("@create");
		}
		Absence absence = new Absence();
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, dateFrom).first();
		if(pd == null){
			pd = new PersonDay(person, dateFrom);
			pd.save();
		}
		absence.personDay = pd;
	
		absence.absenceType = absenceType;
		
		absence.save();
		
		if (absence.id != null) {
//			if(params.get("buonoMensaSi", Boolean.class)==true){
//				/**
//				 * in questo caso bisogna forzare l'assegnazione del buono pasto nonostante l'assenza
//				 */
//				pd.isTicketAvailable = true;
//				pd.save();
//				flash.success(String.format("Assenza di tipo %s inserita per il giorno %s per %s %s con buono mensa assegnato", absenceCode, PersonTags.toDateTime(dateFrom), person.surname, person.name));
//				render("@save");	
//			}
//			else{
							
				LocalDateTime ldtBegin = new LocalDateTime(dateFrom.getYear(), dateFrom.getMonthOfYear(), dateFrom.getDayOfMonth(), 0, 0);
				LocalDateTime ldtEnd = new LocalDateTime(dateFrom.getYear(), dateFrom.getMonthOfYear(), dateFrom.getDayOfMonth(), 23, 59);
							
				if(absenceType.justifiedTimeAtWork.isFixedJustifiedTime() == true && absenceType.mealTicketCalculation == true){
					/**
					 * è un'assenza oraria e il calcolo del buono mensa deve essere fatto lo stesso: devo vedere se il tempo di lavoro
					 */
					if(person.workingTimeType.getMinimalTimeForLunch(dateFrom.getDayOfWeek(), person.workingTimeType) < pd.timeAtWork){
						/**
						 * tolgo dal tempo di lavoro la quantità di ore che il codice di assenza toglie 
						 */
						pd.timeAtWork = pd.timeAtWork-absenceType.justifiedTimeAtWork.minutesJustified;
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
					
					Stamping.delete("Select st from Stamping st " +
							"where st.person = ? and st.date between ? and ? ", person, ldtBegin, ldtEnd);
					
					pd.populatePersonDay();
					pd.save();
				}
				
				flash.success(
					String.format("Assenza di tipo %s inserita per il giorno %s per %s %s", absenceCode, PersonTags.toDateTime(dateFrom), person.surname, person.name));
				render("@save");	
			}
	//	}
		
		
	}

	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void edit(@Required Long absenceId) {
    	Logger.debug("Edit absence called for absenceId=%d", absenceId);
    	
    	Absence absence = Absence.findById(absenceId);
    	if (absence == null) {
    		notFound();
    	}
    	List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
    	
    	List<AbsenceType> allCodes = getAllAbsenceTypes(absence.personDay.date);
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
			flash.success("Timbratura di tipo %s per il giorno %s rimossa", oldAbsenceCode, PersonTags.toDateTime(absence.personDay.date));			
		} else {
			
			AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
			
			Absence existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where pd.person = ? and pd.date = ? " +
					"and a.absenceType = ? and id <> ?", absence.personDay.person, absence.personDay.date, absenceType, absence.id).first();
			if(existingAbsence != null){
				validation.keep();
				params.flash();
				flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(absence.personDay.date));
				edit(absence.id);
				render("@edit");
			}
			if(params.get("buonoMensaSi", Boolean.class) == true){
				PersonDay pd = absence.personDay;
				pd.isTicketAvailable = true;
				pd.save();
				flash.success(String.format("Assenza di tipo %s inserita per il giorno %s per %s %s con buono mensa assegnato", absenceCode, PersonTags.toDateTime(pd.date), pd.person.surname, pd.person.name));
				render("@save");
			}
			absence.absenceType = absenceType;
			absence.save();
			flash.success(
				String.format("Assenza per il giorno %s per %s %s aggiornata con codice %s", PersonTags.toDateTime(absence.personDay.date), absence.personDay.person.surname, absence.personDay.person.name, absenceCode));
		}
		render("@save");
	}
	
	

}
