package controllers;

import it.cnr.iit.epas.CheckMessage;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.JsonPersonEmailBinder;
import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

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
import models.WorkingTimeTypeDay;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.JustifiedTimeAtWork;
import models.exports.PersonEmailFromJson;
import models.exports.PersonPeriodAbsenceCode;
import models.rendering.VacationsRecap;

import org.hibernate.envers.entities.mapper.relation.lazy.proxy.SetProxy;
import org.joda.time.LocalDate;

import play.Logger;
import play.Play;
import play.data.Upload;
import play.data.binding.As;
import play.data.validation.Required;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Absences extends Controller{

	private static List<AbsenceType> getFrequentAbsenceTypes(){

		List<AbsenceType> absenceTypeList = new ArrayList<AbsenceType>();
		try
		{
			//prepared statement
			Connection connection = null;
			if(connection == null)
			{
				Class.forName("org.postgresql.Driver");
				connection = DriverManager.getConnection(
						Play.configuration.getProperty("db.new.url"),
						Play.configuration.getProperty("db.new.user"),
						Play.configuration.getProperty("db.new.password"));
			}

			String query = "select abt.id "
					+ "from absences ab left outer join absence_types abt on ab.absence_type_id = abt.id "
					+ "group by abt.id "
					+ "order by count(*) desc "
					+ "limit 20;";

			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();

			while(rs.next())
			{
				long absenceTypeId = rs.getLong("id");
				AbsenceType abt = AbsenceType.findById(absenceTypeId);
				absenceTypeList.add(abt);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		return absenceTypeList;

	}

	private static List<AbsenceType> getAllAbsenceTypes(LocalDate date){

		return AbsenceType.find("Select abt from AbsenceType abt where abt.validTo > ? order by code", date).fetch();
	}

	//@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void absences(Integer year, Integer month) {
		Person person = Security.getPerson();
		Map<AbsenceType,Integer> absenceTypeInMonth = getAbsenceTypeInMonth(person, year, month);
		String month_capitalized = DateUtility.fromIntToStringMonth(month);
		render(absenceTypeInMonth, person, year, month, month_capitalized);

	}
	
	/**
	 * Una mappa contenente gli AbsenceType fatte dalle persona nel mese e numero di assenze fatte per ogni tipo.
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	private static Map<AbsenceType,Integer> getAbsenceTypeInMonth(Person person, Integer year, Integer month){

		LocalDate beginMonth = new LocalDate(year, month, 1);
		LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
		
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date",
				person, beginMonth, endMonth).fetch();
		
		Map<AbsenceType,Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();

		int i;
		for(PersonDay pd : pdList){
			for (Absence absence : pd.absences) {
				AbsenceType absenceType = absence.absenceType;
				if(absenceType != null){
					boolean stato = absenceCodeMap.containsKey(absenceType);
					if(stato==false){
						i=1;
						absenceCodeMap.put(absenceType,i);            	 
					} else{
						i = absenceCodeMap.get(absenceType);
						absenceCodeMap.remove(absenceType);
						absenceCodeMap.put(absenceType, i+1);
					}
				}            
			}	 
		}       
		return absenceCodeMap;	

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
		List<AbsenceType> absenceList = AbsenceType.find("Select abt from AbsenceType abt order by abt.code").fetch();

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
		MainMenu mainMenu = new MainMenu(year,month,day);
		List<AbsenceType> allCodes = getAllAbsenceTypes(new LocalDate(year,month,day));
		Person person = Person.em().getReference(Person.class, personId);
		LocalDate date = new LocalDate(year, month, day);
		PersonDay personDay = new PersonDay(person, date);
		render(personDay, frequentAbsenceTypeList, allCodes, mainMenu);
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insert(@Required Long personId, @Required Integer yearFrom, 
			@Required Integer monthFrom, @Required Integer dayFrom, @Required String absenceCode, Integer annoFine, Integer meseFine, Integer giornoFine){

		Person person = Person.em().getReference(Person.class, personId);
		LocalDate dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);
		LocalDate dateTo = new LocalDate(annoFine, meseFine, giornoFine);
		Logger.debug("La data fine è: %s", dateTo);
		AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
		Logger.trace("Controllo la presenza dell'absenceType %s richiesto per l'assenza del giorno %s per personId = %s ", absenceType, dateFrom, personId);
		if (absenceType == null) {
			validation.keep();
			params.flash();

			flash.error("Il codice di assenza %s non esiste", params.get("absenceCode"));
			Logger.info("E' stato richiesto l'inserimento del codice di assenza %s per l'assenza del giorno %s per personId = %d. Il codice NON esiste. Se si tratta di un codice di assenza per malattia figlio NUOVO, inserire il nuovo codice nella lista e riprovare ad assegnarlo.", absenceType, dateFrom, personId);
			Stampings.personStamping(personId, yearFrom, monthFrom);
		}

		Logger.debug("Richiesto inserimento della assenza codice = %s della persona %s, dataInizio = %s", absenceCode, person, dateFrom);

		//CONTROLLO INTERVALLO REGOLARE 
		if(dateTo.isBefore(dateFrom))
		{
			flash.error("Data fine precedente alla data inizio. Operazione annullata.");
			Stampings.personStamping(personId, yearFrom, monthFrom);
		}
		
		
		//CONTROLLO CHE IL CODICE INSERITO NON ESISTA GIA IN ALMENO UN GIORNO
		if(absenceTypeAlreadyExist(person, dateFrom, dateTo, absenceType))
		{
			flash.error("Il codice di assenza %s è già presente in almeno uno dei giorni in cui lo si voleva inserire. Controllare", absenceType.code);
			Stampings.personStamping(personId, yearFrom, monthFrom);
		}
		
		//NO DUE CODICI DI ASSENZA GIORNALIERA
		if(allDayAbsenceAlreadyExist(person, dateFrom, dateTo, absenceType))
		{
			flash.error("Non si possono inserire per lo stesso giorno due codici di assenza giornaliera. Operazione annullata.");
			Stampings.personStamping(personId, yearFrom, monthFrom);
		}
	
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
		if(absenceType.code.equals("91"))
		{
			handlerCompensatoryRest(person, dateFrom, dateTo, absenceType);
			return; //inutile
		}
		
		if(absenceType.code.equals("FER"))
		{
			handlerFER(person, dateFrom, dateTo, absenceType);
			return; //inutile
		}
		
		
		/**
		 * controllo sulla possibilità di poter prendere i congedi per malattia dei figli, guardo se il codice di assenza appartiene alla
		 * lista dei codici di assenza da usare per le malattie dei figli
		 */
		//TODO: se il dipendente ha più di 9 figli! non funziona dal 10° in poi
//		if((absenceType.code.startsWith("12") || absenceType.code.startsWith("13")) && absenceType.code.length() == 3){
//			if(!PersonUtility.canTakePermissionIllnessChild(person, dateFrom, absenceType)){
//				/**
//				 * non può usufruire del permesso
//				 */
//				flash.error(String.format("Il dipendente %s %s non può prendere il codice d'assenza %s poichè ha già usufruito del numero" +
//						" massimo di giorni di assenza per quel codice", person.name, person.surname, absenceType.code));
//				//render("@save");
//				Stampings.personStamping(personId, yearFrom, monthFrom);
//				return;
//
//			}
//		}

		/**
		 * in questo pezzo si controlla il poter inserire i codici per le assenze dovute a malattie o ricoveri anche nei giorni festivi.
		 * Da risistemare quando verrà cambiato il database anche in produzione e allora bisognerà controllare che il codice d'assenza inserito 
		 * abbia il campo "considered_week_end" = true
		 */
		if(absenceType.consideredWeekEnd){
			if(dateTo.isBefore(dateFrom) || dateTo.isEqual(dateFrom)){
				PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, new LocalDate(yearFrom, monthFrom, dayFrom)).first();
				if(pd == null){
					pd = new PersonDay(person, dateFrom);
					pd.create();
				}
				Absence absence = new Absence();
				absence.absenceType = absenceType;
				absence.personDay = pd;
				absence.save();
				pd.updatePersonDaysInMonth();
				//Administration.fixPersonSituation(person.id, yearFrom, monthFrom);
				flash.success("Inserito il codice d'assenza %s nel giorno %s", absenceType.code, pd.date);
				//render("@save");
				Stampings.personStamping(personId, pd.date.getYear(), pd.date.getMonthOfYear());
			}
			else{
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date", 
						person, dateFrom, dateTo).fetch();
				if(pdList.size() != 0){
					for(PersonDay pd : pdList){
						Absence absence = new Absence();
						absence.absenceType = absenceType;
						absence.personDay = pd;
						absence.save();
						pd.absences.add(absence);
						pd.save();
						pd.populatePersonDay();
						pd.updatePersonDaysInMonth();
					}
				}
				else{
					while(!dateFrom.isAfter(dateTo)){
						PersonDay pd = new PersonDay(person, dateFrom);
						pd.create();
						Absence absence = new Absence();
						absence.absenceType = absenceType;
						absence.personDay = pd;
						absence.save();
						pd.absences.add(absence);
						pd.merge();

						pd.populatePersonDay();
						pd.save();
						pd.updatePersonDaysInMonth();
						dateFrom = dateFrom.plusDays(1);
					}
					
				}
				
				
				//Administration.fixPersonSituation(person.id, yearFrom, monthFrom);
				flash.success("Inserito codice d'assenza %s per il periodo richiesto", absenceType.code);
				//render("@save");
				Stampings.personStamping(personId, yearFrom, monthFrom);
			}
		}
		
		if(absenceType.absenceTypeGroup != null){
			CheckMessage checkMessage = PersonUtility.checkAbsenceGroup(absenceType, person, dateFrom);
			if(checkMessage.check == false){
				flash.error("Impossibile inserire il codice %s per %s %s. "+checkMessage.message, absenceType.code, person.name, person.surname);
				//render("@save");
				Stampings.personStamping(personId, yearFrom, monthFrom);
			}
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, dateFrom).first();
			if(pd == null){
				pd = new PersonDay(person, dateFrom);
				pd.populatePersonDay();
				pd.save();
			}
			if(checkMessage.check == true && checkMessage.absenceType ==  null){

				Absence absence = new Absence();
				absence.absenceType = absenceType;
				absence.personDay = pd;
				absence.save();
				pd.absences.add(absence);
				
				pd.populatePersonDay();
				pd.save();
				pd.updatePersonDaysInMonth();
				//Administration.fixPersonSituation(person.id, yearFrom, monthFrom);
				flash.success("Aggiunto codice di assenza %s "+checkMessage.message, absenceType.code);
				//render("@save");
				Stampings.personStamping(personId, yearFrom, monthFrom);

			}
			if(checkMessage.check == true && checkMessage.absenceType != null){
				Absence absence = new Absence();
				absence.absenceType = absenceType;
				absence.personDay = pd;
				absence.save();
				pd.absences.add(absence);
				Absence compAbsence = new Absence();
				compAbsence.absenceType = checkMessage.absenceType;
				compAbsence.personDay = pd;
				compAbsence.save();
				pd.absences.add(compAbsence);
				pd.save();
				pd.populatePersonDay();
				pd.updatePersonDaysInMonth();
				//Administration.fixPersonSituation(person.id, yearFrom, monthFrom);
				flash.success("Aggiunto codice di assenza %s "+checkMessage.message, absenceType.code);
				//render("@save");
				Stampings.personStamping(personId, yearFrom, monthFrom);
			}
			
		}
		
		/**
		 * è il caso delle ferie anno passato di cui poter godere oltre il 31/8 (previa autorizzazione).
		 */
		if(absenceType.code.equals("37")){
			 int days = VacationsRecap.remainingPastVacations(yearFrom, person, absenceType);
			 if(days == 0){
				 flash.error("Non si possono inserire ulteriori giorni di assenza perchè è stato raggiunto il limite previsto dal piano" +
				 		"ferie per %s %s, oppure l'anno passato %s %s non aveva un contratto attivo", person.name, person.surname, person.name, person.surname);
				 Stampings.personStamping(personId, yearFrom, monthFrom);
			 }
			 PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
					 person, new LocalDate(yearFrom, monthFrom, dayFrom)).first();
			 
			 Absence absence = new Absence();
			 absence.absenceType = absenceType;
			 if(pd == null){
				 pd = new PersonDay(person, new LocalDate(yearFrom, monthFrom, dayFrom));
				 pd.save();
			 }
			 absence.personDay = pd;
			 absence.save();
			 pd.absences.add(absence);
			 pd.save();
			 pd.populatePersonDay();
			 pd.updatePersonDaysInMonth();
			 flash.success("Inserito codice di assenza %s per %s %s in data %s", absenceType.code, person.name, person.surname,new LocalDate(yearFrom, monthFrom, dayFrom));
			 Stampings.personStamping(personId, yearFrom, monthFrom);
		}		
		
		Absence absence = new Absence();
		Logger.debug("%s %s può usufruire del codice %s", person.name, person.surname, absenceType.code);
		if(dateTo.isBefore(dateFrom) || dateTo.isEqual(dateFrom)){
			Logger.debug("Si intende inserire un'assenza per un giorno solo");
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, dateFrom).first();
			if(pd == null) {
				pd = new PersonDay(person, dateFrom);
				pd.create();
			}
			
			Logger.debug("Creato il personDay %s", pd);
						
			Upload file = params.get("absenceFile" , Upload.class);
			if (file != null && (file.getContentType().equals("application/pdf"))) {

				absence.absenceFile = params.get("absenceFile", Blob.class);

				Logger.debug("file ricevuto: %s %s %s", file.getFileName(), file.getSize(),file.getContentType());
			}

			else if (file != null)	flash.error("Il tipo di file inserito non è supportato");	
				
			absence.absenceType = absenceType;

			pd.addAbsence(absence);
			pd.save();
			Logger.debug("Creata e salvata l'assenza %s con codice %s", absence, absence.absenceType.code);
			pd.populatePersonDay();
			
			if(pd.date.isBefore(new LocalDate(pd.date).dayOfMonth().withMaximumValue())){
				List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ? and pd.date <= ? order by pd.date", 
						pd.person, pd.date, new LocalDate(pd.date).dayOfMonth().withMaximumValue()).fetch();
				for(PersonDay personday : pdList){
					personday.populatePersonDay();
					personday.save();
				}
			}
			pd.updatePersonDaysInMonth();
			
			flash.success(
					String.format("Assenza di tipo %s inserita per il giorno %s per %s %s", absenceCode, PersonTags.toDateTime(dateFrom), person.surname, person.name));
			
			Stampings.personStamping(personId, pd.date.getYear(), pd.date.getMonthOfYear());

		}
		else{

			List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
					person, dateFrom, dateTo).fetch();
			Logger.debug("La lista di personday è composta da %d elementi", pdList.size());
			if(pdList.size() != 0){

				for(PersonDay pdInside : pdList){
					absence = new Absence();
					absence.absenceType = absenceType;
					absence.personDay = pdInside;
					absence.save();
					pdInside.addAbsence(absence);
					pdInside.populatePersonDay();
					pdInside.save();
					
				}
				pdList.get(0).updatePersonDaysInMonth();
			}
			else{

				while(!dateFrom.isAfter(dateTo)){
					Logger.debug("Datefrom: %s DateTo: %s", dateFrom, dateTo);
					PersonDay pdInside = PersonUtility.createPersonDayFromDate(person, dateFrom);
					if(pdInside != null){
						pdInside.create();
						Logger.debug("Creato personDay per il giorno %s ", dateFrom);

						absence = new Absence();
						absence.absenceType = absenceType;
						absence.personDay = pdInside;
						absence.save();
						pdInside.absences.add(absence);
						pdInside.populatePersonDay();
						pdInside.save();
						pdInside.updatePersonDaysInMonth();
					}
					dateFrom = dateFrom.plusDays(1);
				}
			}
			
			flash.success("Inserita assenza %s dal %s al %s", absenceType.code, dateFrom, dateTo);
			//Administration.fixPersonSituation(person.id, yearFrom, monthFrom);
			Stampings.personStamping(personId, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}

	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void editCode(@Required Long absenceCodeId) throws InstantiationException, IllegalAccessException{
		AbsenceType abt = AbsenceType.findById(absenceCodeId);
		List<JustifiedTimeAtWork> justList = new ArrayList<JustifiedTimeAtWork>();
		justList.add(0,JustifiedTimeAtWork.AllDay);
		justList.add(1,JustifiedTimeAtWork.HalfDay);
		justList.add(2,JustifiedTimeAtWork.OneHour);
		justList.add(3,JustifiedTimeAtWork.TwoHours);
		justList.add(4,JustifiedTimeAtWork.ThreeHours);
		justList.add(5,JustifiedTimeAtWork.FourHours);
		justList.add(6,JustifiedTimeAtWork.FiveHours);
		justList.add(7,JustifiedTimeAtWork.SixHours);
		justList.add(8,JustifiedTimeAtWork.SevenHours);
		justList.add(9,JustifiedTimeAtWork.EightHours);
		justList.add(10,JustifiedTimeAtWork.Nothing);
		justList.add(11,JustifiedTimeAtWork.TimeToComplete);
		justList.add(12,JustifiedTimeAtWork.ReduceWorkingTimeOfTwoHours);

		List<Qualification> qualList = Qualification.findAll();
		List<AccumulationType> accType = new ArrayList<AccumulationType>();
		accType.add(0, AccumulationType.always);
		accType.add(1, AccumulationType.monthly);
		accType.add(2, AccumulationType.no);
		accType.add(3, AccumulationType.yearly);
		List<AccumulationBehaviour> behaviourType = new ArrayList<AccumulationBehaviour>();
		behaviourType.add(0, AccumulationBehaviour.nothing);
		behaviourType.add(1, AccumulationBehaviour.noMoreAbsencesAccepted);
		behaviourType.add(2, AccumulationBehaviour.replaceCodeAndDecreaseAccumulation);

		render(abt, justList, qualList, accType, behaviourType);
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void updateCode(){
		AbsenceType absence = AbsenceType.findById(params.get("absenceTypeId", Long.class));
		if(absence == null)
			notFound();
		Logger.debug("Il codice d'assenza da modificare è %s", absence.code);
		absence.description = params.get("descrizione");
		Logger.debug("Il valore di uso interno è: %s", params.get("usoInterno", Boolean.class));
		Logger.debug("Il valore di uso multiplo è: %s", params.get("usoMultiplo", Boolean.class));
		Logger.debug("Il valore di tempo giustificato è: %s", params.get("abt.justifiedTimeAtWork"));
		absence.internalUse = params.get("usoInterno", Boolean.class);		
		absence.multipleUse = params.get("usoMultiplo", Boolean.class);
		absence.validFrom = new LocalDate(params.get("inizio"));
		absence.validTo = new LocalDate(params.get("fine"));
		String justifiedTimeAtWork = params.get("abt.justifiedTimeAtWork");			
		absence.justifiedTimeAtWork = JustifiedTimeAtWork.getByDescription(justifiedTimeAtWork);

		for(int i = 1; i <= 10; i++){
			if(params.get("qualification"+i) != null){
				Qualification q = Qualification.findById(new Long(i));
				if(!absence.qualifications.contains(q))
					absence.qualifications.add(q);
			}
			else{
				Qualification q = Qualification.findById(new Long(i));
				if(absence.qualifications.contains(q))
					absence.qualifications.remove(q);
			}
		}


		absence.mealTicketCalculation = params.get("calcolaBuonoPasto", Boolean.class);
		absence.ignoreStamping = params.get("ignoraTimbrature", Boolean.class);
		if(!params.get("gruppo").equals("")){
			absence.absenceTypeGroup.label = params.get("gruppo");
			absence.absenceTypeGroup.accumulationBehaviour = AccumulationBehaviour.getByDescription((params.get("abt.absenceTypeGroup.accumulationBehaviour")));
			absence.absenceTypeGroup.accumulationType = AccumulationType.getByDescription((params.get("abt.absenceTypeGroup.accumulationType")));
			absence.absenceTypeGroup.limitInMinute = params.get("limiteAccumulo", Integer.class);
			absence.absenceTypeGroup.minutesExcess = params.get("minutiEccesso", Boolean.class);
			String codeToReplace = params.get("codiceSostituzione");
			AbsenceTypeGroup abtg = AbsenceTypeGroup.find("Select abtg from AbsenceTypeGroup abtg where abtg.code = ?", codeToReplace).first();
			absence.absenceTypeGroup = abtg;
		}
		absence.save();

		flash.success("Modificato codice di assenza %s", absence.code);
		Absences.manageAbsenceCode();

	}


	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void edit(@Required Long absenceId) {
		Logger.debug("Edit absence called for absenceId=%d", absenceId);

		Absence absence = Absence.findById(absenceId);
		if (absence == null) {
			notFound();
		}
		LocalDate date = absence.personDay.date;
		List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
		MainMenu mainMenu = new MainMenu(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth());
		List<AbsenceType> allCodes = getAllAbsenceTypes(absence.personDay.date);
		render(absence, frequentAbsenceTypeList, allCodes, mainMenu);				
	}

	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void update() {
		Absence absence = Absence.findById(params.get("absenceId", Long.class));
		if (absence == null) {
			notFound();
		}
		Person person = absence.personDay.person;

		int year = params.get("annoFine", Integer.class);
		int month = params.get("meseFine", Integer.class);
		int day = params.get("giornoFine", Integer.class);
		String oldAbsenceCode = absence.absenceType.code;
		String absenceCode = params.get("absenceCode");

		//Update assenza un giorno solo
		if(absence.personDay.date.isEqual(new LocalDate(year, month, day))){

			//cancellazione
			if (absenceCode == null || absenceCode.isEmpty()) {
				PersonDay pd = absence.personDay;
				absence.delete();
				pd.absences.remove(absence);
				pd.populatePersonDay();
				pd.save();
				if(pd.date.isBefore(new LocalDate(pd.date).dayOfMonth().withMaximumValue()))
				{
					List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date > ? and pd.date <= ? order by pd.date", 
							pd.person, pd.date, new LocalDate(pd.date).dayOfMonth().withMaximumValue()).fetch();
					for(PersonDay personday : pdList){
						personday.populatePersonDay();
						personday.save();
					}
				}
				flash.success("Assenza di tipo %s per il giorno %s rimossa per il dipendente %s %s", 
						oldAbsenceCode, PersonTags.toDateTime(absence.personDay.date), pd.person.name, pd.person.surname);	
				Stampings.personStamping(person.id, year, month);
				//return;
			} 
			//aggiornamento
			else 
			{
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
					Stampings.personStamping(person.id, year, month);
				}
				String mealTicket =  params.get("buonoMensa");
				//Logger.debug("Il valore di buono mensa da param: %s", mealTicket);
				checkMealTicket(pd, mealTicket, absenceType);
				
				Upload file = params.get("absenceFile" , Upload.class);
				if (file != null && (file.getContentType().equals("application/pdf"))) {

					absence.absenceFile = params.get("absenceFile", Blob.class);
					Logger.debug("file ricevuto: %s %s %s", file.getFileName(), file.getSize(),file.getContentType());

				} else if (file != null) {
					flash.error("Il tipo di file inserito non è supportato");
				}
					
				absence.absenceType = absenceType;
				absence.save();

				flash.success(
						String.format("Assenza per il giorno %s per %s %s aggiornata con codice %s", 
								PersonTags.toDateTime(absence.personDay.date), absence.personDay.person.surname, absence.personDay.person.name, absenceCode));
				Stampings.personStamping(person.id, year, month);
				//return;
			}

		}

		/*L'assenza è per più giorni*/
		LocalDate dataInizioAssenze = absence.personDay.date;
		LocalDate dataFineAssenze = new LocalDate(year, month, day);
		Logger.debug("Data fine assenze: %s", dataFineAssenze);
		Logger.debug("Data inizio assenze %s", dataInizioAssenze);
		PersonDay pd = null;
		if(absenceCode.equals("") || absenceCode == null){
			while(!dataInizioAssenze.isAfter(dataFineAssenze)){
				Logger.debug("Intendo cancellare assenza per il giorno %s ", dataInizioAssenze);
				pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", absence.personDay.person, dataInizioAssenze).first();
				if(pd == null){
					dataInizioAssenze = dataInizioAssenze.plusDays(1);
				}
				else{
					Absence abs = Absence.find("Select a from Absence a, PersonDay pd where a.personDay = pd and pd.person = ? and pd.date = ?", 
							pd.person, dataInizioAssenze).first();
					if(abs != null){
						abs.delete();
						pd.absences.remove(abs);
						pd.populatePersonDay();
						pd.save();
					}									

					dataInizioAssenze = dataInizioAssenze.plusDays(1);
				}

			}


		}
		else{
			AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
			while(!dataInizioAssenze.isEqual(dataFineAssenze)){
				pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", absence.personDay.person, dataInizioAssenze).first();
				Absence existingAbsence = Absence.find("Select a from Absence a where a.personDay = ? and a.id <> ?", pd, absence.id).first();
				if(existingAbsence != null){
					validation.keep();
					params.flash();
					flash.error("Il codice di assenza %s è già presente per la data %s", params.get("absenceCode"), PersonTags.toDateTime(absence.personDay.date));
					edit(absence.id);
					//render("@edit");
					Stampings.personStamping(person.id, year, month);
				}
				else{
					Absence abs = Absence.find("Select a from Absence a, PersonDay pd where a.personDay = pd and pd.person = ? and pd.date = ?", 
							absence.personDay.person, dataInizioAssenze).first();
					abs.delete();
					pd.absences.remove(abs);
					pd.populatePersonDay();
					pd.save();
					Absence absenceNew = new Absence();
					absenceNew.absenceType = absenceType;
					absenceNew.personDay = pd;
					absenceNew.save();
					pd.absences.add(absenceNew);
					pd.populatePersonDay();
					pd.save();

				}
				String mealTicket =  params.get("buonoMensa");
				//Logger.debug("Il valore di buono mensa da param: %s", mealTicket);
				//checkMealTicket(pd, mealTicket);
				dataInizioAssenze = dataInizioAssenze.plusDays(1);
			}



		}
		flash.success("Rimossi i codici di assenza per il periodo %s %s", absence.personDay.date, dataFineAssenze);
		Stampings.personStamping(person.id, year, month);


	}
	
	/**
	 * Gestore della logica ticket forzato dall'amministratore, risponde solo in caso di codice 92
	 * @param pd
	 * @param mealTicket
	 */
	private static void checkMealTicket(PersonDay pd, String mealTicket, AbsenceType abt){
		
		if(abt==null || !abt.code.equals("92"))
		{
			pd.isTicketForcedByAdmin = false;	//una assenza diversa da 92 ha per forza campo calcolato
			pd.populatePersonDay();
			return;
		}
			
		
		if(mealTicket!= null && mealTicket.equals("si")){
			pd.isTicketForcedByAdmin = true;
			pd.isTicketAvailable = true;
			pd.populatePersonDay();
			
			

		}
		if(mealTicket!= null && mealTicket.equals("no")){
			pd.isTicketForcedByAdmin = true;
			pd.isTicketAvailable = false;
			pd.populatePersonDay();
			
			
		}

		if(mealTicket!= null && mealTicket.equals("calcolato")){
			pd.isTicketForcedByAdmin = false;
			pd.populatePersonDay();
			
		}
	}
	
	/**
	 * Gestisce l'inserimento dei codici 91 (1 o più consecutivi)
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 */
	private static void handlerCompensatoryRest(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType)
	{
		Logger.debug("Devo inserire un codice %s per %s %s", absenceType.code, person.name, person.surname);
		Configuration config = Configuration.getCurrentConfiguration();
		LocalDate actualDate = dateFrom;
		int taken = 0;
		while(!actualDate.isAfter(dateTo))
		{
			//Costruisco se non esiste il person day
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			if(pd == null){
				pd = new PersonDay(person, actualDate);
				pd.create();
			}
			
			//se e' festa vado oltre
			if(pd.isHoliday())
			{
				actualDate = actualDate.plusDays(1);
				continue;
			}
			
			//verifica se ha esaurito il bonus per l'anno
			if(person.qualification.qualification > 0 && person.qualification.qualification < 4){
				Query query = JPA.em().createQuery("SELECT abs FROM Absence abs WHERE abs.personDay.person = :person "+ 
						"AND abs.personDay.date between :dateStart AND :dateTo AND abs.absenceType.code = :code");
				query.setParameter("person", pd.person).
				setParameter("dateStart", new LocalDate(actualDate.getYear(), 1,1)).
				setParameter("dateTo",actualDate).
				setParameter("code", "91");
				List<Object> resultList = query.getResultList();
				Logger.debug("Il numero di assenze con codice %s fino a oggi è %d", absenceType.code, resultList.size());
				if(resultList.size() >= config.maxRecoveryDaysOneThree){
					actualDate = actualDate.plusDays(1);
					continue;
				}
			}

			//Controllo del residuo

			if(!PersonUtility.canTakeCompensatoryRest(person, pd.date))
			{
				actualDate = actualDate.plusDays(1);
				continue;
			}
			Absence absence = new Absence();
			absence.absenceType = absenceType;
			absence.personDay = pd;
			absence.save();
			pd.absences.add(absence);
			//pd.populatePersonDay();
			//pd.updatePersonDay();
			pd.save();
			taken++;


			actualDate = actualDate.plusDays(1);
		}

		
		
		
		//Administration.fixPersonSituation(person.id, yearFrom, monthFrom);
		//flash.success("Operazione agginta riposi compensativi ", absenceType.code);
		//render("@save");
		if(taken==0)
			flash.error("Non e' stato possibile inserire alcun riposo compensativo");
		else
			flash.success("Inseriti %s riposi compensativi per la persona", taken);

		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
	}

	
	/**
	 * Gestisce l'inserimento dei codici FER, 94-31-32 nell'ordine. Fino ad esaurimento.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 */
	private static void handlerFER(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType)
	{
		//controllo reperibilita'
		LocalDate actualDate = dateFrom;
		while(!actualDate.isAfter(dateTo))
		{
			if(!PersonUtility.canPersonTakeAbsenceInShiftOrReperibility(person, actualDate))	
			{
				flash.error("Operazione annullata in quanto %s %s al giorno %s si trova in turno/reperibilità. \n Contattarlo e chiedere spiegazioni", person.name, person.surname, actualDate);
				Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
			}
			
			actualDate = actualDate.plusDays(1);
		}
		
		//inserimento
		int taken = 0;
		actualDate = dateFrom;
		while(!actualDate.isAfter(dateTo))
		{
						
			AbsenceType abt = PersonUtility.whichVacationCode(person, actualDate);
			
			//FER esauriti
			if(abt==null)
			{
				if(taken==0)
				{
					flash.error("Il dipendente %s %s ha esaurito tutti i codici FER", person.name, person.surname);
					Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
				}
				flash.error("Aggiunti %s codici assenza FER da %s a %s. In data %s il dipendente ha esaurito tutti i codici FER a disposizione.", taken, dateFrom, actualDate.minusDays(1), actualDate);
				PersonUtility.updatePersonDaysIntoInterval(person,dateFrom,dateTo);
				Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
			}
				
			//Insert nuovo FER
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			if(pd == null){
				pd = new PersonDay(person, actualDate);
				pd.create();
			}
			
			//se e' festa vado oltre
			if(pd.isHoliday())
			{
				actualDate = actualDate.plusDays(1);
				continue;
			}
			
			Absence absence = new Absence();
			absence.absenceType = abt;
			absence.personDay = pd;
			absence.save();
			pd.absences.add(absence);
			pd.save();
			
			taken++;
			actualDate = actualDate.plusDays(1);
		}
		if(taken==1)
			flash.success("Aggiunto codice assenza FER per il giorno %s", actualDate);
		if(taken>1)
			flash.success("Aggiunti %s codici assenza FER da %s a %s.", taken, dateFrom, dateTo);
		
		PersonUtility.updatePersonDaysIntoInterval(person,dateFrom,dateTo);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());

	}
	
	/**
	 * Controlla che nell'intervallo passato in args non esistano già assenze per quel tipo
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @return
	 */
	private static boolean absenceTypeAlreadyExist(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType)
	{
		LocalDate actualDate = dateFrom;
		
		while(!actualDate.isAfter(dateTo))
		{
			List<Absence> absenceList = Absence.find("Select a from Absence a, PersonDay pd where a.personDay = pd and pd.person = ? and pd.date = ?", person, actualDate).fetch();
			for(Absence abs : absenceList)
			{
				if(abs.absenceType.equals(absenceType)){
					return true;
				}

			}
			actualDate = actualDate.plusDays(1);
		}
		return false;
	}
	
	/**
	 * Controlla che nell'intervallo passato in args non esista gia' una assenza giornaliera
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @return
	 */
	private static boolean allDayAbsenceAlreadyExist(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType)
	{
		LocalDate actualDate = dateFrom;
		
		while(!actualDate.isAfter(dateTo))
		{
			List<Absence> absenceList = Absence.find("Select a from Absence a, PersonDay pd where a.personDay = pd and pd.person = ? and pd.date = ?", person, actualDate).fetch();
			for(Absence abs : absenceList)
			{
				if(abs != null && abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay && absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay)
				{
					return true;
				}
			}
			actualDate = actualDate.plusDays(1);
		}
		return false;

	}
	

	public static void setPersonEmailForAbsence(){
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insertPersonChildren(){
		List<Person> personList = Person.getActivePersonsInMonth(new LocalDate().getMonthOfYear(), new LocalDate().getYear(), false);
		render(personList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void manageAttachment(){
		render();
	}
}




