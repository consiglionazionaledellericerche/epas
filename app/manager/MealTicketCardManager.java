/*
 * Copyright (C) 2022  Consiglio Nazionale delle Ricerche
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

package manager;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.joda.time.LocalDate;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import dao.ContractDao;
import dao.MealTicketDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import lombok.extern.slf4j.Slf4j;
import models.Contract;
import models.MealTicket;
import models.MealTicketCard;
import models.Office;
import models.Person;
import models.User;
import models.enumerate.BlockType;

@Slf4j
public class MealTicketCardManager {

  private MealTicketDao mealTicketDao;
  private IWrapperFactory wrapperFactory;
  private ContractDao contractDao;
  
  @Inject
  public MealTicketCardManager(MealTicketDao mealTicketDao, IWrapperFactory wrapperFactory,
      ContractDao contractDao) {
    this.mealTicketDao = mealTicketDao;
    this.wrapperFactory = wrapperFactory;
    this.contractDao = contractDao;
  }
  
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
  
  /**
   * Assegna i buoni pasto elettronici inseriti su epas finora alla scheda attuale assegnata al dipendente.
   *
   * @param card l'attuale scheda elettronica per i buoni pasto elettronici
   * @return true se i buoni sono stati assegnati correttamente, false altrimenti.
   */
  public boolean assignOldElectronicMealTicketsToCard(MealTicketCard card) {
    Person person = card.getPerson();
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    Optional<Contract> actualContract = wrPerson.getCurrentContract();
    if (!actualContract.isPresent()) {
      return false;
    }
    List<MealTicket> electronicMealTickets = mealTicketDao
        .getUnassignedElectronicMealTickets(actualContract.get());
    for (MealTicket mealTicket : electronicMealTickets) {
      mealTicket.setMealTicketCard(card);
      mealTicket.save();
    }
    return true;    
  }
  
  public void saveElectronicMealTicketBlock(MealTicketCard card, LocalDate deliveryDate, 
      Integer tickets, User admin, Person person, LocalDate expireDate, Office office) {
    String block = "" + card.getNumber() + deliveryDate.getYear() + deliveryDate.getMonthOfYear();
    for (Integer i = 1; i <= tickets; i++) {
      MealTicket mealTicket = new MealTicket();
      mealTicket.setBlock(block);
      mealTicket.setBlockType(BlockType.electronic);
      mealTicket.setContract(contractDao.getContract(deliveryDate, person));
      mealTicket.setMealTicketCard(card);
      mealTicket.setAdmin(admin.getPerson());
      mealTicket.setDate(deliveryDate);
      mealTicket.setExpireDate(expireDate);
      mealTicket.setOffice(office);
      mealTicket.setNumber(i);
      if (i < 10) {
        mealTicket.setCode(block + "0" + i);
      } else {
        mealTicket.setCode("" + block + i);
      }
      mealTicket.save();
    }
  }
  
  public Map<LocalDate, List<MealTicket>> recapElectronicDelivery(Person person) {
    Map<LocalDate, List<MealTicket>> map = Maps.newHashMap();
    return null;
  }
}
