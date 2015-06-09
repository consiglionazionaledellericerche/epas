package dao;

import javax.inject.Inject;

import models.Person;

/**
 * Modella il Dto contenente le sole informazioni della persona
 * richieste dalla select nel template menu.
 * 
 * @author alessandro
 *
 */
public class PersonLite {
	
	public Long id;
	public String name;
	public String surname;
	
	public Person person = null;
	
	@Inject
	public PersonDao personDao;
	
	public PersonLite(Long id, String name, String surname) {
		this.id = id;
		this.name = name;
		this.surname = surname;
	}
}