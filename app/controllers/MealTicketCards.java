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

package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;
import common.security.SecurityRules;
import dao.ContractDao;
import dao.ContractMonthRecapDao;
import dao.MealTicketCardDao;
import dao.MealTicketDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import it.cnr.iit.epas.DateInterval;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.MealTicketCardManager;
import manager.services.mealtickets.BlockMealTicket;
import manager.services.mealtickets.IMealTicketsService;
import manager.services.mealtickets.MealTicketRecap;
import manager.services.mealtickets.MealTicketStaticUtility;
import models.Contract;
import models.ContractMonthRecap;
import models.MealTicket;
import models.MealTicketCard;
import models.Office;
import models.Person;
import models.User;
import org.apache.commons.compress.archivers.ArchiveException;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller di gestione delle card dei buoni elettronici.
 *
 * @author dario
 *
 */
@Slf4j
@With({Resecure.class})
public class MealTicketCards extends Controller {

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static MealTicketCardManager mealTicketCardManager;
  @Inject
  private static MealTicketCardDao mealTicketCardDao;
  @Inject
  private static ConsistencyManager consistencyManager;
  @Inject
  private static MealTicketDao mealTicketDao;
  @Inject
  private static IMealTicketsService mealTicketService;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  private static ContractDao contractDao;
  @Inject
  private static ContractMonthRecapDao contractMonthRecapDao;

  /**
   * Ritorna la lista delle persone per verificare le associazioni con le card dei buoni 
   * elettronici.
   *
   * @param officeId l'id della sede di cui cercare la lista di persone
   */
  public static void mealTicketCards(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    LocalDate date = LocalDate.now();
    List<ContractMonthRecap> monthRecapList = contractMonthRecapDao
        .getPersonMealticket(new YearMonth(date.getYear(), date.getMonthOfYear()), 
            Optional.<Integer>absent(), Optional.<String>absent(), Sets.newHashSet(office));
    
    render(office, monthRecapList);
  }

  /**
   * Apre la form di inserimento di una nuova tessera elettronica.
   *
   * @param personId l'identificativo della persona per cui inserire una nuova tessera
   */
  public static void addNewCard(Long personId) {
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
    Optional<User> user = Security.getUser();
    MealTicketCard mealTicketCard = new MealTicketCard();
    mealTicketCard.setPerson(person);
    mealTicketCard.setDeliveryOffice(user.get().getPerson().getOffice());
    render(person, mealTicketCard);
  }

  /**
   * Salva la nuova tessera elettronica.
   *
   * @param mealTicketCard la tessera da salvare
   * @param person la persona cui associarla
   * @param office la sede proprietaria che associa la tessea
   */
  public static void saveNewCard(MealTicketCard mealTicketCard, Person person, Office office) {
    notFoundIfNull(person);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    if (mealTicketCard.getDeliveryDate() == null) {
      Validation.addError("mealTicketCard.deliveryDate", "La data deve essere valorizzata!!!");
    }
    if (mealTicketCardDao.getMealTicketCardByDeliveryDate(mealTicketCard
        .getDeliveryDate(), person).isPresent()) {
      Validation.addError("mealTicketCard.deliveryDate", 
          "Esiste già una card con questa data di consegna!!");
    }
    if (mealTicketCard.getNumber() == null || mealTicketCard.getNumber().isEmpty()) {
      Validation.addError("mealTicketCard.number", 
          "Il numero della card deve essere valorizzato!!!");
    }
    if (mealTicketCardDao
        .getMealTicketCardByNumberAndOffice(mealTicketCard.getNumber(), office).isPresent()) {
      Validation.addError("mealTicketCard.number", "Il numero di tessera esiste già!!");
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      person = personDao.getPersonById(person.id);
      mealTicketCard.setPerson(person);
      mealTicketCard.setDeliveryOffice(office);
      render("@addNewCard", mealTicketCard, person);
    }
    mealTicketCardManager.saveMealTicketCard(mealTicketCard, person, office);

    flash.success("Associata nuova tessera a %s", person.getFullname());

    MealTicketCards.mealTicketCards(person.getOffice().id);
  }

