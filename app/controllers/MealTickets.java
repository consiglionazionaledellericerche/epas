package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
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

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.ConsistencyManager;
import manager.services.mealtickets.BlockMealTicket;
import manager.services.mealtickets.IMealTicketsService;
import manager.services.mealtickets.MealTicketRecap;
import manager.services.mealtickets.MealTicketStaticUtility;

import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.Office;
import models.Person;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

@With({Resecure.class})
public class MealTickets extends Controller {

  @Inject
  private static SecurityRules rules;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static ContractDao contractDao;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static IMealTicketsService mealTicketService;
  @Inject
  private static MealTicketDao mealTicketDao;
  @Inject
  private static ContractMonthRecapDao contractMonthRecapDao;

  /**
   * Riepilogo buoni pasto dipendente.
   */
  public static void mealTickets() {

    Optional<User> user = Security.getUser();
    Verify.verify(user.isPresent());
    Verify.verifyNotNull(user.get().person);

    Person person = user.get().person;

    MealTicketRecap recapPrevious = null; // TODO: nella vista usare direttamente optional

    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
    Preconditions.checkState(contract.isPresent());

    // riepilogo contratto corrente
    MealTicketRecap recap = mealTicketService.create(contract.get()).orNull();
    Preconditions.checkNotNull(recap);

    //riepilogo contratto precedente
    Contract previousContract = personDao.getPreviousPersonContract(contract.get());
    if (previousContract != null) {
      Optional<MealTicketRecap> previousRecap = mealTicketService.create(previousContract);
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

    render(person, recap, recapPrevious, deliveryDate, expireDate, today,
        ticketNumberFrom, ticketNumberTo);
  }

  /**
   * I riepiloghi buoni pasto dei dipendenti dell'office per il mese selezionato.
   *
   * @param year     year
   * @param month    month
   * @param officeId officeId
   */
  public static void recapMealTickets(int year, int month, Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    List<ContractMonthRecap> monthRecapList = contractMonthRecapDao
        .getPersonMealticket(new YearMonth(year, month), Optional.<Integer>absent(),
            Optional.<String>absent(), Sets.newHashSet(office));

    render(office, monthRecapList, year, month);
  }

  /**
   * Form di inserimento buoni pasto e riepilogo degli ultimi blocchi inseriti.
   *
   * @param personId persona
   */
  public static void personMealTickets(Long personId) {

    Person person = personDao.getPersonById(personId);
    Preconditions.checkArgument(person.isPersistent());
    rules.checkIfPermitted(person.office);

    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
    Preconditions.checkState(contract.isPresent());

    // riepilogo contratto corrente
    MealTicketRecap recap = mealTicketService.create(contract.get()).orNull();
    Preconditions.checkNotNull(recap);

    LocalDate deliveryDate = LocalDate.now();
    LocalDate today = LocalDate.now();
    //TODO mettere nel default.
    Integer ticketNumberFrom = 1;
    Integer ticketNumberTo = 22;

    LocalDate expireDate = mealTicketDao.getFurtherExpireDateInOffice(person.office);
    User admin = Security.getUser().get();

    render(person, recap, deliveryDate, admin, expireDate, today, ticketNumberFrom, ticketNumberTo);
  }

  /**
   * Riepilogo contratto corrente e precedente dei buoni pasto consegnati alla persona.
   */
  public static void recapPersonMealTickets(Long personId) {

    Person person = personDao.getPersonById(personId);
    Preconditions.checkArgument(person.isPersistent());
    rules.checkIfPermitted(person.office);

    MealTicketRecap recap;
    MealTicketRecap recapPrevious = null; // TODO: nella vista usare direttamente optional

    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
    Preconditions.checkState(contract.isPresent());

    // riepilogo contratto corrente
    Optional<MealTicketRecap> currentRecap = mealTicketService.create(contract.get());
    Preconditions.checkState(currentRecap.isPresent());
    recap = currentRecap.get();

    //riepilogo contratto precedente
    Contract previousContract = personDao.getPreviousPersonContract(contract.get());
    if (previousContract != null) {
      Optional<MealTicketRecap> previousRecap = mealTicketService.create(previousContract);
      if (previousRecap.isPresent()) {
        recapPrevious = previousRecap.get();
      }
    }

    render(person, recap, recapPrevious);
  }

  /**
   * Form per la rimozione/riconsegna dei buoni consegnati al dipendente.
   */
  public static void editPersonMealTickets(Long personId) {

    Person person = personDao.getPersonById(personId);
    Preconditions.checkArgument(person.isPersistent());
    rules.checkIfPermitted(person.office);

    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
    Preconditions.checkState(contract.isPresent());

    // riepilogo contratto corrente
    Optional<MealTicketRecap> currentRecap = mealTicketService.create(contract.get());
    Preconditions.checkState(currentRecap.isPresent());
    MealTicketRecap recap = currentRecap.get();

    render(person, recap);
  }

  /**
   * Trasferisce i buoni avanzati del vecchio contratto a quello nuovo. TODO: renderlo nuovamente
   * operativo quando ce ne sarà bisogno. Adesso il link è oscurato.
   *
   * @param contractId contratto
   */
  public static void mealTicketsLegacy(Long contractId) {

    Contract contract = contractDao.getContractById(contractId);
    Preconditions.checkNotNull(contract);
    Preconditions.checkArgument(contract.isPersistent());

    rules.checkIfPermitted(contract.person.office);

    int mealTicketsTransfered = mealTicketService.mealTicketsLegacy(contract);

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
   *
   * @param personId         persona.
   * @param codeBlock        codice blocco.
   * @param ticketNumberFrom dal codice
   * @param ticketNumberTo   al codice
   * @param deliveryDate     data consegna
   * @param expireDate       data scadenza
   */
  public static void submitPersonMealTicket(Long personId, @Required Long codeBlock,
      @Required @Min(1) @Max(99) Integer ticketNumberFrom,
      @Required @Min(1) @Max(99) Integer ticketNumberTo,
      @Valid @Required LocalDate deliveryDate, @Valid @Required LocalDate expireDate) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    User admin = Security.getUser().get();

    MealTicketRecap recap;
    Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();
    Preconditions.checkState(contract.isPresent());
    // riepilogo contratto corrente
    Optional<MealTicketRecap> currentRecap = mealTicketService.create(contract.get());
    Preconditions.checkState(currentRecap.isPresent());
    recap = currentRecap.get();

    if (ticketNumberFrom > ticketNumberTo) {
      Validation.addError("ticketNumberFrom", "sequenza non valida");
    }

    if (Validation.hasErrors()) {
      response.status = 400;

      render("@personMealTickets", person, recap, codeBlock, ticketNumberFrom, ticketNumberTo,
          deliveryDate, expireDate, admin);
    }

    //Controllo dei parametri


    List<MealTicket> ticketToAddOrdered = Lists.newArrayList();
    ticketToAddOrdered.addAll(mealTicketService
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

      List<BlockMealTicket> blocksError = MealTicketStaticUtility
          .getBlockMealTicketFromOrderedList(ticketsError, Optional.<DateInterval>absent());
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

    consistencyManager.updatePersonRecaps(person.id, deliveryDate);

    flash.success("Il blocco inserito è stato salvato correttamente.");

    personMealTickets(person.id);
  }

  /**
   * Funzione di restituisci buoni alla sede centrale.
   *
   * @param contractId contratto di riferimento
   * @param codeBlock  codice blocco
   * @param first      dal
   * @param last       al
   * @param undo       se voglio annullare la restituzione
   */
  public static void returnPersonCodeBlock(Long contractId, Long codeBlock, int first, int last,
      boolean undo) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsInCodeBlock(codeBlock,
        Optional.fromNullable(contract));

    Preconditions.checkState(mealTicketList.size() > 0);

    BlockMealTicket block = MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        MealTicketStaticUtility.blockPortion(mealTicketList, contract, first, last),
        Optional.<DateInterval>absent()).get(0);

    render(contract, codeBlock, block, undo);
  }

