package controllers;

import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.CheckAbsenceInsert;
import it.cnr.iit.epas.CheckMessage;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.MainMenu;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.persistence.Query;

import models.Absence;
import models.AbsenceType;
import models.AbsenceTypeGroup;
import models.ConfYear;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.Qualification;
import models.enumerate.AccumulationBehaviour;
import models.enumerate.AccumulationType;
import models.enumerate.ConfigurationFields;
import models.enumerate.JustifiedTimeAtWork;
import models.rendering.VacationsRecap;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Blob;
import play.db.jpa.JPA;
import play.libs.Mail;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;

import controllers.Resecure.NoCheck;
import dao.AbsenceTypeDao;

@With( {Resecure.class, RequestInit.class} )
public class Absences extends Controller{

	@Inject
	static SecurityRules rules;
	/**
	 * @deprecated use AbsenceTypeDao.getFrequentTypes()
	 * 
	 * @return la lista dei tipi di competenza più utilizzati
	 */
	@Deprecated
	private static List<AbsenceType> getFrequentAbsenceTypes(){
		return AbsenceType.find("Select abt from AbsenceType abt, Absence abs " +
				"where abs.absenceType = abt group by abt order by sum(abt.id) desc limit 20").fetch();

	}

	private static List<AbsenceType> getAllAbsenceTypes(LocalDate date){

		return AbsenceType.find("Select abt from AbsenceType abt where abt.validTo > ? order by code", date).fetch();
	}

	
	public static void absences(Integer year, Integer month) {
		Person person = Security.getUser().get().person;
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
	//@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	@NoCheck
	public static void manageAbsenceCode(String name, Integer page){
		if(page==null)
			page = 0;
		SimpleResults<AbsenceType> simpleResults = AbsenceTypeDao.getAbsences(Optional.fromNullable(name));
		List<AbsenceType> absenceList = simpleResults.paginated(page).getResults();
		//List<AbsenceType> absenceList = AbsenceType.find("Select abt from AbsenceType abt order by abt.code").fetch();
		
		render(absenceList, name, simpleResults);
	}

	//@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insertAbsenceCode(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
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

	//@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void saveAbsenceCode(){
		rules.checkIfPermitted(Security.getUser().get().person.office);
		
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
		abt.consideredWeekEnd = params.get("weekEnd", Boolean.class);
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
		manageAbsenceCode(null, null);
	}

	public static void create(@Required Long personId, @Required Integer year, @Required Integer month, @Required Integer day) {
		
		Person person = Person.em().getReference(Person.class, personId);
		
		rules.checkIfPermitted(person.office);
		
		Logger.debug("Insert absence called for personId=%d, year=%d, month=%d, day=%d", personId, year, month, day);
		List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
		MainMenu mainMenu = new MainMenu(year,month,day);
		List<AbsenceType> allCodes = getAllAbsenceTypes(new LocalDate(year,month,day));
		
		
		LocalDate date = new LocalDate(year, month, day);
		PersonDay personDay = new PersonDay(person, date);
		render(personDay, frequentAbsenceTypeList, allCodes, mainMenu);
	}

