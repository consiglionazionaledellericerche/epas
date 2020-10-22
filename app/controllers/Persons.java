package controllers;


import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.gdata.util.common.base.Preconditions;
import dao.OfficeDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.UserDao;
import dao.absences.AbsenceComponentDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import helpers.Web;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ContractManager;
import manager.EmailManager;
import manager.OfficeManager;
import manager.PersonManager;
import manager.UserManager;
import manager.configurations.ConfigurationManager;
import manager.recaps.personstamping.PersonStampingRecapFactory;
import manager.services.absences.AbsenceService;
import models.Badge;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonChildren;
import models.PersonDay;
import models.User;
import models.VacationPeriod;
import models.WorkingTimeType;
import org.apache.commons.lang.WordUtils;
import org.joda.time.LocalDate;
import play.data.validation.Equals;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With({Resecure.class})
public class Persons extends Controller {

  @Inject
  static UserManager userManager;
  @Inject
  static PersonManager personManager;
  @Inject
  static EmailManager emailManager;
  @Inject
  static OfficeManager officeManager;
  @Inject
  static PersonDao personDao;
  @Inject
  static WrapperModelFunctionFactory wrapperFunctionFactory;
  @Inject
  static ContractManager contractManager;
  @Inject
  static SecurityRules rules;
  @Inject
  static UserDao userDao;
  @Inject
  static IWrapperFactory wrapperFactory;
  @Inject
  static PersonChildrenDao personChildrenDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static AbsenceService absenceService;
  @Inject
  static PersonStampingRecapFactory stampingsRecapFactory;
  @Inject
  static AbsenceComponentDao absenceComponentDao;


  /**
   * il metodo per ritornare la lista delle persone.
   *
   * @param name l'eventuale nome su cui restringere la ricerca.
   */
  public static void list(Long officeId, String name) {

    Office office;
    if (officeId == null) {
      office = officeDao.getOfficeById(Long.parseLong(session.get("officeSelected")));
    } else {
      office = officeDao.getOfficeById(officeId);
    }
    notFoundIfNull(office);

    rules.checkIfPermitted(office);

    List<Person> simplePersonList = personDao
        .listFetched(Optional.fromNullable(name), ImmutableSet.of(office), false, null, null, false)
        .list();

    List<IWrapperPerson> personList =
        FluentIterable.from(simplePersonList).transform(wrapperFunctionFactory.person()).toList();

    render(personList, office);
  }


  /**
   * metodo che gestisce la pagina di inserimento persona.
   */
  public static void insertPerson() {

    Person person = new Person();
    Contract contract = new Contract();

    render(person, contract);
  }

  /**
   * metodo che salva la persona inserita con il suo contratto.
   *
   * @param person la persona da inserire
   * @param contract il contratto associato alla persona
   */
  public static void save(@Valid @Required Person person, @Valid Contract contract) {

    if (contract.endDate != null && !contract.endDate.isAfter(contract.beginDate)) {
      Validation.addError("contract.endDate", "Dev'essere successivo all'inizio del contratto");
    }

    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori indicati");
      render("@insertPerson", person, contract);
    }

    rules.checkIfPermitted(person.office);

    person.name = WordUtils.capitalizeFully(person.name);
    person.surname = WordUtils.capitalizeFully(person.surname);

    personManager.properPersonCreate(person);
    person.save();

    contract.person = person;

    if (!contractManager.properContractCreate(contract, Optional.absent(), false)) {
      flash.error(
          "Errore durante la creazione del contratto. " + "Assicurarsi di inserire date valide.");
      params.flash(); // add http parameters to the flash scope
      edit(person.id);
    }

    person.save();

    userManager.generateRecoveryToken(person);
    emailManager.newUserMail(person);

    log.info("Creata nuova persona: id[{}] - {}", person.id, person.fullName());

    JPA.em().flush();
    JPA.em().clear();

    // La ricomputazione nel caso di creazione persona viene fatta alla fine.
    person = personDao.getPersonById(person.id);
    person.beginDate = LocalDate.now().withDayOfMonth(1).withMonthOfYear(1).minusDays(1);
    person.save();