  /**
   * Metodo di rimozione di una tessera elettronica.
   *
   * @param mealTicketCardId l'identificativo della tessera da rimuovere
   */
  public static void deleteCard(Long mealTicketCardId) {
    java.util.Optional<MealTicketCard> mealTicketCard = mealTicketCardDao
        .getMealTicketCardById(mealTicketCardId);
    if (mealTicketCard.isPresent()) {
      if (!mealTicketCard.get().getMealTickets().isEmpty()) {
        flash.error("La tessera presenta %s buoni elettronici associati. "
            + "Rimuoverli prima di eliminarla!", 
            mealTicketCard.get().getMealTickets().size());
      } else {
        mealTicketCard.get().delete();
        flash.success("Tessera correttamente rimossa"); 
      }      
           
    } else {
      flash.error("Nessuna tessera corrispondente all'id selezionato. Verificare.");
    }
    MealTicketCards.mealTicketCards(mealTicketCard.get().getPerson().getOffice().id);
  }
  
  /**
   * Informazioni sulla card elettronica.
   *
   * @param mealTicketCardId l'identificativo della card
   */
  public static void infoCard(Long mealTicketCardId) {
    java.util.Optional<MealTicketCard> card = mealTicketCardDao
        .getMealTicketCardById(mealTicketCardId);
    if (card.isPresent()) {
      MealTicketCard mealTicketCard = card.get();
      render(mealTicketCard);
    }
  }
  
  /**
   * Ritorna la pagina di inserimento dei buoni pasto elettronici.
   *
   * @param personId l'identificativo della persona
   * @param year l'anno
   * @param month il mese
   */
  public static void personMealTickets(Long contractId, Integer year, Integer month) {
    
    //Person person = personDao.getPersonById(personId);
    Contract contract = contractDao.getContractById(contractId);

    //Preconditions.checkArgument(person.isPersistent());
    Preconditions.checkArgument(contract.getPerson().isPersistent());
    rules.checkIfPermitted(contract.getPerson().getOffice());
    
    if (year == null || month == null) {
      year = LocalDate.now().getYear();
      month = LocalDate.now().getMonthOfYear();
    }
    MealTicketRecap recap = mealTicketService.create(contract).orNull();
    Preconditions.checkNotNull(recap);
    LocalDate deliveryDate = LocalDate.now();
    MealTicketCard card = contract.getPerson().actualMealTicketCard();
    LocalDate expireDate = mealTicketDao
        .getFurtherExpireDateInOffice(contract.getPerson().getOffice());
    List<MealTicket> unAssignedElectronicMealTickets = mealTicketDao
        .getUnassignedElectronicMealTickets(contract);
    User admin = Security.getUser().get();
    Person person = contract.getPerson();
    render(person, card, admin, deliveryDate, year, month,  
        expireDate, recap, unAssignedElectronicMealTickets);
  }
  
  /**
   * Assegna i buoni elettronici senza card alla card attualmente in uso dal dipendente.
   *
   * @param cardId l'identificativo della card elettronica
   * @param personId l'identificativo della persona
   */
  public static void assignOrphanElectronicMealTickets(Long cardId, Long personId) {
    java.util.Optional<MealTicketCard> card = mealTicketCardDao.getMealTicketCardById(cardId);
    if (!card.isPresent()) {
      flash.error("Non sono presenti card associate al dipendente! "
          + "Assegnare una card e riprovare!");
    } else {
      mealTicketCardManager.assignOldElectronicMealTicketsToCard(card.get());
      flash.success("Buoni elettronici associati correttamente alla card %s", 
          card.get().getNumber());
    }
    personMealTickets(personId, LocalDate.now().getYear(), LocalDate.now().getMonthOfYear());
  }

  /**
   * Salva i buoni elettronici sulla card del dipendente.
   *
   * @param personId l'identificativo del dipendente
   * @param card la card su cui caricare i buoni
   * @param deliveryDate la data di consegna dei buoni
   * @param tickets il numero di buoni da caricare
   * @param expireDate la data di scadenza dei buoni
   */
  public static void submitPersonMealTicket(Long personId, MealTicketCard card, 
      LocalDate deliveryDate, Integer tickets, @Valid @Required LocalDate expireDate) {
    
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());
    Office office = person.getOffice();
    User admin = Security.getUser().get();
    
    mealTicketCardManager.saveElectronicMealTicketBlock(card, deliveryDate, tickets, 
        admin, expireDate, office);
    consistencyManager.updatePersonRecaps(person.id, deliveryDate);
    flash.success("Il blocco inserito è stato salvato correttamente.");

