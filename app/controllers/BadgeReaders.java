package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.mysema.query.SearchResults;

import dao.BadgeReaderDao;
import dao.BadgeSystemDao;
import dao.RoleDao;

import helpers.Web;

import manager.SecureManager;

import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Person;
import models.User;

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

import java.util.HashMap;
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
  private static SecureManager secureManager;

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
  public static void changePassword(@Valid User user,
      @MinSize(5) @Required String newPass) {

    notFoundIfNull(user.badgeReader);
    BadgeReader badgeReader = user.badgeReader;
    rules.checkIfPermitted(badgeReader.owner);
    
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
  public static void save(@Valid BadgeReader badgeReader, @Valid User user) {

    rules.checkIfPermitted(badgeReader.owner);
    
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
    
    rules.checkIfPermitted(badgeReader.owner);

    // if(badgeReader.seats.isEmpty()) {
    badgeReader.delete();
    flash.success(Web.msgDeleted(BadgeReader.class));
    index();
    // }
    flash.error(Web.msgHasErrors());
    index();
  }

  /**
   * Gestore associazioni con i BadgeSystem.
   * @param badgeReaderId
   */
  public static void joinBadgeSystems(Long badgeReaderId) {
    
    final BadgeReader badgeReader = badgeReaderDao.byId(badgeReaderId);
    notFoundIfNull(badgeReader);
    
    rules.checkIfPermitted(badgeReader.owner);

    render("@joinBadgeSystems", badgeReader);
  }
  
  /**
   * Salva la nuova associazione.
   * @param badgeReader.
   */
  public static void saveBadgeSystems(@Valid BadgeReader badgeReader, boolean confirmed) {
    
    rules.checkIfPermitted(badgeReader.owner);
    
    // TODO:
    //creare gli uro mancanti, cancellare quelli non più usati
    
    
    List<Badge> badgesDefinitelyToRemove = Lists.newArrayList();
    List<Badge> badgesToRemove = Lists.newArrayList();
    Set<BadgeSystem> badgeSystemsToRemove = Sets.newHashSet();
    //rimuovere i badge non più usati
    for (Badge badge : badgeReader.badges) {
      //rimozione
      if (!badgeReader.badgeSystems.contains(badge.badgeSystem)) {
        badgesToRemove.add(badge);
        if (badge.badgeSystem.badgeReaders.size() == 1) {
          badgesDefinitelyToRemove.add(badge);
        }
        if (!badgeSystemsToRemove.contains(badge.badgeSystem)) {
          badgeSystemsToRemove.add(badge.badgeSystem);
        }
        continue;
      }
    }

    List<Badge> violatedBadges = Lists.newArrayList();
    List<Badge> badgesToSave = Lists.newArrayList();

    for (BadgeSystem badgeSystem : badgeReader.badgeSystems) {
      // Per ogni badge System ora associato
      if (badgeSystemsToRemove.contains(badgeSystem)) {
        continue;
      }
      // Prendere i codici del badge system
      Set<String> codes = Sets.newHashSet();
      for (Badge oldBadge : badgeSystem.badges) {
        if (!codes.contains(oldBadge.code)) {
         codes.add(oldBadge.code);

          Badge badge = new Badge();
          badge.person = oldBadge.person;
          badge.badgeSystem = badgeSystem;
          badge.badgeReader = badgeReader;
          badge.code = oldBadge.code;
         
          //Controllare che esistano nel badgeReader
          Optional<Badge> alreadyExists = BadgeSystems.alreadyExists(badge);
          if (alreadyExists.isPresent()) {
            if (!badgeSystemsToRemove.contains(alreadyExists.get().badgeSystem) 
                && !alreadyExists.get().person.equals(badge.person)) {
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

    flash.success(Web.msgSaved(BadgeReader.class));
    index();
    
  }

  private static String key(Badge badge) {
    String key = badge.badgeReader.id+"-"+badge.code; 
    return key;
  }

  
 
}
