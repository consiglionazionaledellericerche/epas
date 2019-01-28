package manager;

import dao.RoleDao;
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

  @Inject
  public BadgeManager(RoleDao roleDao) {
    this.roleDao = roleDao;
  }

  /**
   * Rimuove gli zero davanti al codice se presenti e persiste il dato.
   *
   * @param badge il badge da normalizzare
   * @param persist se si vuole persistere la normalizzazione
   */
  public void normalizeBadgeCode(Badge badge, boolean persist) {
    try {
      String code = badge.code;
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

    boolean alreadyPresent = badgeSystem.badges.stream().anyMatch(badge -> badge.code.equals(code));

    if (!alreadyPresent) {
      for (BadgeReader badgeReader : badgeSystem.badgeReaders) {
        Badge badge = new Badge();
        badge.person = person;
        badge.setCode(code);
        badge.badgeSystem = badgeSystem;
        badge.badgeReader = badgeReader;
        badge.save();
      }
      log.info("Creato nuovo badge {} per {}", code, person);
    }
    // Restituisce false se non è stato creato
    return !alreadyPresent;
  }

  public BadgeSystem getOrCreateDefaultBadgeSystem(Office office) {
    String namePattern = office.name.replaceAll("\\s+", "");
    String badgeGroupName = "Badges" + namePattern;
    // Caso semplice: non c'è ancora un gruppo Badge
    Optional<BadgeSystem> badgeSystemOptional = office.badgeSystems.stream()
        .filter(badgeSystem -> {
          return badgeSystem.name.equals(badgeGroupName);
        }).findFirst();

    BadgeSystem badgeSystem;
    // TODO: 28/01/19 se non c'è il gruppo predefinito ma ce n'è un'altro, che faccio?
    if (badgeSystemOptional.isPresent()) {
      badgeSystem = badgeSystemOptional.get();
    } else {
      // Creo il Client timbrature
      BadgeReader client = new BadgeReader();
      client.user = new User();
      client.code = "Reader" + namePattern;
      client.user.username = namePattern.toLowerCase(Locale.ITALY);
      client.user.owner = office;
      client.user.save();

      // Creo il Ruolo per il nuovo utente
      UsersRolesOffices uro = new UsersRolesOffices();
      uro.office = office;
      uro.role = roleDao.getRoleByName(Role.BADGE_READER);
      uro.user = client.user;
      uro.save();

      // Creo il gruppo badge associato al nuovo Client
      badgeSystem = new BadgeSystem();
      badgeSystem.name = badgeGroupName;
      badgeSystem.office = office;
      badgeSystem.badgeReaders.add(client);
      badgeSystem.save();

      client.badgeSystems.add(badgeSystem);
      client.save();

    }
    return badgeSystem;
  }
}
