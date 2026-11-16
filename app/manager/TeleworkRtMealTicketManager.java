/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import manager.configurations.ConfigurationManager;
import manager.configurations.EpasParam;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;
import models.Contract;
import models.Office;
import models.Person;
import models.PersonDay;
import models.PersonalWorkingTime;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.definitions.DefaultAbsenceType;
import models.enumerate.MealTicketBehaviour;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.jobs.Job;

/**
 * Ricalcolo del buono pasto nei giorni con il codice 103RT (telelavoro ricercatori/tecnologi).
 *
 * <p>Il 103RT impediva il buono pasto; ora lo consente e il buono matura solo se le
 * timbrature in telelavoro raggiungono la soglia prevista dall'orario. I giorni dal 2 luglio
 * 2026 già calcolati con il vecchio comportamento vanno ricalcolati.</p>
 *
 * <p>La simulazione ricalcola in memoria ogni giorno con il 103RT e annulla la transazione,
 * quindi non modifica nulla. L'esecuzione allinea il 103RT sul database (se non è già stato
 * allineato all'enumerato) e ricalcola i dipendenti che maturano almeno un buono, in un
 * {@link Job} asincrono. Una sola esecuzione per volta, con lo stato in memoria.</p>
 */
@Slf4j
public class TeleworkRtMealTicketManager {

  /**
   * Prima data da cui ricalcolare i giorni con il 103RT.
   */
  public static final LocalDate MEAL_TICKET_BEGIN = TeleworkMealTicketManager.MEAL_TICKET_BEGIN;

  /**
   * Minuti di lavoro previsti dall'orario nel giorno sotto i quali il buono non spetterebbe.
   */
  public static final int MIN_DAILY_WORKING_MINUTES =
      TeleworkMealTicketManager.MIN_DAILY_WORKING_MINUTES;

  private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
  private static volatile RecomputeStatus status;

  private final AbsenceDao absenceDao;
  private final AbsenceTypeDao absenceTypeDao;
  private final ConsistencyManager consistencyManager;
  private final PersonDayManager personDayManager;
  private final ConfigurationManager configurationManager;
  private final IWrapperFactory wrapperFactory;

