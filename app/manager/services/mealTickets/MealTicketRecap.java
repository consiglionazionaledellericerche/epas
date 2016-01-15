package manager.services.mealTickets;

import com.google.common.collect.Lists;

import it.cnr.iit.epas.DateInterval;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import models.Contract;
import models.MealTicket;
import models.PersonDay;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Riepilogo buoni pasto di un contratto.
 * @author alessandro
 */
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PACKAGE)
public class MealTicketRecap {
  
  private Contract contract;
  
  private LocalDate dateExpire = null;
  private LocalDate dateRunOut = null;

  private List<PersonDay> personDaysMealTickets = Lists.newArrayList();
  private List<MealTicket> mealTicketsReceivedExpireOrderedAsc = Lists.newArrayList();
  
  private List<MealTicket> mealTicketsReceivedDeliveryOrderedDesc = Lists.newArrayList();
  
  private int remaining = 0;
  
  private int sourcedInInterval = 0;

  private DateInterval mealTicketInterval = null;
}
