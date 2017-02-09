package jobs;

import com.google.common.base.Optional;

import dao.CompetenceCodeDao;

import java.util.List;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import manager.CompetenceManager;

import models.CompetenceCode;
import models.enumerate.LimitType;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.Play;
import play.jobs.Job;
import play.jobs.On;



@SuppressWarnings("rawtypes")
@Slf4j
@On("0 0 7 1 * ?") //il primo giorno di ogni mese alle 7.00
public class BonusJob extends Job {
  
  @Inject
  static CompetenceManager competenceManager;
  @Inject
  static CompetenceCodeDao competenceCodeDao;

  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    log.info("Start Job bonus");
    LocalDate date = LocalDate.now().minusDays(1);
    YearMonth yearMonth = new YearMonth(date.getYear(), date.getMonthOfYear());
    List<CompetenceCode> codeList = competenceCodeDao
        .getCompetenceCodeByLimitType(LimitType.onMonthlyPresence);
    codeList.forEach(item -> {
      competenceManager.applyBonus(Optional.absent(), item, yearMonth);
    });
    

    log.info("End Job bonus");
  }
}
