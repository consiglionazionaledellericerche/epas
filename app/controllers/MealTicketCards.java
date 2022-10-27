package controllers;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import common.security.SecurityRules;
import dao.OfficeDao;
import dao.PersonDao;
import models.Office;
import models.Person;
import play.mvc.Controller;
import play.mvc.With;

@With({Resecure.class})
public class MealTicketCards extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static PersonDao personDao;
  
  public static void mealTicketCards(Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    List<Person> personList = personDao.activeWithNumber(office);
    render(office, personList);
  }
  
  public static void addNewCard(Long personId) {
    
  }
}
