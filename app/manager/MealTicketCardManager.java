package manager;

import org.joda.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import models.MealTicketCard;
import models.Office;
import models.Person;

@Slf4j
public class MealTicketCardManager {

  public void saveMealTicketCard(MealTicketCard mealTicketCard, Person person, Office office) {
    
    MealTicketCard previous = person.actualMealTicketCard();
    if (previous != null) {
      log.info("Termino la validità della precedente tessera per {}", person.getFullname());
      previous.setActive(false);
      previous.setEndDate(LocalDate.now().minusDays(1));
      previous.save();
    }
    mealTicketCard.setActive(true);
    mealTicketCard.setPerson(person);
    mealTicketCard.setDeliveryOffice(office);
    mealTicketCard.setBeginDate(LocalDate.now());
    mealTicketCard.setEndDate(null);
    mealTicketCard.save();
    log.info("Aggiunta nuova tessera con identificativo {} a {}", 
        mealTicketCard.getNumber(), person.getFullname());
  }
  
  
}
