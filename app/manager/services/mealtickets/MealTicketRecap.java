package manager.services.mealtickets;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import models.Contract;
import models.MealTicket;
import models.PersonDay;

import org.joda.time.LocalDate;

/**
 * Riepilogo buoni pasto di un contratto.
 *
 * @author alessandro
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PACKAGE)
public class MealTicketRecap {

  private Contract contract;

  private LocalDate dateExpire = null;
  private LocalDate dateRunOut = null;

  private List<PersonDay> personDaysMealTickets = Lists.newArrayList();
  private List<MealTicket> mealTicketReturnedDeliveryOrderDesc = Lists.newArrayList();
  private List<MealTicket> mealTicketsReceivedExpireOrderedAsc = Lists.newArrayList();
  private List<MealTicket> mealTicketsReceivedExpireOrderedAscPostInit = Lists.newArrayList();
  private List<MealTicket> mealTicketsReceivedDeliveryOrderedDesc = Lists.newArrayList();

  private int remaining = 0;

  private int sourcedInInterval = 0;

  private DateInterval mealTicketInterval = null;

  /**
   * Il residio negativo (cambiato di segno). Significativo solo se dateExpire != null.
   *
   * @return il residuo negativo.
   */
  public int getNegativeResidual() {
    return (this.sourcedInInterval + this.mealTicketsReceivedDeliveryOrderedDesc.size()
        - this.personDaysMealTickets.size()) * -1;
  }

  /**
   * Tutti i blocchi consegnati di un mealTicketRecap (dal più vecchio).
   */
  public List<BlockMealTicket> getBlockMealTicketReceived() {

    return MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        this.getMealTicketsReceivedExpireOrderedAsc(),
        Optional.fromNullable(this.getMealTicketInterval()));
  }

  /**
   * @return la lista dei blocchi consegnati precedentemente all'inizializzazione se presente.
   */
  public List<BlockMealTicket> getBlockPreviousInitialization() {

    if (this.contract.sourceDateMealTicket == null) {
      return Lists.newArrayList();
    }

    DateInterval interval = new DateInterval(this.contract.beginDate,
        this.contract.sourceDateMealTicket);

    return MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        this.getMealTicketsReceivedExpireOrderedAsc(),
        Optional.of(interval));
  }

  /**
   * Ritorna i blocchi di buoni pasto consegnati alla persona nell anno year (dal più vecchio).
   */
  public List<BlockMealTicket> getBlockMealTicketReceivedInYear(Integer year) {

    DateInterval yearInterval =
        new DateInterval(new LocalDate(year, 1, 1), new LocalDate(year, 12, 31));

    return MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        this.getMealTicketsReceivedExpireOrderedAsc(), Optional.fromNullable(yearInterval));
  }

  /**
   * I blocchi consegnati del contratto (da quelli consegnati per ultimi).
   *
   * @return blocchi.
   */
  public List<BlockMealTicket> getBlockMealTicketReceivedDeliveryDesc() {

    return MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        this.getMealTicketsReceivedDeliveryOrderedDesc(),
        Optional.fromNullable(this.getMealTicketInterval()));
  }

  /**
   * I blocchi riconsegnati del contratto (da quelli consegnati per ultimi).
   */
  public List<BlockMealTicket> getBlockMealTicketReturnedDeliveryDesc() {
    return MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        this.getMealTicketReturnedDeliveryOrderDesc(),
        Optional.fromNullable(this.getMealTicketInterval()));
  }


}
