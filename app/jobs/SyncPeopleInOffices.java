package jobs;

import dao.GeneralSettingDao;
import dao.OfficeDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.sync.SynchronizationManager;
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
@On("0 15 6,15 * * ?")
public class SyncPeopleInOffices extends Job<Void> {

  @Inject
  static OfficeDao officeDao;
  
  @Inject
  static SynchronizationManager synchronizationManager;
  
  @Inject
  static GeneralSettingDao settings;
  
  /**
   * Esecuzione Job.
   */
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))
        || !settings.generalSetting().syncOfficesEnabled) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    
    log.info("Avvio sincronizzazione delle persone negli uffici.");

    val offices = officeDao.allOffices().list();
    log.debug("Avvio sincronizzazione delle persone assegnate agli uffici presenti ({}).", 
        offices.size());
    offices.stream().forEach(office -> {
      try {
        val syncResult = synchronizationManager.syncPeopleInOffice(office, false);
        if (syncResult.getMessages().size() > 0) {
          log.info("Importate/sincronizzate correttamente alcune persone per l'ufficio {}.", 
              office.getName());
          log.info("Risultato sincronizzazione: {}", syncResult);
        } else {
          log.info("Non ci sono persone da importare/sincronizzare per l'ufficio {}.",
              office.getName());
        }
      } catch (RuntimeException e) {
        log.error("Errore durante l'importazione/sincronizzazione delle persone per l'ufficio {}.",
            office.getName(), e);
      }
    });
    log.info("Terminata sincronizzazione delle persone negli uffici.");
  }
}