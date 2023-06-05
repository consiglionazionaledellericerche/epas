package jobs;

import com.google.common.base.Optional;
import dao.OfficeDao;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ConsistencyManager;
import manager.PeriodManager;
import manager.attestati.dto.internal.TipoBlocchettoSede;
import manager.attestati.service.CertificationsComunication;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.recaps.recomputation.RecomputeRecap;
import models.Configuration;
import models.Office;
import models.base.IPropertyInPeriod;
import models.enumerate.BlockType;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;

/**
 * Job per la gestione dei blocchetti e dei buoni pasto.
 *
 * @author dario
 *
 */
@Slf4j
//@On("0 0 6 * * ?") //tutte le mattine alle 6.00
//@OnApplicationStart
public class MealTicketBlockTypeJob extends Job<Void> {
  
  @Inject
  static CertificationsComunication certificationsComunication;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static ConfigurationManager configurationManager;
  @Inject
  static PeriodManager periodManager;
  @Inject
  static ConsistencyManager consistencyManager;

  @Override
  public void doJob() {
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Start meal ticket block type Job");

    LocalDate date = LocalDate.now();
    //recupero solo le sedi attive
    List<Office> officeList = officeDao.allEnabledOffices();
    
    for (Office office : officeList) {
      BlockType blockType = null;
      TipoBlocchettoSede tipo = null;
      log.info("Richiedo ad attestati info per sede: {}", office.getName());
      //chiedo ad attestati l'informazione...
      try {
        tipo = certificationsComunication
            .getTipoBlocchetto(date.getYear(), date.getMonthOfYear(), office);
        log.info("Recuperate info su tipo di blocchetto per la sede {}", office.getName());
      } catch (NoSuchFieldException ex) {
        log.error("Errore nel recupero dei campi inviati: {} ", ex.toString());
        ex.printStackTrace();
      } catch (ExecutionException ex) {
        log.error("Errore in esecuzione del job MealTicketBlockTypeJob: {}", ex.toString());
        ex.printStackTrace();
      }
      //verifico che sia coerente
      switch (tipo.tipoBuonoPasto) {
        case "C":
          blockType = BlockType.papery;
          break;
        case "E":
          blockType = BlockType.electronic;
          break;
        case "M":
          blockType = BlockType.papery;
          break;
        default:
          log.error("Informazione ricevuta da Attestati non comprensibile. "
                + "Contattare Attestati per info sulla sede {}", office.getName());
          return;
          
      }
      //aggiorno la configurazione
      Configuration newConfiguration = (Configuration) configurationManager
          .updateEnum(EpasParam.MEAL_TICKET_BLOCK_TYPE, office, blockType, 
              Optional.absent(), Optional.absent(), false);
      
      List<IPropertyInPeriod> periodRecaps = periodManager.updatePeriods(newConfiguration, false);
      RecomputeRecap recomputeRecap =
          periodManager.buildRecap(newConfiguration.getOffice().getBeginDate(),
              Optional.fromNullable(LocalDate.now()),
              periodRecaps, Optional.<LocalDate>absent());
      recomputeRecap.epasParam = newConfiguration.getEpasParam();
      periodManager.updatePeriods(newConfiguration, true);

      consistencyManager.performRecomputation(newConfiguration.getOffice(),
          newConfiguration.getEpasParam().recomputationTypes, recomputeRecap.recomputeFrom);
      log.info("Aggiornato parametro per la sede {} con valore {}", 
          office.getName(), blockType.description);
      
    }
    log.info("End meal ticket block type Job");
  }
}
