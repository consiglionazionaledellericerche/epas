/**
 * 
 */
package it.cnr.iit.epas;

import java.util.List;

import play.cache.Cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import models.Person;

/**
 * @author cristian
 *
 */
@Getter
public class MainMenu {

	public static final String PERSON_ID_CACHE_PREFIX = "personId.";
	@Setter
	private Long personId = null;

	private int year;
	private int month;
	
	@Setter
	private ActionMenuItem action = null;

	private List<Person> persons;
	
	public MainMenu(int year, int month) {
		this.year = year;
		this.month = month;
	}
	
	public MainMenu(Long personId, int year, int month, ActionMenuItem action) {
		this.personId = personId;
		this.year = year; this.month = month;
		this.action = action;
	}
	
	public MainMenu(Long personId, int year, int month, ActionMenuItem action, List<Person> persons) {
		this(personId, year, month, action);
		this.persons = persons;
	}
	
	public Person getPerson() {
		Person person = Cache.get(PERSON_ID_CACHE_PREFIX, Person.class);
		
		if (person == null) {
			person = Person.findById(personId);
			Cache.set(PERSON_ID_CACHE_PREFIX, person, "30mn");
		}
		
		return person;
	}
}
