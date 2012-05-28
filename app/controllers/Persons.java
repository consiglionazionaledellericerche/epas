package controllers;

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
		List<Person> personList = Person.find("order by surname").fetch();
		render(personList);
	}

	@Check(Security.INSERT_AND_UPDATE_PERSON)
	public static void form(Long id) {
		if(id == null) {
			render();
		}
		Person person = Person.findById(id);
		//FIXME: questo deve essere cambiato in person.contract
		Contract contract = person.getLastContract();
		
		if (contract == null) {
			contract = new Contract();
		}
		
		Location location = person.location;
		if (location == null) {
			location = new Location();
		}
		
		ContactData contactData = person.contactData;
		if (contactData == null) {
			contactData = new ContactData();
		}
		
		render(person, contract, location, contactData);
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
		
		Logger.debug("saving contract, beginContract = %s, endContract = %s", contract.beginContract, contract.endContract);
		contract.person = person;
		contract.save();
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
	 * @param request
	 * metodo che crea una nuova persona e la persiste sul db prendendo i dati in input dalla request passata come parametro
	 */
	public void createPerson2(){
		/**
		 * creo la persona
		 */
		Person person = new Person();
		person.name = params.get("nome");
		person.surname = params.get("nognome");
		person.bornDate = new Date(params.get("dataNascita"));
		person.password = params.get("password");
		person.number = new Integer(params.get("matricola"));
		person.qualification = new Integer(params.get("qualifica"));
		person.save();
		/**
		 * creo la locazione se almeno uno dei campi per la locazione non è nullo
		 */
		if(params.get("dipartimento")!=null || params.get("stanza")!=null || params.get("sede")!=null){
			Location location = new Location();
			location.person = person;
			location.department = params.get("dipartimento");
			location.room = params.get("stanza");
			location.headOffice = params.get("sede");
			location.save();
		}
		/**
		 * creo il contact data se almeno uno dei campi non è vuoto
		 */
		if(params.get("email")!=null || params.get("telephone")!=null ){
			ContactData contactData = new ContactData();
			contactData.person = person;
			contactData.email = params.get("email");
			contactData.telephone = params.get("telefono");
			contactData.save();
		}

		/**
		 * creo il contratto ammesso che sia stato valorizzato il campo
		 */
		if(request.params.get("contratto")!= null){
			Contract contract = new Contract();
			contract.person = person;
			contract.beginContract = new LocalDate(params.get("inizioContratto"));
			contract.endContract = new LocalDate(params.get("fineContratto"));
			contract.save();
		}
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


}
