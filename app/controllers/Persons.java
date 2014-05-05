package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;

import models.Competence;
import models.CompetenceCode;
import models.ConfGeneral;
import models.ContactData;
import models.Contract;
import models.ContractWorkingTimeType;
import models.InitializationTime;
import models.Location;
import models.Office;
import models.Permission;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.Qualification;
import models.User;
import models.UsersPermissionsOffices;
//import models.RemoteOffice;
import models.VacationCode;
import models.WorkingTimeType;
import models.enumerate.ConfigurationFields;
import net.sf.oval.constraint.MinLength;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;

import dao.PersonDao;

@With( {Secure.class, RequestInit.class} )
public class Persons extends Controller {

	public static final String USERNAME_SESSION_KEY = "username";

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void edit(Long personId){
		Person person = Person.findById(personId);
	
		LocalDate date = new LocalDate();
		List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ? order by con.beginContract", person).fetch();
		List<Office> officeList = Security.getOfficeAllowed();	
		
		InitializationTime initTime = InitializationTime.find("Select init from InitializationTime init where init.person = ?", person).first();
		Integer month = date.getMonthOfYear();
		Integer year = date.getYear();
		Long id = person.id;		
		render(person, contractList, initTime, month, year, id, officeList);
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void personCompetence(Long personId, Integer month, Integer year){
		
		if(personId == null)
			personId = params.get("id", Long.class);
		Person person = Person.findById(personId);
		LocalDate date = null;
		if(month == null || year == null)
			date = new LocalDate(params.get("year", Integer.class), params.get("month", Integer.class), 1).dayOfMonth().withMaximumValue();
		else
			date = new LocalDate(year, month, 1).dayOfMonth().withMaximumValue();
		int weekDayAvailability;
		int holidaysAvailability;
		int daylightWorkingDaysOvertime;
		CompetenceCode cmpCode1 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "207").first();
		CompetenceCode cmpCode2 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "208").first();
		CompetenceCode cmpCode3 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "S1").first();
		
		Competence comp1 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode1).first();
		Competence comp2 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode2).first();
		Competence comp3 = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.competenceCode = code and comp.person = ?" +
				" and comp.year = ? and comp.month = ? and code = ?", person, date.getYear(), date.getMonthOfYear(), cmpCode3).first();
		if(comp1 != null)
			weekDayAvailability = comp1.valueApproved;
		else
			weekDayAvailability = 0;
		if(comp2 != null)
			holidaysAvailability = comp2.valueApproved;
		else
			holidaysAvailability = 0;
		if(comp3 != null)
			daylightWorkingDaysOvertime = comp3.valueApproved;
		else
			daylightWorkingDaysOvertime = 0;
		
		int progressive = 0;
		PersonDay lastPreviousPersonDayInMonth = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
				"and pd.date >= ? and pd.date < ? ORDER by pd.date DESC", person, date.dayOfMonth().withMinimumValue(), date).first();
		if(lastPreviousPersonDayInMonth != null)
			progressive = lastPreviousPersonDayInMonth.progressive /60;
		int mese = date.getMonthOfYear();
		
		render(weekDayAvailability, holidaysAvailability, daylightWorkingDaysOvertime, person, progressive, mese, year);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void saveCompetence(){
		
		int month = params.get("month", Integer.class);
		int year = params.get("year", Integer.class);
		Long personId = params.get("personId", Long.class);
		int progressive = params.get("progressive", Integer.class);
		
		Person person = Person.findById(personId);
		int weekDayAvailability = params.get("weekDayAvailability", Integer.class);
		int holidaysAvailability = params.get("holidaysAvailability", Integer.class);
		int daylightWorkingDaysOvertime = params.get("daylightWorkingDaysOvertime", Integer.class);
		Competence comp = Competence.find("Select cmp from Competence cmp, CompetenceCode code where cmp.competenceCode = code and " +
				"cmp.person = ? and cmp.month = ? and cmp.year = ? and code.code = ?", person, month, year, "S1").first();
		
		Competence comp1 = Competence.find("Select cmp from Competence cmp, CompetenceCode code where cmp.competenceCode = code and " +
				"cmp.person = ? and cmp.month = ? and cmp.year = ? and code.code = ?", person, month, year, "207").first();
		
		Competence comp2 = Competence.find("Select cmp from Competence cmp, CompetenceCode code where cmp.competenceCode = code and " +
				"cmp.person = ? and cmp.month = ? and cmp.year = ? and code.code = ?", person, month, year, "208").first();
		
		if(comp1 != null){
			if(comp1.valueApproved != weekDayAvailability){
				comp1.valueApproved = weekDayAvailability;
				comp1.save();
				person.competences.add(comp1);
				person.save();				
			}
		}
		else{
			comp1 = new Competence();
			comp1.person = person;
			comp1.competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "207").first();
			comp1.month = month;
			comp1.year = year;
			comp1.valueApproved = weekDayAvailability;
			comp1.save();
			person.competences.add(comp1);
			person.save();
		}
		
		if(comp2 != null){
			if(comp2.valueApproved != holidaysAvailability){
				comp2.valueApproved = holidaysAvailability;
				comp2.save();	
				person.competences.add(comp2);
				person.save();				
			}
		}
		else{
			comp2 = new Competence();
			comp2.person = person;
			comp2.competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "208").first();
			comp2.month = month;
			comp2.year = year;
			comp2.valueApproved = holidaysAvailability;
			comp2.save();
			person.competences.add(comp2);
			person.save();			
		}
		flash.success("Aggiornato il valore per le reperibilità di %s %s", person.name, person.surname);
		if(comp != null){
			if(comp.valueApproved != daylightWorkingDaysOvertime && daylightWorkingDaysOvertime < progressive){
				comp.valueApproved = daylightWorkingDaysOvertime;
				comp.save();	
				person.competences.add(comp);
				person.save();				
			}
			else{
				flash.error("Non è stato possibile aggiornare il valore delle ore di straordinario: valore troppo alto rispetto al massimo consentito nel mese per %s %s", 
						person.name, person.surname);
				render("@Stampings.redirectToIndex");
			}
		}
		else{
			comp = new Competence();
			comp.person = person;
			comp.competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", "S1").first();
			comp.month = month;
			comp.year = year;
			comp.valueApproved = daylightWorkingDaysOvertime;
			comp.save();		
			person.competences.add(comp);
			person.save();
		}
		flash.success("Aggiornato valore dello straordinario per %s %s", person.name, person.surname);
		
		render("@Stampings.redirectToIndex");
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void editVacation(Long personId){
		Person person = Person.findById(personId);
		render(person);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void updateContractWorkingTimeType(Long id)
	{
		List<WorkingTimeType> wttList = WorkingTimeType.findAll();
		Contract contract = Contract.findById(id);
		render(contract, wttList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void splitContractWorkingTimeType(ContractWorkingTimeType cwtt, LocalDate splitDate)
	{
		//Controllo integrità richiesta
		if(cwtt==null)
		{
			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}
		if(validation.hasError("splitDate"))
		{
			flash.error("Errore nel fornire il parametro data. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(cwtt.contract.person.id);
		}
		if(!DateUtility.isDateIntoInterval(splitDate, new DateInterval(cwtt.beginDate, cwtt.endDate)))
		{
			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			Persons.edit(cwtt.contract.person.id);
		}
		DateInterval first = new DateInterval(cwtt.beginDate, splitDate.minusDays(1));
		if(! DateUtility.isIntervalIntoAnother(first, cwtt.contract.getContractDateInterval())){
			flash.error("Errore nel fornire il parametro data. La data deve essere contenuta nel periodo da dividere.");
			Persons.edit(cwtt.contract.person.id);
		}
		//agire
		ContractWorkingTimeType cwtt2 = new ContractWorkingTimeType();
		cwtt2.contract = cwtt.contract;
		cwtt2.beginDate = splitDate;
		cwtt2.endDate = cwtt.endDate;
		cwtt2.workingTimeType = cwtt.workingTimeType;
		cwtt2.save();

		cwtt.endDate = splitDate.minusDays(1);
		cwtt.save();
		flash.success("Orario di lavoro correttamente suddiviso in due sottoperiodi con tipo orario %s.", cwtt.workingTimeType.description);
		Persons.edit(cwtt.contract.person.id);	
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void deleteContractWorkingTimeType(ContractWorkingTimeType cwtt)
	{
		if(cwtt==null)
		{
			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}	
		Contract contract = cwtt.contract;
		int index = contract.contractWorkingTimeType.indexOf(cwtt);
		if(contract.contractWorkingTimeType.size()<index)
		{
			flash.error("Impossibile completare la richiesta, controllare i log.");
			Persons.edit(cwtt.contract.person.id);	
		}
		ContractWorkingTimeType previous = contract.contractWorkingTimeType.get(index-1);
		previous.endDate = cwtt.endDate;
		previous.save();
		cwtt.delete();
		flash.success("Orario di lavoro eliminato correttamente. Attribuito al periodo eliminato il tipo orario %s.", previous.workingTimeType.description);
		Persons.edit(cwtt.contract.person.id);	
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void changeTypeOfContractWorkingTimeType(ContractWorkingTimeType cwtt, WorkingTimeType newWtt)
	{
		if(cwtt==null || newWtt==null)
		{
			flash.error("Impossibile completare la richiesta, controllare i log.");
			Application.indexAdmin();
		}
		cwtt.workingTimeType = newWtt;
		cwtt.save();
		flash.success("Cambiato correttamente tipo orario per il periodo a %s.", cwtt.workingTimeType.description);
		Persons.edit(cwtt.contract.person.id);
	}

	@Check(Security.VIEW_PERSON_LIST)
	public static void list(String name){
		LocalDate startEra = new LocalDate(1900,1,1);
		LocalDate endEra = new LocalDate(9999,1,1);
		List<Person> personList = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(Security.getOfficeAllowed()), 
				false, 
				startEra, 
				endEra)
				.list();
		LocalDate date = new LocalDate();
		List<Person> activePerson = PersonDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(Security.getOfficeAllowed()), 
				false, 
				date, 
				date)
				.list();
		//List<Person> personList = Person.getActivePersonsSpeedyInPeriod(startEra, endEra, Security.getOfficeAllowed(), false);
		
		//List<Person> activePerson = Person.getActivePersonsInDay(date.getDayOfMonth(), date.getMonthOfYear(), date.getYear(), Security.getOfficeAllowed(), false);
		
		render(personList, activePerson);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void insertPerson() throws InstantiationException, IllegalAccessException {
		Person person = new Person();
		Contract contract = new Contract();
		Location location = new Location();
		ContactData contactData = new ContactData();
		InitializationTime initializationTime = new InitializationTime();
		List<Office> officeList = Security.getOfficeAllowed();
		List<Office> office = Office.find("Select office from Office office where office.office is null").fetch();
		List<WorkingTimeType> wttList = WorkingTimeType.findAll();
		Logger.debug("Lista office: %s", office.get(0).name);
		render(person, contract, location, contactData, initializationTime, officeList, wttList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void save() {
		if(validation.hasErrors()) {
			if(request.isAjax()) error("Invalid value");
			render("@insertPerson");
		}
		
		/* creazione persona */
		Person person = null;
		Location location = new Location();
		ContactData contactData = new ContactData();
		
		Contract contract = new Contract();		
		person = new Person();		
		Logger.debug("Saving person...");
		
		if(params.get("name").equals("") || params.get("surname").equals("")){
			flash.error("Inserire nome e cognome per la persona che si intende salvare in anagrafica");
			render("@list");
		}
		person.name = params.get("name");
		
		person.surname = params.get("surname");
		
		person.number = params.get("number", Integer.class);
				
		Qualification qual = Qualification.findById(new Long(params.get("person.qualification", Integer.class)));
		person.qualification = qual;
				
		Office office = Office.findById(new Long(params.get("person.office", Integer.class)));
		if(office != null)
			person.office = office;
		else{
			Logger.debug("L'ufficio che si tenta di inserire per %s %s è nullo. Non inserisco niente", person.name, person.surname);
			flash.error("L'ufficio di appartenenza non può essere nullo.");
			render("@list");
		}
		person.save();
		
		/* creazione utente */
		User user = new User();
		user.username = params.get("name").toLowerCase()+'.'+params.get("surname").toLowerCase(); 
		Codec codec = new Codec();
		user.password = codec.hexMD5("epas");
		user.person = person;
		
		/*permesso viewPersonalSituation */
		Permission per = Permission.find("Select per from Permission per where per.description = ?", "viewPersonalSituation").first();
		
		/*Aggiungere lo user_permission_office per la persona con permesso quello appena recuperato dal db e come ufficio quello della 
		 * persona che si va a creare*/
		user.userPermissionOffices = new ArrayList<UsersPermissionsOffices>();
		 
		//user.permissions.add(per);
		user.save();
		UsersPermissionsOffices upo = new UsersPermissionsOffices();
		upo.user =	user;
		upo.permission = per;
		upo.office = user.person.office;
		upo.save();
		user.userPermissionOffices.add(upo);
		user.save();
		person.user = user;
		person.save();
		
		/*creazione location */
		location.department = params.get("department");
		location.headOffice = params.get("headOffice");
		location.room = params.get("room");
		location.person = person;
		location.save();
		Logger.debug("Saving contact data...");
		
		/* creazione contactData */
		contactData.email = params.get("email");
		contactData.telephone = params.get("telephone");
		contactData.person = person;
		contactData.save();
		
		/* creazione contratto */
		Logger.debug("Begin contract: %s", params.get("beginContract"));
		if(params.get("beginContract") == null){
			flash.error("Il contratto di %s %s deve avere una data di inizio. Utente cancellato. Reinserirlo con la data di inizio contratto valorizzata.", 
					person.name, person.surname);
			//person.delete();
			render("@list");
		}
		LocalDate expireContract = null;
		LocalDate beginContract = new LocalDate(params.get("beginContract"));
		if(params.get("expireContract").equals("") || params.get("expireContract") == null)
			contract.expireContract = null;
		else			
			expireContract = new LocalDate(params.get("expireContract"));
		contract.beginContract = beginContract;
		contract.expireContract = expireContract;
		
		contract.person = person;
		contract.onCertificate = params.get("onCertificate", Boolean.class);
		contract.save();
		contract.setVacationPeriods();
		
		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = contract.beginContract;
		cwtt.endDate = contract.expireContract;
		cwtt.workingTimeType = WorkingTimeType.findById(params.get("wtt", Long.class));
		cwtt.contract = contract;
		cwtt.save();
		
		
		
		Long personId = person.id;
		List<String> usernameList = PersonUtility.composeUsername(person.name, person.surname);
		render("@insertUsername", personId, usernameList, person);
		
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void insertUsername(Person person){
		//Logger.debug("Id persona: %d", personId);
		//Person person = Person.findById(personId);
		List<String> usernameList = new ArrayList<String>();
		usernameList = PersonUtility.composeUsername(person.name, person.surname);
		render(person, usernameList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void updateUsername(){
		Long id = params.get("person", Long.class);
		Person person = Person.findById(id);
		Logger.debug("Il valore selezionato come username è: %s", params.get("username"));
		Logger.debug("La persona che si vuole modificare è: %s %s", person.name, person.surname);
		person.user.username = params.get("username");
		person.user.save();
		person.save();
		
		flash.success("%s %s inserito in anagrafica con il valore %s come username", person.name, person.surname, person.user.username);
		render("@Stampings.redirectToIndex");
		
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void update(){
		Long personId = params.get("personId", Long.class);		
		
		Person person = Person.findById(personId);
		ContactData contactData = person.contactData;
		Location location = person.location;
		
		InitializationTime initTime = InitializationTime.find("Select init from InitializationTime init where init.person = ?", person).first();
		
		if(!params.get("name").equals(person.name))
			person.name = params.get("name");
		
		if(!params.get("surname").equals(person.surname))
			person.surname = params.get("surname");
		
		if(!params.get("number").equals(person.number))
			person.number = params.get("number", Integer.class);
		
		if(person.badgeNumber == null || !person.badgeNumber.equals(params.get("badgeNumber")))
			person.badgeNumber = params.get("badgeNumber");
		
		Logger.debug("Sede: %s", params.get("person.office"));
		if(person.office == null || !person.office.id.equals(new Long(params.get("person.office")))){
			person.office = Office.findById(Long.parseLong((params.get("person.office"))));
		}
		
		
		if(contactData != null){
			if(contactData.email == null || !contactData.email.equals(params.get("email"))){
				contactData.email = params.get("email");
			}
			if(contactData.telephone == null || !contactData.telephone.equals(params.get("telephone"))){
				contactData.telephone = params.get("telephone");
			}
			contactData.save();
		}
		else{
			contactData = new ContactData();
			if(params.get("email") != null)
				contactData.email = params.get("email");
			if(params.get("telephone") != null)
				contactData.telephone = params.get("telephone");
			contactData.save();
		}
		
		if(location != null){
			if(location.department == null || !location.department.equals(params.get("department"))){
				location.department = params.get("department");
			}
			if(location.headOffice == null || !location.headOffice.equals(params.get("headOffice"))){
				location.headOffice = params.get("headOffice");
			}
			if(location.room == null || !location.room.equals(params.get("room"))){
				location.room = params.get("room");
			}
			location.save();
		}
		else{
			location = new Location();
			if(params.get("department") != null)
				location.department = params.get("department");
			if(params.get("headOffice") != null)
				location.headOffice = params.get("headOffice");
			if(params.get("room") != null)
				location.room = params.get("room");
			location.save();
		}
		
		if(initTime != null){
			if(initTime.residualMinutesCurrentYear == null || ! initTime.residualMinutesCurrentYear.equals(params.get("minutesCurrentYear", Integer.class)))
				initTime.residualMinutesCurrentYear = params.get("minutesCurrentYear", Integer.class);
			if(initTime.residualMinutesPastYear == null || ! initTime.residualMinutesPastYear.equals(params.get("minutesPastYear", Integer.class)))
				initTime.residualMinutesPastYear = params.get("minutesPastYear", Integer.class);
			initTime.save();
		}
//		else{
//			initTime = new InitializationTime();
//			if(params.get("minutesPastYear") != null)
//				initTime.residualMinutesPastYear = params.get("minutesPastYear", Integer.class);
//			if(params.get("minutesCurrentYear") != null)
//				initTime.residualMinutesCurrentYear = params.get("minutesCurrentYear", Integer.class);
//			initTime.save();
//		}
		if(person.number != null && ! person.number.equals(params.get("number", Integer.class)))
			person.number = params.get("number", Integer.class);
		//Logger.debug("Qualifica: %d", params.get("person.qualification", Integer.class));
		if(person.qualification != null && person.qualification.qualification != params.get("person.qualification", Integer.class)){
			Qualification q = Qualification.find("Select q from Qualification q where q.qualification = ?", params.get("person.qualification", Integer.class)).first();
			person.qualification = q;
		}
		if(person.qualification == null){
			Qualification q = Qualification.find("Select q from Qualification q where q.qualification = ?", params.get("person.qualification", Integer.class)).first();
			person.qualification = q;
		}
		
		person.save();
		flash.success("Modificate informazioni per l'utente %s %s", person.name, person.surname);
		//Application.indexAdmin();
		Persons.list(null);	
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void insertContract(Person person){
		if(person == null)
		{
			flash.error("Persona inesistente. Operazione annullata.");
			Persons.list(null);
		}
		Contract con = new Contract();
		List<WorkingTimeType> wttList = WorkingTimeType.findAll();
		render(con, person, wttList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void saveContract(@Required LocalDate dataInizio, @Valid LocalDate dataFine, Person person, WorkingTimeType wtt, boolean onCertificate){
			
		//Controllo parametri
		if(person==null)
		{
			flash.error("Persona inesistente. Operazione annullata.");
			Persons.list(null);
		}
		if(dataInizio==null){
			flash.error("Errore nel fornire il parametro data inizio contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(person.id);
		}
		if(validation.hasErrors())
		{
			flash.error("Errore nel fornire il parametro data fine contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(person.id);
		}
		//Date non si sovrappongono con gli altri contratti della personas	
		DateInterval contractInterval = new DateInterval(dataInizio, dataFine);
		List<Contract> allContract = Contract.find("Select c from Contract c where c.person = ?", person).fetch();
		for(Contract c : allContract)
		{
			if(DateUtility.intervalIntersection(contractInterval, c.getContractDateInterval()) != null)
			{
				flash.error("Il nuovo contratto si interseca con contratti precedenti. Controllare le date di inizio e fine. Operazione annulalta.");
				Persons.edit(person.id);
			}
		}
		//Tipo orario
		if(wtt == null)
		{
			flash.error("Errore nel fornire il parametro tipo orario. Operazione annullata.");
			Persons.edit(person.id);
		}

		//Creazione nuovo contratto
		Contract contract = new Contract();
		contract.beginContract = dataInizio;
		contract.expireContract = dataFine;
		contract.onCertificate = onCertificate;
		contract.person = person;
		contract.save();
		contract.setVacationPeriods();
		contract.save();
		ContractWorkingTimeType cwtt = new ContractWorkingTimeType();
		cwtt.beginDate = dataInizio;
		cwtt.endDate = dataFine;
		cwtt.workingTimeType = wtt;
		cwtt.contract = contract;
		cwtt.save();
		contract.save();
		flash.success("Il contratto per %s %s è stato correttamente salvato", person.name, person.surname);
		
		Persons.edit(person.id);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void modifyContract(Long contractId){
		Contract contract = Contract.findById(contractId);
		if(contract == null)
		{
			flash.error("Non è stato trovato nessun contratto con id %s per il dipendente ", contractId);
			Persons.list(null);
		}
		render(contract);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void deleteContract(Long contractId){
		Contract contract = Contract.findById(contractId);
		if(contract == null)
		{
			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}
		render(contract);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void deleteContractConfirmed(Long contractId){
		Contract contract = Contract.findById(contractId);
		if(contract == null)
		{
			flash.error("Contratto inesistente. Operazione annullata.");
			Persons.list(null);
		}
		contract.delete();
		flash.error("Contratto eliminato con successo.");
		Persons.edit(contract.person.id);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void updateContract(Contract contract, @Required LocalDate begin, @Valid LocalDate expire, @Valid LocalDate end, boolean onCertificate){
		
		//Controllo dei parametri
		if(contract == null)
		{
			flash.error("Contratto inesistente, operazione annullata");
			Persons.list(null);
		}
		if(begin==null){
			flash.error("Errore nel fornire il parametro data inizio contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(contract.person.id);
		}
		if(validation.hasError("expire"))
		{
			flash.error("Errore nel fornire il parametro data fine contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(contract.person.id);
		}
		if(validation.hasError("end"))
		{
			flash.error("Errore nel fornire il parametro data terminazione contratto. Inserire la data nel corretto formato aaaa-mm-gg");
			Persons.edit(contract.person.id);
		}

		contract.beginContract = begin;
		contract.expireContract = expire;
		contract.endContract = end;
		contract.onCertificate = onCertificate;
		contract.setVacationPeriods();
		contract.updateContractWorkingTimeType();
		//contract.buildContractYearRecap();
		contract.save();
		
		flash.success("Aggiornato contratto per il dipendente %s %s", contract.person.name, contract.person.surname);
		
		Persons.edit(contract.person.id);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void updateSourceContract(Long contractId)
	{
		Contract contract = Contract.findById(contractId);
		LocalDate initUse = new LocalDate(ConfGeneral.getFieldValue(ConfigurationFields.InitUseProgram.description, Security.getUser().person.office));
//		LocalDate initUse = ConfGeneral.getConfGeneral().initUseProgram;
		render(contract, initUse);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void saveSourceContract(Contract contract)
	{
		if(contract.sourceVacationLastYearUsed==null) contract.sourceVacationLastYearUsed=0;
		if(contract.sourceVacationCurrentYearUsed==null) contract.sourceVacationCurrentYearUsed=0;
		if(contract.sourcePermissionUsed==null) contract.sourcePermissionUsed=0;
		if(contract.sourceRemainingMinutesCurrentYear==null) contract.sourceRemainingMinutesCurrentYear=0;
		if(contract.sourceRemainingMinutesLastYear==null) contract.sourceRemainingMinutesLastYear=0;
		if(contract.sourceRecoveryDayUsed==null) contract.sourceRecoveryDayUsed=0;
		
		contract.save();
		//Ricalcolo dei riepiloghi
		contract.buildContractYearRecap();
		
		Persons.edit(contract.person.id);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void discard(){
		Persons.list(null);
	}

	/**
	 * cancella una persona dal database
	 * @param person
	 */
	@Check(Security.DELETE_PERSON)
	public static void deletePerson(Long personId){
		Person person = Person.findById(personId);
		//person.contactData.delete();
		//person.location.delete();
		//person.personShift.delete();
		//person.reperibility.delete();
		//person.save();
		person.delete();
		flash.success("La persona %s %s e' stata terminata.", person.surname, person.name);
		
		render("@Stampings.redirectToIndex");

	}
	

	/**
	 * 
	 * @param personId permette all'utente amministratore di cambiare la propria password.
	 */
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void changePassword(){
		User user = Security.getUser();
		notFoundIfNull(user);
		render(user);
	}
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void savePassword(@MinLength(5) @Required String vecchiaPassword, 
			@MinLength(5) @Required String nuovaPassword, @MinLength(5) @Required String confermaPassword){
		
		User user = User.find("SELECT u FROM User u where username = ? and password = ?", 
				Security.getUser().username, Hashing.md5().hashString(vecchiaPassword,  Charsets.UTF_8).toString()).first();
		if(user == null) {
			flash.error("Nessuna corrispondenza trovata fra utente e vecchia password inserita.");
			Persons.changePassword();
		}
		
		if(validation.hasErrors() || !nuovaPassword.equals(confermaPassword)) {
			flash.error("Tutti i campi devono essere valorizzati. "
					+ "La passord deve essere almeno lunga 5 caratteri. Operazione annullata.");
			Persons.changePassword();
		}
		
		notFoundIfNull(user);
		
		
				
		
		
		
		

		Codec codec = new Codec();
		
		user.password = codec.hexMD5(nuovaPassword);
		user.save();
		flash.success(Messages.get("passwordSuccessfullyChanged"));
		Persons.changePassword();
	}
	
	@Check(Security.DELETE_PERSON)
	public static void terminatePerson(Long personId){
		
		Person person = Person.findById(personId);
		render(person);		
	}
	
	@Check(Security.DELETE_PERSON)
	public static void terminateContract(Long personId){

		String end = params.get("endContract");

		if(end != null){
			LocalDate endContract = new LocalDate(end);
			Logger.debug("La data di terminazione anticipata è %s", endContract);

			Person person = Person.findById(params.get("personId", Long.class));
			Contract contract = person.getCurrentContract();
			contract.endContract = endContract;
			person.save();
			contract.save();		
			flash.success(String.format("Aggiunta data di terminazione anticipata del rapporto di lavoro per il dipendente %s %s ",
					person.name, person.surname));			
		}
		else{
			flash.error(String.format("Errore nel parametro passato dalla form, la data è nulla o non corretta"));
			
		}
		edit(personId);
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void insertChild(Long personId){
		Person person = Person.findById(personId);
		PersonChildren personChildren = new PersonChildren();
//		render(personChildren);
		render(person, personChildren);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void saveChild(){
		PersonChildren personChildren = new PersonChildren();
		Person person = Person.findById(params.get("personId", Long.class));
		personChildren.name = params.get("name");
		personChildren.surname = params.get("surname");
		personChildren.bornDate = new LocalDate(params.get("bornDate"));
		personChildren.person = person;
		personChildren.save();
		person.save();
		flash.success("Aggiunto %s %s nell'anagrafica dei figli di %s %s", personChildren.name, personChildren.surname, person.name, person.surname);
		Application.indexAdmin();
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void personChildrenList(Long personId){
		Person person = Person.findById(personId);
		render(person);
	}
	
}