    personMealTickets(person.id, deliveryDate.getYear(), deliveryDate.getMonthOfYear());
  }
  
  /**
   * La pagina di modifica dei buoni elettronici consegnati al dipendente.
   *
   * @param personId l'identificativo della persona
   * @param year l'anno
   * @param month il mese
   */
  public static void editPersonMealTickets(Long personId, Integer year, Integer month) {
    notFoundIfNull(personId);
    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.getOffice());

    // riepilogo contratto corrente
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    Optional<MealTicketRecap> currentRecap = mealTicketService
        .create(wrPerson.getCurrentContract().get());
    Preconditions.checkState(currentRecap.isPresent());
    MealTicketRecap recap = currentRecap.get();
    
    render(person, recap, year, month);
  }
  
  /**
   * La pagina di riepilogo dei buoni elettronici consegnati al dipendente.
   *
   * @param personId l'identificativo della persona
   * @param year l'anno
   * @param month il mese
   */
  public static void recapPersonMealTickets(Long personId, Integer year, Integer month) {
    Person person = personDao.getPersonById(personId);
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    Contract contract = wrPerson.getCurrentContract().get();
    Preconditions.checkState(contract.isPersistent());
    Preconditions.checkArgument(contract.getPerson().isPersistent());
    rules.checkIfPermitted(contract.getPerson().getOffice());

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
    
    render(person, recap, recapPrevious, year, month);
  }
  
  /**
   * Ritorna la schermata di cancellazione dei buoni.
   *
   * @param contractId l'identificativo del contratto
   * @param codeBlock il codice di blocco 
   * @param first il primo buono consegnato
   * @param last l'ultimo buono consegnato
   */
  public static void deleteElectronicMealTicketFromCard(Long contractId, 
      String codeBlock, int first, int last) {
    Contract contract = contractDao.getContractById(contractId);
    notFoundIfNull(contract);
    rules.checkIfPermitted(contract.getPerson().getOffice());

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
    rules.checkIfPermitted(contract.getPerson().getOffice());

    List<MealTicket> mealTicketList = mealTicketDao.getMealTicketsInCodeBlock(codeBlock,
        Optional.fromNullable(contract));

    Preconditions.checkState(mealTicketList.size() > 0);

    List<MealTicket> mealTicketToRemove = MealTicketStaticUtility
        .blockPortion(mealTicketList, contract, first, last);
    LocalDate pastDate = LocalDate.now();
    for (MealTicket mealTicket : mealTicketList) {
      if (mealTicket.getDate().isBefore(pastDate)) {
        pastDate = mealTicket.getDate();
      }
    }

    List<BlockMealTicket> blocks = MealTicketStaticUtility.getBlockMealTicketFromOrderedList(
        mealTicketToRemove, Optional.<DateInterval>absent());

    if (!confirmed) {
      response.status = 400;
      confirmed = true;
      render("@deleteElectronicMealTicketFromCard", contract, 
          codeBlock, blocks, first, last, confirmed);
    }

    int deleted = 0;
    for (MealTicket mealTicket : mealTicketToRemove) {
      if (mealTicket.getDate().isBefore(pastDate)) {
        pastDate = mealTicket.getDate();
      }

      mealTicket.delete();
      deleted++;
    }

    consistencyManager.updatePersonSituation(contract.getPerson().id, pastDate);

    flash.success("Blocco di %d buoni rimosso correttamente.", deleted);
    //TODO: provvisorio ci vanno anno e mese da cui sono partito per fare la modifica
    editPersonMealTickets(contract.getPerson().id, Integer.parseInt(session.get("yearSelected")), 
        Integer.parseInt(session.get("monthSelected")));
  }
  
  /**
   * Genera il report in formato excel per i buoni elettronici.
   *
   * @param officeId l'identificativo della sede
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @throws IOException eccezione I/O
   * @throws ArchiveException eccezione in generazione d'archivio
   */
  public static void generateReport(Long officeId, Integer year, Integer month) 
      throws IOException, ArchiveException {
    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);
    rules.checkIfPermitted(office);
    Set<Office> offices = Sets.newHashSet(office);
    LocalDate beginDate = new LocalDate(year, month, 1);
    LocalDate endDate = beginDate.dayOfMonth().withMaximumValue();
    List<Person> personList = personDao.list(
        Optional.<String>absent(), offices, false, beginDate, 
        endDate, true).list();
    InputStream file = null;
    file = mealTicketCardManager
        .buildFile(office, personList, year, month);

    renderBinary(file, "export.zip", false);
  }
}

