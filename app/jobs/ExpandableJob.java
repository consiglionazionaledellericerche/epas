package jobs;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.OfficeDao;
import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayInTroubleManager;

import models.Person;
import models.enumerate.Troubles;

import org.joda.time.LocalDate;

import play.Play;
import play.jobs.Job;
import play.jobs.On;

import java.util.List;

import javax.inject.Inject;

//@On("0 34 15 ? * *")
@SuppressWarnings("rawtypes")
@Slf4j
@On("0 0 15 ? * MON,WED,FRI")
public class ExpandableJob extends Job {

  private static final String JOBS_CONF = "jobs.active";
  @Inject
  private static PersonDayInTroubleManager personDayInTroubleManager;
  @Inject
  private static OfficeDao officeDao;
  @Inject
  private static PersonDao personDao;

  public void doJob() {

    // in modo da inibire l'esecuzione dei job in base alla configurazione
    if ("false".equals(Play.configuration.getProperty(JOBS_CONF))) {
      log.info("ExpandableJob Interrotto. Disattivato dalla configurazione.");
      return;
    }

    log.info("Start Job expandable");

    LocalDate fromDate = LocalDate.now().minusMonths(2);
    LocalDate toDate = LocalDate.now().minusDays(1);

    List<Person> personList = personDao.list(
            Optional.<String>absent(),
            Sets.newHashSet(officeDao.getAllOffices()),
            false,
            fromDate,
            toDate,
            true).list();

    try {
      personDayInTroubleManager.sendTroubleEmails(personList, fromDate, toDate, Lists.newArrayList(
          Troubles.NO_ABS_NO_STAMP));
      
    } catch (Exception e) {
      e.printStackTrace();
    }

    log.info("Concluso Job expandable");
  }
}
