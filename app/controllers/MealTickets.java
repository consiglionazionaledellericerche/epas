package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.ContractMonthRecapDao;
import dao.MealTicketDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;

import it.cnr.iit.epas.DateInterval;

import lombok.extern.slf4j.Slf4j;

import manager.ConfGeneralManager;
import manager.ConsistencyManager;
import manager.MealTicketManager;
import manager.services.mealtickets.BlockMealTicket;
import manager.services.mealtickets.MealTicketRecap;
import manager.services.mealtickets.MealTicketRecapFactory;

import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.Office;
import models.Person;
import models.User;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

@Slf4j
@With({Resecure.class, RequestInit.class})
public class MealTickets extends Controller {

  @Inject
  private static SecurityRules rules;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static MealTicketRecapFactory mealTicketFactory;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static MealTicketDao mealTicketDao;
  @Inject
  private static MealTicketManager mealTicketManager;
  @Inject
  private static ContractMonthRecapDao contractMonthRecapDao;
  @Inject
  private static ContractDao contractDao;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static ConfGeneralManager confGeneralManager;
  
  /**
   * I riepiloghi buoni pasto dei dipendenti dell'office per il mese selezionato.
   * @param year year
   * @param month month
   * @param officeId officeId
   */
  public static void recapMealTickets(int year, int month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    
    List<ContractMonthRecap> monthRecapList = contractMonthRecapDao
            .getPersonMealticket(new YearMonth(year, month), Optional.<Integer>absent(), 
                Optional.<String>absent(), Sets.newHashSet(office));

    //La data inizio utilizzo dei buoni pasto.
    Optional<LocalDate> officeStartDate = confGeneralManager
        .getLocalDateFieldValue(Parameter.DATE_START_MEAL_TICKET, office);

    render(office, monthRecapList, officeStartDate, year, month);
  }
  
  public static void personMealTickets(Long personId) {

    Person person = personDao.getPersonById(personId);
    Preconditions.checkArgument(person.isPersistent());
    rules.checkIfPermitted(person.office);

    MealTicketRecap recap;
    MealTicketRecap recapPrevious = null; // TODO: nella vista usare direttamente optional

    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
    Preconditions.checkState(contract.isPresent());

    // riepilogo contratto corrente
    Optional<MealTicketRecap> currentRecap = mealTicketFactory.create(contract.get());
    Preconditions.checkState(currentRecap.isPresent());
    recap = currentRecap.get();

    //riepilogo contratto precedente
    Contract previousContract = personDao.getPreviousPersonContract(contract.get());
    if (previousContract != null) {
      Optional<MealTicketRecap> previousRecap = mealTicketFactory.create(previousContract);
      if (previousRecap.isPresent()) {
        recapPrevious = previousRecap.get();
      }
    }

    LocalDate deliveryDate = LocalDate.now();
    LocalDate today = LocalDate.now();
    //TODO mettere nel default.
    Integer ticketNumberFrom = 1;
    Integer ticketNumberTo = 22;
    
    LocalDate expireDate = mealTicketDao.getFurtherExpireDateInOffice(person.office);
    User admin = Security.getUser().get();
    
    

    render(person, recap, recapPrevious, deliveryDate, admin, expireDate, today, 
        ticketNumberFrom, ticketNumberTo);
  }

  public static void mealTicketsLegacy(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    Preconditions.checkNotNull(contract);
    Preconditions.checkArgument(contract.isPersistent());

    rules.checkIfPermitted(contract.person.office);

    int mealTicketsTransfered = mealTicketManager.mealTicketsLegacy(contract);

    if (mealTicketsTransfered == 0) {
      flash.error("Non e' stato trasferito alcun buono pasto. "
          + "Riprovare o effettuare una segnalazione.");
    } else {
      flash.success("Trasferiti con successo %s buoni pasto per %s %s",
              mealTicketsTransfered, contract.person.name, contract.person.surname);
    }

    MealTickets.recapMealTickets(LocalDate.now().getYear(), LocalDate.now().getMonthOfYear(), 
        contract.person.office.id);
  }

