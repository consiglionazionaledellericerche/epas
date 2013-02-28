package controllers;

import it.cnr.iit.epas.ActionMenuItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.ContactData;
import models.Contract;
import models.InitializationTime;
import models.Location;
import models.Person;
import models.PersonDay;
import models.Qualification;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Persons extends Controller {

	public static final String USERNAME_SESSION_KEY = "username";

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void edit(Long personId){
		Person person = Person.findById(personId);
		/**
		 * questa data serve come default per far vedere durante la visualizzazione le eventuali reperibilità e straordinari di quella persona
		 * in quel mese in quell'anno
		 */
		LocalDate date = new LocalDate();
		List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ? order by con.beginContract", person).fetch();
		int weekDayAvailability;
		int holidaysAvailability;
		int daylightWorkingDaysOvertime;
		CompetenceCode cmpCode1 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "207").first();
		CompetenceCode cmpCode2 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "208").first();
		CompetenceCode cmpCode3 = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "S1").first();
		Logger.debug("Anno e mese: %s %s", date.getYear(), date.getMonthOfYear());
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
		InitializationTime initTime = InitializationTime.find("Select init from InitializationTime init where init.person = ?", person).first();
		
				
		render(person, contractList, weekDayAvailability, holidaysAvailability, daylightWorkingDaysOvertime, progressive, initTime);
	}


	@Check(Security.VIEW_PERSON_LIST)
	public static void list(){

		List<Person> personList = Person.find("Select p from Person p where p.name <> ? order by p.surname", "Admin").fetch();
		Logger.debug("La lista delle persone: %s", personList.toString());
		
		render(personList);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void insertPerson() {
		Person person = new Person();
		Contract contract = new Contract();
		Location location = new Location();
		ContactData contactData = new ContactData();
		InitializationTime initializationTime = new InitializationTime();
		render(person, contract, location, contactData, initializationTime);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void save() {
		if(validation.hasErrors()) {
			if(request.isAjax()) error("Invalid value");
			render("@insertPerson");
		}
		Person person = null;
		Location location = new Location();
		ContactData contactData = new ContactData();
		
		
		//FIXME: viene sempre creato un contratto nuovo ma ci dovrebbe essere la possibilità
		//di modificare un contratto esistente. Fare in questo o in un altro metodo?
		Contract contract = new Contract();		
		person = new Person();		
		Logger.debug("Saving person...");
		
		person.name = params.get("name");
		person.surname = params.get("surname");
		person.number = params.get("number", Integer.class);
		Qualification qual = Qualification.findById(new Long(params.get("person.qualification", Integer.class)));
		person.qualification = qual;
		person.save();
		
		Logger.debug("saving location, deparment = %s", location.department);
		location.department = params.get("department");
		location.headOffice = params.get("headOffice");
		location.room = params.get("room");
		location.person = person;
		location.save();
		Logger.debug("Saving contact data...");
		
		contactData.email = params.get("email");
		contactData.telephone = params.get("telephone");
		contactData.person = person;
		contactData.save();
		
		Date begin = params.get("beginContract", Date.class);
		Date end = params.get("expireContract", Date.class);
		LocalDate beginContract = new LocalDate(begin);
		LocalDate expireContract = new LocalDate(end);
				
		contract.beginContract = beginContract;
		contract.expireContract = expireContract;
		contract.person = person;
		contract.save();
		Logger.debug("saving contract, beginContract = %s, endContract = %s", contract.beginContract, contract.expireContract);
		
		if(params.get("minutesPastYear", Integer.class) != null || params.get("minutesCurrentYear", Integer.class) != null){
			InitializationTime initTime = new InitializationTime();
			initTime.person = person;
			if(params.get("minutesCurrentYear", Integer.class) != null)
				initTime.residualMinutesCurrentYear = params.get("minutesCurrentYear", Integer.class);
			else
				initTime.residualMinutesCurrentYear = 0;
			if(params.get("minutesPastYear", Integer.class) != null)
				initTime.residualMinutesPastYear = params.get("minutesPastYear", Integer.class);
			else
				initTime.residualMinutesPastYear = 0;
			initTime.date = new LocalDate();
			initTime.save();
			Logger.debug("Saving initialization time for %s %s with value %s minutes for past year and %s minutes for current year", 
					person.name, person.surname, initTime.residualMinutesPastYear, initTime.residualMinutesCurrentYear);
		}		
		
		flash.success(String.format("Inserita nuova persona in anagrafica: %s %s ",person.name, person.surname));
		Application.indexAdmin();
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void update(){
		Long personId = params.get("personId", Long.class);		
		
		Person person = Person.findById(personId);
		ContactData contactData = person.contactData;
		Location location = person.location;
		InitializationTime initTime = InitializationTime.find("Select init from InitializationTime init where init.person = ?", person).first();
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
			if(initTime.residualMinutesCurrentYear == null || initTime.residualMinutesCurrentYear != params.get("minutesCurrentYear", Integer.class))
				initTime.residualMinutesCurrentYear = params.get("minutesCurrentYear", Integer.class);
			if(initTime.residualMinutesPastYear == null || initTime.residualMinutesPastYear != params.get("minutesPastYear", Integer.class))
				initTime.residualMinutesPastYear = params.get("minutesPastYear", Integer.class);
			initTime.save();
		}
		else{
			initTime = new InitializationTime();
			if(params.get("minutesPastYear") != null)
				initTime.residualMinutesPastYear = params.get("minutesPastYear", Integer.class);
			if(params.get("minutesCurrentYear") != null)
				initTime.residualMinutesCurrentYear = params.get("minutesCurrentYear", Integer.class);
			initTime.save();
		}
		flash.success("Modificate informazioni di contatto o di locazione per l'utente %s %s", person.name, person.surname);
		Application.indexAdmin();
			
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void modifyContract(Long contractId){
		if(contractId != null){
			Contract contract = Contract.findById(contractId);
			if(contract == null){
				flash.error("Non è stato trovato nessun contratto con id %s per il dipendente ", contractId);
				Application.indexAdmin();
			}
			
			//Person person = Person.find("Select person from Person person where person.contract = ?", contract).first();
			render(contract);
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void updateContract(){
		Long contractId = params.get("contractId", Long.class);
		Contract contract = Contract.findById(contractId);
		if(!contract.beginContract.isEqual(params.get("beginContract", LocalDate.class)))
			contract.beginContract = params.get("beginContract", LocalDate.class);
		if(!contract.expireContract.isEqual(params.get("expireContract", LocalDate.class)))
			contract.expireContract = params.get("expireContract", LocalDate.class);
		if(contract.endContract == null && params.get("endContract", LocalDate.class) != null)
			contract.endContract = params.get("endContract", LocalDate.class);
		
		contract.save();
		flash.success("Aggiornato contratto per il dipendente %s %s", contract.person.name, contract.person.surname);
		Application.indexAdmin();
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void discard(){
		render("@list");
	}

	/**
	 * cancella una persona dal database
	 * @param person
	 */
	@Check(Security.DELETE_PERSON)
	public static void deletePerson(Long personId){
		Person person = Person.findById(personId);
		if(person != null){
			List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ?", person).fetch();
			contractList.clear();
			person.delete();
			flash.success(String.format("Eliminato %s %s", person.name, person.surname + "dall'anagrafica insieme alle sue info su locazione" +
					", contatti e lista contratti."));
			Application.indexAdmin();
		}
		else{
			flash.error("L'id passato come parametro non corrisponde a nessuna persona in anagrafica. Controllare id = %s", personId);
			Application.indexAdmin();
		}
	}
	

	/**
	 * 
	 * @param personId permette all'utente di cambiare la propria password.
	 */
	public static void changePassword(Long personId){
		Person person = Person.findById(personId);
		render(person);
	}
	
//	@Check(Security.INSERT_AND_UPDATE_PASSWORD)
	public static void savePassword(){
		Long personId = params.get("personId", Long.class);
		
		Person p = Person.findById(personId);
		String nuovaPassword = params.get("nuovaPassword");
		String confermaPassword = params.get("confermaPassword");
		if(nuovaPassword.equals(confermaPassword)){
			p.password = nuovaPassword;
			p.save();
			Logger.debug("Salvata la nuova password per %s %s", p.surname, p.name);
			flash.success("Aggiornata la password per %s %s", p.surname, p.name);
			Application.indexAdmin();
		}
		else{
			Logger.debug("Errore, password diverse per %s %s", p.surname, p.name);
			flash.error("Le due password non sono coincidenti!!!");
			
			changePassword(personId);
			render("@changePassword");
		}
			
		
	}
	
	@Check(Security.DELETE_PERSON)
	public static void terminatePerson(Long personId){
		
		Person person = Person.findById(personId);
		render(person);		
	}
	
	@Check(Security.DELETE_PERSON)
	public static void terminateContract(Long personId){
		Date end = params.get("endContract", Date.class);
		
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
		Application.indexAdmin();
	}
	
	
}
