package manager.services.mealtickets;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import lombok.extern.slf4j.Slf4j;
import models.Contract;
import models.MealTicket;
import models.PersonDay;

import org.joda.time.LocalDate;

import java.util.List;

/**
 * Costruisce il recap dei buoni pasto.
 * @author alessandro
 */
@Slf4j
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
   * @param expireOrderedAsc buoni pasto ordinati per data scadenza crescente
   * @param expireOrderedAscPostInit buoni pasto validi ordinati per data scadenza crescente
   * @param deliveryOrderedDesc buoni pasto ordinati per data consegna descrescente
   * @param returnedDeliveryOrderedDesc buoni pasto riconsegnati ordinati per data consegna asc
   * @return il recap
   */
  public MealTicketRecap buildMealTicketRecap(Contract contract, DateInterval mealTicketInterval, 
      List<PersonDay> personDays, 
      List<MealTicket> expireOrderedAsc, List<MealTicket> expireOrderedAscPostInit, 
      List<MealTicket> deliveryOrderedDesc, List<MealTicket> returnedDeliveryOrderedDesc) {

    MealTicketRecap mealTicketRecap = new MealTicketRecap();
    
    mealTicketRecap.setContract(contract);

    mealTicketRecap.setMealTicketInterval(
        new DateInterval(mealTicketInterval.getBegin(), LocalDate.now()));

    mealTicketRecap.setPersonDaysMealTickets(personDays);

    mealTicketRecap.setMealTicketsReceivedExpireOrderedAsc(expireOrderedAsc);
    mealTicketRecap.setMealTicketsReceivedExpireOrderedAscPostInit(expireOrderedAscPostInit);
    mealTicketRecap.setMealTicketsReceivedDeliveryOrderedDesc(deliveryOrderedDesc);
    mealTicketRecap.setMealTicketReturnedDeliveryOrderDesc(returnedDeliveryOrderedDesc);

    if (contract.getSourceDateMealTicket() != null 
        && DateUtility.isDateIntoInterval(contract.getSourceDateMealTicket().plusDays(1), 
            mealTicketRecap.getMealTicketInterval())) {
      
      mealTicketRecap.setSourcedInInterval(contract.getSourceRemainingMealTicket());
    }
    
    //Imposto i rimanenti
    mealTicketRecap.setRemaining(mealTicketRecap
        .getMealTicketsReceivedExpireOrderedAscPostInit().size() 
        - mealTicketRecap.getPersonDaysMealTickets().size() 
        + mealTicketRecap.getSourcedInInterval());
    
    //Matching dei buoni coi giorni

    //init lazy variable buoni non utilizzati
    for (MealTicket mealTicket : mealTicketRecap.getMealTicketsReceivedExpireOrderedAsc()) {
      mealTicket.used = false;
    }
    
    //se non ci sono giorni nessun buono è stato utilizzato
    if (mealTicketRecap.getPersonDaysMealTickets().isEmpty()) {
      return mealTicketRecap;
    }
    
    int sourced = mealTicketRecap.getSourcedInInterval();
    int nextTicketToAssign = 0;
    
    for (PersonDay personDay : mealTicketRecap.getPersonDaysMealTickets()) {
      
      //Assegno al personDay tutti i buoni da inizializzazione
      if (sourced > 0) {
        sourced--;
        continue;
      }
      
      //Non ho altri buoni pasto da assegnare. Run Out.
      if (nextTicketToAssign 
          >= mealTicketRecap.getMealTicketsReceivedExpireOrderedAscPostInit().size()) {
        mealTicketRecap.setDateRunOut(personDay.date);
        return mealTicketRecap;
      }
      
      MealTicket mealTicket = mealTicketRecap.getMealTicketsReceivedExpireOrderedAscPostInit()
          .get(nextTicketToAssign);
      nextTicketToAssign++;
      
      //Mi salvo la data in cui ho iniziato a consumare buoni pasto scaduti 
      if (mealTicketRecap.getDateExpire() == null && 
          personDay.date.isAfter(mealTicket.expireDate)) {
        mealTicketRecap.setDateExpire(personDay.date);
      }
      
      personDay.mealTicketAssigned = mealTicket;
      mealTicket.used = true;
    }

    return mealTicketRecap;
  }


}
