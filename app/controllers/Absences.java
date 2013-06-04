package controllers;

import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.Configuration;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.PersonTags;
import models.Qualification;
import models.Stamping;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Blob;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Absences extends Controller{

	private static List<AbsenceType> getFrequentAbsenceTypes(){
		return AbsenceType.find("Select abt from AbsenceType abt, Absence abs " +
				"where abs.absenceType = abt group by abt order by sum(abt.id) desc limit 20").fetch();

	}

	private static List<AbsenceType> getAllAbsenceTypes(LocalDate date){

		return AbsenceType.find("Select abt from AbsenceType abt where abt.validTo > ? order by code", date).fetch();
	}

	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void absences(Long personId, Integer year, Integer month) {
		Person person = null;
		//Logger.debug("Il valore tra i params dell'id della persona è: %d", params.get("personId", Long.class));
		person = Security.getPerson();
		//Logger.debug("La persona presa dal security è: %s %s", person.name, person.surname);
		//		if(personId == null)
		//			person = Person.findById(params.get("personId", Long.class));
		//		else
		//			person = Person.findById(personId);

		Logger.trace("Anno: "+year);    	
		Logger.trace("Mese: "+month);
		PersonMonth personMonth = PersonMonth.byPersonAndYearAndMonth(person, year, month);

		if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
		if(year==null || month==null){
			render(personMonth);
		}
		else{
			//Logger.debug("Sono dentro il ramo else della creazione del month recap");
			//Logger.debug("Il month recap è formato da: " +person.id+ ", " +year.intValue()+ ", " +month.intValue());

			render(personMonth);
		}

	}

	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void absenceInMonth(Long personId, String absenceCode, int year, int month){
		List<LocalDate> dateAbsences = new ArrayList<LocalDate>();
		Person person = Person.findById(personId);
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, new LocalDate(year,month,1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences != null){
				for(Absence abs : pd.absences){
					if(abs.absenceType.code.equals(absenceCode))
						dateAbsences.add(pd.date);
				}
			}
		}
		render(dateAbsences, absenceCode);
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
	public static void create(@Required Long personId, @Required Integer year, @Required Integer month, @Required Integer day) {
		Logger.debug("Insert absence called for personId=%d, year=%d, month=%d, day=%d", personId, year, month, day);
		List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();

		List<AbsenceType> allCodes = getAllAbsenceTypes(new LocalDate(year,month,day));
		Person person = Person.em().getReference(Person.class, personId);
		LocalDate date = new LocalDate(year, month, day);
		PersonDay personDay = new PersonDay(person, date);
		render(personDay, frequentAbsenceTypeList, allCodes);
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insert(@Required Long personId, @Required Integer yearFrom, @Required Integer monthFrom, @Required Integer dayFrom, @Required String absenceCode){

		Person person = Person.em().getReference(Person.class, personId);
		LocalDate dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);

		AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
		Logger.debug("L'absenceType è: %s", absenceType);
		if (absenceType == null) {
			validation.keep();
			params.flash();
			flash.error("Il codice di assenza %s non esiste", params.get("absenceCode"));
			create(personId, yearFrom, monthFrom, dayFrom);
			render("@create");
		}


		Logger.debug("Richiesto inserimento della assenza codice = %s della persona %s, dataInizio = %s", absenceCode, person, dateFrom);

		Absence existingAbsence = Absence.find("Select a from Absence a, PersonDay pd where a.personDay = pd and pd.person = ? and pd.date = ?" +
				" and a.absenceType = ?", person, dateFrom, absenceType).first();
		if(existingAbsence != null){
			validation.keep();
			params.flash();
			flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(dateFrom));
			create(personId, yearFrom, monthFrom, dayFrom);
			render("@create");
		}	
		
		/**
		 * controllo sulla possibilità di poter prendere i congedi per malattia dei figli, guardo se il codice di assenza appartiene alla
		 * lista dei codici di assenza da usare per le malattie dei figli
		 */
		if(absenceType.code.equals("12") || absenceType.code.equals("122") || absenceType.code.equals("123") || absenceType.code.equals("13")
				|| absenceType.code.equals("132") || absenceType.code.equals("133") || absenceType.code.equals("134")){
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
					person, new LocalDate(yearFrom, monthFrom, dayFrom)).first();
			if(pd == null)
				pd = new PersonDay(person, new LocalDate(yearFrom, monthFrom, dayFrom));
			if(PersonUtility.canTakePermissionIllnessChild(person, pd.date, absenceType)){
				/**
				 * può usufruire del permesso
				 */
				Absence absence = new Absence();
				if(params.get("datasize", Blob.class) != null){
					absence.absenceRequest = params.get("datasize", Blob.class);
				}
				else 
					absence.absenceRequest = null;
				absence.personDay = pd;

				absence.absenceType = absenceType;

				absence.save();
				flash.success(
						String.format("Assenza di tipo %s inserita per il giorno %s per %s %s", absenceCode, PersonTags.toDateTime(dateFrom), person.surname, person.name));
				render("@save");	
			}
			else{
				/**
				 * non può usufruire del permesso
				 */
				flash.error(String.format("Il dipendente %s %s non può prendere il codice d'assenza %s poichè ha già usufruito del numero" +
						" massimo di giorni di assenza per quel codice", person.name, person.surname, absenceType.code));
				render("@save");
			}
		}
		
		/**
		 * controllo che le persone che richiedono il riposo compensativo, che hanno una qualifica compresa tra 1 e 3, non abbiano superato
		 * il massimo numero di giorni di riposo compensativo consentiti e presenti in configurazione
		 */
		if(absenceType.code.equals("94") && person.qualification.qualification > 0 && person.qualification.qualification < 4){
			Configuration config = Configuration.getCurrentConfiguration();
			LocalDate actualDate = new LocalDate(yearFrom, monthFrom, dayFrom);
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			if(pd == null){
					pd = new PersonDay(person, actualDate);
					pd.create();
			}
			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
					person, new LocalDate(yearFrom, 1,1), actualDate).fetch();
			int counter = 0;
			for(PersonDay perd : pdList){
				if(perd != null && (perd.absences != null || perd.absences.size() > 0)){
					for(Absence abs : perd.absences){
						if(abs.absenceType.code.equals("94"))
							counter++;
					}		
				}				
			}
			Logger.debug("Fino ad oggi, %s, sono stati utilizzati %d giorni di riposo compensativo da %s %s", actualDate, counter, person.name, person.surname);
			if(counter >= config.maxRecoveryDaysOneThree){
				flash.error("Il dipendente %s %s non può usufruire del codice di assenza %s poichè ha raggiunto il limite previsto per" +
						"quel codice", person.name, person.surname, absenceType.code);
				render("@save");
			}
			else{
				Absence absence = new Absence();
				if(params.get("datasize", Blob.class) != null){
					absence.absenceRequest = params.get("datasize", Blob.class);
				}
				else 
					absence.absenceRequest = null;
				absence.personDay = pd;
				absence.absenceType = absenceType;
				absence.save();
				pd.populatePersonDay();
				pd.save();
				flash.success("Inserito codice %s per %s %s in data %s", absenceType.code, person.name, person.surname, actualDate);
				render("@save");
			}
				
		}
		
		LocalDate date = new LocalDate();
		if(dateFrom.isAfter(date)){
			/**
			 * la data su cui si deve fare il controllo del personDay è successiva alla data odierna
			 */
			PersonDay pd = new PersonDay(person, dateFrom);
			pd.create();
			Absence absence = new Absence();

			if(params.get("datasize", Blob.class) != null){
				absence.absenceRequest = params.get("datasize", Blob.class);
			}
			else 
				absence.absenceRequest = null;
			absence.personDay = pd;
			absence.absenceType = absenceType;
			absence.save();
			absence.personDay.populatePersonDay();
			absence.personDay.save();
			
		}
		else{
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, dateFrom).first();
			Absence absence = new Absence();
			
			absence.personDay = pd;
			absence.absenceType = absenceType;

			if(params.get("datasize", Blob.class) != null){
				absence.absenceRequest = params.get("datasize", Blob.class);
			}
			else 
				absence.absenceRequest = null;

			absence.create();
			absence.personDay.populatePersonDay();
			absence.personDay.save();
		}	
		
		flash.success("Assenza di tipo %s inserita per il giorno %s per %s %s", absenceCode, PersonTags.toDateTime(dateFrom), person.surname, person.name);
		render("@save");

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
			PersonDay pd = absence.personDay;
			absence.delete();
			pd.populatePersonDay();
			pd.save();
			flash.success("Timbratura di tipo %s per il giorno %s rimossa per il dipendente %s %s", 
					oldAbsenceCode, PersonTags.toDateTime(absence.personDay.date), pd.person.name, pd.person.surname);			
		} 
		else {

			AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
			Logger.debug("AbsenceType: %s", absenceType.code);
			PersonDay pd = absence.personDay;
			Logger.debug("PersonDay: %s", pd);
			Absence existingAbsence = Absence.find("Select a from Absence a where a.personDay = ? and a.id <> ?", pd, absence.id).first();

			if(existingAbsence != null){
				validation.keep();
				params.flash();
				flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(absence.personDay.date));
				edit(absence.id);
				render("@edit");
			}
			if((params.get("buonoMensaSi") != null) || (params.get("buonoMensaNo") != null) || (params.get("buonoMensaCalcolato") != null)){
				if(params.get("buonoMensaSi", Boolean.class) == true){

					pd.isTicketAvailable = true;
					pd.save();

				}
				if(params.get("buonoMensaNo", Boolean.class) == true){
					pd.isTicketAvailable = false;
					pd.save();
				}

				if(params.get("buonoMensaCalcolato", Boolean.class) == true){
					pd.populatePersonDay();
					pd.save();
				}
			}


			if(params.get("datasize", Blob.class) != null){
				absence.absenceRequest = params.get("datasize", Blob.class);
			}
			absence.absenceType = absenceType;
			absence.save();

			flash.success(
					String.format("Assenza per il giorno %s per %s %s aggiornata con codice %s", 
							PersonTags.toDateTime(absence.personDay.date), absence.personDay.person.surname, absence.personDay.person.name, absenceCode));
		}
		render("@save");
	}



}
