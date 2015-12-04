package controllers;

import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;

import dao.OfficeDao;
import dao.PersonDao;
import dao.UserDao;

import helpers.Web;

import models.Institute;
import models.Office;
import models.Person;
import models.User;
import models.UsersRolesOffices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class Administrators extends Controller {

  private static final String SUDO_USERNAME = "sudo.username";
  private static final String USERNAME = "username";

  private static final Logger log = LoggerFactory.getLogger(Institutes.class);

  @Inject
  private static SecurityRules rules;

  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static UserDao userDao;

  /**
   * metodo che ritorna la form di inserimento amministratore per la sede passata per parametro.
   * @param officeId l'id della sede a cui associare amministratore e ruolo.
   */
  public static void blank(Long officeId) {

    Office office = officeDao.getOfficeById(officeId);
    notFoundIfNull(office);

    // deve avere tecnicalAdmin sull'office, oppure super admin
    rules.checkIfPermitted(office);

    UsersRolesOffices uro = new UsersRolesOffices();
    uro.office = office;

    render(uro);
  }

  public static void save(@Valid UsersRolesOffices uro) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", uro,
              validation.errorsMap());
      flash.error(Web.msgHasErrors());

      render("@insertNewAdministrator", uro);
    } else {

      rules.checkIfPermitted(uro.office);

      uro.save();
      flash.success(Web.msgSaved(Institute.class));
      Offices.edit(uro.office.id);
    }
  }


  public static void delete(Long uroId) {

    final UsersRolesOffices uro = UsersRolesOffices.findById(uroId);
    notFoundIfNull(uro);

    rules.checkIfPermitted(uro.office);

    uro.delete();
    flash.success(Web.msgDeleted(UsersRolesOffices.class));
    Offices.edit(uro.office.id);

  }

//	public static void deleteSelfAsAdministrator(Long officeId) {
//
//		Office office = officeDao.getOfficeById(officeId);
//		if(office==null) {
//			flash.error("La sede per la quale si vuole rimuovere l'amministratore è inesistente. Riprovare o effettuare una segnalazione.");
//			Offices.showOffices(null);
//		}
//
//		Person person = Security.getUser().get().person;
//		render(office, person);
//
//	}
//
//	public static void insertSystemUro(Long officeId) {
//
//		Office office = officeDao.getOfficeById(officeId);
//		if(office==null) {
//			flash.error("La sede per la quale si vuole definire l'account di sistema "
//					+ "è inesistente. Riprovare o effettuare una segnalazione.");
//			Offices.showOffices(null);
//		}
//
//		List<Role> systemRoles = roleDao.getSystemRolesOffices();
//
//		IWrapperOffice wrapperOffice = wrapperFactory.create(office);
//
//		render(wrapperOffice, systemRoles);
//
//	}
//
//	/**
//	 * Crea un nuovo userRoleOffice di sistema. 
//	 * Se l'user è già presente utilizza quello. Altrimenti ne viene creato uno
//	 * nuovo.
//	 * 
//	 * @param office
//	 * @param username
//	 * @param password
//	 * @param role
//	 */
//	public static void saveSystemUro(Office office, String username, 
//			String password, Role role) {
//
//		Preconditions.checkNotNull(office);
//		Preconditions.checkState(office.isPersistent());
//		Preconditions.checkNotNull(role);
//		Preconditions.checkState(role.isPersistent());
//
//		Preconditions.checkState(username != null && username != "");
//		Preconditions.checkState(password != null && password != "");
//
//		User user =	userDao.getUserByUsernameAndPassword(username, Optional.<String>absent());
//		if (user != null) {
//			if (!user.password.equals(Codec.hexMD5(password))) {
//				flash.error(username + " è già presente come account di sistema."
//						+ " Inserire la password corretta o creare un nuovo account si sistema.");
//				Offices.showOffices(null);
//			}
//			if ( !user.isSystemUser() ) {
//				flash.error("Impossibile utilizzare un user non di sistema.");
//				Offices.showOffices(null);
//			}
//		} else {
//			user = new User();
//			user.username = username;
//			user.password = Codec.hexMD5(password);
//			user.save();
//		}
//				
//		officeManager.setUro(user, office, role);
//
//		flash.success("Associazione account di sistema inserita con successo.");
//
//		Offices.showOffices(null);
//
//	}
//
//	public static void deleteSystemUro(Long systemUroId) {
//
//		UsersRolesOffices systemUro = usersRolesOfficesDao.getById(systemUroId);
//
//		Preconditions.checkNotNull(systemUro);
//
//		//Check Account di sistema
//		List<Role> systemRoles = roleDao.getSystemRolesOffices();
//
//		boolean isSystemRole = false;
//		for(Role role : systemRoles) 
//			if(role.name.equals(systemUro.role.name))
//				isSystemRole = true;
//
//		Preconditions.checkState(isSystemRole);
//
//		systemUro.delete();
//		
//		flash.success("Associazione account di sistema rimossa con successo.");
//
//		Offices.showOffices(null);
//
//	}

  /**
   * Switch in un'altro user
   */
  public static void switchUserTo(long id) {

    final User user = userDao.getUserByIdAndPassword(id, Optional.<String>absent());
    notFoundIfNull(user);

    // salva il precedente
    session.put(SUDO_USERNAME, session.get(USERNAME));
    // recupera
    session.put(USERNAME, user.username);
    // redirect alla radice
    redirect(Play.ctxPath + "/");
  }

  /**
   * Switch nell'user di una persona.
   */
  public static void switchUserToPersonUser(long id) {

    final Person person = personDao.getPersonById(id);
    notFoundIfNull(person);
    Preconditions.checkNotNull(person.user);
    switchUserTo(person.user.id);
  }

  /**
   * ritorna alla precedente persona.
   */
  public static void restoreUser() {
    if (session.contains(SUDO_USERNAME)) {
      session.put(USERNAME, session.get(SUDO_USERNAME));
      session.remove(SUDO_USERNAME);
    }
    // redirect alla radice
    redirect(Play.ctxPath + "/");
  }

}