  /**
   * Aggiunta di un blocchetto alla persona.
   * @param personId persona.
   * @param codeBlock codice blocco.
   * @param ticketNumberFrom dal codice
   * @param ticketNumberTo al codice
   * @param deliveryDate data consegna
   * @param expireDate data scadenza
   */
  public static void submitPersonMealTicket(Long personId, @Required Integer codeBlock, 
      @Required Integer ticketNumberFrom, @Required Integer ticketNumberTo, 
      @Valid @Required LocalDate deliveryDate, @Valid @Required LocalDate expireDate) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    User admin = Security.getUser().get();
    
    // TODO: spezzare le action....
    
    MealTicketRecap recap;
    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
    Preconditions.checkState(contract.isPresent());
    // riepilogo contratto corrente
    Optional<MealTicketRecap> currentRecap = mealTicketFactory.create(contract.get());
    Preconditions.checkState(currentRecap.isPresent());
    recap = currentRecap.get();
 
    if (validation.hasErrors()) {
      response.status = 400;
      
      render("@personMealTickets", person, recap, codeBlock, ticketNumberFrom, ticketNumberTo, 
          deliveryDate, expireDate, admin);
    }
    
    //Controllo dei parametri
    
    
    List<MealTicket> ticketToAddOrdered = Lists.newArrayList();
    ticketToAddOrdered.addAll(mealTicketManager
        .buildBlockMealTicket(codeBlock, ticketNumberFrom, ticketNumberTo, expireDate));

    List<MealTicket> ticketsError = Lists.newArrayList();
    
    //Controllo esistenza
    for (MealTicket mealTicket : ticketToAddOrdered) {

      MealTicket exist = mealTicketDao.getMealTicketByCode(mealTicket.code);
      if (exist != null) {
        
        ticketsError.add(exist);
      }
    }
    if (!ticketsError.isEmpty()) {
      
      List<BlockMealTicket> blocksError = mealTicketManager
          .getBlockMealTicketReceivedIntoInterval(ticketsError, Optional.<DateInterval>absent());
      render("@personMealTickets", person, recap, codeBlock, ticketNumberFrom, ticketNumberTo, 
          deliveryDate, expireDate, admin, blocksError);
    }

    Set<Contract> contractUpdated = Sets.newHashSet();

    //Persistenza
    for (MealTicket mealTicket : ticketToAddOrdered) {
      mealTicket.date = deliveryDate;
      mealTicket.contract = contractDao.getContract(mealTicket.date, person);
      mealTicket.admin = admin.person;
      mealTicket.save();

      contractUpdated.add(mealTicket.contract);
    }

    consistencyManager.updatePersonSituation(person.id, LocalDate.now());

    flash.success("Il blocco inserito è stato salvato correttamente.");
    
    personMealTickets(person.id);
  }

  /**
   * Rimuove il blocco dal database. 
   * 
   * @param codeBlock blocco
   */
  public static void deletePersonMealTicket(Long contractId, int codeBlock, 
      int first, int last, boolean confirmed) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);
    
    List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsInCodeBlock(codeBlock);

    Preconditions.checkState(mealTicketList.size() > 0);

    List<MealTicket> mealTicketToRemove = Lists.newArrayList();
    LocalDate pastDate = LocalDate.now();
    //Controllo di consistenza.
    for (MealTicket mealTicket : mealTicketList) {
      if (mealTicket.number >= first && mealTicket.number <= last) {
        if (!mealTicket.contract.equals(contract)) {
          // un buono nell'intervallo non appartiene al contratto effettivo!!! 
          //non si dovrebbe verificare.
          log.error("Il buono pasto {} non appartiene al contratto previsto {}.", 
              mealTicket.code, contract);
          throw new IllegalStateException();
        }
        mealTicketToRemove.add(mealTicket);
        if (mealTicket.date.isBefore(pastDate)) {
          pastDate = mealTicket.date;
        }
      }
    }

    if (!confirmed) {
      confirmed = true;
      render(contract, codeBlock, first, last, confirmed);
    }

    int deleted = 0;
    for (MealTicket mealTicket : mealTicketToRemove) {
      if (mealTicket.date.isBefore(pastDate)) {
        pastDate = mealTicket.date;
      }

      mealTicket.delete();
      deleted++;  
    }

    consistencyManager.updatePersonSituation(contract.person.id, pastDate);

    flash.success("Blocco di %d buoni rimosso correttamente.", deleted);

    personMealTickets(contract.person.id);
  }

  public static void findCodeBlock() {
    
    render();
  }
  
  public static void returnedMealTickets(Long officeId) {
    
    render();
  }

}
