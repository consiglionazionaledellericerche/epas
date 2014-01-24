package models.exports;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Person;

import org.joda.time.LocalDate;

/**
 * 
 * @author dario
 *
 */
public class PersonEmailFromJson {
	
	public List<Person> persons = new ArrayList<Person>();
//	public String dateFrom;
//	public String dateTo;

	public PersonEmailFromJson(List<Person> persons) {
		this.persons = persons;
//		this.dateFrom = dateFrom;
//		this.dateTo = dateTo;
	}

}
