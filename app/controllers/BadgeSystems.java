package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.mysema.query.SearchResults;

import dao.BadgeDao;
import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.PersonDao;

import helpers.Web;

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
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;

import javax.inject.Inject;


@With({Resecure.class, RequestInit.class})
public class BadgeSystems extends Controller {

  private static final Logger log = LoggerFactory.getLogger(BadgeSystems.class);

  @Inject
  private static BadgeSystemDao badgeSystemDao;
  @Inject
  private static BadgeReaderDao badgeReaderDao;
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
   * @param name nome del lettore badge su cui si vuole filtrare.
   */
  public static void list(String name) {

    SearchResults<?> results =
        badgeSystemDao.badgeSystems(Optional.<String>fromNullable(name),
            Optional.<BadgeReader>absent()).listResults();

    render(results, name);
  }


  /**
   * @param id identificativo del gruppo badge.
   */
  public static void show(Long id) {
    final BadgeSystem badgeSystem = BadgeSystem.findById(id);
    notFoundIfNull(badgeSystem);
    render(badgeSystem);
  }

  /**
   * @param id identificativo del gruppo badge.
   */
  public static void edit(Long id) {

    final BadgeSystem badgeSystem = badgeSystemDao.byId(id);
    notFoundIfNull(badgeSystem);
    rules.checkIfPermitted(badgeSystem.office);

    SearchResults<?> badgeReadersResults = badgeReaderDao.badgeReaders(Optional.<String>absent(),
        Optional.fromNullable(badgeSystem)).listResults();

    List<Badge> allBadges = badgeSystemDao.badges(badgeSystem);
    List<Badge> badges = Lists.newArrayList();
    // FIXME: metodo non efficiente.
    for (Badge badge : allBadges) {
      boolean toPick = true;
      // TODO: al posto di questo for usare una mappa
      for (Badge picked : badges) {
        if (badge.badgeSystem.equals(picked.badgeSystem) && badge.code.equals(picked.code)) {
          toPick = false;
        }
      }
      if (toPick) {
        badges.add(badge);
      }
    }

    List<Person> personsOldBadge = personDao.activeWithBadgeNumber(badgeSystem.office);

    render(badgeSystem, badgeReadersResults, badges, personsOldBadge);

  }

  public static void blank() {
    render();
  }


