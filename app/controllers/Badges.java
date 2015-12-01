package controllers;

import com.google.common.base.Optional;

import dao.BadgeDao;
import dao.BadgeReaderDao;
import dao.PersonDao;

import helpers.Web;

import models.Badge;
import models.BadgeReader;
import models.Person;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.With;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

@With({Resecure.class, RequestInit.class})
public class Badges extends Controller {

  private static final Logger log = LoggerFactory.getLogger(Badges.class);

  @Inject
  private static PersonDao personDao;
  @Inject
  private static BadgeReaderDao badgeReaderDao;
  @Inject
  private static BadgeDao badgeDao;

  /**
   * @param id del badge che si intende eliminare.
   */
  public static void delete(Long id) {
    final Badge badge = Badge.findById(id);
    notFoundIfNull(badge);

    // if(badgeReader.seats.isEmpty()) {
    render(badge);
  }

  /**
   * 
   * @param badge l'oggetto badge che si vuole eliminare.
   */
  public static void deleteBadge(Badge badge) {
    //final Badge badge = Badge.findById(id);
    badge.delete();
    flash.success(Web.msgDeleted(Badge.class));
    Persons.list(null);
    // }
    flash.error(Web.msgHasErrors());
    Persons.list(null);
  }
  /**
   * @param id della persona a cui si intende assegnare il nuovo badge.
   */
  public static void joinPersonToBadge(Long id) {
    Person person = personDao.getPersonById(id);
    
    Badge badge = new Badge();
    render(person, badge);
  }

  /**
   * controller che gestisce la vista di allocazione badge per la persona.
   *
   * @param badge       il badge
   * @param badgeReader il badge reader
   * @param person      la persona
   */
  public static void allocateBadge(Badge badge,
                                   List<Long> badgeReader, Person person) {

    if (badge.code == null) {
      flash.error("Popolare correttamente il campo numero badge");
      joinPersonToBadge(person.id);
    }
    if (badgeReader == null || badgeReader.size() == 0) {
      flash.error("Inserire almeno un lettore badge a cui associare il badge");
      joinPersonToBadge(person.id);
    }

    for (Long id : badgeReader) {
      BadgeReader br = badgeReaderDao.byId(id);
      Optional<Badge> check = badgeDao.byCode(badge.code,
              Optional.fromNullable(br));
      if (check.isPresent()) {
        flash.error("Al lettore %s è già associato un badge "
                + "con identificativo %s", br.code, badge.code);
        render("@joinPersonToBadge", badge, badgeReader, person);
      }
      badge.person = person;
      badge.badgeReader = br;
      badge.save();

    }
    flash.success("Associato il badge %s a %s per i lettori selezionati",
            badge.code, person.fullName());
    Persons.list(null);
  }

  /**
   * @param id del badge su cui si vuole fare operazioni.
   */
  public static void manageBadge(Long id) {
    Badge badge = badgeDao.byId(id);
    render(badge);
  }
  
  /**
   * 
   * @param id del badge che si vuole editare.
   */
  public static void edit(Long id) {
    Badge badge = badgeDao.byId(id);
    Person person = badge.person;
    BadgeReader reader = badge.badgeReader;
    render(badge, person, reader);
  }
  
  /**
   * 
   * @param badge il badge di cui si vuole cambiare il code.
   */
  public static void updateBadge(Badge badge, Person person, BadgeReader reader) {
    Optional<Badge> existingBadge = badgeDao.byCode(badge.code, 
        Optional.fromNullable(badge.badgeReader));
    if (existingBadge.isPresent()) {
      flash.error("E' già esistente un badge con codice %s. "
          + "Inserire altro codice per %s", badge.code, person.fullName());
      
    } else {      
      person.badges.remove(badge);
      Set<Badge> badgeList = person.badges;
      for (Badge b : badgeList) {
        if (b.badgeReader == reader) {
          b.delete();
          person.save();
          Badge newBadge = new Badge();
          newBadge.badgeReader = reader;
          newBadge.code = badge.code;
          newBadge.person = person;
          newBadge.save();
        }
          
      }      
      flash.success("Modificato codice del badge di %s", person.fullName());
    }
    Persons.list(null);
  }

}
