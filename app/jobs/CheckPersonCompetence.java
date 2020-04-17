package jobs;

import com.beust.jcommander.internal.Maps;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.NumberPath;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import dao.CompetenceCodeDao;
import dao.PersonDao;
import lombok.extern.slf4j.Slf4j;
import models.CompetenceCode;
import models.Person;
import models.PersonCompetenceCodes;
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

  
  public class PersonCompetenceCodeDto {
    long personId;
    long competenceCodeId;
    
    public PersonCompetenceCodeDto(long id, long id2) {
      this.personId = id;
      this.competenceCodeId = id2;
    }
  }
  
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Lanciata procedura di check dei periodi di competenza abilitati per persona.");
    
    List<PersonCompetenceCodeDto> list = competenceCodeDao.getDuplicates();
    log.info("Trovate {} pcc con date sovrapposte", list.size());

    for (PersonCompetenceCodeDto dto : list) {
      Person person = personDao.getPersonById(dto.personId);
      CompetenceCode code = competenceCodeDao.getCompetenceCodeById(dto.competenceCodeId);
      List<PersonCompetenceCodes> subList = competenceCodeDao.listByPersonAndCode(person, code);
      for (PersonCompetenceCodes pcc : subList) {
        //TODO: controllare le date e unificare i personCompetenceCodes
      }
    }
    log.info("Procedura terminata");
    
  }
}
