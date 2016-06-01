package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.mysema.query.SearchResults;

import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;

import helpers.Web;

import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;


@With({Resecure.class, RequestInit.class})
public class BadgeReaders extends Controller {

  private static final Logger log = LoggerFactory.getLogger(BadgeReaders.class);

  @Inject
  private static BadgeReaderDao badgeReaderDao;
  @Inject
  private static BadgeSystemDao badgeSystemDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static RoleDao roleDao;
  @Inject
  private static UsersRolesOfficesDao uroDao;

  public static void index() {
    flash.keep();
    list(null);
  }

  /**
   *
   * @param name nome del lettore badge su cui si vuole filtrare.
   */
  public static void list(String name) {

    SearchResults<?> results =
        badgeReaderDao.badgeReaders(Optional.<String>fromNullable(name),
            Optional.<BadgeSystem>absent()).listResults();

    render(results, name);
  }


  /**
   *
   * @param id identificativo del lettore badge.
   */
  public static void show(Long id) {
    final BadgeReader badgeReader = BadgeReader.findById(id);
    notFoundIfNull(badgeReader);
    render(badgeReader);
  }

  /**
   *
   * @param id identificativo del lettore badge.
   */
  public static void edit(Long id) {


    final BadgeReader badgeReader = badgeReaderDao.byId(id);
    notFoundIfNull(badgeReader);
    rules.checkIfPermitted(badgeReader.user.owner);

    SearchResults<?> results = badgeSystemDao.badgeSystems(Optional.<String>absent(),
        Optional.fromNullable(badgeReader)).listResults();

    render(badgeReader, results);

  }

  public static void blank() {
    render();
  }

  /**
   * @param id identificativo del badge reader da eliminare.
   */
  public static void delete(Long id) {

    final BadgeReader badgeReader = BadgeReader.findById(id);
    notFoundIfNull(badgeReader);
    rules.checkIfPermitted(badgeReader.user.owner);

    //elimino la sorgente se non è associata ad alcun gruppo.
    if (badgeReader.badgeSystems.isEmpty()) {

      badgeReader.delete();

      // FIXME: issue della rimozione user delle persone che riferiscono lo storico.
      badgeReader.user.delete();

      flash.success(Web.msgDeleted(BadgeSystem.class));

      index();
    }
    flash.error("Per poter eliminare il gruppo è necessario che non sia associato ad alcuna"
        + "sorgente timbrature");
    edit(badgeReader.id);
  }


