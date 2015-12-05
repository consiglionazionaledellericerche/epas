package controllers;

import com.google.common.base.Optional;

import com.mysema.query.SearchResults;

import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.OfficeDao;

import helpers.Web;

import manager.SecureManager;

import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Valid;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

import security.SecurityRules;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;


@With({Resecure.class, RequestInit.class})
public class BadgeSystems extends Controller {

  private static final Logger log = LoggerFactory.getLogger(BadgeSystems.class);

  @Inject
  private static BadgeSystemDao badgeSystemDao;
  @Inject
  private static BadgeReaderDao badgeReaderDao;
  @Inject
  private static SecurityRules rules;
  @Inject
  private static OfficeDao officeDao;

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
        badgeSystemDao.badgeSystems(Optional.<String>fromNullable(name), 
            Optional.<BadgeReader>absent()).listResults();

    render(results, name);
  }


  /**
   * 
   * @param id identificativo del lettore badge.
   */
  public static void show(Long id) {
    final BadgeSystem badgeSystem = BadgeSystem.findById(id);
    notFoundIfNull(badgeSystem);
    render(badgeSystem);
  }

  /**
   * 
   * @param id identificativo del lettore badge.
   */
  public static void edit(Long id) {

    final BadgeSystem badgeSystem = badgeSystemDao.byId(id);
    notFoundIfNull(badgeSystem);
    rules.checkIfPermitted(badgeSystem.owner);
    
    SearchResults<?> badgeReadersResults = badgeReaderDao.badgeReaders(Optional.<String>absent(),
        Optional.fromNullable(badgeSystem)).listResults();
  
    render(badgeSystem, badgeReadersResults);

  }

  public static void blank() {
    render();
  }


  /**
   * 
   * @param badgeSystem l'oggetto per cui si vogliono cambiare le impostazioni.
   */
  public static void updateInfo(@Valid BadgeSystem badgeSystem) {

    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeSystem, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@edit", badgeSystem);
    }

    rules.checkIfPermitted(badgeSystem.owner);
    badgeSystem.save();

    flash.success(Web.msgSaved(BadgeSystem.class));
    edit(badgeSystem.id);
  }


  /**
   * 
   * @param badgeSystem l'oggetto badge reader da salvare.
   * @param user l'utente creato a partire dal badge reader.
   */
  public static void save(@Valid BadgeSystem badgeSystem) {


    if (Validation.hasErrors()) {
      response.status = 400;
      log.warn("validation errors for {}: {}", badgeSystem, validation.errorsMap());
      flash.error(Web.msgHasErrors());
      render("@blank", badgeSystem);
    }

    badgeSystem.save();
    flash.success(Web.msgSaved(BadgeSystem.class));
    index();
  }


  /**
   * 
   * @param id identificativo del badge reader da eliminare.
   */
  public static void delete(Long id) {
    final BadgeSystem badgeSystem = BadgeSystem.findById(id);
    notFoundIfNull(badgeSystem);

    // if(badgeSystem.seats.isEmpty()) {
    badgeSystem.delete();
    flash.success(Web.msgDeleted(BadgeSystem.class));
    index();
    // }
    flash.error(Web.msgHasErrors());
    index();
  }
  
  public static void joinOffices(Long badgeSystemId) {
    final BadgeSystem badgeSystem = badgeSystemDao.byId(badgeSystemId);
    notFoundIfNull(badgeSystem);
    
    List<Office> allOffices = officeDao.allOffices().list();
    
    // TODO: controlli??
    render("@joinOffices", badgeSystem, allOffices);
  }
  
  public static void saveOffices(@Valid BadgeSystem badgeSystem) {
    
    
    // TODO check della fattibilità
    
    flash.success(Web.msgSaved(BadgeSystem.class));
    
    // TODO la generazione degli uro con ruolo badgeReader
    badgeSystem.save();
    
    edit(badgeSystem.id);
    
  }
  
}
