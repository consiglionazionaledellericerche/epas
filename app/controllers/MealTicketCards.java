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
import com.google.gdata.util.common.base.Preconditions;
import common.security.SecurityRules;
import dao.MealTicketCardDao;
import dao.MealTicketDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.WrapperPerson;
import java.util.List;
import javax.inject.Inject;
import manager.ConsistencyManager;
import manager.MealTicketCardManager;
import manager.services.mealtickets.IMealTicketsService;
import manager.services.mealtickets.MealTicketRecap;
import models.Contract;
import models.MealTicket;
import models.MealTicketCard;
import models.Office;
import models.Person;
import models.User;
import org.joda.time.LocalDate;
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
    List<Person> personList = personDao.activeWithNumber(office);
    render(office, personList);
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
    if (mealTicketCard.getNumber() < 1) {
      Validation.addError("mealTicketCard.number", 
          "Il numero della card deve essere maggiore di zero!!!");
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
      mealTicketCard.get().delete();
      flash.success("Tessera correttamente rimossa");      
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
  public static void personMealTickets(Long personId, Integer year, Integer month) {
    
    Person person = personDao.getPersonById(personId);

    Preconditions.checkArgument(person.isPersistent());
    rules.checkIfPermitted(person.getOffice());
    
    if (year == null || month == null) {
      year = LocalDate.now().getYear();
      month = LocalDate.now().getMonthOfYear();
    }
    IWrapperPerson wrPerson = wrapperFactory.create(person);
    MealTicketRecap recap = mealTicketService.create(wrPerson.getCurrentContract().get()).orNull();
    Preconditions.checkNotNull(recap);
    LocalDate deliveryDate = LocalDate.now();
    MealTicketCard card = person.actualMealTicketCard();
    LocalDate expireDate = mealTicketDao
        .getFurtherExpireDateInOffice(person.getOffice());
    List<MealTicket> unAssignedElectronicMealTickets = mealTicketDao
        .getUnassignedElectronicMealTickets(wrPerson.getCurrentContract().get());
    User admin = Security.getUser().get();
    
    render(person, card, admin, deliveryDate, year, month, 
        expireDate, recap, unAssignedElectronicMealTickets);
  }
  
  /**
   * Assegna i buoni elettronici senza card alla card attualmente in uso dal dipendente.
   *
   * @param cardId l'identificativo della card elettronica
   * @param personId l'identificativo della persona
   * @param unAssignedElectronicMealTickets la lista dei buoni da assegnare
   */
  public static void assignOrphanElectronicMealTickets(Long cardId, Long personId) {
    java.util.Optional<MealTicketCard> card = mealTicketCardDao.getMealTicketCardById(cardId);
    Person person = personDao.getPersonById(personId);
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
        admin, person, expireDate, office);
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
  
  public static void deleteElectronicMealTicketFromCard() {
    
  }
}

