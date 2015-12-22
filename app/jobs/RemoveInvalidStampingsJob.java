package jobs;

import com.google.common.base.Optional;

import dao.PersonDayDao;

import lombok.extern.slf4j.Slf4j;

import manager.ConsistencyManager;

import models.Person;
import models.PersonDay;
import models.Stamping;

import org.joda.time.LocalDate;

import play.Logger;
import play.jobs.Job;

import java.util.List;

import javax.inject.Inject;

@Slf4j
public class RemoveInvalidStampingsJob extends Job {

  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static ConsistencyManager consistencyManager;

  private final Person person;
  private final LocalDate begin;
  private final LocalDate end;

  public RemoveInvalidStampingsJob(Person person, LocalDate begin, LocalDate end) {
    super();
    this.person = person;
    this.begin = begin;
    this.end = end;
  }

  public void doJob() {

    log.info("Inizio Job RemoveInvalidStampingsJob per {},Dal {} al {}", person, begin, end);
    List<PersonDay> persondays = personDayDao.getPersonDayInPeriod(person, begin, Optional.of(end));

    for (PersonDay pd : persondays) {
      for (Stamping stamping : pd.stampings) {
        if (!stamping.valid) {
          log.info("Eliminazione timbratura non valida per {} in data {} : {}",
              pd.person.fullName(), pd.date, stamping);
          stamping.delete();
        }
      }
    }

    consistencyManager.updatePersonSituation(person.id, begin);

    Logger.info("Terminato Job RemoveInvalidStampingsJob per %s,Dal %s al %s", person, begin, end);
  }
}
