package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.ContactData;
import models.Contract;
import models.Location;
import models.Person;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class Persons extends Controller {

	public static final String USERNAME_SESSION_KEY = "username";

	public static void insert(){
		render();
	}


	@Check(Security.VIEW_PERSON_LIST)
	public static void list(){
		List<Person> personList = Person.findAll();
		render(personList);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void form(Long id) {
		if(id == null) {
			render();
		}
		Person person = Person.findById(id);
		List<Contract> contracts = person.contracts;
		
		if (contracts == null) {
			contracts = new ArrayList<Contract>();
		}
		
		Location location = person.location;
		if (location == null) {
			location = new Location();
		}
		
		ContactData contactData = person.contactData;
		if (contactData == null) {
			contactData = new ContactData();
		}
		
		render(person, contracts, location, contactData);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void save(@Valid @Required Person person, @Valid Location location, @Valid ContactData contactData, @Valid Contract contract) {
		if(validation.hasErrors()) {
			if(request.isAjax()) error("Invalid value");
			render("@form", person, location, contactData, contract);
		}
		
		person.save();
		Logger.debug("saving location, deparment = %s", location.department);
		location.person = person;
		location.save();
		
		contactData.person = person;
		contactData.save();
		
		Logger.debug("saving contract, beginContract = %s, endContract = %s", contract.beginContract, contract.expireContract);
		contract.person = person;
		contract.save();
		list();
	}
	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void discard(){
		list();
	}
	

	/**
	 * metodo che crea una nuova persona e la persiste sul db coi soli campi obbligatori
	 */
	public void createBasicPerson(String nome, String cognome, String password){
		Person person = new Person();
		person.name = nome;
		person.surname = cognome;
		person.password = password;
		person.save();
	}


	/**
	 * 
	 * @param person
	 * aggiorna la entry sul db di quella persona 
	 */
	public void updateBasicPerson(Person person){
		Person p = Person.findById(person.id);
		if(p != null){
			//	request.params.
		}
	}

	/**
	 * cancella una persona dal database
	 * @param person
	 */
	public void deletePerson(Person person){
		if(person != null){
			person.delete();
			person.save();
		}
	}
	
	@Check(Security.INSERT_AND_UPDATE_PASSWORD)
	public static void changePassword(Person person){
//		String username = session.get(USERNAME_SESSION_KEY);
//		
//		Person p = Person.find("byUsername", username).first();
		render(person);
	}
	
	@Check(Security.INSERT_AND_UPDATE_PASSWORD)
	public static void savePassword(Person person){
		Person p = Person.findById(person.id);
		p.password = params.get("nuovaPassword");
		p.save();
	}


}
