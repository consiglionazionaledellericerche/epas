/**
 *
 */
package models.exports;

import models.Person;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arianna
 */
public class PersonsList {

  public List<Person> persons = new ArrayList<Person>();

  public PersonsList(List<Person> persons) {
    this.persons = persons;
  }
}
