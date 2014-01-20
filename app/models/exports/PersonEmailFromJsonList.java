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
public class PersonEmailFromJsonList {
	
	public List<Person> persons = new ArrayList<Person>();
	public LocalDate dateFrom;
	public LocalDate dateTo;

//	public PersonEmailFromJsonList(List<Person> persons, LocalDate dateFrom, LocalDate dateTo) {
//		this.persons = persons;
//		this.dateFrom = dateFrom;
//		this.dateTo = dateTo;
//	}
}
