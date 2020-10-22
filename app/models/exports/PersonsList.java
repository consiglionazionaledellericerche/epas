package models.exports;

import java.util.ArrayList;
import java.util.List;
import models.Person;

/**
 * La lista di persone.
 * @author arianna
 */
public class PersonsList {

  public List<Person> persons = new ArrayList<Person>();

  public PersonsList(List<Person> persons) {
    this.persons = persons;
  }
}
