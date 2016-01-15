package manager.services.mealTickets.wrapper;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.CompetenceDao;
import dao.MealTicketDao;
import dao.OfficeDao;

import it.cnr.iit.epas.DateInterval;

import lombok.Getter;

import manager.services.mealTickets.BlockMealTicket;
import manager.services.mealTickets.IMealTicketsService;
import manager.services.mealTickets.MealTicketRecap;
import manager.services.mealTickets.MealTicketsServiceImpl.MealTicketOrder;

import models.Competence;
import models.CompetenceCode;
import models.MealTicket;
import models.Office;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * @author alessandro
 */
public class WrapperMealTicketRecap implements IWrapperMealTicketRecap {

  @Getter
  private final MealTicketRecap recap;
  
  private final IMealTicketsService mealTicketsService;
  
  @Inject
  WrapperMealTicketRecap(
      @Assisted MealTicketRecap recap, IMealTicketsService mealTicketsService) {
    this.recap = recap;
    this.mealTicketsService = mealTicketsService;
  }

  /**
   * Tutti i blocchi consegnati di un mealTicketRecap (dal più vecchio).
   * @param mealTicketRecap
   * @return
   */
  public List<BlockMealTicket> getBlockMealTicketReceived() {

    return mealTicketsService.getBlockMealTicketReceivedIntoInterval(
        recap.getMealTicketsReceivedExpireOrderedAsc(), 
        Optional.fromNullable(recap.getMealTicketInterval()));
  }

  /**
   * Ritorna i blocchi di buoni pasto consegnati alla persona nell anno year (dal più vecchio).
   */
  public List<BlockMealTicket> getBlockMealTicketReceivedInYear(Integer year) {

    DateInterval yearInterval = 
        new DateInterval(new LocalDate(year, 1, 1), new LocalDate(year, 12, 31));

    return mealTicketsService.getBlockMealTicketReceivedIntoInterval(
        recap.getMealTicketsReceivedExpireOrderedAsc(), Optional.fromNullable(yearInterval));
  }

  /**
   * I blocchi consegnati del contratto (da quelli consegnati per ultimi).
   * @return blocchi.
   */
  public List<BlockMealTicket> getBlockMealTicketReceivedDeliveryDesc() {

    return mealTicketsService.getBlockMealTicketReceivedIntoInterval(
        recap.getMealTicketsReceivedDeliveryOrderedDesc(), 
        Optional.fromNullable(recap.getMealTicketInterval()));
  }

}
