package manager;

import org.joda.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import models.MealTicketCard;
import models.Office;
import models.Person;

@Slf4j
public class MealTicketCardManager {

  public String saveMealTicketCard(MealTicketCard mealTicketCard, Person person, Office office) {
    String s = "";
    //TODO: inserire qui la lista delle precedenti tessere da disattivare e da "chiudere" inserendo
    // la endDate
    mealTicketCard.setActive(true);
    mealTicketCard.setPerson(person);
    mealTicketCard.setDeliveryOffice(office);
    mealTicketCard.setBeginDate(LocalDate.now());
    mealTicketCard.setEndDate(null);
    mealTicketCard.save();
    return s;
  }
}
