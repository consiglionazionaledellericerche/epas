package manager;

import com.google.common.collect.Lists;
import dao.BadgeReaderDao;
import dao.PersonDao;
import dao.RoleDao;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import models.Badge;
import models.BadgeReader;
import models.BadgeSystem;
import models.Office;
import models.Person;
import models.Role;
import models.User;
import models.UsersRolesOffices;
import synch.perseoconsumers.people.PeoplePerseoConsumer;
import synch.perseoconsumers.people.PersonBadge;

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
  private final PersonDao personDao;
  private final PeoplePerseoConsumer peoplePerseoConsumer;
  
  /**
   * Default constructor.
   */
  @Inject
  public BadgeManager(RoleDao roleDao, BadgeReaderDao badgeReaderDao,
      PersonDao personDao, PeoplePerseoConsumer peoplePerseoConsumer) {
    this.roleDao = roleDao;
    this.badgeReaderDao = badgeReaderDao;
    this.personDao = personDao;
    this.peoplePerseoConsumer = peoplePerseoConsumer;
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
      log.info("Exception: {}", ignored);
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
  public List<Badge> createPersonBadges(
      Person person, String code, BadgeSystem badgeSystem) {

    final String codeNormalized = String.valueOf(Integer.valueOf(code));
    val badges = checkBadgeTransfer(person, code, badgeSystem);
    
    Optional<Badge> alreadyPresent = badgeSystem.badges.stream()
        .filter(badge -> badge.code.equals(codeNormalized) 
            && badge.person.equals(person)).findAny();
    
    
    if (alreadyPresent.isPresent()) {
      log.debug("Il Badge n. {} è già presente per {} - {}", codeNormalized, person, person.office);
      return badges;
    } else {
      for (BadgeReader badgeReader : badgeSystem.badgeReaders) {
        Badge badge = new Badge();
        badge.person = person;
        badge.code = codeNormalized;
        badge.badgeSystem = badgeSystem;
        badge.badgeReader = badgeReader;
        badge.save();
        badges.add(badge);
      }
      log.info("Creato nuovo badge {} per {}", codeNormalized, person);
    }
    return badges;
  }
  
  /**
   * Rimuove da tutte le altre persone il badge indicato.
   * Il badge da rimuovere viene cercato in tutti quelli appartenenti al badgeSystem.
   * 
   * @return la lista dei badge rimossi  
   */
  private List<Badge> checkBadgeTransfer(Person person, String code, BadgeSystem badgeSystem) {
    final String codeNormalized = String.valueOf(Integer.valueOf(code));
    val transferedBadges = Lists.<Badge>newArrayList();
    for (BadgeReader br : badgeSystem.badgeReaders) {
      val badges = br.badges.stream()
          .filter(badge -> badge.code.equals(codeNormalized) 
              && !badge.person.equals(person)).collect(Collectors.toList());
      
      badges.stream().forEach(b -> {
        log.info("Cambio attribuzione badge n. {} (id = {}) "
            + "da {} a {}.",
            b.code, b.id, b.person.getFullname(), person.getFullname());
        b.person = person;
        b.save();
      });
      transferedBadges.addAll(badges);
    }
            
    return transferedBadges;    
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
  
  /**
   * Importa/aggiorna i badge di un ufficio.
   * @param office l'Ufficio di cui importare i badge.
   * @return la lista dei badge importati/aggiornati.
   */
  public List<Badge> importBadges(Office office) {    
    
    List<PersonBadge> importedBadges = Lists.newArrayList();
    try {
      //Vengono filtrati tutti i badge uguali. Solo il primo incontrato
      //viene importato.
      importedBadges = 
          Lists.newArrayList(peoplePerseoConsumer.getOfficeBadges(office.perseoId).get()
          .stream().collect(Collectors.toCollection(
              () -> new TreeSet<PersonBadge>(
                  (pb1, pb2) -> pb1.getBadge().compareTo(pb2.getBadge())))));
    } catch (InterruptedException | ExecutionException e) {
      log.error("Impossibile importare i badge della sede con perseoId {}: {}",
          office.perseoId, e.getMessage());
      throw new IllegalStateException("Impossibile importare i badge");
    }

    val badges = Lists.<Badge>newArrayList();
    if (!importedBadges.isEmpty()) {
      BadgeSystem badgeSystem = getOrCreateDefaultBadgeSystem(office);

      importedBadges.forEach(personBadge -> {
        Person person = personDao.getPersonByPerseoId(personBadge.getPersonId());
        if (person == null) {
          log.warn("Sincronizzazione Badge: persona con perseoId={} non presente",
              personBadge.getPersonId());
        } else {
          badges.addAll(createPersonBadges(person, personBadge.getBadge(), badgeSystem));          
        }
      });
    }
    return badges;
  }
}
