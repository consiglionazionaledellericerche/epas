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
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import dao.MealTicketDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import javax.inject.Inject;
import manager.ConsistencyManager;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import models.Configuration;
import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.Office;
import models.PersonDay;
import models.dto.MealTicketComposition;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

/**
 * Implementazione di produzione del servizio meal tickets.
 *
 * @author Alessandro Martelli
 */
public class MealTicketsServiceImpl implements IMealTicketsService {

  @Inject
  private PersonDao personDao;
  private MealTicketDao mealTicketDao;
  private IWrapperFactory wrapperFactory;
  private ConsistencyManager consistencyManager;
  private MealTicketRecapBuilder mealTicketRecapBuilder;
  private ConfigurationManager configurationManager;

  /**
   * Costrutture.
   *
   * @param personDao            personDao
   * @param mealTicketDao        mealTicketDao
   * @param configurationManager configurationManager
   * @param consistencyManager   consistencyManager
   * @param wrapperFactory       wrapperFactory
   */
  @Inject
  public MealTicketsServiceImpl(PersonDao personDao,
      MealTicketDao mealTicketDao,
      ConsistencyManager consistencyManager,
      ConfigurationManager configurationManager,
      MealTicketRecapBuilder mealTicketRecapBuilder,
      IWrapperFactory wrapperFactory) {

    this.personDao = personDao;
    this.mealTicketDao = mealTicketDao;
    this.consistencyManager = consistencyManager;
    this.configurationManager = configurationManager;
    this.mealTicketRecapBuilder = mealTicketRecapBuilder;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Creat un'istanza di MealTicketRecap a partire da un Contract.
   */
  @Override
  public Optional<MealTicketRecap> create(Contract contract) {

    Preconditions.checkNotNull(contract);

    Optional<DateInterval> dateInterval = getContractMealTicketDateInterval(contract);

    if (!dateInterval.isPresent()) {
      return Optional.<MealTicketRecap>absent();
    }

    List<PersonDay> personDays = personDao.getPersonDayIntoInterval(contract.getPerson(),
        dateInterval.get(), true);

    List<MealTicket> expireOrderedAsc = mealTicketDao
        .contractMealTickets(contract, Optional.absent(),
            MealTicketOrder.ORDER_BY_EXPIRE_DATE_ASC, false);
    
    List<MealTicket> expireOrderedAscPostInit = mealTicketDao
        .contractMealTickets(contract, dateInterval,
            MealTicketOrder.ORDER_BY_EXPIRE_DATE_ASC, false);

    List<MealTicket> deliveryOrderedDesc = mealTicketDao
        .contractMealTickets(contract, Optional.absent(),
            MealTicketOrder.ORDER_BY_DELIVERY_DATE_DESC, false);

    List<MealTicket> returnedDeliveryOrderedDesc = mealTicketDao
        .contractMealTickets(contract, Optional.absent(),
            MealTicketOrder.ORDER_BY_DELIVERY_DATE_DESC, true);

    return Optional.fromNullable(mealTicketRecapBuilder.buildMealTicketRecap(
        contract, dateInterval.get(), personDays, expireOrderedAsc, expireOrderedAscPostInit, 
        deliveryOrderedDesc, returnedDeliveryOrderedDesc));
  }

  /**
   * Ritorna l'intervallo valido ePAS per il contratto riguardo la gestione dei buoni pasto. (scarto
   * la parte precedente a source se definita, e la parte precedente alla data inizio utilizzo per
   * la sede della persona).
   *
   * @return null in caso non vi siano giorni coperti dalla gestione dei buoni pasto.
   */
  @Override
  public Optional<DateInterval> getContractMealTicketDateInterval(Contract contract) {

    DateInterval intervalForMealTicket = wrapperFactory.create(contract)
        .getContractDatabaseIntervalForMealTicket();

    LocalDate officeStartDate = (LocalDate) configurationManager
        .configValue(contract.getPerson().getOffice(), EpasParam.DATE_START_MEAL_TICKET);

    if (officeStartDate.isBefore(intervalForMealTicket.getBegin())) {
      return Optional.of(intervalForMealTicket);
    }
    if (DateUtility.isDateIntoInterval(officeStartDate, intervalForMealTicket)) {
      return Optional.of(new DateInterval(officeStartDate,
          intervalForMealTicket.getEnd()));
    }

    return Optional.<DateInterval>absent();
  }

  /**
   * Genera la lista di MealTicket appartenenti al blocco identificato dal codice codeBlock e dagli
   * estremi.
   *
   * @param codeBlock  codice blocco
   * @param first      il primo codice
   * @param last       l'ultimo codice
   * @param expireDate la data di scadenza
   * @return la lista dei buoni
   */
  @Override
  public List<MealTicket> buildBlockMealTicket(String codeBlock, BlockType blockType,
      Integer first, Integer last, LocalDate expireDate, Office office) {

    List<MealTicket> mealTicketList = Lists.newArrayList();

    for (int i = first; i <= last; i++) {

      MealTicket mealTicket = new MealTicket();
      mealTicket.setExpireDate(expireDate);
      mealTicket.setBlock(codeBlock);
      mealTicket.setBlockType(blockType);
      mealTicket.setOffice(office);
      mealTicket.setNumber(i);


      if (i < 10) {
        mealTicket.setCode(codeBlock + "0" + i);
      } else {
        mealTicket.setCode("" + codeBlock + i);
      }
      
      mealTicketList.add(mealTicket);
    }

    return mealTicketList;
  }

  /**
   * Verifica che nel contratto precedente a contract siano avanzati dei buoni pasto assegnati. In
   * tal caso per quei buoni pasto viene modificata la relazione col contratto successivo e cambiata
   * la data di attribuzione in modo che ricada all'inizio del nuovo contratto.
   *
   * @return il numero di buoni pasto trasferiti fra un contratto e l'altro.
   */
  @Override
  public int mealTicketsLegacy(Contract contract) {

    Contract previousContract = personDao.getPreviousPersonContract(contract);
    if (previousContract == null) {
      return 0;
    }

    IWrapperContract wrContract = wrapperFactory.create(previousContract);
    DateInterval previousContractInterval = wrContract.getContractDateInterval();

    Optional<ContractMonthRecap> recap = wrContract.getContractMonthRecap(
        new YearMonth(previousContractInterval.getEnd()));

    if (!recap.isPresent() || recap.get().getRemainingMealTickets() == 0) {
      return 0;
    }

    int mealTicketsTransfered = 0;

    List<MealTicket> contractMealTicketsDesc = mealTicketDao
        .contractMealTickets(previousContract, Optional.absent(), 
            MealTicketOrder.ORDER_BY_DELIVERY_DATE_DESC, false);
      
    LocalDate pastDate = LocalDate.now();
    for (int i = 0; i < recap.get().getRemainingMealTickets(); i++) {

      MealTicket ticketToChange = contractMealTicketsDesc.get(i);
      if (ticketToChange.getDate().isBefore(pastDate)) {
        pastDate = ticketToChange.getDate();
      }
      ticketToChange.setContract(contract);
      ticketToChange.setDate(contract.getBeginDate());
      ticketToChange.save();
      mealTicketsTransfered++;
    }

    consistencyManager.updatePersonSituation(contract.getPerson().id, pastDate);

    return mealTicketsTransfered;
  }


  /**
   * I tipi di ordinamento per la selezione della lista dei buoni pasto.
   *
   * @author Alessandro Martelli
   */
  public static enum MealTicketOrder {
    ORDER_BY_EXPIRE_DATE_ASC,
    ORDER_BY_DELIVERY_DATE_DESC
  }


  @Override
  public MealTicketComposition whichBlock(MealTicketRecap recap, 
      ContractMonthRecap monthRecap, Contract contract) {
    BlockType blockType = null;
    int buoniCartacei = 0;
    int buoniElettronici = 0;
    int buoniUsati = monthRecap.getBuoniPastoUsatiNelMese();
    int buoniDaConteggiare = 0;
    MealTicketComposition composition = new MealTicketComposition();
    //TODO: controllare che esista una inizializzazione e inserirla nel computo dei buoni
    List<BlockMealTicket> list = recap.getBlockMealTicketReceivedDeliveryDesc();
    if (monthRecap.getRemainingMealTickets() < 0) {
      //devo guardare quale sia il default e contare quanti sono i buoni senza copertura
      buoniDaConteggiare = buoniUsati;
      composition.isBlockMealTicketTypeKnown = false;
      final java.util.Optional<Configuration> conf = 
          contract.getPerson().getOffice().getConfigurations().stream()
          .filter(configuration -> 
          configuration.getEpasParam() == EpasParam.MEAL_TICKET_BLOCK_TYPE).findFirst();
      if (conf.isPresent()) {
        try { 
          blockType = BlockType.valueOf(conf.get().getFieldValue()); 
        } catch (Exception e) {
          blockType = BlockType.electronic;
        }
        switch (blockType) {
          case electronic:
            buoniElettronici = buoniDaConteggiare;
            break;
          case papery:
            buoniCartacei = buoniDaConteggiare;
            break;
          default:
            //log.warn("Errore nel parsing dell'enumerato per il tipo di blocchetto. Verificare.");
            break;
        }
        composition.blockType = blockType;
      }

    } else {
      int dimBlocchetto = 0;
      composition.isBlockMealTicketTypeKnown = true;
      buoniDaConteggiare = buoniUsati;
      for (BlockMealTicket block : list) {
        dimBlocchetto = block.getDimBlock();
        while (buoniDaConteggiare > 0 && dimBlocchetto != 0) {
          switch (block.getBlockType()) {
            case papery:
              buoniCartacei++;
              break;
            case electronic:
              buoniElettronici++;
              break;
            default:
              break;
          }
          dimBlocchetto--;
          buoniDaConteggiare--;
        }
      }
    }
    composition.electronicMealTicket = buoniElettronici;
    composition.paperyMealTicket = buoniCartacei;

    return composition;
  }

}