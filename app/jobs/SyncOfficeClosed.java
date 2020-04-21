package jobs;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import dao.PersonDao;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@Slf4j
@OnApplicationStart(async = true)
public class SyncOfficeClosed extends Job<Void> {

  @Inject
  static PersonDao personDao;
  
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    
    int count = 0;
    List<Office> list = Office.findAll();
    List<Office> offices = null;
    for (Office office : list) {
      if (office.endDate != null) {
        continue;
      }
      log.debug("Analizzo la sede: {}", office.name);
      offices = Lists.newArrayList();
      offices.add(office);
      List<Person> people = personDao.listFetched(Optional.<String>absent(),
          new HashSet<Office>(offices), false, LocalDate.now(), LocalDate.now(), true).list();
      if (people.isEmpty()) {
        log.info("Non ci sono persone con contratto attivo sulla sede {}. "
            + "Inserisco la data di chiusura sede.", office.name);
        office.endDate = LocalDate.now();
        office.save();
        count++;
      }
    }
    log.info("Sono state chiuse {} sedi.", count);
  }
}
