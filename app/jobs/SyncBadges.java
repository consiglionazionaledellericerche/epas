package jobs;

import dao.GeneralSettingDao;
import dao.OfficeDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.BadgeManager;
import play.Play;
import play.jobs.Job;
import play.jobs.On;

/**
 * Sincronizza i badge dei vari uffici.
 * Questa funzionalità non è disponibile per tutte le installazioni di ePAS.
 * 
 * @author cristian
 * @since 19/06/2019
 */
@Slf4j
@On("0 0 7,16 * * ?") // Ore 7 e 14
public class SyncBadges extends Job<Void> {

  @Inject
  static OfficeDao officeDao;
  
  @Inject
  static BadgeManager badgeManager;
  
  @Inject
  static GeneralSettingDao settings;
  
  /**
   * Esecuzione Job.
   */
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))
        || !settings.generalSetting().syncBadgesEnabled) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    
    log.info("Avvio sincronizzazione dei numeri di badge.");

    val offices = officeDao.allOffices().list();
    log.debug("Ci sono {} uffici di cui sincronizzare i badge", offices.size());
    offices.stream().forEach(office -> {
      try {
        val badges = badgeManager.importBadges(office);
        if (!badges.isEmpty()) {
          log.info("Importati/sincronizzati correttamente {} badge per l'ufficio {}", 
              badges.size(), office.getName());
        } else {
          log.info("Non ci sono badge da importare/sincronizzare per l'ufficio {}",
              office.getName());
        }
      } catch (RuntimeException e) {
        log.error("Errore durante l'importazione dei badge per l'ufficio {}.",
            office.getName(), e);
      }
    });
    log.info("Terminata sincronizzazione dei numeri di badge.");
  }
}
