package controllers;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.querydsl.core.QueryResults;
import dao.OfficeDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import helpers.Web;
import it.cnr.iit.epas.NullStringBinder;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.SecureManager;
import models.Office;
import models.User;
import models.UsersRolesOffices;
import models.enumerate.AccountRole;
import play.data.binding.As;
import play.data.validation.Equals;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@Slf4j
@With({Resecure.class})
public class Users extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static UserDao userDao;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static UsersRolesOfficesDao uroDao;
  @Inject
  static SecureManager secureManager;

  public static void index() {
    flash.keep();
    list(null, false, null);
  }

  /**
   * metodo che renderizza la lista di utenti fisici.
   *
   * @param name il nome utente su cui filtrare.
   */
  public static void list(@As(binder = NullStringBinder.class) String name,
      boolean onlyEnabled, Office office) {

    User user = Security.getUser().get();

    Set<Office> offices = office.isPersistent() ? ImmutableSet.of(office) :
        secureManager.officesTechnicalAdminAllowed(user);

    QueryResults<?> results = userDao.listUsersByOffice(Optional.fromNullable(name),
        offices, onlyEnabled).listResults();

    render(results, name);
  }

  /**
   * Lista degli utenti "orfani".
   * @param name il nome che può servire come restrizione nella ricerca degli utenti
   */
  public static void noOwnerUsers(
      @As(binder = NullStringBinder.class) String name) {

    QueryResults<?> results = userDao
        .noOwnerUsers(Optional.fromNullable(name)).listResults();

    render(results, name);
  }

  /**
   * Permette di editare gli accountRoles (ruoli di sistema).
   * @param userId l'id dell'user da editare
   */
  public static void editAccountRoles(Long userId) {
    final User user = userDao.getUserByIdAndPassword(userId, Optional.absent());
    notFoundIfNull(user);
    rules.checkIfPermitted(user);
    render(user);
  }

  /**
   * Permette di salvare gli accountRoles (ruoli di sistema) ad un utente.
   * @param userId l'utente cui associare i nuovi ruoli
   * @param roles i ruoli da associare
   */
  public static void saveAccountRoles(Long userId, List<AccountRole> roles) {

    final User user = userDao.getUserByIdAndPassword(userId, Optional.absent());
    notFoundIfNull(user);

    if (roles == null) {
      user.roles.clear();
    } else {
      user.roles = Sets.newHashSet(roles);
    }

    user.save();
    flash.success(Web.msgModified(AccountRole.class));
    edit(user.id);
  }

  /**
   * Aggiunge il ruolo all'utente.
   * @param userId l'utente a cui aggiungere il ruolo
   */
  public static void addRole(Long userId) {
    final User user = userDao.getUserByIdAndPassword(userId, Optional.absent());
    notFoundIfNull(user);
    rules.checkIfPermitted(user);
    render(user);
  }

  /**
   * Salva il ruolo per l'utente sulla sede.
   * @param userRoleOffice l'oggetto userRoleOffice da salvare
   */
  public static void saveRole(@Valid UsersRolesOffices userRoleOffice) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", userRoleOffice, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      final User user = userRoleOffice.user;
      render("@addRole", user, userRoleOffice);
    }

    rules.checkIfPermitted(userRoleOffice);

    userRoleOffice.save();
    final User user = userRoleOffice.user;
    flash.success(Web.msgCreated(UsersRolesOffices.class));
    edit(user.id);
  }

  /**
   * Rimuove il ruolo.
   * @param uroId l'identificativo dell'userRoleOffice da eliminare
   */
  public static void removeRole(Long uroId) {

    final UsersRolesOffices uro = uroDao.getById(uroId);

    notFoundIfNull(uro);

    rules.checkIfPermitted(uro);

    if (uro.isPersistent()) {
      uro.delete();
      flash.success(Web.msgDeleted(UsersRolesOffices.class));
    }

    edit(uro.user.id);
  }

  /**
   * Mostra le caratteristiche dell'utente.
   * @param userId l'identificativo dell'utente da mostrare
   */
  public static void show(Long userId) {
    final User user = userDao.getUserByIdAndPassword(userId, Optional.absent());
    notFoundIfNull(user);
    rules.checkIfPermitted(user);
    render(user);
  }

  /**
   * Permette l'edit dell'utente.
   * @param userId l'identificativo dell'utente da editare
   */
  public static void edit(Long userId) {
    final User user = userDao.getUserByIdAndPassword(userId, Optional.absent());
    notFoundIfNull(user);

    rules.checkIfPermitted(user);
    render(user);
  }

  public static void blank() {
    render("@edit");
  }

  /**
   * Salva l'utente con la nuova password.
   * @param user l'utente da salvare
   * @param password la password da salvare
   * @param confirmPassword la password da confermare
   */
  public static void save(@Required @Valid User user, String password,
      @Equals("password") String confirmPassword) {

    notFoundIfNull(user);
    // Nuovo utente, nessun ruolo di sistema e nessun owner specificato
    if (!user.isPersistent() && !Security.getUser().get().isSystemUser()
        && user.roles.isEmpty() && user.owner == null) {
      validation.addError("user.owner", "Specificare una sede proprietaria");
    }
    if (Validation.hasErrors()) {
      log.warn("validation errors for {}: {}", user, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", user, password, confirmPassword);
    }

    if (!Strings.isNullOrEmpty(password)) {
      user.password = Codec.hexMD5(password);
    }

    rules.checkIfPermitted(user);

    user.save();

    flash.success(Web.msgModified(User.class));
    edit(user.id);
  }

}