  /**
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
   * @param badgeSystem badgeSystem
   * @param user        l'utente creato a partire dal badge reader.
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
   * @param id identificativo del badge reader da eliminare.
   */
  public static void delete(Long id) {
    final BadgeSystem badgeSystem = BadgeSystem.findById(id);
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

  public static void joinBadges(Long badgeSystemId) {

    final BadgeSystem badgeSystem = badgeSystemDao.byId(badgeSystemId);
    notFoundIfNull(badgeSystem);

    rules.checkIfPermitted(badgeSystem.office);

    List<Person> activePersons = personDao.list(Optional.<String>absent(),
        Sets.newHashSet(badgeSystem.office), false, LocalDate.now(), LocalDate.now(), true).list();

    render("@joinBadges", badgeSystem, activePersons);
  }

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

  public static void saveBadges(@Valid BadgeSystem badgeSystem, @Required String code,
                                @Valid Person person, boolean personFixed) {

    rules.checkIfPermitted(badgeSystem.office);

    List<Person> activePersons = Lists.newArrayList();
    if (!validation.hasError("badgeSystem")) {
      activePersons = personDao.list(Optional.<String>absent(),
          Sets.newHashSet(badgeSystem.office), false, LocalDate.now(), LocalDate.now(), true).list();
    }

    if (validation.hasErrors()) {
      response.status = 400;
      render("@joinBadges", badgeSystem, code, person, activePersons, personFixed);
    }

    List<Badge> violatedBadges = Lists.newArrayList();
    List<Badge> validBadges = Lists.newArrayList();

    for (BadgeReader badgeReader : badgeSystem.badgeReaders) {
      Badge badge = new Badge();
      badge.person = person;
      badge.code = code;
      badge.badgeSystem = badgeSystem;
      badge.badgeReader = badgeReader;

      Optional<Badge> alreadyExists = alreadyExists(badge);
      if (alreadyExists.isPresent()) {
        if (!alreadyExists.get().person.equals(badge.person)) {
          violatedBadges.add(alreadyExists.get());
        }
      } else {
        validBadges.add(badge);
      }
    }

    if (!violatedBadges.isEmpty()) {
      validation.addError("code", "già assegnato in almeno una sorgente timbrature.");
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
   * TODO: spostare nel manager o nel wrapper.
   */
  public static Optional<Badge> alreadyExists(Badge badge) {
    Optional<Badge> old = badgeDao.byCode(badge.code, badge.badgeReader);
    if (!old.isPresent()) {
      return Optional.<Badge>absent();
    } else {
      return old;
    }
  }

  /**
   * Associa nel gruppo per tutti i dipendenti il vecchio campo person.badgeNumber
   */
  public static void joinOldBadgeNumbers(Long badgeSystemId) {

    BadgeSystem badgeSystem = badgeSystemDao.byId(badgeSystemId);
    notFoundIfNull(badgeSystem);
    rules.checkIfPermitted(badgeSystem.office);

    List<Person> personsOldBadge = personDao.activeWithBadgeNumber(badgeSystem.office);

    int personsInError = 0;

    for (Person person : personsOldBadge) {

      List<Badge> violatedBadges = Lists.newArrayList();
      List<Badge> validBadges = Lists.newArrayList();

      for (BadgeReader badgeReader : badgeSystem.badgeReaders) {
        Badge badge = new Badge();
        badge.person = person;
        badge.code = person.badgeNumber.replaceFirst("^0+(?!$)", "");
        badge.badgeSystem = badgeSystem;
        badge.badgeReader = badgeReader;

        Optional<Badge> alreadyExists = alreadyExists(badge);
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
        badge.code = (person.number + "").replaceFirst("^0+(?!$)", "");
        badge.badgeSystem = badgeSystem;
        badge.badgeReader = badgeReader;

        Optional<Badge> alreadyExists = alreadyExists(badge);
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

  public static void personBadges(Long personId) {

    Person person = personDao.getPersonById(personId);
    notFoundIfNull(person.office);
    rules.checkIfPermitted(person.office);

    // FIXME: metodo non efficiente.
    List<Badge> badges = Lists.newArrayList();
    for (Badge badge : person.badges) {
      boolean toPick = true;
      // TODO: al posto di questo for usare una mappa
      for (Badge picked : badges) {
        if (badge.badgeSystem.equals(picked.badgeSystem) && badge.code.equals(picked.code)) {
          toPick = false;
        }
      }
      if (toPick) {
        badges.add(badge);
      }
    }

    render(person, badges);
  }

  public static void deleteBadgePerson(Long badgeId) {

    final Badge badge = badgeDao.byId(badgeId);
    notFoundIfNull(badge);
    rules.checkIfPermitted(badge.badgeSystem.office);

    boolean personFixed = true;
    boolean confirmed = true;
    render("@delete", badge, personFixed, confirmed);

  }

  public static void deleteBadge(Long badgeId, boolean confirmed, boolean personFixed) {

    final Badge badge = badgeDao.byId(badgeId);
    notFoundIfNull(badge);
    rules.checkIfPermitted(badge.badgeSystem.office);
    if (!confirmed) {
      confirmed = true;
      render("@delete", badge, confirmed);
    }

    for (Badge badgeToRemove : badge.person.badges) {
      if (badgeToRemove.badgeSystem.equals(badge.badgeSystem)
          && badgeToRemove.code.equals(badge.code)) {

        badgeToRemove.delete();
      }
    }

    flash.success("Badge Rimosso con successo");

    if (personFixed) {
      personBadges(badge.person.id);
    }

    edit(badge.badgeSystem.id);

  }


}
