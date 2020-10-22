package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.querydsl.core.QueryResults;
import dao.BadgeDao;
import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.PersonDao;
import helpers.Web;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import manager.BadgeManager;
import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Person;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.db.jpa.GenericModel;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;


@With(Resecure.class)
public class BadgeSystems extends Controller {

  private static final Logger log = LoggerFactory.getLogger(BadgeSystems.class);

  @Inject
  private static BadgeSystemDao badgeSystemDao;
  @Inject
  private static BadgeReaderDao badgeReaderDao;
  @Inject
  private static BadgeManager badgeManager;
  @Inject
  private static BadgeDao badgeDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static PersonDao personDao;

  public static void index() {
    flash.keep();
    list(null);
  }

  /**
   * Ritorna la lista dei gruppi badge.
   * @param name nome del lettore badge su cui si vuole filtrare.
   */
  public static void list(String name) {

    QueryResults<?> results =
        badgeSystemDao.badgeSystems(Optional.fromNullable(name),
            Optional.absent()).listResults();

    render(results, name);
  }

  /**
   * Ritorna le informazioni del gruppo badge.
   * @param id identificativo del gruppo badge.
   */
  public static void show(Long id) {
    final BadgeSystem badgeSystem = badgeSystemDao.byId(id);
    notFoundIfNull(badgeSystem);
    render(badgeSystem);
  }

  /**
   * Permette la modifica del gruppo badge.
   * @param id identificativo del gruppo badge.
   */
  public static void edit(Long id) {

    final BadgeSystem badgeSystem = badgeSystemDao.byId(id);
    notFoundIfNull(badgeSystem);
    rules.checkIfPermitted(badgeSystem.office);

    QueryResults<?> badgeReadersResults = badgeReaderDao.badgeReaders(Optional.absent(),
        Optional.fromNullable(badgeSystem)).listResults();

    List<Badge> badges = badgeSystemDao.badges(badgeSystem)
        .stream().distinct().collect(Collectors.toList());

    render(badgeSystem, badgeReadersResults, badges);

  }

  public static void blank() {
    render();
  }


