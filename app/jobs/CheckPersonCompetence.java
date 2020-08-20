package jobs;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberPath;
import dao.CompetenceCodeDao;
import dao.PersonDao;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.CompetenceCode;
import models.Person;
import models.PersonCompetenceCodes;
import models.dto.PersonCompetenceCodeDto;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@Slf4j
@OnApplicationStart(async = true)
public class CheckPersonCompetence extends Job<Void> {

  @Inject
  static CompetenceCodeDao competenceCodeDao;
  @Inject
  static PersonDao personDao;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Lanciata procedura di check dei periodi di competenza abilitati per persona.");
    
    log.info("Rimuovo periodi con date sballate");
    List<PersonCompetenceCodes> wrongPcc = competenceCodeDao.getWrongs();
    int size = wrongPcc.size();
    wrongPcc.forEach(pcc -> pcc.delete());
    log.info("Eliminati {} periodi con date sballate", size);
    

    List<PersonCompetenceCodeDto> list = competenceCodeDao.getDuplicates();
    log.info("Trovate {} pcc con date sovrapposte", list.size());

    for (PersonCompetenceCodeDto dto : list) {
      Person person = personDao.getPersonById(dto.personId);
      CompetenceCode code = competenceCodeDao.getCompetenceCodeById(dto.competenceCodeId);
      List<PersonCompetenceCodes> subList = competenceCodeDao.listByPersonAndCode(person, code);

      log.debug("Trovati {} periodi", subList.size());
      //List<PersonCompetenceCodes> toDelete = Lists.newArrayList();
      PersonCompetenceCodes temp = null;
      for (PersonCompetenceCodes pcc : subList) {

        if (temp == null) {
          temp = pcc;
          continue;
        }
        DateInterval interval = new DateInterval(pcc.beginDate, pcc.endDate);
        if (DateUtility.isIntervalIntoAnother(interval, 
            new DateInterval(temp.beginDate, temp.endDate))) {
          //toDelete.add(pcc); 
          pcc.delete();
          log.debug("Cancellato {}", pcc.toString());

        }
        if (DateUtility.isIntervalIntoAnother(new DateInterval(temp.beginDate, temp.endDate), 
            interval)) {
          //toDelete.add(prev);
          temp.delete();
          log.debug("Cancellato {}", temp.toString());
          temp = pcc;

        }
        //log.debug("Accorpati personCompetenceCodes {} e {}", prev.toString(), pcc.toString());
      }
      //toDelete.forEach(pcc -> pcc.delete());
    }
    log.info("Procedura terminata");

  }
}