    configurationManager.updateConfigurations(person);

    contractManager.recomputeContract(contract, Optional.<LocalDate>absent(), true, false);

    flash.success("Persona inserita correttamente in anagrafica - %s", person.fullName());

    list(null, null);
  }

  /**
   * metodo per visualizzare e modificare le informazioni di una persona.
   *
   * @param personId l'id della persona da modificare
   */
  public static void edit(Long personId) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);

    rules.checkIfPermitted(person.office);

    render(person);
  }

  /**
   * il metodo che permette il salvataggio delle informazioni modificate di una persona.
   *
   * @param person la persona da modificare
   */
  public static void update(@Valid Person person) {

    notFoundIfNull(person);

    if (Validation.hasErrors()) {
      log.warn("validation errors for {}: {}", person, validation.errorsMap());
      flash.error("Correggere gli errori indicati.");
      Validation.keep();
      edit(person.id);
    }

    rules.checkIfPermitted(person.office);

    person.save();
    flash.success("Modificate informazioni per l'utente %s %s", person.name, person.surname);

    // FIXME: la modifica della persona dovrebbe far partire qualche ricalcolo??
    // esempio qualifica, office possono far cambiare qualche decisione dell'alg.

    edit(person.id);
  }

  /**
   * metodo che permette la cancellazione di una persona.
   *
   * @param personId l'id della persona da cancellare
   */
  public static void deletePerson(Long personId) {

    Person person = personDao.getPersonById(personId);

    notFoundIfNull(person);

    rules.checkIfPermitted(person.office);

    render(person);
  }


  /**
   * il metodo che svolge l'effettiva cancellazione della persona.
   *
   * @param personId l'id della persona da cancellare
   */
  @SuppressWarnings("deprecation")
  public static void deletePersonConfirmed(Long personId) {

    Person person = personDao.getPersonById(personId);

    notFoundIfNull(person);

    // FIX per oggetto in entityManager monco.
    person.refresh();

    rules.checkIfPermitted(person.office);

    // FIXME: se non spezzo in transazioni errore HashMap.
    // Per adesso spezzo l'eliminazione della persona in tre fasi.
    // person.delete() senza errore HashMap dovrebbe però essere sufficiente.

    for (Contract c : person.contracts) {
      c.delete();
    }

    for (Badge b : person.badges) {
      b.delete();
    }

    JPAPlugin.closeTx(false);
    JPAPlugin.startTx(false);
    person = personDao.getPersonById(personId);

    for (PersonDay pd : person.personDays) {
      pd.delete();
    }

    JPAPlugin.closeTx(false);
    JPAPlugin.startTx(false);
    person = personDao.getPersonById(personId);

    person.delete();

    flash.success("La persona %s %s eliminata dall'anagrafica" + " insieme a tutti i suoi dati.",
        person.name, person.surname);

    list(null, null);

  }

  /**
   * metodo che mostra la situazione delle ferie secondo il corrente piano ferie.
   *
   * @param personId l'id della persona di cui si intende vedere la situazione.
   */
  public static void showCurrentVacation(Long personId) {

    Person person = personDao.getPersonById(personId);

    notFoundIfNull(person);

    rules.checkIfPermitted(person.office);

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    Preconditions.checkState(wrPerson.getCurrentVacationPeriod().isPresent());

    VacationPeriod vp = wrPerson.getCurrentVacationPeriod().get();
    render(person, vp);
  }

  /**
   * metoto che mostra l'attuale contratto e orario di lavoro associato della persona.
   *
   * @param personId l'id della persona di cui si intendono vedere queste informazioni
   */
  public static void showCurrentContractWorkingTimeType(Long personId) {

    Person person = personDao.getPersonById(personId);

    notFoundIfNull(person);

    rules.checkIfPermitted(person.office);

    IWrapperPerson wrPerson = wrapperFactory.create(person);

    Preconditions.checkState(wrPerson.getCurrentContractWorkingTimeType().isPresent());

    ContractWorkingTimeType cwtt = wrPerson.getCurrentContractWorkingTimeType().get();

    WorkingTimeType wtt = cwtt.workingTimeType;

    render(person, cwtt, wtt);
  }

  /**
   * Modifica password.
   */
  public static void changePassword() {
    User user = Security.getUser().get();
    notFoundIfNull(user);
    render(user);
  }

  /**
   * Salva la nuova password.
   *
   * @param vecchiaPassword vecchia password
   * @param nuovaPassword nuova password
   * @param confermaPassword ripeti
   */
  public static void savePassword(@Required String vecchiaPassword,
      @MinSize(5) @Required String nuovaPassword, @Required @Equals(value = "nuovaPassword",
          message = "Le password non corrispondono") String confermaPassword) {

    if (Validation.hasErrors()) {
      flash.error("Correggere gli errori riportati");
      final User user = Security.getUser().get();
      render("@changePassword", vecchiaPassword, nuovaPassword, confermaPassword, user);
    }

    final User user = userDao.getUserByIdAndPassword(Security.getUser().get().id,
        Optional.of(Codec.hexMD5(vecchiaPassword)));

    if (user == null) {
      flash.error("Nessuna corrispondenza trovata fra utente e vecchia password inserita.");
      changePassword();
    }

    notFoundIfNull(user);

    user.password = Codec.hexMD5(nuovaPassword);
    user.save();
    flash.success(Messages.get("passwordSuccessfullyChanged"));
    changePassword();
  }

  /**
   * Lista figli del dipendente.
   *
   * @param personId personId
   */
  public static void children(Long personId) {

    Person person = personDao.getPersonById(personId);
    render(person);
  }

  /**
   * Nuovo figlio per il dipendente.
   *
   * @param personId personId
   */
  public static void insertChild(Long personId) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);
    PersonChildren child = new PersonChildren();
    child.person = person;
    render(child);
  }

  /**
   * Modifica figlio.
   *
   * @param childId childId
   */
  public static void editChild(Long childId) {

    PersonChildren child = personChildrenDao.getById(childId);
    notFoundIfNull(child);
    render("@insertChild", child);
  }

  /**
   * Rimozione figlio.
   *
   * @param childId childId.
   */
  public static void deleteChild(Long childId, boolean confirmed) {

    PersonChildren child = personChildrenDao.getById(childId);
    notFoundIfNull(child);
    Person person = child.person;
    rules.checkIfPermitted(person.office);

    if (!confirmed) {
      render("@deleteChild", child);
    }
    child.delete();
    JPA.em().flush();

    // Scan degli errori sulle assenze
    LocalDate eldest = child.bornDate;
    person.refresh();
    for (PersonChildren otherChild : child.person.personChildren) {
      if (eldest.isAfter(otherChild.bornDate)) {
        eldest = otherChild.bornDate;
      }
    }
    absenceService.scanner(child.person, eldest);

    flash.error("Eliminato %s %s dall'anagrafica dei figli di %s", child.name, child.surname,
        person.getFullname());

    children(person.id);
  }


  /**
   * Salva il figlio.
   *
   * @param child child
   */
  public static void saveChild(@Valid PersonChildren child) {

    if (!Validation.hasErrors()) {
      for (PersonChildren otherChild : personChildrenDao.getAllPersonChildren(child.person)) {
        if (child.isPersistent() && otherChild.id == child.id) {
          continue;
        }
        if (otherChild.name.equals(child.name) && otherChild.surname.equals(child.surname)
            || otherChild.name.equals(child.surname) && otherChild.surname.equals(child.name)) {
          Validation.addError("child.name", "nome e cognome già presenti.");
          Validation.addError("child.surname", "nome e cognome già presenti.");
        }
      }
    }
    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors: {}", validation.errorsMap());
      render("@insertChild", child);
    }

    rules.checkIfPermitted(child.person.office);
    child.save();

    JPA.em().flush();
    child.person.refresh();


    log.info("Aggiunto/Modificato {} {} nell'anagrafica dei figli di {}", child.name, child.surname,
        child.person);
    flash.success(Web.msgSaved(PersonChildren.class));

    children(child.person.id);
  }

}
