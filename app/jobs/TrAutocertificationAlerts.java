package jobs;

import com.google.common.collect.ImmutableList;
import dao.PersonDao;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayInTroubleManager;
import models.enumerate.Troubles;
import org.joda.time.LocalDate;
import play.Play;
import play.jobs.Job;
import play.jobs.On;


@Slf4j
// Ogni giorno alle 15 dal lunedì al venerdì
@On("0 0 15 ? * MON-FRI")
public class TrAutocertificationAlerts extends Job<Void> {

  private static final int DAYS = 5;

  @Inject
  static PersonDayInTroubleManager personDayInTroubleManager;
  @Inject
  static PersonDao personDao;


  /**
   * Esecuzione Job.
   */
  @Override
  public void doJob() {

    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }

    final LocalDate today = LocalDate.now();
    final LocalDate from;
    final LocalDate to;

    // Quinto giorno del mese
    final LocalDate fifthDayOfMonth = today.withDayOfMonth(DAYS);
    // Quintultimo giorno del mese
    final LocalDate fifthFromLast = today.dayOfMonth()
        .withMaximumValue().minusDays(DAYS);

    // Solo i primi 5 e gli ultimi 5 giorni del mese
    if (today.isAfter(fifthDayOfMonth) && today.isBefore(fifthFromLast)) {
      return;
    }
    // Se sono a inizio mese verifico tutto il mese precedente
    if (!today.isAfter(fifthDayOfMonth)) {
      from = today.minusMonths(1).dayOfMonth().withMinimumValue();
      to = today.minusMonths(1).dayOfMonth().withMaximumValue();
    } else {
      // Tutto il mese attuale fino a ieri
      from = today.dayOfMonth().withMinimumValue();
      to = today.minusDays(1);
    }

    log.info("Start Job TrAutocertificationAlerts");

    personDayInTroubleManager.sendTroubleEmails(personDao.trWithAutocertificationOn(),
        from, to, ImmutableList.of(Troubles.NO_ABS_NO_STAMP, Troubles.UNCOUPLED_WORKING));

    log.info("Concluso Job TrAutocertificationAlerts");
  }
}
