package manager.services.mealTickets;

import com.google.common.collect.Lists;

import models.Contract;
import models.MealTicket;
import models.Person;

import org.joda.time.LocalDate;

import java.util.List;

public class BlockMealTicket {

  public Contract contract;
  public Integer codeBlock;
  public List<MealTicket> mealTickets;

  public BlockMealTicket(Integer codeBlock) {

    this.codeBlock = codeBlock;
    this.mealTickets = Lists.newArrayList();
  }

  /**
   * La dimensione del blocchetto.
   */
  public Integer getDimBlock() {
    return this.mealTickets.size();
  }

  /**
   * Il numero di buoni rimanenti all'interno del blocchetto. N.B. Il metodo ritorna un valore
   * valido solo se la variabile lazy used è valorizzata per tutti i buoni pasto del blocchetto.
   * (Per valorizzarla occorre passare dalla MealTicketRecap.build())
   */
  public Integer getRemaining() {
    Integer consumed = this.getConsumed();
    if (consumed == null) {
      return null;
    }
    return this.getDimBlock() - this.getConsumed();
  }

  /**
   * Il numero di buoni consumati all'interno del blocchetto. N.B. Il metodo ritorna un valore
   * valido solo se la variabile lazy used è valorizzata per tutti i buoni pasto del blocchetto.
   * (Per valorizzarla occorre passare dalla MealTicketRecap.build())
   */
  public Integer getConsumed() {
    Integer count = 0;
    for (MealTicket mealTicket : this.mealTickets) {
      if (mealTicket.used == null) {
        return null;
      }
      if (mealTicket.used) {
        count++;
      }
    }
    return count;
  }

  /**
   * Il giorno di attribuzione del blocco.
   */
  public LocalDate getDate() {
    if (this.getDimBlock() > 0) {
      return this.mealTickets.get(0).date;
    }
    return null;
  }

  /**
   * @return il giorno di scadenza dei buoni appartenenti al blocchetto.
   */
  public LocalDate getExpireDate() {
    if (this.getDimBlock() > 0) {
      return this.mealTickets.get(0).expireDate;
    }
    return null;
  }

  /**
   * @return l'amministratore assegnatario del blocchetto.
   */
  public Person getAdmin() {
    if (this.getDimBlock() > 0) {
      return this.mealTickets.get(0).admin;
    }
    return null;
  }

  /**
   * @return la data di consegna (inserimento ePAS) del blocchetto.
   */
  public LocalDate getReceivedDate() {
    if (this.getDimBlock() > 0) {
      return this.mealTickets.get(0).date;
    }
    return null;
  }

  /**
   * @return il primo number del blocco.
   */
  public int getFirst() {
    return this.mealTickets.get(0).number;
  }

  /**
   * @return l'ultimo number del blocco.
   */
  public int getLast() {
    return this.mealTickets.get(this.mealTickets.size() - 1).number;
  }

}
