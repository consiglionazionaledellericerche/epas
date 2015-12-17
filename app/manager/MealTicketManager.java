package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import dao.MealTicketDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.recaps.mealTicket.BlockMealTicket;

import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

/**
 * Manager per MealTicket.
 *
 * @author alessandro
 */
public class MealTicketManager {
  
  /**
   * I tipi di ordinamento per la selezione della lista dei buoni pasto.
   * @author alessandro
   *
   */
  public static enum MealTicketOrder {
    ORDER_BY_EXPIRE_DATE_ASC,
    ORDER_BY_DELIVERY_DATE_DESC
  }

  private final PersonDao personDao;
  private final MealTicketDao mealTicketDao;
  private final ConfGeneralManager confGeneralManager;
  private final IWrapperFactory wrapperFactory;
  private final ConsistencyManager consistencyManager;
  
  /**
   * Costrutture.
   * @param personDao personDao
   * @param mealTicketDao mealTicketDao
   * @param confGeneralManager confGeneralDao
   * @param consistencyManager consistencyManager
   * @param wrapperFactory wrapperFactory
   */
  @Inject
  public MealTicketManager(PersonDao personDao,
                           MealTicketDao mealTicketDao,
                           ConfGeneralManager confGeneralManager,
                           ConsistencyManager consistencyManager,
                           IWrapperFactory wrapperFactory) {
    this.personDao = personDao;
    this.mealTicketDao = mealTicketDao;
    this.confGeneralManager = confGeneralManager;
    this.consistencyManager = consistencyManager;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * 
   *
   * @param codeBlock il codice del blocco di meal ticket
   * @param dimBlock la dimensione del blocco di meal ticket
   * @param expireDate la data di scadenza dei buoni nel blocco
   * @return la lista di MealTicket appartenenti al blocco.
   */
  
  /**
   * Genera la lista di MealTicket appartenenti al blocco identificato dal codice codeBlock e dagli
   * estremi. 
   * @param codeBlock codice blocco
   * @param first il primo codice
   * @param last l'ultimo codice
   * @param expireDate la data di scadenza
   * @return la lista dei buoni
   */
  public List<MealTicket> buildBlockMealTicket(Integer codeBlock, Integer first, Integer last,
      LocalDate expireDate) {

    List<MealTicket> mealTicketList = Lists.newArrayList();

    for (int i = first; i <= last; i++) {

      MealTicket mealTicket = new MealTicket();
      mealTicket.expireDate = expireDate;
      mealTicket.block = codeBlock;
      mealTicket.number = i;

      if (i < 10) {
        mealTicket.code = codeBlock + "0" + i;
      } else {
        mealTicket.code = "" + codeBlock + i;
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
  public int mealTicketsLegacy(Contract contract) {

    Contract previousContract = personDao.getPreviousPersonContract(contract);
    if (previousContract == null) {
      return 0;
    }

    IWrapperContract wrContract = wrapperFactory.create(previousContract);
    DateInterval previousContractInterval = wrContract.getContractDateInterval();

    Optional<ContractMonthRecap> recap = wrContract.getContractMonthRecap(
            new YearMonth(previousContractInterval.getEnd()));

    if (!recap.isPresent() || recap.get().remainingMealTickets == 0) {
      return 0;
    }

    int mealTicketsTransfered = 0;

    List<MealTicket> contractMealTicketsDesc = mealTicketDao
            .getOrderedMealTicketInContract(previousContract);

    LocalDate pastDate = LocalDate.now();
    for (int i = 0; i < recap.get().remainingMealTickets; i++) {

      MealTicket ticketToChange = contractMealTicketsDesc.get(i);
      if (ticketToChange.date.isBefore(pastDate)) {
        pastDate = ticketToChange.date;
      }
      ticketToChange.contract = contract;
      ticketToChange.date = contract.beginDate;
      ticketToChange.save();
      mealTicketsTransfered++;
    }

    consistencyManager.updatePersonSituation(contract.person.id, pastDate);

    return mealTicketsTransfered;
  }

  /**
   * Ritorna l'intervallo valido ePAS per il contratto riguardo la gestione dei buoni pasto. (scarto
   * la parte precedente a source se definita, e la parte precedente alla data inizio utilizzo per
   * la sede della persona).
   *
   * @return null in caso non vi siano giorni coperti dalla gestione dei buoni pasto.
   */
  public Optional<DateInterval> getContractMealTicketDateInterval(Contract contract) {

    DateInterval intervalForMealTicket = wrapperFactory.create(contract)
            .getContractDatabaseIntervalForMealTicket();

    Optional<LocalDate> officeStartDate = confGeneralManager
            .getLocalDateFieldValue(Parameter.DATE_START_MEAL_TICKET, contract.person.office);

    if (officeStartDate.isPresent()) {
      if (officeStartDate.get().isBefore(intervalForMealTicket.getBegin())) {
        return Optional.fromNullable(intervalForMealTicket);
      }
      if (DateUtility
              .isDateIntoInterval(officeStartDate.get(), intervalForMealTicket)) {
        return Optional.fromNullable(new DateInterval(officeStartDate.get(),
                intervalForMealTicket.getEnd()));
      }
    }

    return Optional.<DateInterval>absent();
  }

  /**
   * Genera i blocchi di codici consecutivi a partire dalla lista ordinata di buoni pasto.
   * @param mealTicketListOrdered una lista di buoni pasto ordinata 
   *   per data di scadenza e per codice blocco.
   * @param interval intervallo da considerare.
   * @return i blocchi
   */
  public List<BlockMealTicket> getBlockMealTicketReceivedIntoInterval(
          List<MealTicket> mealTicketListOrdered, Optional<DateInterval> interval) {

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
        currentBlock.mealTickets.add(mealTicket);
        currentBlock.contract = mealTicket.contract;
        continue;
      }

      //Stesso blocco
      Long previous = Long.parseLong(previousMealTicket.code) + 1;
      Long actual = Long.parseLong(mealTicket.code);
      if (previous.equals(actual) && previousMealTicket.contract.equals(mealTicket.contract)) {
        currentBlock.mealTickets.add(mealTicket);
      } else {
        //Nuovo blocco
        blockList.add(currentBlock);
        currentBlock = new BlockMealTicket(mealTicket.block);
        currentBlock.mealTickets.add(mealTicket);
        currentBlock.contract = mealTicket.contract;
      }
      previousMealTicket = mealTicket;
    }

    if (currentBlock != null) {
      blockList.add(currentBlock);
    }
    return blockList;

  }
}
