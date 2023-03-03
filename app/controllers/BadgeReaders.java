/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.querydsl.core.QueryResults;
import common.security.SecurityRules;
import dao.BadgeDao;
import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.RoleDao;
import dao.UsersRolesOfficesDao;
import helpers.Web;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import models.Zone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

/**
 * Controller per la gestione dei BadgeReaders.
 */
@With({Resecure.class})
public class BadgeReaders extends Controller {

  private static final Logger log = LoggerFactory.getLogger(BadgeReaders.class);

  @Inject
  private static BadgeReaderDao badgeReaderDao;
  @Inject
  private static BadgeSystemDao badgeSystemDao;
  @Inject
  private static BadgeDao badgeDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static RoleDao roleDao;
  @Inject
  private static UsersRolesOfficesDao uroDao;

  /**
   * Lista dei badgeReader disponibili.
   */
  public static void index() {
    flash.keep();
    list(null);
  }

  /**
   * Ritorna la lista dei badge reader.
   *
   * @param name nome del lettore badge su cui si vuole filtrare.
   */
  public static void list(String name) {

    QueryResults<?> results =
        badgeReaderDao.badgeReaders(Optional.<String>fromNullable(name),
            Optional.<BadgeSystem>absent()).listResults();

    render(results, name);
  }


  /**
   * Render delle informazioni sul badgereader.
   *
   * @param id identificativo del lettore badge.
   */
  public static void show(Long id) {
    final BadgeReader badgeReader = BadgeReader.findById(id);
    notFoundIfNull(badgeReader);
    render(badgeReader);
  }

  /**
   * Render della pagina di modifica del lettore badge.
   *
   * @param id identificativo del lettore badge.
   */
  public static void edit(Long id) {

    final BadgeReader badgeReader = badgeReaderDao.byId(id);
    notFoundIfNull(badgeReader);
    rules.checkIfPermitted(badgeReader.getUser().getOwner());

    QueryResults<?> results = badgeSystemDao.badgeSystems(Optional.<String>absent(),
        Optional.fromNullable(badgeReader)).listResults();

    List<Zone> zoneList = badgeReader.getZones();

    render(badgeReader, results, zoneList);

  }

  public static void blank() {
    render();
  }

  /**
   * Cancella il badge reader.
   *
   * @param id identificativo del badge reader da eliminare.
   */
  public static void delete(Long id) {

    final BadgeReader badgeReader = BadgeReader.findById(id);
    notFoundIfNull(badgeReader);
    rules.checkIfPermitted(badgeReader.getUser().getOwner());

    //elimino la sorgente se non è associata ad alcun gruppo.
    if (badgeReader.getBadgeSystems().isEmpty()) {

      badgeReader.delete();

      // FIXME: issue della rimozione user delle persone che riferiscono lo storico.
      badgeReader.getUser().delete();

      flash.success(Web.msgDeleted(BadgeSystem.class));

      index();
    }
    flash.error("Per poter eliminare il gruppo è necessario che non sia associato ad alcuna"
        + "sorgente timbrature");
    edit(badgeReader.id);
  }