  /**
   * Permette l'aggiornamento delle info sul gruppo badge.
   * @param badgeSystem l'oggetto per cui si vogliono cambiare le impostazioni.
   */
  public static void updateInfo(@Valid BadgeSystem badgeSystem) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeSystem, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", badgeSystem);
    }

    rules.checkIfPermitted(badgeSystem.office);
    badgeSystem.save();

    flash.success(Web.msgSaved(BadgeSystem.class));
    edit(badgeSystem.id);
  }


  /**
   * Salva il gruppo badge.
   * @param badgeSystem badgeSystem da salvare.
   */
  public static void save(@Valid BadgeSystem badgeSystem) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeSystem, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@blank", badgeSystem);
    }

    rules.checkIfPermitted(badgeSystem.office);

    badgeSystem.save();

    index();
  }

  /**
   * Cancella il gruppo badge.
   * @param id identificativo del badge reader da eliminare.
   */
  public static void delete(Long id) {
    final BadgeSystem badgeSystem = badgeSystemDao.byId(id);
    notFoundIfNull(badgeSystem);
    rules.checkIfPermitted(badgeSystem.office);

    //elimino il gruppo se non è associato ad alcuna sorgente.
    if (badgeSystem.badgeReaders.isEmpty()) {
      badgeSystem.delete();
      flash.success(Web.msgDeleted(BadgeSystem.class));
      index();
    }
    flash.error("Per poter eliminare il gruppo è necessario che non sia associato ad alcuna"
        + "sorgente timbrature");
    edit(badgeSystem.id);
  }

  /**
   * Permette l'apertura della pagina per il salvataggio dei badge sul gruppo.
   * @param badgeSystemId l'identificativo del gruppo badge
   */
  public static void joinBadges(Long badgeSystemId) {

    final BadgeSystem badgeSystem = badgeSystemDao.byId(badgeSystemId);
    notFoundIfNull(badgeSystem);

    rules.checkIfPermitted(badgeSystem.office);

    /*
     * Dato che nell'edit della singola persona non viene inibito per nessuno l'inserimento dei
     * badge, anche qui recupero la lista completa del personale dell'ufficio.
     * Decidere se c'è la necessità di impedirlo per qualcuno e uniformare questa decisione sia
     * in questa vista che nell'edit della singola persona. (e ovviamente implementare lo stesso
     * controllo anche nella save).
     */
    List<Person> officePeople = personDao.list(Optional.absent(),
        Sets.newHashSet(badgeSystem.office), false, null, null, false).list();

    render("@joinBadges", badgeSystem, officePeople);
  }

  /**
   * Permette la join del badge alla persona.
   * @param personId l'identificativo della persona
   */
  public static void joinBadgesPerson(Long personId) {

    final Person person = personDao.getPersonById(personId);
    notFoundIfNull(person);
    rules.checkIfPermitted(person.office);

    boolean personFixed = true;

    BadgeSystem badgeSystem = null;
    if (!person.office.badgeSystems.isEmpty()) {
      badgeSystem = person.office.badgeSystems.get(0);
    }

    render("@joinBadges", person, badgeSystem, personFixed);

  }

  /**
   * Salva il badge alla persona nel gruppo badge.
   * @param badgeSystem il gruppo badge
   * @param code il numero del badge
   * @param person la persona cui salvare il badge
   * @param personFixed se è fixata la persona
   */
  public static void saveBadges(
      @Valid BadgeSystem badgeSystem, @Required String code, @Valid Person person,
      boolean personFixed) {

    rules.checkIfPermitted(badgeSystem.office);

    List<Person> activePersons = Lists.newArrayList();
    if (!Validation.hasError("badgeSystem")) {
      activePersons =
          personDao.list(
              Optional.absent(),
              Sets.newHashSet(badgeSystem.office), false,
              LocalDate.now(), LocalDate.now(), true).list();
    }

    if (Validation.hasErrors()) {
      response.status = 400;
      render("@joinBadges", badgeSystem, code, person, activePersons, personFixed);
    }

    List<Badge> violatedBadges = Lists.newArrayList();
    List<Badge> validBadges = Lists.newArrayList();

    for (BadgeReader badgeReader : badgeSystem.badgeReaders) {
      Badge badge = new Badge();
      badge.person = person;
      badge.code = code;
      badgeManager.normalizeBadgeCode(badge, false);
      badge.badgeSystem = badgeSystem;
      badge.badgeReader = badgeReader;

      Optional<Badge> alreadyExists = badgeDao.byCode(code, badgeReader);
      if (alreadyExists.isPresent()) {
        if (!alreadyExists.get().person.equals(badge.person)) {
          violatedBadges.add(alreadyExists.get());
        }
      } else {
        validBadges.add(badge);
      }
    }

    if (!violatedBadges.isEmpty()) {
      Validation.addError("code", "già assegnato in almeno una sorgente timbrature.");
      response.status = 400;
      render("@joinBadges", badgeSystem, code, person, activePersons, violatedBadges, personFixed);
    }

    for (Badge badge : validBadges) {
      badge.save();
    }

    flash.success(Web.msgSaved(Badge.class));
    if (personFixed) {
      personBadges(person.id);
    }
    edit(badgeSystem.id);
  }


  /**
   * Associa nel gruppo per tutti i dipendenti il vecchio campo person.number
   */
  public static void joinPersonNumbers(Long badgeSystemId) {

    BadgeSystem badgeSystem = badgeSystemDao.byId(badgeSystemId);
    notFoundIfNull(badgeSystem);
    rules.checkIfPermitted(badgeSystem.office);

    List<Person> personsOldBadge = personDao.activeWithNumber(badgeSystem.office);

    int personsInError = 0;

    for (Person person : personsOldBadge) {

      List<Badge> violatedBadges = Lists.newArrayList();
      List<Badge> validBadges = Lists.newArrayList();

      for (BadgeReader badgeReader : badgeSystem.badgeReaders) {
        Badge badge = new Badge();
        badge.person = person;
        badge.code = person.number.replaceFirst("^0+(?!$)", "");
        badge.badgeSystem = badgeSystem;
        badge.badgeReader = badgeReader;

        Optional<Badge> alreadyExists = badgeDao.byCode(badge.code, badgeReader);
        if (alreadyExists.isPresent()) {
          if (!alreadyExists.get().person.equals(badge.person)) {
            violatedBadges.add(alreadyExists.get());
          }
        } else {
          validBadges.add(badge);
        }
      }

      if (!violatedBadges.isEmpty()) {
        // segnalare il caso in modo più puntuale?
        personsInError++;
      } else {
        for (Badge badge : validBadges) {
          badge.save();
        }
        //person.badgeNumber = null;
        //person.save();
      }
    }

    if (personsInError == 0) {
      flash.success("Operazione completata con successo.");
    } else {
      flash.error("Operazione completata ma per %s dipendenti non è stato possibile "
          + "inserire il badge a causa di conflitto codici.", personsInError);
    }

    edit(badgeSystem.id);
  }

  /**
   * Render dei badge della persona.
   * @param personId l'id della persona
   */
  public static void personBadges(Long personId) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person.office);
    rules.checkIfPermitted(person.office);

    render(person);
  }

  /**
   * Permette la cancellazione di un badge.
   * @param badgeId identificativo del badge
   */
  public static void deleteBadgePerson(Long badgeId) {

    final Badge badge = badgeDao.byId(badgeId);
    notFoundIfNull(badge);
    rules.checkIfPermitted(badge.badgeSystem.office);

    boolean personFixed = true;
    boolean confirmed = true;
    render("@delete", badge, personFixed, confirmed);
  }

  /**
   * Cancella il badge.
   * @param badgeId l'identificativo del badge da cancellare
   * @param confirmed se è confermata la cancellazione
   * @param personFixed se la persona è fixata
   */
  public static void deleteBadge(Long badgeId, boolean confirmed, boolean personFixed) {

    final Badge badge = badgeDao.byId(badgeId);
    notFoundIfNull(badge);
    rules.checkIfPermitted(badge.badgeSystem.office);
    if (!confirmed) {
      confirmed = true;
      render("@delete", badge, confirmed);
    }

    badgeDao.byCodeAndPerson(badge.code, badge.person).forEach(GenericModel::delete);

    flash.success("Badge Rimosso con successo");

    if (personFixed) {
      personBadges(badge.person.id);
    }

    edit(badge.badgeSystem.id);
  }

}
