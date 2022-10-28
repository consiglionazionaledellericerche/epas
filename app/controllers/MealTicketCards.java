package controllers;

import com.google.common.base.Optional;
import common.security.SecurityRules;
import dao.MealTicketCardDao;
import dao.OfficeDao;
import dao.PersonDao;
import java.util.List;
import javax.inject.Inject;
import manager.MealTicketCardManager;
import models.MealTicketCard;
import models.Office;
import models.Person;
import models.User;
import org.joda.time.LocalDate;
import play.data.validation.Validation;
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
  @Inject
  private static MealTicketCardDao mealTicketCardDao;

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
    if (mealTicketCard.getDeliveryDate() == null) {
      Validation.addError("mealTicketCard.deliveryDate", "La data deve essere valorizzata!!!");
    }
    if (mealTicketCard.getNumber() < 1) {
      Validation.addError("mealTicketCard.number", 
          "Il numero della card deve essere maggiore di zero!!!");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      person = personDao.getPersonById(person.id);
      mealTicketCard.setPerson(person);
      mealTicketCard.setDeliveryOffice(office);
      render("@addNewCard", mealTicketCard, person);
    }
    mealTicketCardManager.saveMealTicketCard(mealTicketCard, person, office);

    flash.success("Associata nuova tessera a %s", person.getFullname());

    MealTicketCards.mealTicketCards(person.getOffice().id);
  }

  /**
   * Metodo di rimozione di una tessera elettronica.
   *
   * @param mealTicketCardId l'identificativo della tessera da rimuovere
   */
  public static void deleteCard(Long mealTicketCardId) {
    java.util.Optional<MealTicketCard> mealTicketCard = mealTicketCardDao
        .getMealTicketCardById(mealTicketCardId);
    if (mealTicketCard.isPresent()) {
      mealTicketCard.get().delete();
      flash.success("Tessera correttamente rimossa");      
    } else {
      flash.error("Nessuna tessera corrispondente all'id selezionato. Verificare.");
    }
    MealTicketCards.mealTicketCards(mealTicketCard.get().getPerson().getOffice().id);
  }
}