  /**
   *
   * @param badgeReader l'oggetto per cui si vogliono cambiare le impostazioni.
   */
  public static void updateInfo(@Valid BadgeReader badgeReader, @Valid Office owner) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeReader, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", badgeReader);
    }

    rules.checkIfPermitted(owner);
    badgeReader.user.owner = owner;
    badgeReader.save();
    badgeReader.user.save();

    flash.success(Web.msgSaved(BadgeReader.class));
    edit(badgeReader.id);
  }


  /**
   * @param user Utente a cui modificare la password.
   * @param newPass nuova password da associare al lettore.
   */
  public static void changePassword(@Valid User user,
                                    @MinSize(5) @Required String newPass) {

    notFoundIfNull(user.badgeReader);
    BadgeReader badgeReader = user.badgeReader;
    rules.checkIfPermitted(badgeReader.user.owner);

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", user, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", badgeReader);
    }

    Codec codec = new Codec();
    user.password = codec.hexMD5(newPass);
    user.save();

    flash.success(Web.msgSaved(BadgeReader.class));
    edit(badgeReader.id);

  }

  /**
   *
   * @param badgeReader l'oggetto badge reader da salvare.
   * @param user l'utente creato a partire dal badge reader.
   */
  public static void save(@Valid BadgeReader badgeReader, @Valid Office office, @Valid User user) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeReader, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@blank", badgeReader, office, user);
    }
    if (user.password.length() < 5) {
      response.status = 400;
      validation.addError("user.password", "almeno 5 caratteri");
      render("@blank", badgeReader, office, user);
    }
    
    rules.checkIfPermitted(office);

    badgeReader.user = user;
    badgeReader.user.owner = office;
    badgeReader.user.password = new Codec().hexMD5(badgeReader.user.password);
    badgeReader.user.save();
    badgeReader.save();

    flash.success(Web.msgSaved(BadgeReader.class));
    index();
  }

  /**
   * Gestore associazioni con i BadgeSystem.
   */
  public static void joinBadgeSystems(Long badgeReaderId) {

    final BadgeReader badgeReader = badgeReaderDao.byId(badgeReaderId);
    notFoundIfNull(badgeReader);

    rules.checkIfPermitted(badgeReader.user.owner);

    render("@joinBadgeSystems", badgeReader);
  }

  /**
   * Salva la nuova associazione.
   */
  public static void saveBadgeSystems(@Valid BadgeReader badgeReader, boolean confirmed) {

    rules.checkIfPermitted(badgeReader.user.owner);

    // TODO:
    //creare gli uro mancanti, cancellare quelli non più usati

    //Costruisco un pò di strutture dati di utilità....
    Set<BadgeSystem> badgeSystemsAdd = Sets.newHashSet();
    Set<BadgeSystem> badgeSystemsRemove = Sets.newHashSet();
    Set<BadgeSystem> badgeSystemsRemain = Sets.newHashSet();
    List<Badge> badgesDefinitelyToRemove = Lists.newArrayList();
    List<Badge> badgesToRemove = Lists.newArrayList();
    for (Badge badge : badgeReader.badges) {
      if (badgeReader.badgeSystems.contains(badge.badgeSystem)) {
        if (badgeSystemsRemain.contains(badge.badgeSystem)) {
          badgeSystemsRemain.add(badge.badgeSystem);
        }
      } else {
        if (!badgeSystemsRemove.contains(badge.badgeSystem)) {
          badgeSystemsRemove.add(badge.badgeSystem);
        }
        if (badge.badgeSystem.badgeReaders.size() == 1) {
          badgesDefinitelyToRemove.add(badge);
        }
        badgesToRemove.add(badge);
      }

    }
    for (BadgeSystem badgeSystem : badgeReader.badgeSystems) {
      if (!badgeSystemsRemain.contains(badgeSystem)) {
        badgeSystemsAdd.add(badgeSystem);
      }
    }

    List<Badge> violatedBadges = Lists.newArrayList();
    List<Badge> badgesToSave = Lists.newArrayList();

    for (BadgeSystem badgeSystem : badgeSystemsAdd) {

      // Prendere i codici del badge system
      Set<String> codes = Sets.newHashSet();
      for (Badge otherBadge : badgeSystem.badges) {
        if (!codes.contains(otherBadge.code)) {
          codes.add(otherBadge.code);
          Badge badge = new Badge();
          badge.person = otherBadge.person;
          badge.badgeSystem = badgeSystem;
          badge.badgeReader = badgeReader;
          badge.code = otherBadge.code;

          //Controllare che esistano nel badgeReader
          Optional<Badge> alreadyExists = BadgeSystems.alreadyExists(badge);
          if (alreadyExists.isPresent()) {
            if (!alreadyExists.get().person.equals(badge.person)) {
              violatedBadges.add(badge);
              violatedBadges.add(alreadyExists.get());
            }
          } else {
            badgesToSave.add(badge);
          }
        }
      }
    }

    if (!violatedBadges.isEmpty()) {
      response.status = 400;
      render("@joinBadgeSystems", badgeReader, violatedBadges);
      index();
    }

    if (!badgesDefinitelyToRemove.isEmpty() && !confirmed) {
      response.status = 400;
      render("@joinBadgeSystems", badgeReader, badgesDefinitelyToRemove);
    }

    badgeReader.save();
    for (Badge badge : badgesToRemove) {
      badge.delete();
    }
    for (Badge badge : badgesToSave) {
      badge.save();
    }

    // I RUOLI

    Role role = roleDao.getRoleByName(Role.BADGE_READER);

    for (BadgeSystem badgeSystem : badgeSystemsRemove) {
      Optional<UsersRolesOffices> uro = uroDao.getUsersRolesOffices(badgeReader.user,
          role, badgeSystem.office);
      if (uro.isPresent()) {
        uro.get().delete();
        log.info("UserRoleOffice rimosso: {}", uro);
      } else {
        log.warn("L'userRoleOffice da rimuovere {} {} {} avrebbe dovuto esistere.",
            badgeReader.code, role.name, badgeSystem.office);
      }
    }
    for (BadgeSystem badgeSystem : badgeSystemsAdd) {
      Optional<UsersRolesOffices> uro = uroDao.getUsersRolesOffices(badgeReader.user,
          role, badgeSystem.office);
      if (!uro.isPresent()) {
        UsersRolesOffices uroNew = new UsersRolesOffices();
        uroNew.office = badgeSystem.office;
        uroNew.role = role;
        uroNew.user = badgeReader.user;
        uroNew.save();
        log.info("UserRoleOffice creato: {}", uroNew);
      } else {
        log.warn("L'userRoleOffice da inserire {} {} {} esisteva già.",
            badgeReader.code, role.name, badgeSystem.office);
      }
    }

    flash.success(Web.msgSaved(BadgeReader.class));
    index();

  }


}
