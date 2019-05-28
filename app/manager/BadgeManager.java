package manager;

import dao.BadgeReaderDao;
import dao.RoleDao;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;

/**
 * TODO: In questo manager andrebbero spostati i metodi che gestiscono i controllers BadgeReaders e
 * BadgeSystems.
 *
 * @author alessandro
 */
@Slf4j
public class BadgeManager {

  private final RoleDao roleDao;
  private final BadgeReaderDao badgeReaderDao;

  @Inject
  public BadgeManager(RoleDao roleDao, BadgeReaderDao badgeReaderDao) {
    this.roleDao = roleDao;
    this.badgeReaderDao = badgeReaderDao;
  }

  /**
   * Rimuove gli zero davanti al codice se presenti e persiste il dato.
   *
   * @param badge il badge da normalizzare
   * @param persist se si vuole persistere la normalizzazione
   */
  public void normalizeBadgeCode(Badge badge, boolean persist) {
    try {
      String code = badge.code.trim();
      Integer number = Integer.parseInt(code);
      badge.code = String.valueOf(number);
      if (!code.equals(String.valueOf(number))) {
        if (persist) {
          badge.save();
          log.info("Normalizzato e persistito badge.code: da {} a {}", code, number);
        } else {
          log.info("Normalizzato badge.code: da {} a {}", code, number);
        }
      }
    } catch (Exception ignored) {
      //Tipo String
      //log.info("Impossibile {}", badge.code);
    }
  }

  /**
   * Crea il badge su un gruppoBadge per ogni sorgente timbratura associata.
   *
   * @param person la persona alla quale associare il badge
   * @param code il codice badge
   * @param badgeSystem il gruppo badge sul quale associare il badge
   * @return true se l'operazione va a buon fine, false se il badge è già presente.
   */
  public boolean createPersonBadge(Person person, String code, BadgeSystem badgeSystem) {

    final String codeNormalized = String.valueOf(Integer.valueOf(code));
    Optional<Badge> alreadyPresent = badgeSystem.badges.stream()
        .filter(badge -> badge.code.equals(codeNormalized)).findAny();

    if (alreadyPresent.isPresent()) {
      log.warn("Il Badge n. {} è già presente per {} - {}", codeNormalized, person, person.office);
    } else {
      for (BadgeReader badgeReader : badgeSystem.badgeReaders) {
        Badge badge = new Badge();
        badge.person = person;
        badge.code = codeNormalized;
        badge.badgeSystem = badgeSystem;
        badge.badgeReader = badgeReader;
        badge.save();
      }
      log.info("Creato nuovo badge {} per {}", codeNormalized, person);
    }
    // Restituisce false se non è stato creato
    return !alreadyPresent.isPresent();
  }

  /**
   * Cerca il BadgeSystem associato all'ufficio passato e se non c'è lo crea. 
   */
  public BadgeSystem getOrCreateDefaultBadgeSystem(Office office) {
    String namePattern = office.name.replaceAll("\\s+", "");
    BadgeSystem badgeSystem;

    // Se non ne esiste uno lo creo
    if (office.badgeSystems.isEmpty()) {

      List<BadgeReader> readers = badgeReaderDao.getBadgeReaderByOffice(office);
      BadgeReader badgeReader;

      if (readers.isEmpty()) {
        // Creo il Client timbrature se non è già presente
        badgeReader = new BadgeReader();
        badgeReader.user = new User();
        badgeReader.code = namePattern;
        badgeReader.user.username = namePattern.toLowerCase(Locale.ITALY);
        badgeReader.user.owner = office;
        badgeReader.user.save();

        // Creo il Ruolo per il nuovo utente
        UsersRolesOffices uro = new UsersRolesOffices();
        uro.office = office;
        uro.role = roleDao.getRoleByName(Role.BADGE_READER);
        uro.user = badgeReader.user;
        uro.save();
      } else {
        // prendo il primo se c'è già un client configurato
        badgeReader = readers.iterator().next();
      }

      // Creo il gruppo badge associato al nuovo Client
      badgeSystem = new BadgeSystem();
      badgeSystem.name = namePattern;
      badgeSystem.office = office;
      badgeSystem.badgeReaders.add(badgeReader);
      badgeSystem.save();

      badgeReader.badgeSystems.add(badgeSystem);
      badgeReader.save();
    } else {
      // Altrimenti prendo il default o il primo che trovo
      badgeSystem = office.badgeSystems.stream()
          .filter(bs -> {
            return bs.name.equals(namePattern);
          }).findFirst().orElse(office.badgeSystems.iterator().next());
    }

    return badgeSystem;
  }
}