  /**
   * Permette l'edit delle informazioni sul badge reader.
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
    badgeReader.getUser().setOwner(owner);
    badgeReader.save();
    badgeReader.getUser().save();

    flash.success(Web.msgSaved(BadgeReader.class));
    edit(badgeReader.id);
  }


  /**
   * Permette il cambio della password per l'utente badge reader.
   *
   * @param user Utente a cui modificare la password.
   * @param newPass nuova password da associare al lettore.
   */
  public static void changePassword(@Valid User user,
      @MinSize(5) @Required String newPass) {

    notFoundIfNull(user.getBadgeReader());
    BadgeReader badgeReader = user.getBadgeReader();
    rules.checkIfPermitted(badgeReader.getUser().getOwner());

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", user, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", badgeReader);
    }

    user.updatePassword(newPass);
    user.save();

    flash.success(Web.msgSaved(BadgeReader.class));
    edit(badgeReader.id);

  }

  /**
   * Permette il salvataggio del badge reader.
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
    if (user.getPassword().length() < 5) {
      response.status = 400;
      Validation.addError("user.password", "almeno 5 caratteri");
      render("@blank", badgeReader, office, user);
    }

    rules.checkIfPermitted(office);

    badgeReader.setUser(user);
    badgeReader.getUser().setOwner(office);
    badgeReader.getUser().updatePassword(badgeReader.getUser().getPassword());
    badgeReader.getUser().save();
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

    rules.checkIfPermitted(badgeReader.getUser().getOwner());

    render("@joinBadgeSystems", badgeReader);
  }

  /**
   * Salva la nuova associazione.
   */
  public static void saveBadgeSystems(@Valid BadgeReader badgeReader, boolean confirmed) {

    rules.checkIfPermitted(badgeReader.getUser().getOwner());

    // TODO:
    //creare gli uro mancanti, cancellare quelli non più usati

    //Costruisco un pò di strutture dati di utilità....
    Set<BadgeSystem> badgeSystemsAdd = Sets.newHashSet();
    Set<BadgeSystem> badgeSystemsRemove = Sets.newHashSet();
    Set<BadgeSystem> badgeSystemsRemain = Sets.newHashSet();
    List<Badge> badgesDefinitelyToRemove = Lists.newArrayList();
    List<Badge> badgesToRemove = Lists.newArrayList();
    for (Badge badge : badgeReader.getBadges()) {
      if (badgeReader.getBadgeSystems().contains(badge.getBadgeSystem())) {
        if (badgeSystemsRemain.contains(badge.getBadgeSystem())) {
          badgeSystemsRemain.add(badge.getBadgeSystem());
        }
      } else {
        if (!badgeSystemsRemove.contains(badge.getBadgeSystem())) {
          badgeSystemsRemove.add(badge.getBadgeSystem());
        }
        if (badge.getBadgeSystem().getBadgeReaders().size() == 1) {
          badgesDefinitelyToRemove.add(badge);
        }
        badgesToRemove.add(badge);
      }

    }
    for (BadgeSystem badgeSystem : badgeReader.getBadgeSystems()) {
      if (!badgeSystemsRemain.contains(badgeSystem)) {
        badgeSystemsAdd.add(badgeSystem);
      }
    }

    List<Badge> violatedBadges = Lists.newArrayList();
    List<Badge> badgesToSave = Lists.newArrayList();

    for (BadgeSystem badgeSystem : badgeSystemsAdd) {

      // Prendere i codici del badge system
      Set<String> codes = Sets.newHashSet();
      for (Badge otherBadge : badgeSystem.getBadges()) {
        if (!codes.contains(otherBadge.getCode())) {
          codes.add(otherBadge.getCode());
          Badge badge = new Badge();
          badge.setPerson(otherBadge.getPerson());
          badge.setBadgeSystem(badgeSystem);
          badge.setBadgeReader(badgeReader);
          badge.setCode(otherBadge.getCode());

          //Controllare che esistano nel badgeReader
          Optional<Badge> alreadyExists = badgeDao.byCode(badge.getCode(), badgeReader);
          if (alreadyExists.isPresent()) {
            if (!alreadyExists.get().getPerson().equals(badge.getPerson())) {
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
      Optional<UsersRolesOffices> uro = uroDao.getUsersRolesOffices(badgeReader.getUser(),
          role, badgeSystem.getOffice());
      if (uro.isPresent()) {
        log.info("UserRoleOffice rimosso: {}", uro.get());
        uro.get().delete();
      } else {
        log.warn("L'userRoleOffice da rimuovere {} {} {} avrebbe dovuto esistere.",
            badgeReader.getCode(), role.getName(), badgeSystem.getOffice());
      }
    }
    for (BadgeSystem badgeSystem : badgeSystemsAdd) {
      Optional<UsersRolesOffices> uro = uroDao.getUsersRolesOffices(badgeReader.getUser(),
          role, badgeSystem.getOffice());
      if (!uro.isPresent()) {
        UsersRolesOffices uroNew = new UsersRolesOffices();
        uroNew.setOffice(badgeSystem.getOffice());
        uroNew.setRole(role);
        uroNew.setUser(badgeReader.getUser());
        uroNew.save();
        log.info("UserRoleOffice creato: {}", uroNew);
      } else {
        log.warn("L'userRoleOffice da inserire {} {} {} esisteva già.",
            badgeReader.getCode(), role.getName(), badgeSystem.getOffice());
      }
    }

    flash.success(Web.msgSaved(BadgeReader.class));
    index();

  }

}