/**
 * 
 */
package it.cnr.iit.epas;

import java.util.List;

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
	
}
