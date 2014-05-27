/**
 * 
 */
package controllers;

import javax.inject.Inject;

import models.Person;
import dao.PersonDao;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

/**
 * @author cristian
 *
 */
@With(Resecure.class)
public class TestDrools extends Controller {

	@Inject
	static SecurityRules rules;
	
	public static void youMustBeLoggedIn() {
		renderText("You must be loggedIn to see this message");
	}
	
	public static void youMustBe(int number) {
		Person person = Person.findByNumber(number);
		notFoundIfNull(person);
		rules.checkIfPermitted(person);
		
		renderText("La matricola passata deve essere = 9802 per vedere questo messaggio");
	}
}
