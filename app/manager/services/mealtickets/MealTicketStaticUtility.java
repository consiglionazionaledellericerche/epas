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
import models.MealTicketCard;


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
    Optional<MealTicketCard> card = Optional.absent();
    int previousBlockLength;
    String previousCode;
    for (MealTicket mealTicket : mealTicketListOrdered) {

      if (interval.isPresent()
          && !DateUtility.isDateIntoInterval(mealTicket.getDate(), interval.get())) {
        continue;
      }

      //Primo buono pasto
      if (currentBlock == null) {
        previousMealTicket = mealTicket;
        if (mealTicket.getMealTicketCard() != null) {
          card = Optional.fromNullable(mealTicket.getMealTicketCard());
        }
        currentBlock = new BlockMealTicket(mealTicket.getBlock(), 
            mealTicket.getBlockType(), card);
        currentBlock.getMealTickets().add(mealTicket);
        currentBlock.setContract(mealTicket.getContract());
        continue;
      }

      //Stesso blocco

      previousBlockLength = previousMealTicket.getBlock().length();
      previousCode = 
          previousMealTicket.getCode().substring(previousBlockLength, 
              previousMealTicket.getCode().length());
      int actualBlockLength = mealTicket.getBlock().length();
      String actualCode = mealTicket.getCode().substring(actualBlockLength, 
          mealTicket.getCode().length());
      BigDecimal previous = new BigDecimal(previousCode).add(BigDecimal.ONE);  
      BigDecimal actual = new BigDecimal(actualCode);
      if (previousMealTicket.getBlock().equals(mealTicket.getBlock()) 
          && previous.compareTo(actual) == 0 
          && previousMealTicket.getContract().equals(mealTicket.getContract())
          && previousMealTicket.isReturned() == mealTicket.isReturned()) {
        currentBlock.getMealTickets().add(mealTicket);
      } else {
        //Nuovo blocco
        blockList.add(currentBlock);
        if (mealTicket.getMealTicketCard() != null) {
          card = Optional.fromNullable(mealTicket.getMealTicketCard());
        } else {
          card = Optional.absent();
        }
        currentBlock = new BlockMealTicket(mealTicket.getBlock(), 
            mealTicket.getBlockType(), card);
        currentBlock.getMealTickets().add(mealTicket);
        currentBlock.setContract(mealTicket.getContract());
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
        codeBlock = mealTicket.getBlock();
      }

      // Tutti i buoni della lista devono appartenere allo stesso blocco.
      Preconditions.checkArgument(codeBlock.equals(mealTicket.getBlock()));

      if (mealTicket.getNumber() >= first && mealTicket.getNumber() <= last) {
        if (!mealTicket.getContract().equals(contract)) {
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
