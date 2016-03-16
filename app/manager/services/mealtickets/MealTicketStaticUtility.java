package manager.services.mealtickets;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import lombok.extern.slf4j.Slf4j;

import models.Contract;
import models.MealTicket;

import java.util.List;

public class MealTicketStaticUtility {

  /**
   * Costruisce i blocchi di codici consecutivi a partire dalla lista ordinata di buoni pasto.
   * @param mealTicketListOrdered una lista di buoni pasto ordinata
   *     per data di scadenza e per codice blocco.
   * @param interval intervallo da considerare.
   * @return i blocchi
   */
  public static List<BlockMealTicket> getBlockMealTicketFromOrderedList(
          List<MealTicket> mealTicketListOrdered, Optional<DateInterval> interval) {

    // TODO check della lista che sia effettivamente ordinata?
    
    List<BlockMealTicket> blockList = Lists.newArrayList();
    BlockMealTicket currentBlock = null;
    MealTicket previousMealTicket = null;

    for (MealTicket mealTicket : mealTicketListOrdered) {

      if (interval.isPresent()
          && !DateUtility.isDateIntoInterval(mealTicket.date, interval.get())) {
        continue;
      }
      
      //Primo buono pasto
      if (currentBlock == null) {
        previousMealTicket = mealTicket;
        currentBlock = new BlockMealTicket(mealTicket.block);
        currentBlock.getMealTickets().add(mealTicket);
        currentBlock.setContract(mealTicket.contract);
        continue;
      }

      //Stesso blocco
      Long previous = Long.parseLong(previousMealTicket.code) + 1;
      Long actual = Long.parseLong(mealTicket.code);
      if (previous.equals(actual) && previousMealTicket.contract.equals(mealTicket.contract) 
          && previousMealTicket.returned == mealTicket.returned) {
        currentBlock.getMealTickets().add(mealTicket);
      } else {
        //Nuovo blocco
        blockList.add(currentBlock);
        currentBlock = new BlockMealTicket(mealTicket.block);
        currentBlock.getMealTickets().add(mealTicket);
        currentBlock.setContract(mealTicket.contract);
      }
      previousMealTicket = mealTicket;
    }

    if (currentBlock != null) {
      blockList.add(currentBlock);
    }
    return blockList;
  }
  
  /**
   * La porzione di blocco associato al contratto.
   * @param blockMealTicketsOrdered i buoni pasto del blocco.
   * @param contract contratto 
   * @param first primo 
   * @param last ultimo
   * @return la lista
   */
  public static List<MealTicket> blockPortion(List<MealTicket> blockMealTicketsOrdered,
      Contract contract, int first, int last) {
    
    List<MealTicket> mealTickets = Lists.newArrayList();
    Integer codeBlock = null;
    //Controllo di consistenza.
    for (MealTicket mealTicket : blockMealTicketsOrdered) {
      if (codeBlock == null) {
        codeBlock = mealTicket.block;
      }
      
      // Tutti i buoni della lista devono appartenere allo stesso blocco.
      Preconditions.checkArgument(codeBlock.equals(mealTicket.block));
          
      if (mealTicket.number >= first && mealTicket.number <= last) {
        if (!mealTicket.contract.equals(contract)) {
          // un buono nell'intervallo non appartiene al contratto effettivo!!! 
          //non si dovrebbe verificare.
          throw new IllegalStateException();
        }
        mealTickets.add(mealTicket);
      }
    }
    return mealTickets;
  }
  
}
