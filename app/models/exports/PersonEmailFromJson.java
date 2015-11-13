package models.exports;

import models.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author dario
 *
 */
public class PersonEmailFromJson {
	
	public List<Person> persons = new ArrayList<Person>();


	public PersonEmailFromJson(List<Person> persons) {
		this.persons = persons;
	}

}
