/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager.services.mealtickets;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.gdata.util.common.base.Preconditions;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.math.BigDecimal;
import java.util.List;
import models.Contract;
import models.MealTicket;


/**
 * Contiene vari metodi di utilità per la gestione dei meal ticket.
 */
public class MealTicketStaticUtility {

  /**
   * Costruisce i blocchi di codici consecutivi a partire dalla lista ordinata di buoni pasto.
   *
   * @param mealTicketListOrdered una lista di buoni pasto ordinata per data di scadenza e per
   *                              codice blocco.
   * @param interval              intervallo da considerare.
   * @return i blocchi
   */
  public static List<BlockMealTicket> getBlockMealTicketFromOrderedList(
      List<MealTicket> mealTicketListOrdered, Optional<DateInterval> interval) {

    // TODO check della lista che sia effettivamente ordinata?

    List<BlockMealTicket> blockList = Lists.newArrayList();
    BlockMealTicket currentBlock = null;
    MealTicket previousMealTicket = null;
    int previousBlockLength;
    String previousCode;
    for (MealTicket mealTicket : mealTicketListOrdered) {

      if (interval.isPresent()
          && !DateUtility.isDateIntoInterval(mealTicket.date, interval.get())) {
        continue;
      }

      //Primo buono pasto
      if (currentBlock == null) {
        previousMealTicket = mealTicket;
        currentBlock = new BlockMealTicket(mealTicket.block, mealTicket.blockType);
        currentBlock.getMealTickets().add(mealTicket);
        currentBlock.setContract(mealTicket.contract);
        continue;
      }

      //Stesso blocco

      previousBlockLength = previousMealTicket.block.length();
      previousCode = 
          previousMealTicket.code.substring(previousBlockLength, previousMealTicket.code.length());
      int actualBlockLength = mealTicket.block.length();
      String actualCode = mealTicket.code.substring(actualBlockLength, mealTicket.code.length());
      BigDecimal previous = new BigDecimal(previousCode).add(BigDecimal.ONE);  
      BigDecimal actual = new BigDecimal(actualCode);
      if (previousMealTicket.block.equals(mealTicket.block) && previous.compareTo(actual) == 0 
          && previousMealTicket.contract.equals(mealTicket.contract)
          && previousMealTicket.returned == mealTicket.returned) {
        currentBlock.getMealTickets().add(mealTicket);
      } else {
        //Nuovo blocco
        blockList.add(currentBlock);
        currentBlock = new BlockMealTicket(mealTicket.block, mealTicket.blockType);
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
   *
   * @param blockMealTicketsOrdered i buoni pasto del blocco.
   * @param contract                contratto
   * @param first                   primo
   * @param last                    ultimo
   * @return la lista
   */
  public static List<MealTicket> blockPortion(List<MealTicket> blockMealTicketsOrdered,
      Contract contract, int first, int last) {

    List<MealTicket> mealTickets = Lists.newArrayList();
    String codeBlock = null;
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
