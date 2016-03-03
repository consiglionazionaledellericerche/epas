package manager.services.mealTickets;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import models.Contract;
import models.MealTicket;
import models.PersonDay;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Costruisce il recap dei buoni pasto.
 * @author alessandro
 */
public class MealTicketRecapBuilder {

  /**
   * Effettua il mapping ottimo fra i giorni in cui la persona ha maturato il diritto al buono pasto
   * ed il buono pasto, selezionando quello con scadenza più imminente rimasto in quella data. <br>
   * Intercetta i seguenti malfunzionalmenti. <br>
   * 1) MEAL_TICKET_RUN_OUT: in error finisce la data in cui sono esauriti i buoni pasto.<br>
   * 2) MEAL_TICKET_EXPIRED: in error finisce la data in cui si inizia a consumare buoni pasto 
   * scaduti.<br>
   * Se non ci sono errori viene salvato il numero di buoni pasto rimanenti.
   * @param contract contract
   * @param mealTicketInterval intervallo buoni pasto da considerare
   * @param personDays giorni da considerare
   * @param expireOrderedDesc buoni pasto ordinati per data scadenza decrescente
   * @param deliveryOrderedAsc buoni pasto ordinati per data consegna crescente
   * @param returnedDeliveryOrderedAsc buoni pasto riconsegnati ordinati per data consegna crescente
   * @return il recap
   */
  public MealTicketRecap buildMealTicketRecap(Contract contract, DateInterval mealTicketInterval, 
      List<PersonDay> personDays, List<MealTicket> expireOrderedAsc, 
      List<MealTicket> deliveryOrderedDesc, List<MealTicket> returnedDeliveryOrderedDesc) {

    MealTicketRecap mealTicketRecap = new MealTicketRecap();
    
    mealTicketRecap.setContract(contract);

    mealTicketRecap.setMealTicketInterval(
        new DateInterval(mealTicketInterval.getBegin(), LocalDate.now()));

    mealTicketRecap.setPersonDaysMealTickets(personDays);

    mealTicketRecap.setMealTicketsReceivedExpireOrderedAsc(expireOrderedAsc);
    mealTicketRecap.setMealTicketsReceivedDeliveryOrderedDesc(deliveryOrderedDesc);
    mealTicketRecap.setMealTicketReturnedDeliveryOrderDesc(returnedDeliveryOrderedDesc);

    if (contract.getSourceDateMealTicket() != null 
        && DateUtility.isDateIntoInterval(contract.getSourceDateMealTicket().plusDays(1), 
            mealTicketRecap.getMealTicketInterval())) {
      
      mealTicketRecap.setSourcedInInterval(contract.getSourceRemainingMealTicket());
    }
    
    int sourced = mealTicketRecap.getSourcedInInterval();
    
    //MAPPING
    //init lazy variable
    for (MealTicket mealTicket : mealTicketRecap.getMealTicketsReceivedExpireOrderedAsc()) {
      mealTicket.used = false;
    }
    //mapping
    for (int i = 0; i < mealTicketRecap.getPersonDaysMealTickets().size(); i++) {
      
      // scarto i giorni sourced
      if (sourced > 0) {
        sourced--;
        continue;
      }
      
      PersonDay currentPersonDay = mealTicketRecap.getPersonDaysMealTickets().get(i);

      if (mealTicketRecap.getMealTicketsReceivedExpireOrderedAsc().size() == i) {
        mealTicketRecap.setDateRunOut(currentPersonDay.date);
        return mealTicketRecap;
      }
      
      //Attribuire i buoni pasto ai personDay... e taggarli come usati. TODO: da verificare
      int index = i - mealTicketRecap.getSourcedInInterval();
      if (index > 0 && index < mealTicketRecap.getMealTicketsReceivedExpireOrderedAsc().size()) {
        MealTicket currentMealTicket = mealTicketRecap
            .getMealTicketsReceivedExpireOrderedAsc()
            .get(i - mealTicketRecap.getSourcedInInterval());

        if (currentPersonDay.date.isAfter(currentMealTicket.expireDate)) {
          mealTicketRecap.setDateExpire(currentPersonDay.date);
          //continue;
          //return;
        }

        currentPersonDay.mealTicketAssigned = currentMealTicket;
        currentMealTicket.used = true;
      }
    }

    mealTicketRecap.setRemaining(mealTicketRecap.getMealTicketsReceivedExpireOrderedAsc().size() 
        - mealTicketRecap.getPersonDaysMealTickets().size() + mealTicketRecap.getSourcedInInterval());

    return mealTicketRecap;
  }


}