	private static void insertAbsence(Long personId, Integer yearFrom, 
			Integer monthFrom, Integer dayFrom, String absenceCode, String finoa, Blob file, String mealTicket) throws EmailException
	{
		Person person = Person.em().getReference(Person.class, personId);
		LocalDate dateFrom = new LocalDate(yearFrom, monthFrom, dayFrom);
		
		AbsenceType absenceType = AbsenceType.find("byCode", absenceCode).first();
		
		if (absenceType == null) {
			validation.keep();
			params.flash();
			flash.error("Il codice di assenza %s non esiste", params.get("absenceCode"));
			Logger.info("E' stato richiesto l'inserimento del codice di assenza %s per l'assenza del giorno %s per personId = %d. Il codice NON esiste. Se si tratta di un codice di assenza per malattia figlio NUOVO, inserire il nuovo codice nella lista e riprovare ad assegnarlo.", absenceType, dateFrom, personId);
			Stampings.personStamping(personId, yearFrom, monthFrom);
		}
		
		//Controlli di correttezza richiesta
		LocalDate dateTo = null;
		if(finoa==null || finoa.equals("")) {
			dateTo = dateFrom;
		}
		else {
			try {
				dateTo = new LocalDate(finoa);	
			}
			catch (Exception e) {
				flash.error("Errore inserimento campo data fine codice assenza. Operazione annullata.");
				Stampings.personStamping(personId, yearFrom, monthFrom);
			}
		}
	
		if(dateTo.isBefore(dateFrom))
		{
			flash.error("Data fine precedente alla data inizio. Operazione annullata.");
			Stampings.personStamping(personId, yearFrom, monthFrom);
		}
		if(absenceTypeAlreadyExist(person, dateFrom, dateTo, absenceType))
		{
			flash.error("Il codice di assenza %s è già presente in almeno uno dei giorni in cui lo si voleva inserire. Controllare", absenceType.code);
			Stampings.personStamping(personId, yearFrom, monthFrom);
		}
		if(allDayAbsenceAlreadyExist(person, dateFrom, dateTo, absenceType))
		{
			flash.error("Non si possono inserire per lo stesso giorno due codici di assenza giornaliera. Operazione annullata.");
			Stampings.personStamping(personId, yearFrom, monthFrom);
		}
		/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	

		
		if(absenceType.code.equals("91"))
		{
			handlerCompensatoryRest(person, dateFrom, dateTo, absenceType, file);
			return;
		}
		
		if(absenceType.code.equals("FER"))
		{
			handlerFER(person, dateFrom, dateTo, absenceType, file);
			return;
		}
		
		if(absenceType.code.equals("37")){
			handler37(person, dateFrom, dateTo, absenceType, file);
			return;
		}	

		if((absenceType.code.startsWith("12") || absenceType.code.startsWith("13")) && absenceType.code.length() == 3){
			handlerChildIllness(person, dateFrom, dateTo, absenceType, file);
			return;
		}

		if(absenceType.absenceTypeGroup != null){
			handlerAbsenceTypeGroup(person, dateFrom, dateTo, absenceType, file);
			return;
		}
		
		if(absenceType.consideredWeekEnd){
			handlerIllnessOrDischarge(person, dateFrom, dateTo, absenceType, file);
			return;
		}
			
		handlerGenericAbsenceType(person, dateFrom, dateTo, absenceType, file, mealTicket);


	}

	

	public static void insert(@Required Long personId, @Required Integer yearFrom, 
			@Required Integer monthFrom, @Required Integer dayFrom, @Required String absenceCode, String finoa, Blob file, String mealTicket) throws EmailException{

		Person person = Person.findById(personId);
		
		rules.checkIfPermitted(person.office);
		
		//Ho dovuto implementare un involucro perchè quando richiamavo questo medoto da update il campo blob era null.
		insertAbsence(personId, yearFrom, monthFrom, dayFrom, absenceCode, finoa, file, mealTicket);
		
	}

