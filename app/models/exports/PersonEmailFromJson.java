package models.exports;

import java.util.ArrayList;
import java.util.List;
import models.Person;

/**
 * Lista delle persone via mail dal json.
 * @author dario
 */
public class PersonEmailFromJson {

  public List<Person> persons = new ArrayList<Person>();


  public PersonEmailFromJson(List<Person> persons) {
    this.persons = persons;
  }

}
