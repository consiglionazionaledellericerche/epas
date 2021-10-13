package jobs;

import com.google.common.collect.Lists;
import dao.OfficeDao;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.CheckGreenPassManager;
import manager.EmailManager;
import models.CheckGreenPass;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;
import play.jobs.OnApplicationStart;

@Slf4j
@OnApplicationStart(async = true)
//@On("0 15 11 ? * MON-FRI") //tutti i giorni dal lunedi al venerdi di ogni mese alle 11.15
public class CheckGreenPassJob extends Job {
  
  static final String GREENPASS_CONF = "greenpass.active";
  
  @Inject
  static CheckGreenPassManager passManager;
  @Inject
  static OfficeDao officeDao;
  @Inject
  static EmailManager emailManager;

  @Override
  public void doJob() {
    
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    
    if (!"true".equals(Play.configuration.getProperty(GREENPASS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    
    log.info("Start Check Green Pass Job");
    List<Person> list = Lists.newArrayList();
    List<CheckGreenPass> listDrawn = Lists.newArrayList();
    List<Office> offices = officeDao.allEnabledOffices();
    for (Office office: offices) {
      log.info("Seleziono la lista dei sorteggiati per {}", office.name);
      list = passManager.peopleActiveInDate(LocalDate.now().minusDays(1), office);
      listDrawn = passManager.peopleDrawn(list);
      if (listDrawn.isEmpty()) {
        log.warn("Nessuna persona selezionata per la sede {}! Verificare con l'amministrazione", 
            office.name);        
      } else {
        for (CheckGreenPass gp : listDrawn) {
          //Invio una mail a ciascun dipendente selezionato
          emailManager.infoDrawnPersonForCheckingGreenPass(gp.person);
          log.info("Inviata mail informativa per il controllo green pass a {}", gp.person);
        }
      }
      
    }
    log.debug("End Check Green Pass Job");
  }
}
