/**
 * 
 */
package controllers;

import java.util.List;

import models.Person;

import play.db.jpa.JPA;
import play.mvc.Controller;

/**
 * @author cristian
 *
 */
public class Reperibility extends Controller {

	public static void personList() {
		List<Person> persons = Person.find("SELECT p FROM Person p JOIN p.reperibility").fetch();
		render(persons);
	}
}
