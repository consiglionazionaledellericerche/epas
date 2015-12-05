package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.mysema.query.SearchResults;
import com.mysql.jdbc.CachedResultSetMetaData;

import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.PersonDao;
import dao.RoleDao;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

import helpers.Web;

import manager.BadgeManager;
import manager.SecureManager;

import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;

import net.sf.oval.constraint.MinLength;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Required;
import play.data.validation.Valid;
import play.data.validation.Validation;
import play.libs.Codec;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;
import java.util.Map;
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
  private static SecureManager secureManager;
  @Inject
  private static BadgeManager badgeManager;

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
    rules.checkIfPermitted(badgeReader.owner);

    SearchResults<?> results = badgeSystemDao.badgeSystems(Optional.<String>absent(),
        Optional.fromNullable(badgeReader)).listResults();
    
    render(badgeReader, results);

  }

  public static void blank() {
    render();
  }


  /**
   * 
   * @param badgeReader l'oggetto per cui si vogliono cambiare le impostazioni.
   */
  public static void updateInfo(@Valid BadgeReader badgeReader) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeReader, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", badgeReader);
    }

    rules.checkIfPermitted(badgeReader.owner);
    badgeReader.save();

    flash.success(Web.msgSaved(BadgeReader.class));
    edit(badgeReader.id);
  }


  /**
   * @param id identificativo del badge reader.
   * @param newPass nuova password da associare al lettore.
   */
  public static void changePassword(Long id, @MinLength(5) @Required String newPass) {


    final BadgeReader badgeReader = BadgeReader.findById(id);
    notFoundIfNull(badgeReader);
    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeReader, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", badgeReader, newPass);
    }

    Codec codec = new Codec();
    badgeReader.user.password = codec.hexMD5(newPass);
    flash.success(Web.msgSaved(BadgeReader.class));
    edit(id);

  }

  /**
   * 
   * @param badgeReader l'oggetto badge reader da salvare.
   * @param user l'utente creato a partire dal badge reader.
   */
  public static void save(@Valid BadgeReader badgeReader, @Valid User user) {


    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeReader, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@blank", badgeReader);
    }
    if (user.password.length() < 5) {
      response.status = 400;
      validation.addError("user.password", "almeno 5 caratteri");
      render("@blank", badgeReader, user);
    }


    Codec codec = new Codec();
    user.password = codec.hexMD5(user.password);
    user.save();
    badgeReader.user = user;
    badgeReader.save();
    flash.success(Web.msgSaved(BadgeReader.class));
    index();
  }


  /**
   * 
   * @param id identificativo del badge reader da eliminare.
   */
  public static void delete(Long id) {
    final BadgeReader badgeReader = BadgeReader.findById(id);
    notFoundIfNull(badgeReader);

    // if(badgeReader.seats.isEmpty()) {
    badgeReader.delete();
    flash.success(Web.msgDeleted(BadgeReader.class));
    index();
    // }
    flash.error(Web.msgHasErrors());
    index();
  }

  public static void joinBadgeSystems(Long badgeReaderId) {
    
    final BadgeReader badgeReader = badgeReaderDao.byId(badgeReaderId);
    notFoundIfNull(badgeReader);
    
    rules.checkIfPermitted(badgeReader.owner);

    render("@joinBadgeSystems", badgeReader);
  }
  
  public static void saveBadgeSystems(@Valid BadgeReader badgeReader) {
    
    rules.checkIfPermitted(badgeReader.owner);
    
    flash.success(Web.msgSaved(BadgeReader.class));
    badgeReader.save();
    
    edit(badgeReader.id);
    
  }
  
 
}