  /**
   * Esecuzione del comando di restituisci buoni alla sede centrale.
   *
   * @param contractId contratto di riferimento
   * @param codeBlock  codice blocco
   * @param first      dal
   * @param last       al
   * @param undo       se voglio annullare la restituzione
   * @param confirmed  conferma
   */
  public static void performReturnPersonCodeBlock(Long contractId, Long codeBlock,
      int first, int last, boolean undo, boolean confirmed) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsInCodeBlock(codeBlock,
        Optional.fromNullable(contract));

    Preconditions.checkState(mealTicketList.size() > 0);

    List<MealTicket> blockPortionToReturn = MealTicketStaticUtility
        .blockPortion(mealTicketList, contract, first, last);
    for (MealTicket mealTicket : blockPortionToReturn) {
      if (!undo) {
        mealTicket.returned = true;
      } else {
        mealTicket.returned = false;
      }
    }
    List<BlockMealTicket> blocks = MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        mealTicketList, Optional.<DateInterval>absent());

    if (!confirmed) {
      response.status = 400;
      confirmed = true;
      render("@returnPersonCodeBlock", contract, codeBlock, blocks, first, last, undo, confirmed);
    }

    // Perform
    LocalDate pastDate = LocalDate.now();
    for (MealTicket mealTicket : mealTicketList) {
      if (mealTicket.date.isBefore(pastDate)) {
        pastDate = mealTicket.date;
      }
    }
    for (MealTicket mealTicket : blockPortionToReturn) {
      if (mealTicket.date.isBefore(pastDate)) {
        pastDate = mealTicket.date;
      }
      mealTicket.save();
    }
    consistencyManager.updatePersonSituation(contract.person.id, pastDate);

    flash.success("%d buoni riconsegnati correttamente.", blockPortionToReturn.size());

    editPersonMealTickets(contract.person.id);
  }


  /**
   * Funzione di eliminazione inserimento blocco alla persona.
   *
   * @param contractId contratto di riferimento
   * @param codeBlock  codice blocco
   * @param first      dal
   * @param last       al
   */
  public static void deletePersonCodeBlock(Long contractId, Long codeBlock, int first, int last) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsInCodeBlock(codeBlock,
        Optional.fromNullable(contract));

    Preconditions.checkState(mealTicketList.size() > 0);

    BlockMealTicket block = MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        MealTicketStaticUtility.blockPortion(mealTicketList, contract, first, last),
        Optional.<DateInterval>absent()).get(0);

    render(contract, codeBlock, block);
  }

  /**
   * Esecuzione comando di eliminazione inserimento blocco alla persona.
   *
   * @param contractId contratto di riferimento
   * @param codeBlock  codice blocco
   * @param first      dal
   * @param last       al
   * @param confirmed  conferma
   */
  public static void performDeletePersonCodeBlock(Long contractId, Long codeBlock,
      int first, int last, boolean confirmed) {

    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.person.office);

    List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsInCodeBlock(codeBlock,
        Optional.fromNullable(contract));

    Preconditions.checkState(mealTicketList.size() > 0);

    List<MealTicket> mealTicketToRemove = MealTicketStaticUtility
        .blockPortion(mealTicketList, contract, first, last);
    LocalDate pastDate = LocalDate.now();
    for (MealTicket mealTicket : mealTicketList) {
      if (mealTicket.date.isBefore(pastDate)) {
        pastDate = mealTicket.date;
      }
    }

    List<BlockMealTicket> blocks = MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        mealTicketToRemove, Optional.<DateInterval>absent());

    if (!confirmed) {
      response.status = 400;
      confirmed = true;
      render("@deletePersonCodeBlock", contract, codeBlock, blocks, first, last, confirmed);
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

    editPersonMealTickets(contract.person.id);
  }

  /**
   * Funzione di Ricerca di un blocco nel database ePAS.
   *
   * @param code codice match
   */
  public static void findCodeBlock(String code) {

    List<BlockMealTicket> blocks = Lists.newArrayList();
    if (code != null && !code.isEmpty()) {
      List<MealTicket> mealTicket = mealTicketDao.getMealTicketsMatchCodeBlock(code,
          Optional.<Office>absent());
      blocks = MealTicketStaticUtility
          .getBlockMealTicketFromOrderedList(mealTicket, Optional.<DateInterval>absent());
    }
    render(blocks, code);
  }

  /**
   * Elenco di buoni riconsegnati per la sede office.
   *
   * @param officeId sede
   * @param code     codice match
   */
  public static void returnedMealTickets(Long officeId, String code) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    if (code == null) {
      code = "";
    }
    List<BlockMealTicket> blocks = Lists.newArrayList();
    List<MealTicket> mealTicket = mealTicketDao.getMealTicketsMatchCodeBlock(code,
        Optional.fromNullable(office));
    blocks = MealTicketStaticUtility
        .getBlockMealTicketFromOrderedList(mealTicket, Optional.<DateInterval>absent());

    render(office, code, blocks);
  }

}
