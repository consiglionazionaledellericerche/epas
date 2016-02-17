package controllers;

import com.google.common.base.Optional;

import com.mysema.query.SearchResults;

import dao.OfficeDao;
import dao.RoleDao;
import dao.UserDao;
import dao.UsersRolesOfficesDao;
import dao.history.HistoryValue;

import helpers.Web;

import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class Users extends Controller{

  private static final Logger log = LoggerFactory.getLogger(BadgeReaders.class);

  @Inject
  private static SecurityRules rules;

  @Inject
  private static UserDao userDao;
  @Inject
  private static RoleDao roleDao;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static UsersRolesOfficesDao uroDao;

  public static void index() {
    flash.keep();
    list(null);
  }

  /**
   * metodo che renderizza la lista di utenti fisici.
   * @param name il nome utente su cui filtrare.
   */
  public static void list(String name) {
    Office office = Security.getUser().get().person.office;
    SearchResults<?> results =
        userDao.listUsersByOffice(Optional.<String>fromNullable(name), 
            office, true).listResults();   

    render(results, name);
  }

  /**
   * metodo che renderizza la lista di utenti di sistema.
   * @param name il nome utente su cui filtrare.
   */
  public static void systemList(String name) {
    Office office = Security.getUser().get().person.office;
    SearchResults<?> results =
        userDao.listUsersByOffice(Optional.<String>fromNullable(name), 
            office, false).listResults();

    render(results, name);
  }

  public static void systemBlank() {
    render();
  }

  public static void blank() {
    render();
  }

  /**
   * metodo che renderizza l'utente e gli userRoleOffice ad esso associati.
   * @param id l'identificativo dell'utente da editare
   */
  public static void edit(Long id) {
    User user = userDao.getUserByIdAndPassword(id, Optional.<String>absent());
    List<UsersRolesOffices> uroList = user.usersRolesOffices;
    render(user, uroList);
  }

  /**
   * metodo che permette il salvataggio delle modifiche di un user.
   * @param user l'utente modificato da salvare
   */
  public static void updateInfo(@Valid User user) {
    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", user, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", user);
    }
    rules.checkIfPermitted(user);
    user.save();

    flash.success(Web.msgSaved(User.class));
    edit(user.id);
  }


  /**
   * metodo che salva l'utente sulla base dei parametri recuperati dalla form.
   * @param user l'utente da salvare
   * @param roles la lista di id dei ruoli da assegnare
   * @param offices la lista di id degli uffici su cui assegnare gli uro
   */
  public static void save(@Valid User user, List<Long> roles, List<Long> offices) {

    rules.checkIfPermitted(Security.getUser().get().person.office);

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", user, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@blank", user);
    }
    if (user.password.length() < 5) {
      response.status = 400;
      validation.addError("user.password", "almeno 5 caratteri");
      render("@systemBlank", user);
    }
    if (roles == null) {
      response.status = 400;
      validation.addError("roles", "Almeno un ruolo");
      render("@systemBlank", user);
    }
    
    if (offices == null) {
      response.status = 400;
      validation.addError("offices", "Almeno una sede");
      render("@systemBlank", user);
    }
    
    Codec codec = new Codec();
    user.password = codec.hexMD5(user.password);
    user.save();
    for (long id : roles) {

      Role role = roleDao.getRoleById(id);
      for (long ide : offices) {
        UsersRolesOffices uro = new UsersRolesOffices();
        Office office = officeDao.getOfficeById(ide);
        uro.user = user;
        uro.office = office;
        uro.role = role;
        uro.save();
      }
    }   

    flash.success(Web.msgSaved(User.class));
    systemList(null);
  }


  /**
   * metodo che cancella un user recuperato tramite l'id passato come parametro.
   * @param id l'id dell'utente da cancellare
   */
  public static void delete(Long id) {
    User user = userDao.getUserByIdAndPassword(id, Optional.<String>absent());
    notFoundIfNull(user);

    rules.checkIfPermitted(Security.getUser().get().person.office);

    List<UsersRolesOffices> uroList = uroDao.getUsersRolesOfficesByUser(user);
    if (uroList.isEmpty()) {
      flash.error(Web.msgHasErrors());
      index();
    }
    if (user.person == null) {
      for (UsersRolesOffices uro : uroList) {
        uro.delete();
      }
      user.delete();
    } else {
      flash.error(Messages.get("crud.userDeleteError"));
      index();
    }
    flash.success(Web.msgDeleted(User.class));
    index();
  }
}
