package controllers;

import com.google.common.base.Optional;
import common.security.SecurityRules;
import dao.OfficeDao;
import dao.PersonDao;
import manager.MealTicketCardManager;
import java.util.List;
import javax.inject.Inject;
import models.MealTicketCard;
import models.Office;
import models.Person;
import models.User;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller di gestione delle card dei buoni elettronici.
 *
 * @author dario
 *
 */
@With({Resecure.class})
public class MealTicketCards extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static MealTicketCardManager mealTicketCardManager;
  
  /**
   * Ritorna la lista delle persone per verificare le associazioni con le card dei buoni 
   * elettronici.
   *
   * @param officeId l'id della sede di cui cercare la lista di persone
   */
  public static void mealTicketCards(Long officeId) {
    
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    List<Person> personList = personDao.activeWithNumber(office);
    render(office, personList);
  }
  
  /**
   * Apre la form di inserimento di una nuova tessera elettronica.
   *
   * @param personId l'identificativo della persona per cui inserire una nuova tessera
   */
  public static void addNewCard(Long personId) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
    Optional<User> user = Security.getUser();
    MealTicketCard mealTicketCard = new MealTicketCard();
    mealTicketCard.setPerson(person);
    mealTicketCard.setDeliveryOffice(user.get().getPerson().getOffice());
    render(person, mealTicketCard);
  }
  
  /**
   * Salva la nuova tessera elettronica.
   *
   * @param mealTicketCard la tessera da salvare
   * @param person la persona cui associarla
   * @param office la sede proprietaria che associa la tessea
   */
  public static void saveNewCard(MealTicketCard mealTicketCard, Person person, Office office) {
    notFoundIfNull(person);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    String result = mealTicketCardManager.saveMealTicketCard(mealTicketCard, person, office);
    if (result.isEmpty()) {
      flash.success("Associata nuova tessera a %s", person.getFullname());
    } else {
      flash.error("Qualcosa è andato storto...invia una segnalazione ai responsabili di ePAS");
    }    
    MealTicketCards.mealTicketCards(person.getOffice().id);
  }
  
  public static void deleteCard(Long mealTicketCardId) {
    //TODO: eliminare solo se non esistono altre tessere!!!
  }
}
