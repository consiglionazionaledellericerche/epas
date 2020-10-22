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
import helpers.validators.LocalDateIsNotFuture;
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
import play.data.validation.CheckWith;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.i18n.Messages;
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
   * @param contractId l'id del contratto di cui vedere i buoni inseriti
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   */
  public static void personMealTickets(Long contractId, Integer year, Integer month) {

    Contract contract = contractDao.getContractById(contractId);

    Preconditions.checkState(contract.isPersistent());
    Preconditions.checkArgument(contract.person.isPersistent());
    rules.checkIfPermitted(contract.person.office);

    if (year == null || month == null) {
      year = LocalDate.now().getYear();
      month = LocalDate.now().getMonthOfYear();
    }

    // riepilogo contratto corrente
    MealTicketRecap recap = mealTicketService.create(contract).orNull();
    Preconditions.checkNotNull(recap);

    LocalDate deliveryDate = LocalDate.now();
    LocalDate today = LocalDate.now();
    //TODO mettere nel default.
    Integer ticketNumberFrom = 1;
    Integer ticketNumberTo = 22;

    LocalDate expireDate = mealTicketDao.getFurtherExpireDateInOffice(contract.person.office);
    User admin = Security.getUser().get();
    Person person = contract.person;
    render(person, recap, deliveryDate, admin, expireDate, 
        today, ticketNumberFrom, ticketNumberTo, year, month);
  }

  /**
   * Riepilogo contratto corrente e precedente dei buoni pasto consegnati alla persona.
   */
  public static void recapPersonMealTickets(Long contractId, int year, int month) {

    Contract contract = contractDao.getContractById(contractId);
    Preconditions.checkState(contract.isPersistent());
    Preconditions.checkArgument(contract.person.isPersistent());
    rules.checkIfPermitted(contract.person.office);

    MealTicketRecap recap;
    MealTicketRecap recapPrevious = null; // TODO: nella vista usare direttamente optional

    // riepilogo contratto corrente
    Optional<MealTicketRecap> currentRecap = mealTicketService.create(contract);
    Preconditions.checkState(currentRecap.isPresent());
    recap = currentRecap.get();

    //riepilogo contratto precedente
    Contract previousContract = personDao.getPreviousPersonContract(contract);
    if (previousContract != null) {
      Optional<MealTicketRecap> previousRecap = mealTicketService.create(previousContract);
      if (previousRecap.isPresent()) {
        recapPrevious = previousRecap.get();
      }
    }
    Person person = contract.person;
    render(person, recap, recapPrevious, year, month);
  }

  /**
   * Form per la rimozione/riconsegna dei buoni consegnati al dipendente.
   */
  public static void editPersonMealTickets(Long contractId, int year, int month) {

    Contract contract = contractDao.getContractById(contractId);
    //Person person = personDao.getPersonById(personId);
    Preconditions.checkState(contract.isPersistent());
    Preconditions.checkArgument(contract.person.isPersistent());
    rules.checkIfPermitted(contract.person.office);

    // riepilogo contratto corrente
    Optional<MealTicketRecap> currentRecap = mealTicketService.create(contract);
    Preconditions.checkState(currentRecap.isPresent());
    MealTicketRecap recap = currentRecap.get();
    Person person = contract.person;
    render(person, recap, year, month);
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
   * @param contractId       contratto.
   * @param codeBlock        codice blocco.
   * @param ticketNumberFrom dal codice
   * @param ticketNumberTo   al codice
   * @param deliveryDate     data consegna
   * @param expireDate       data scadenza
   */
  public static void submitPersonMealTicket(Long contractId, @Required String codeBlock,
      @Required @Min(1) @Max(99) Integer ticketNumberFrom,
      @Required @Min(1) @Max(99) Integer ticketNumberTo,
      @Valid @Required @CheckWith(LocalDateIsNotFuture.class) LocalDate deliveryDate, 
      @Valid @Required LocalDate expireDate) {

    Contract contract = contractDao.getContractById(contractId);
    Person person = contract.person;
    notFoundIfNull(contract.person);

    rules.checkIfPermitted(contract.person.office);
    Preconditions.checkState(contract.isPersistent());
    User admin = Security.getUser().get();

    MealTicketRecap recap;
    //Optional<Contract> contract = wrapperFactory.create(person).getCurrentContract();

    // riepilogo contratto corrente
    Optional<MealTicketRecap> currentRecap = mealTicketService.create(contract);
    Preconditions.checkState(currentRecap.isPresent());
    recap = currentRecap.get();

    if (ticketNumberFrom > ticketNumberTo) {
      Validation.addError("ticketNumberFrom", "sequenza non valida");
    }

    if (Validation.hasErrors()) {

      render("@personMealTickets", person, recap, codeBlock, ticketNumberFrom, ticketNumberTo,
          deliveryDate, expireDate, admin);
    }

    //Controllo dei parametri
    Office office = person.office;
    if (office == null) {
      flash.error("dramma");
      render("@personMealTickets", person, recap, codeBlock, ticketNumberFrom, ticketNumberTo,
          deliveryDate, expireDate, admin);
    }

    List<MealTicket> ticketToAddOrdered = Lists.newArrayList();
    ticketToAddOrdered.addAll(mealTicketService.buildBlockMealTicket(codeBlock, 
        ticketNumberFrom, ticketNumberTo, expireDate, office));

    ticketToAddOrdered.forEach(ticket -> {
      validation.valid(ticket);          

    });
    if (Validation.hasErrors()) {

      Validation.errors().forEach(error -> {
        if (error.getKey().equals(".code")) {
          flash.error(Messages.get("mealTicket.error"));
          render("@personMealTickets", person, recap, codeBlock, ticketNumberFrom, ticketNumberTo,
              deliveryDate, expireDate, admin);
        }
      });

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

    personMealTickets(contract.id, deliveryDate.getYear(), deliveryDate.getMonthOfYear());
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
  public static void returnPersonCodeBlock(Long contractId, String codeBlock, int first, int last,
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
  public static void performReturnPersonCodeBlock(Long contractId, String codeBlock,
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
    //TODO: provvisorio ci vanno anno e mese da cui sono partito per fare la modifica
    editPersonMealTickets(contract.id, Integer.parseInt(session.get("yearSelected")), 
        Integer.parseInt(session.get("monthSelected")));
  }


  /**
   * Funzione di eliminazione inserimento blocco alla persona.
   *
   * @param contractId contratto di riferimento
   * @param codeBlock  codice blocco
   * @param first      dal
   * @param last       al
   */
  public static void deletePersonCodeBlock(Long contractId, String codeBlock, int first, int last) {

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
  public static void performDeletePersonCodeBlock(Long contractId, String codeBlock,
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
    //TODO: provvisorio ci vanno anno e mese da cui sono partito per fare la modifica
    editPersonMealTickets(contract.id, Integer.parseInt(session.get("yearSelected")), 
        Integer.parseInt(session.get("monthSelected")));
  }

  /**
   * Funzione di Ricerca di un blocco nel database ePAS.
   *
   * @param code codice match
   */
  public static void findCodeBlock(String code) {

    List<BlockMealTicket> blocks = Lists.newArrayList();
    List<MealTicket> mealTicket = Lists.newArrayList();
    if (code != null && !code.isEmpty()) {
      if (Security.getUser().get().isSystemUser()) {
        mealTicket = mealTicketDao.getMealTicketsMatchCodeBlock(code,
            Optional.<Office>absent());

      } else {
        mealTicket = mealTicketDao
            .getMealTicketsMatchCodeBlock(code, 
                Optional.of(Security.getUser().get().person.office));
      }
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
