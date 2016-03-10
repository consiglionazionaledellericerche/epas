package manager.services.mealTickets;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import dao.MealTicketDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import manager.ConfigurationManager;
import manager.ConsistencyManager;

import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.PersonDay;
import models.enumerate.EpasParam;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import java.util.List;

import javax.inject.Inject;

/**
 * Implementazione di produzione del servizio meal tickets.
 * @author alessandro
 *
 */
public class MealTicketsServiceImpl implements IMealTicketsService {

  /**
   * I tipi di ordinamento per la selezione della lista dei buoni pasto.
   * @author alessandro
   *
   */
  public static enum MealTicketOrder {
    ORDER_BY_EXPIRE_DATE_ASC,
    ORDER_BY_DELIVERY_DATE_DESC
  }

  @Inject
  private PersonDao personDao;
  private MealTicketDao mealTicketDao;
  private IWrapperFactory wrapperFactory;
  private ConsistencyManager consistencyManager;
  private MealTicketRecapBuilder mealTicketRecapBuilder;
  private ConfigurationManager configurationManager;
  
  /**
   * Costrutture.
   * @param personDao personDao
   * @param mealTicketDao mealTicketDao
   * @param configurationManager configurationManager
   * @param consistencyManager consistencyManager
   * @param wrapperFactory wrapperFactory
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

  @Override
  public Optional<MealTicketRecap> create(Contract contract) {

    Preconditions.checkNotNull(contract);

    Optional<DateInterval> dateInterval = getContractMealTicketDateInterval(contract);

    if (!dateInterval.isPresent()) {
      return Optional.<MealTicketRecap>absent();
    }
    
    List<PersonDay> personDays = personDao.getPersonDayIntoInterval(contract.person, 
        dateInterval.get(), true);
    
    List<MealTicket> expireOrderedAsc = mealTicketDao
        .getMealTicketAssignedToPersonIntoInterval(contract, dateInterval.get(), 
            MealTicketOrder.ORDER_BY_EXPIRE_DATE_ASC, false);
    
    List<MealTicket> deliveryOrderedDesc = mealTicketDao
        .getMealTicketAssignedToPersonIntoInterval(contract, dateInterval.get(), 
            MealTicketOrder.ORDER_BY_DELIVERY_DATE_DESC, false);
    
    List<MealTicket> returnedDeliveryOrderedDesc = mealTicketDao
        .getMealTicketAssignedToPersonIntoInterval(contract, dateInterval.get(), 
            MealTicketOrder.ORDER_BY_DELIVERY_DATE_DESC, true);

    return Optional.fromNullable(mealTicketRecapBuilder.buildMealTicketRecap(
        contract, dateInterval.get(), personDays, 
        expireOrderedAsc, deliveryOrderedDesc, returnedDeliveryOrderedDesc));
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

    LocalDate officeStartDate = (LocalDate)configurationManager
        .configValue(contract.person.office, EpasParam.DATE_START_MEAL_TICKET);

    if (officeStartDate.isBefore(intervalForMealTicket.getBegin())) {
      return Optional.fromNullable(intervalForMealTicket);
    }
    if (DateUtility
        .isDateIntoInterval(officeStartDate, intervalForMealTicket)) {
      return Optional.fromNullable(new DateInterval(officeStartDate,
          intervalForMealTicket.getEnd()));
    }

    return Optional.<DateInterval>absent();
  }
  
  /**
   * Genera la lista di MealTicket appartenenti al blocco identificato dal codice codeBlock e dagli
   * estremi.
   * @param codeBlock codice blocco
   * @param first il primo codice
   * @param last l'ultimo codice
   * @param expireDate la data di scadenza
   * @return la lista dei buoni
   */
  @Override
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

  

}
