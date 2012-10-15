package controllers;

import it.cnr.iit.epas.ActionMenuItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.ContactData;
import models.Contract;
import models.Location;
import models.Person;
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
		List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ? order by con.beginContract", person).fetch();
		
		render(person, contractList);
	}


	@Check(Security.VIEW_PERSON_LIST)
	public static void list(){
		List<Person> personList = Person.find("Select p from Person p where p.name <> ?", "Admin").fetch();
		Logger.debug("La lista delle persone: %s", personList.toString());
		//List<Person> personList = Person.findAll();
		
		render(personList);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void insertPerson() {
		Person person = new Person();
		Contract contract = new Contract();
		Location location = new Location();
		ContactData contactData = new ContactData();
		render(person, contract, location, contactData);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void save(Long personId) {
		if(validation.hasErrors()) {
			if(request.isAjax()) error("Invalid value");
			render("@insertPerson");
		}
		Person person = null;
		Location location = null;
		ContactData contactData = null;
		Contract contract = null;
		if(personId == null){
			person = new Person();
			location = new Location();
			contactData = new ContactData();
			contract = new Contract();
		}
		else{
			person = Person.findById(personId);
		}
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
		
		LocalDate beginContract = params.get("beginContract", LocalDate.class);
		contract.beginContract = beginContract;
		LocalDate expireContract = params.get("expireContract", LocalDate.class);
		contract.expireContract = expireContract;
		contract.person = person;
		contract.save();
		Logger.debug("saving contract, beginContract = %s, endContract = %s", contract.beginContract, contract.expireContract);
		
		flash.success(String.format("Inserita nuova persona in anagrafica: %s %s ",person.name, person.surname));
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
	public void deletePerson(Long personId){
		Person person = Person.findById(personId);
		if(person != null){
			Location location = person.location;
			location.delete();
			ContactData contactData = person.contactData;
			contactData.delete();
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
	
	@Check(Security.INSERT_AND_UPDATE_PASSWORD)
	public static void changePassword(Long personId){
		Person person = Person.findById(personId);
		render(person);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PASSWORD)
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
	

}
