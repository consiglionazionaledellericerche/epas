package models.exports;

import models.Person;

import org.joda.time.LocalDate;

public class AbsenceFromClient {

	public Person person;
	public LocalDate date;
	public String code;
	
	public int inizio;
	public int fine;
	public int durata;
	
	public String tipog;
}