  /**
   * Costruttore per l'injection.
   */
  @Inject
  public TeleworkRtMealTicketManager(AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao,
      ConsistencyManager consistencyManager, PersonDayManager personDayManager,
      ConfigurationManager configurationManager, IWrapperFactory wrapperFactory) {
    this.absenceDao = absenceDao;
    this.absenceTypeDao = absenceTypeDao;
    this.consistencyManager = consistencyManager;
    this.personDayManager = personDayManager;
    this.configurationManager = configurationManager;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Esito del ricalcolo di un giorno con il 103RT.
   */
  @RequiredArgsConstructor
  public enum Outcome {
    GAINS_TICKET("Matura il buono pasto"),
    ALREADY_TICKET("Buono pasto già presente"),
    NOT_ENOUGH_TIME("Timbrature insufficienti per il buono pasto"),
    NOT_PROVIDED("Buono pasto non previsto dall'orario nel giorno"),
    FORCED("Buono pasto impostato dall'amministratore"),
    NO_WORKING_TIME("Nessun contratto o orario di lavoro nel giorno");

    @Getter
    private final String label;
  }

  /**
   * Singolo giorno con il 103RT, con l'esito del ricalcolo.
   */
  @Getter
  @RequiredArgsConstructor
  public static class AnalyzedDay {
    private final Person person;
    private final LocalDate date;
    private final Outcome outcome;
    private final String workingTimeType;
    private final String partTime;
    /** Minuti di lavoro previsti dall'orario nel giorno, null se non c'è orario. */
    private final Integer scheduledMinutes;
    private final int stampingMinutes;
    private final String otherCodes;

    /**
     * Il buono matura in un giorno in cui l'orario prevede meno di 6 ore di lavoro.
     */
    public boolean isWithoutRight() {
      return outcome == Outcome.GAINS_TICKET && scheduledMinutes != null
          && scheduledMinutes < MIN_DAILY_WORKING_MINUTES;
    }
  }

  /**
   * Riepilogo per dipendente, orario, esito e diritto al buono.
   */
  @Getter
  @RequiredArgsConstructor
  public static class Row {
    private final Person person;
    private final String workingTimeType;
    private final String partTime;
    private final Outcome outcome;
    private final boolean withoutRight;
    private final List<AnalyzedDay> analyzedDays;

    public int getDays() {
      return analyzedDays.size();
    }

    public LocalDate getFirstDate() {
      return analyzedDays.get(0).getDate();
    }

    public LocalDate getLastDate() {
      return analyzedDays.get(analyzedDays.size() - 1).getDate();
    }

    /**
     * Le date con le ore timbrate (e gli altri codici, se presenti) separate da virgola.
     */
    public String getDetails() {
      return analyzedDays.stream()
          .map(d -> d.getDate().toString("dd/MM") + " " + hours(d.getStampingMinutes())
              + (d.getOtherCodes().isEmpty() ? "" : " (" + d.getOtherCodes() + ")"))
          .collect(Collectors.joining(", "));
    }

    /**
     * Le ore previste dall'orario nei giorni del gruppo (es. "7:12" o "5:12, 7:12").
     */
    public String getScheduledHours() {
      return analyzedDays.stream().map(AnalyzedDay::getScheduledMinutes)
          .filter(m -> m != null).distinct().sorted().map(Row::hours)
          .collect(Collectors.joining(", "));
    }

    static String hours(int minutes) {
      return String.format("%d:%02d", minutes / 60, minutes % 60);
    }
  }

  /**
   * Esito della simulazione.
   */
  @Getter
  @RequiredArgsConstructor
  public static class Report {
    private final LocalDate from;
    private final LocalDate to;
    private final List<Row> gaining;
    private final List<Row> unchanged;

    public int getDaysGaining() {
      return gaining.stream().mapToInt(Row::getDays).sum();
    }

    public long getPeopleGaining() {
      return gaining.stream().map(Row::getPerson).distinct().count();
    }

    public int getDaysWithoutRight() {
      return gaining.stream().filter(Row::isWithoutRight).mapToInt(Row::getDays).sum();
    }

    public int getDaysUnchanged() {
      return unchanged.stream().mapToInt(Row::getDays).sum();
    }
  }

  /**
   * Stato dell'ultimo ricalcolo avviato (in corso o terminato).
   */
  @Getter
  public static class RecomputeStatus {
    private final LocalDate from;
    private final LocalDate to;
    private final String office;
    private final LocalDateTime startedAt = LocalDateTime.now();
    private volatile LocalDateTime endedAt;
    private volatile int totalPeople;
    private volatile int processedPeople;
    private volatile int days;
    private volatile String error;
    private final List<String> failedPeople = Collections.synchronizedList(Lists.newArrayList());

    RecomputeStatus(LocalDate from, LocalDate to, String office) {
      this.from = from;
      this.to = to;
      this.office = office;
    }

    public boolean isRunning() {
      return endedAt == null;
    }

    /**
     * Durata in minuti del ricalcolo (fino ad ora se ancora in corso).
     */
    public long getMinutes() {
      LocalDateTime end = endedAt == null ? LocalDateTime.now() : endedAt;
      return (end.toDate().getTime() - startedAt.toDate().getTime()) / 60_000;
    }
  }

  /**
   * Lo stato dell'ultimo ricalcolo avviato dall'avvio dell'applicazione, se presente.
   */
  public static Optional<RecomputeStatus> lastRecompute() {
    return Optional.fromNullable(status);
  }

  /**
   * Avvia il ricalcolo in un job asincrono.
   *
   * @param from data di inizio (non prima del 2 luglio 2026)
   * @param to data di fine (non oltre oggi)
   * @param office sede dei dipendenti da considerare, tutte se assente
   * @return false se c'è già un ricalcolo in corso e quindi non ne è stato avviato un altro
   */
  public boolean startRecompute(LocalDate from, LocalDate to, Optional<Office> office) {
    if (!RUNNING.compareAndSet(false, true)) {
      return false;
    }
    final RecomputeStatus current = new RecomputeStatus(from, to,
        office.isPresent() ? office.get().getName() : null);
    status = current;
    final Long officeId = office.isPresent() ? office.get().id : null;
    try {
      new Job<Void>() {
        @Override
        public void doJob() {
          try {
            Optional<Office> jobOffice = officeId == null ? Optional.<Office>absent()
                : Optional.fromNullable((Office) Office.findById(officeId));
            recompute(from, to, jobOffice, current);
          } catch (RuntimeException ex) {
            log.error("Errore nel ricalcolo del buono pasto nei giorni con 103RT", ex);
            current.error = ex.getMessage() == null ? ex.toString() : ex.getMessage();
          } finally {
            current.endedAt = LocalDateTime.now();
            RUNNING.set(false);
          }
        }
      }.now();
    } catch (RuntimeException ex) {
      current.error = ex.toString();
      current.endedAt = LocalDateTime.now();
      RUNNING.set(false);
      throw ex;
    }
    log.info("Avviato in background il ricalcolo del buono pasto nei giorni con 103RT dal {} "
        + "al {} per la sede {}", from, to,
        current.getOffice() == null ? "tutte" : current.getOffice());
    return true;
  }

  /**
   * Simula il ricalcolo dei giorni con il 103RT nel periodo, con il 103RT che consente il
   * buono pasto. I giorni vengono ricalcolati in memoria e la transazione corrente viene
   * marcata per il rollback: non viene salvato nulla.
   *
   * @param from data di inizio (non prima del 2 luglio 2026)
   * @param to data di fine (non oltre oggi)
   * @param office sede dei dipendenti da considerare, tutte se assente
   * @return il report con i giorni che maturano il buono e quelli che restano invariati
   */
  public Report analyze(LocalDate from, LocalDate to, Optional<Office> office) {
    final EntityManager em = JPA.em();
    final FlushModeType flushMode = em.getFlushMode();
    // Nessuna scrittura sul db durante la simulazione, nemmeno prima delle query.
    em.setFlushMode(FlushModeType.COMMIT);
    JPA.setRollbackOnly();
    try {
      List<Absence> absences = absenceDao.getAbsenceByCodeInPeriod(Optional.absent(),
          Optional.of(DefaultAbsenceType.A_103RT.getCode()), from, to,
          Optional.absent(), false, true);
      log.info("Trovati {} codici 103RT dal {} al {}", absences.size(), from, to);

      // Simulazione con il nuovo comportamento anche se il db non è ancora allineato.
      absences.stream().map(Absence::getAbsenceType).distinct()
          .forEach(t -> t.setMealTicketBehaviour(MealTicketBehaviour.allowMealTicket));

      Set<Long> personDays = Sets.newHashSet();
      List<AnalyzedDay> analyzed = absences.stream()
          .filter(a -> !office.isPresent()
              || office.get().equals(a.getPersonDay().getPerson().getOffice()))
          .filter(a -> personDays.add(a.getPersonDay().id))
          .map(a -> simulate(a.getPersonDay(), a))
          .collect(Collectors.toList());

      Map<String, List<AnalyzedDay>> groups = Maps.newLinkedHashMap();
      for (AnalyzedDay d : analyzed) {
        String key = d.getPerson().id + "|" + d.getWorkingTimeType() + "|" + d.getOutcome()
            + "|" + d.isWithoutRight();
        groups.computeIfAbsent(key, k -> Lists.newArrayList()).add(d);
      }
      List<Row> rows = groups.values().stream()
          .map(l -> new Row(l.get(0).getPerson(), l.get(0).getWorkingTimeType(),
              l.get(0).getPartTime(), l.get(0).getOutcome(), l.get(0).isWithoutRight(), l))
          .sorted(Comparator.comparing((Row r) -> !r.isWithoutRight())
              .thenComparing(r -> r.getPerson().getSurname())
              .thenComparing(r -> r.getPerson().getName())
              .thenComparing(Row::getFirstDate))
          .collect(Collectors.toList());

      return new Report(from, to,
          rows.stream().filter(r -> r.getOutcome() == Outcome.GAINS_TICKET)
              .collect(Collectors.toList()),
          rows.stream().filter(r -> r.getOutcome() != Outcome.GAINS_TICKET)
              .collect(Collectors.toList()));
    } finally {
      em.setFlushMode(flushMode);
    }
  }

  /**
   * Allinea il 103RT sul db e ricalcola i dipendenti che maturano almeno un buono pasto, ognuno
   * in una transazione separata: un errore su un dipendente annulla solo il suo ricalcolo.
   */
  private void recompute(LocalDate from, LocalDate to, Optional<Office> office,
      RecomputeStatus current) {
    Report report = analyze(from, to, office);
    Map<Long, LocalDate> firstDateByPerson = Maps.newLinkedHashMap();
    Map<Long, Integer> daysByPerson = Maps.newHashMap();
    Map<Long, String> names = Maps.newHashMap();
    for (Row row : report.getGaining()) {
      names.put(row.getPerson().id, row.getPerson().getFullname());
      firstDateByPerson.merge(row.getPerson().id, row.getFirstDate(),
          (d1, d2) -> d1.isBefore(d2) ? d1 : d2);
      daysByPerson.merge(row.getPerson().id, row.getDays(), Integer::sum);
    }
    current.totalPeople = firstDateByPerson.size();
    // La simulazione ha modificato in memoria i giorni: va annullata.
    JPAPlugin.closeTx(true);

    JPAPlugin.startTx(false);
    AbsenceType rt = absenceTypeDao
        .getAbsenceTypeByCode(DefaultAbsenceType.A_103RT.getCode()).get();
    if (rt.getMealTicketBehaviour() != MealTicketBehaviour.allowMealTicket) {
      log.info("Allineato il comportamento del 103RT sul buono pasto: da {} a {}",
          rt.getMealTicketBehaviour(), MealTicketBehaviour.allowMealTicket);
      rt.setMealTicketBehaviour(MealTicketBehaviour.allowMealTicket);
      rt.save();
    }
    JPAPlugin.closeTx(false);

    for (Map.Entry<Long, LocalDate> entry : firstDateByPerson.entrySet()) {
      JPAPlugin.startTx(false);
      try {
        consistencyManager.updatePersonSituation(entry.getKey(), entry.getValue());
        JPAPlugin.closeTx(false);
        current.days += daysByPerson.get(entry.getKey());
        log.info("Ricalcolati i giorni con 103RT di {} dal {}: {} buoni pasto maturati",
            names.get(entry.getKey()), entry.getValue(), daysByPerson.get(entry.getKey()));
      } catch (RuntimeException ex) {
        log.error("Errore nel ricalcolo dei giorni con 103RT per {} (id={})",
            names.get(entry.getKey()), entry.getKey(), ex);
        try {
          JPAPlugin.closeTx(true);
        } catch (RuntimeException rollbackEx) {
          log.warn("Rollback non riuscito per la persona id={}", entry.getKey(), rollbackEx);
        }
        current.failedPeople.add(names.get(entry.getKey()));
      }
      current.processedPeople++;
    }
    JPAPlugin.startTx(false);
    log.info("Ricalcolati i giorni con 103RT per {} dipendenti ({} buoni pasto), "
        + "{} dipendenti in errore", current.processedPeople - current.failedPeople.size(),
        current.days, current.failedPeople.size());
  }

  /**
   * Ricalcola in memoria il giorno come farebbe il ricalcolo della situazione del dipendente.
   */
  private AnalyzedDay simulate(PersonDay pd, Absence rt) {
    final boolean ticketBefore = pd.isTicketAvailable();
    final String otherCodes = pd.getAbsences().stream()
        .filter(a -> !a.equals(rt))
        .map(a -> a.getAbsenceType().getCode())
        .sorted()
        .collect(Collectors.joining(", "));

    IWrapperPersonDay wpd = wrapperFactory.create(pd);
    Optional<Contract> contract = wpd.getPersonDayContract();
    Optional<WorkingTimeTypeDay> wttd = contract.isPresent()
        ? wpd.getWorkingTimeTypeDay() : Optional.<WorkingTimeTypeDay>absent();
    boolean beforeSourceDate = contract.isPresent()
        && contract.get().getSourceDateResidual() != null
        && pd.getDate().isBefore(contract.get().getSourceDateResidual());
    if (!wttd.isPresent() || beforeSourceDate) {
      return new AnalyzedDay(pd.getPerson(), pd.getDate(), Outcome.NO_WORKING_TIME, "", "",
          null, pd.getStampingsTime() == null ? 0 : pd.getStampingsTime(), otherCodes);
    }

    WorkingTimeType wtt = wttd.get().getWorkingTimeType();
    final Outcome outcome;
    if (pd.isTicketForcedByAdmin()) {
      outcome = Outcome.FORCED;
    } else {
      Office office = pd.getPerson().getOffice();
      LocalTimeInterval lunch = (LocalTimeInterval) configurationManager
          .configValue(office, EpasParam.LUNCH_INTERVAL, pd.getDate());
      LocalTimeInterval work;
      Optional<PersonalWorkingTime> pwt = wpd.getPersonalWorkingTime();
      if (pwt.isPresent()) {
        work = new LocalTimeInterval(pwt.get().getTimeSlot().getBeginSlot(),
            pwt.get().getTimeSlot().getEndSlot());
      } else {
        work = (LocalTimeInterval) configurationManager
            .configValue(office, EpasParam.WORK_INTERVAL, pd.getDate());
      }
      personDayManager.updateTimeAtWork(pd, wttd.get(), wpd.isFixedTimeAtWork(),
          lunch.from, lunch.to, work.from, work.to, Optional.absent());

      if (pd.isTicketAvailable()) {
        outcome = ticketBefore ? Outcome.ALREADY_TICKET : Outcome.GAINS_TICKET;
      } else if (!wttd.get().mealTicketEnabled() || pd.isHoliday()) {
        outcome = Outcome.NOT_PROVIDED;
      } else {
        outcome = Outcome.NOT_ENOUGH_TIME;
      }
    }
    return new AnalyzedDay(pd.getPerson(), pd.getDate(), outcome, wtt.getDescription(),
        partTime(wtt), wttd.get().getWorkingTime(),
        pd.getStampingsTime() == null ? 0 : pd.getStampingsTime(), otherCodes);
  }

  /**
   * Tipologia dell'orario: normale, part time orizzontale o verticale, con l'indicazione
   * dell'allattamento se presente nella descrizione.
   */
  static String partTime(WorkingTimeType wtt) {
    final String type;
    if (!Boolean.TRUE.equals(wtt.getHorizontal())) {
      type = "Part time verticale";
    } else if (wtt.getWorkingTimeTypeDays().stream().filter(d -> !d.isHoliday())
        .allMatch(d -> d.getWorkingTime() >= WorkingTimeType.WORKTIME_BASE)) {
      type = "Normale";
    } else {
      type = "Part time orizzontale";
    }
    boolean breastfeeding = wtt.getDescription() != null
        && wtt.getDescription().toLowerCase().contains("allatt");
    return breastfeeding ? type + " (allattamento)" : type;
  }
}