	//@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void editCode(@Required Long absenceCodeId) throws InstantiationException, IllegalAccessException{
		rules.checkIfPermitted(Security.getUser().get().person.office);
		
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
		absence.consideredWeekEnd = params.get("weekEnd", Boolean.class);
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
		Absences.manageAbsenceCode(null, null);

	}


	public static void edit(@Required Long absenceId) {
		Logger.debug("Edit absence called for absenceId=%d", absenceId);

		Absence absence = Absence.findById(absenceId);
		if (absence == null) {
			notFound();
		}
		
		rules.checkIfPermitted(absence.personDay.person.office);
		
		LocalDate date = absence.personDay.date;
		List<AbsenceType> frequentAbsenceTypeList = getFrequentAbsenceTypes();
		MainMenu mainMenu = new MainMenu(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth());
		List<AbsenceType> allCodes = getAllAbsenceTypes(absence.personDay.date);
		render(absence, frequentAbsenceTypeList, allCodes, mainMenu);				
	}
    
	public static void update(Blob file) throws EmailException {
		Absence absence = Absence.findById(params.get("absenceId", Long.class));
		if (absence == null) {
			notFound();
		}
		
		rules.checkIfPermitted(absence.personDay.person.office);
		
		if(file != null && file.exists()){
			Logger.debug("ricevuto file di tipo: %s", file.type());
		}
		

		Person person = absence.personDay.person;
		LocalDate dateFrom =  absence.personDay.date;		
		LocalDate dateTo = null;
		
		String finoa = params.get("finoa");
		if(finoa==null || finoa.equals(""))
			dateTo = dateFrom;
		else {
			try {
				dateTo = new LocalDate(finoa);
			} catch(Exception e) {
				flash.error("Errore nell'inserimento del campo Fino A, inserire una data valida. Operazione annullata");
				Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
			}
		}
		
		
		int yearFrom = dateFrom.getYear();
		int monthFrom = dateFrom.getMonthOfYear();
		int dayFrom = dateFrom.getDayOfMonth();
		
		AbsenceType newAbsenceType = AbsenceType.find("byCode", params.get("absenceCode")).first();
		String mealTicket =  params.get("buonoMensa");

		int deleted = removeAbsencesInPeriod(person, dateFrom, dateTo, absence.absenceType);
		if(newAbsenceType==null)
		{
			flash.success("Rimossi %s codici assenza di tipo %s", deleted, absence.absenceType.code);
			PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}
		insertAbsence(person.id, yearFrom, monthFrom, dayFrom, newAbsenceType.code, dateTo.toString(), file, mealTicket);

	}
	
	/**
	 * Gestore della logica ticket forzato dall'amministratore, risponde solo in caso di codice 92
	 * @param date
	 * @param person
	 * @param mealTicket
	 * @param abt
	 */
	private static void checkMealTicket(LocalDate date, Person person, String mealTicket, AbsenceType abt){
		
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		if(pd == null)
			pd = new PersonDay(person, date);
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
	 * @throws EmailException 
	 */
	private static void handlerCompensatoryRest(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType,Blob file) throws EmailException
	{
		LocalDate actualDate = dateFrom;
		int taken = 0;

		while(!actualDate.isAfter(dateTo))
		{
			Integer maxRecoveryDaysOneThree = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays13.description, actualDate.getYear(), person.office));

	
			//verifica se ha esaurito il bonus per l'anno
			if(person.qualification.qualification > 0 && person.qualification.qualification < 4){
				Query query = JPA.em().createQuery("SELECT abs FROM Absence abs WHERE abs.personDay.person = :person "+ 
						"AND abs.personDay.date between :dateStart AND :dateTo AND abs.absenceType.code = :code");
				query.setParameter("person", person).
				setParameter("dateStart", new LocalDate(actualDate.getYear(), 1,1)).
				setParameter("dateTo",actualDate).
				setParameter("code", "91");
				List<Object> resultList = query.getResultList();
				Logger.debug("Il numero di assenze con codice %s fino a oggi è %d", absenceType.code, resultList.size());
				if(resultList.size() >= maxRecoveryDaysOneThree){
					actualDate = actualDate.plusDays(1);
					continue;
				}
			}
			
			//Controllo del residuo
			if(!PersonUtility.canTakeCompensatoryRest(person, actualDate))
			{
				
				actualDate = actualDate.plusDays(1);
				continue;
			}

			taken = taken + insertAbsencesInPeriod(person, actualDate, actualDate, absenceType, true, file).totalAbsenceInsert;

			actualDate = actualDate.plusDays(1);
		}
		
		if(taken==0)
			flash.error("Non e' stato possibile inserire alcun riposo compensativo (bonus esaurito o residuo insufficiente)");
		else
			flash.success("Inseriti %s riposi compensativi per la persona", taken);

		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
	}

	/**
	 * Gestisce l'inserimento esplicito del codice 31
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 */
	private static void handler31(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, Blob file) {
		
	}
	
	/**
	 * Gestisce l'inserimento esplicito del codice 32
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 */
	private static void handler32(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, Blob file) {
		
	}
	
	/**
	 * Gestisce l'inserimento esplicito del codice 94
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 */
	private static void handler94(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, Blob file) {
		
	}
	
	/**
	 * Gestisce l'inserimento dei codici FER, 94-31-32 nell'ordine. Fino ad esaurimento.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @throws EmailException 
	 */
	private static void handlerFER(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, Blob file) throws EmailException
	{
		//controllo reperibilita'
		LocalDate actualDate = dateFrom;

		//inserimento
		int taken = 0;
		
		actualDate = dateFrom;
		while(!actualDate.isAfter(dateTo))
		{
						
			AbsenceType wichFer = PersonUtility.whichVacationCode(person, actualDate);
			
			//FER esauriti
			if(wichFer==null)
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

			taken = taken + insertAbsencesInPeriod(person, actualDate, actualDate, wichFer, true, file).totalAbsenceInsert;
			actualDate = actualDate.plusDays(1);
			
		}
		actualDate = actualDate.minusDays(1);

		
		if(taken==1)
			flash.success("Aggiunto codice assenza FER per il giorno %s", actualDate);
		if(taken>1)
			flash.success("Aggiunti %s codici assenza FER da %s a %s.", taken, dateFrom, dateTo);
		
		PersonUtility.updatePersonDaysIntoInterval(person,dateFrom,dateTo);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());

	}
	

	/**
	 * Gestisce una richiesta di inserimento codice 37 (utilizzo ferie anno precedente scadute)
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 * @throws EmailException 
	 */
	private static void handler37(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, Blob file) throws EmailException
	{
		if(dateFrom.getYear() != dateTo.getYear())
		{
			flash.error("I recuperi ferie anno precedente possono essere assegnati solo per l'anno corrente");
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}

		int remaining37 = VacationsRecap.remainingPastVacationsAs37(dateFrom.getYear(), person);
		if(remaining37 == 0){
			flash.error("La persona selezionata non dispone di ulteriori giorni ferie anno precedente");
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
		}
		
		LocalDate actualDate = dateFrom;
		int taken = 0;
		while(!actualDate.isAfter(dateTo) && taken<=remaining37)
		{

			taken = taken + insertAbsencesInPeriod(person, actualDate, actualDate, absenceType, true, file).totalAbsenceInsert;
			actualDate = actualDate.plusDays(1);
		}

		if(taken > 0)
			flash.success("Inseriti %s codice 37 per la persona", taken);
		else
			flash.error("Impossibile inserire codici 37 per la persona");
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
		
	}
	
	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param absenceType
	 * @throws EmailException 
	 */
	private static void handlerChildIllness(Person person, LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, Blob file) throws EmailException
	{
		/**
		 * controllo sulla possibilità di poter prendere i congedi per malattia dei figli, guardo se il codice di assenza appartiene alla
		 * lista dei codici di assenza da usare per le malattie dei figli
		 */
		//TODO: se il dipendente ha più di 9 figli! non funziona dal 10° in poi
		
		Boolean esito = PersonUtility.canTakePermissionIllnessChild(person, dateFrom, absenceType);

		if(esito==null)
		{
			flash.error("ATTENZIONE! In anagrafica la persona selezionata non ha il numero di figli sufficienti per valutare l'assegnazione del codice di assenza nel periodo selezionato. "
					+ "Accertarsi che la persona disponga dei privilegi per usufruire dal codice e nel caso rimuovere le assenze inserite.");
		}
		else if(!esito)
		{
			flash.error(String.format("Il dipendente %s %s non può prendere il codice d'assenza %s poichè ha già usufruito del numero" +
					" massimo di giorni di assenza per quel codice o non ha figli che possono usufruire di quel codice", person.name, person.surname, absenceType.code));
			Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
			return;
		}

		LocalDate actualDate = dateFrom;
		int taken = 0;
		while(!actualDate.isAfter(dateTo))
		{

			taken = taken + insertAbsencesInPeriod(person, actualDate, actualDate, absenceType, !absenceType.consideredWeekEnd, file).totalAbsenceInsert;
			actualDate = actualDate.plusDays(1);
		}
		if(taken > 0)
			flash.success("Inseriti %s codici assenza per la persona", taken);
		else
			flash.error("Impossibile inserire codici di assenza per malattia figli");
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
	
	}
	
	/**
	 * Gestore assenze di malattia e congedo (in cui considerer week end è true)
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 * @throws EmailException 
	 */
	private static void handlerIllnessOrDischarge(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType,Blob file) throws EmailException
	{
		int taken = insertAbsencesInPeriod(person, dateFrom, dateTo, absenceType, false, file).totalAbsenceInsert;
		flash.success("Inseriti %s codici assenza per la persona", taken);
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
	}
	
	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param file
	 * @throws EmailException 
	 */
	private static void handlerAbsenceTypeGroup(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, Blob file) throws EmailException
	{

		LocalDate actualDate = dateFrom;
		while(actualDate.isBefore(dateTo) || actualDate.isEqual(dateTo)){

			CheckMessage checkMessage = PersonUtility.checkAbsenceGroup(absenceType, person, actualDate);
			if(checkMessage.check == false){
				flash.error("Impossibile inserire il codice %s per %s %s. "+checkMessage.message, absenceType.code, person.name, person.surname);
				Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
			}
			
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			if(pd == null){
				pd = new PersonDay(person, actualDate);
				pd.populatePersonDay();
				pd.save();
			}
			if(checkMessage.check == true && checkMessage.absenceType ==  null){

				Absence absence = new Absence();
				absence.absenceType = absenceType;
				absence.personDay = pd;
				
				if (file != null && file.exists()) {
					absence.absenceFile = file;
				}
				
				absence.save();

				pd.populatePersonDay();
				pd.save();
				pd.updatePersonDaysInMonth();

			}
			else if(checkMessage.check == true && checkMessage.absenceType != null){
				Absence absence = new Absence();
				absence.absenceType = absenceType;
				absence.personDay = pd;
				
				if (file != null && file.exists()) {
					absence.absenceFile = file;
				}
				
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
				
			}
			actualDate = actualDate.plusDays(1);
		}

		flash.success("Aggiunto codice di assenza %s ", absenceType.code);
		Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
		
	}
	
	private static void handlerGenericAbsenceType(Person person,LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, Blob file, String mealTicket) throws EmailException
	{
		LocalDate actualDate = dateFrom;
		int taken = 0;
		while(!actualDate.isAfter(dateTo))
		{

			taken = taken + insertAbsencesInPeriod(person, actualDate, actualDate, absenceType, false, file).totalAbsenceInsert;

			checkMealTicket(actualDate, person, mealTicket, absenceType);


			actualDate = actualDate.plusDays(1);
		}

		if(taken > 0)
			flash.success("Inseriti %s codici assenza per la persona", taken);
		else
			flash.error("Impossibile inserire il codice di assenza %s", absenceType.code);
		PersonUtility.updatePersonDaysIntoInterval(person, dateFrom, dateTo);
		Stampings.personStamping(person.id, dateFrom.getYear(), dateFrom.getMonthOfYear());
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
	
	/**
	 * controlla se si sta prendendo un codice di assenza in un giorno in cui si è reperibili
	 * @return true se si sta prendendo assenza per un giorno in cui si è reperibili, false altrimenti
	 */
	private static boolean checkIfAbsenceInReperibilityOrInShift(Person person, LocalDate date){
		//controllo se la persona è in reperibilità
		PersonReperibilityDay prd = PersonReperibilityDay.find("Select prd from PersonReperibilityDay prd where prd.date = ? and prd.personReperibility.person = ?", 
				date, person).first();
				
		//controllo se la persona è in turno
		PersonShiftDay psd = PersonShiftDay.find("Select psd from PersonShiftDay psd where psd.date = ? and psd.personShift.person = ?",
				date, person).first();
		if(psd == null && prd == null)		
			return false;
		else
			return true;
	}
	
	@Check(Security.INSERT_AND_UPDATE_ABSENCE)
	public static void insertPersonChildren(){
		int month = new LocalDate().getMonthOfYear();
		int year = new LocalDate().getYear();
		List<Person> personList = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), false);
		render(personList);
	}
	
	public static class AttachmentsPerCodeRecap {
		
		List<Absence> absenceSameType = new ArrayList<Absence>();
		
		public String getCode() {
			return absenceSameType.get(0).absenceType.code;
		}
		
	}
	
	
	public static void manageAttachmentsPerCode(Integer year, Integer month){
		
		rules.checkIfPermitted("");
		LocalDate beginMonth = new LocalDate(year, month, 1);
		
		//Prendere le assenze ordinate per tipo
		List<Absence> absenceList = Absence.find("Select abs from Absence abs where abs.absenceType.absenceTypeGroup is null and " +
				"abs.personDay.date between ? and ? and abs.absenceFile is not null order by abs.absenceType.code", 
				beginMonth, beginMonth.dayOfMonth().withMaximumValue()).fetch();
	
		List<AttachmentsPerCodeRecap> attachmentRecapList = new ArrayList<AttachmentsPerCodeRecap>();
		AttachmentsPerCodeRecap currentRecap = new AttachmentsPerCodeRecap();
		AbsenceType currentAbt = null;
		
		for(Absence abs : absenceList) {
			
			if(currentAbt == null) {
				
				currentAbt = abs.absenceType;
			}
			else if( !currentAbt.code.equals(abs.absenceType.code) ) {
				
				//finalizza tipo
				if( currentRecap.absenceSameType.size() > 0 )		/* evitato con la query abs.absenceFile is not null */
					attachmentRecapList.add(currentRecap);
				currentRecap = new AttachmentsPerCodeRecap();
				//nuovo tipo
				currentAbt = abs.absenceType;
			}
			if(abs.absenceFile.get() != null) {
				
				currentRecap.absenceSameType.add(abs);
			}
		}
		
		//finalizza ultimo tipo
		if( currentRecap.absenceSameType.size() > 0 )
			attachmentRecapList.add(currentRecap);
		
		render(attachmentRecapList, year, month);
	}
	
	
	public static void downloadAttachment(long id){
		Logger.debug("Assenza con id: %d", id);
		   Absence absence = Absence.findById(id);
		   rules.checkIfPermitted(absence.personDay.person.office);
		   notFoundIfNull(absence);
		   response.setContentTypeIfNotSet(absence.absenceFile.type());
		   Logger.debug("Allegato relativo all'assenza: %s", absence.absenceFile.getFile());
		   renderBinary(absence.absenceFile.get(), absence.absenceFile.length());
	}

	
	
	public static void manageAttachmentsPerPerson(Long personId, Integer year, Integer month){
		Person person = Person.findById(personId);
		if(person == null){
			flash.error("Persona inesistente");
			YearlyAbsences.showGeneralMonthlyAbsences(year, month, null, null);
		}
		rules.checkIfPermitted(person.office);
		List<Absence> personAbsenceListWithFile = new ArrayList<Absence>();
		List<Absence> personAbsenceList = Absence.find("Select abs from Absence abs where abs.personDay.person = ? " +
				"and abs.personDay.date between ? and ?", 
				person, new LocalDate(year, month,1), new LocalDate(year, month,1).dayOfMonth().withMaximumValue()).fetch();
		for(Absence abs : personAbsenceList){
			if (abs.absenceFile.get() != null){
				personAbsenceListWithFile.add(abs);
			}
		}
		render(personAbsenceListWithFile, year, month, person);

		
	}	
	
	
	public static void absenceInPeriod(Long personId){

		List<Person> personList = Person.getActivePersonsInDay(LocalDate.now(),
				Security.getOfficeAllowed(), false);
		if(personId == null)
			personId = Security.getUser().get().person.id;
		Person person = Person.findById(personId);
		if(person == null){
			flash.error("Persona inesistente");
			Stampings.personStamping(Security.getUser().get().person.id, new LocalDate().getYear(), new LocalDate().getMonthOfYear());
		}
		
		
		rules.checkIfPermitted(person.office);
		
		LocalDate dateFrom = null;
		LocalDate dateTo = null;
		
		try {
			
			String dataInizio = params.get("dataInizio");
			String dataFine = params.get("dataFine");
			dateFrom = new LocalDate(dataInizio);
			dateTo = new LocalDate(dataFine);
		} catch (Exception e) {
			
			flash.error("Errore nell'inserimento dei parametri. Valorizzare correttamente data inizio e data fine secondo il formato aaaa-mm-dd");
			render(personId, personList);
		}
		
		List<Absence> missioni = new ArrayList<Absence>();
		List<Absence> ferie = new ArrayList<Absence>();
		List<Absence> riposiCompensativi = new ArrayList<Absence>();
		List<Absence> altreAssenze = new ArrayList<Absence>();
		
		List<Absence> absenceList = Absence.find("Select abs from Absence abs where abs.personDay.person = ? " +
				"and abs.personDay.date between ? and ? and abs.absenceType.justifiedTimeAtWork = ?", person, dateFrom, dateTo, JustifiedTimeAtWork.AllDay).fetch();

		for(Absence abs : absenceList){
			if(abs.absenceType.code.equals("92")){
				missioni.add(abs);
			}
			else if(abs.absenceType.code.equals("31") || abs.absenceType.code.equals("32") || abs.absenceType.code.equals("94")){
				ferie.add(abs);
			}
			else if(abs.absenceType.code.equals("91")){
				riposiCompensativi.add(abs);
			}
			else
				altreAssenze.add(abs);
		}
		render(personList, person, absenceList, dateFrom, dateTo, missioni, ferie, riposiCompensativi, altreAssenze, personId);

	}

	
	/**
	 * Inserisce l'assenza absenceType nel person day della persona nel periodo indicato. Se dateFrom = dateTo inserisce nel giorno singolo.
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param absenceType
	 * @param considerHoliday
	 * @param file
	 * @throws EmailException 
	 */
	private static CheckAbsenceInsert insertAbsencesInPeriod(Person person, LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType, boolean notInHoliday, Blob file) throws EmailException
	{
		CheckAbsenceInsert cai = new CheckAbsenceInsert(0,null, false, 0);
		LocalDate actualDate = dateFrom;

		boolean esito = false;
		
		while(!actualDate.isAfter(dateTo))
		{
			//se non devo considerare festa ed è festa vado oltre
			if(notInHoliday && person.isHoliday(actualDate))
			{
				actualDate = actualDate.plusDays(1);
				continue;
			}
			//Controllo il caso di inserimento di codice 31: verifico che sia valido il periodo in cui voglio inserirlo
			if(absenceType.code.equals("31") && !absenceType.code.equals(PersonUtility.whichVacationCode(person, actualDate).code)){
				flash.error("Si prova a inserire un codice di assenza (%s , %s) che non è prendibile per il giorno %s", 
						absenceType.code, absenceType.description, actualDate);
				
				Stampings.personStamping(person.id, actualDate.getYear(), actualDate.getMonthOfYear());
			}
			//Costruisco se non esiste il person day
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			if(pd == null){
				pd = new PersonDay(person, actualDate);
				pd.create();
			}
			esito = checkIfAbsenceInReperibilityOrInShift(person, actualDate);
			if(esito==true){
				cai.insertInShiftOrReperibility = true;
				cai.howManyAbsenceInReperibilityOrShift++;
				cai.dateInTrouble.add(actualDate);
								
			}
			
			//creo l'assenza e l'aggiungo
			Absence absence = new Absence();
			absence.absenceType = absenceType;
			absence.personDay = pd;
			if (file != null && file.exists()) {
				absence.absenceFile = file;
			}
			absence.save();
			Logger.info("Inserita nuova assenza %s per %s %s in data: %s", 
					absence.absenceType.code, absence.personDay.person.name, absence.personDay.person.surname, absence.personDay.date);
			pd.absences.add(absence);
			pd.populatePersonDay();
			//pd.save();
			
			cai.totalAbsenceInsert++;
			
			actualDate = actualDate.plusDays(1);
		}
		//controllo che ci siano date in cui l'assenza sovrascrive una reperibilità o un turno e nel caso invio la mail
		if(cai.dateInTrouble.size() > 0)
			sendEmail(person, cai);

		return cai;
	}
	
	/**
	 * metodo che invia la mail contenente i giorni in cui ci sono inserimenti di assenza in turno o reperibilità
	 * @param person
	 * @param cai
	 * @throws EmailException
	 */
	private static void sendEmail(Person person, CheckAbsenceInsert cai) throws EmailException{
		MultiPartEmail email = new MultiPartEmail();

//		email.addTo(person.contactData.email);
		email.addTo(person.email);
		//Da attivare, commentando la riga precedente, per fare i test così da evitare di inviare mail a caso ai dipendenti...
		//email.addTo("dario.tagliaferri@iit.cnr.it");
		email.setFrom("epas@iit.cnr.it");
		
		email.setSubject("Segnalazione inserimento assenza in giorno con reperibilità/turno");
		String date = "";
		for(LocalDate data : cai.dateInTrouble){
			date = date+data+' ';
		}
		email.setMsg("E' stato richiesto l'inserimento di una assenza per il giorno "+date+ 
				" per il quale risulta una reperibilità o un turno attivi. "+'\n'+
				"Controllare tramite la segreteria del personale."+'\n'+
				'\n'+
				"Servizio ePas");
		Mail.send(email); 
	}
	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 */
	private static int removeAbsencesInPeriod(Person person, LocalDate dateFrom, LocalDate dateTo, AbsenceType absenceType)
	{
		LocalDate today = new LocalDate();
		LocalDate actualDate = dateFrom;
		int deleted = 0;
		while(!actualDate.isAfter(dateTo))
		{
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, actualDate).first();
			if(pd == null)
			{
				actualDate = actualDate.plusDays(1);
				continue;
			}
			List<Absence> absenceList = Absence.find("Select ab from Absence ab, PersonDay pd where ab.personDay = pd and pd.person = ? and pd.date = ?", 
					person, actualDate).fetch();
			for(Absence absence : absenceList)
			{
				if(absence.absenceType.code.equals(absenceType.code))
				{
					absence.delete();
					pd.absences.remove(absence);
					pd.isTicketForcedByAdmin = false;
					pd.populatePersonDay();
					deleted++;
					Logger.info("Rimossa assenza del %s per %s %s", actualDate, person.name, person.surname);
				}
			}
			if(pd.date.isAfter(today) && pd.absences.isEmpty() && pd.absences.isEmpty()){
				pd.delete();
			}
			actualDate = actualDate.plusDays(1);
		}
		return deleted;
	}
	
	
}




