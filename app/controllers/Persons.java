package controllers;

import java.util.Date;
import java.util.List;

import org.joda.time.LocalDate;

import models.ContactData;
import models.Contract;
import models.Location;
import models.MonthRecap;
import models.Permission;
import models.Person;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import play.mvc.Http.Request;

//@With(Secure.class)
//@Check("administrator")
public class Persons extends Controller{
	
	public static final String USERNAME_SESSION_KEY = "username";

	//@Check("administrator")
	public static void insert(){
		render();
	}
	

	public static void update(){
//		String username = session.get(USERNAME_SESSION_KEY);
//		if(username != null){			
//			Person person = Person.find("byUsername", username).first();
//			for(Permission p : person.permissions){
//				if(p.description.equals("administrator")){
					List<Person> personList = Person.find("Select per from Person per order by per.surname").fetch();
					
					render(personList);
//				}
//				else{
//					flash.error("This person cannot access this area");
//			        Application.index();
//				}
//			}
			
//		}
		
	}
	
	public static void updatePerson(){
//		String username = session.get(USERNAME_SESSION_KEY);
//		if(username != null){			
//		Person person = (Person) renderArgs.get("person");
//			person = Person.find("byUsername", ).first();
//			for(Permission p : person.permissions){
//				if(p.description.equals("administrator")){
			Person person = params.get("person",Person.class);
			person.save();
			if(person != null){
				List<Contract> contractList = Contract.find("Select con from Contract con where con.person = ? " +
						"order by con.beginContract", person).fetch();
				render(person, contractList);
			}
					
				}
//				else{
//					flash.error("This person cannot access this area");
//			        Application.index();
//				}
//			}
//		}
//	}
	
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
	public void createPerson(){
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
