package controllers;

import dao.PersonDao;

import models.Person;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import javax.inject.Inject;

/**
 * @author cristian
 */
@With(Resecure.class)
public class TestDrools extends Controller {

  @Inject
  private static PersonDao personDao;
  @Inject
  private static SecurityRules rules;

  public static void youMustBeLoggedIn() {
    renderText("You must be loggedIn to see this message");
  }

  public static void youMustBe(int number) {
    Person person = personDao.getPersonByNumber(number);
    notFoundIfNull(person);

    rules.checkIfPermitted(person.office);

    renderText("La matricola passata deve essere = 9802 per vedere questo messaggio");
  }
}
