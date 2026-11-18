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
import dao.AbsenceDao;
import dao.AbsenceTypeDao;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.definitions.DefaultAbsenceType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.jobs.Job;

/**
 * Conversione dei codici di telelavoro 103 nel codice 103BP (telelavoro con maturazione
 * del buono pasto) per i dipendenti con orario normale o part time orizzontale di almeno
 * 6 ore giornaliere.
 *
 * <p>La conversione parte dal 2 luglio 2026: i codici 103 inseriti da quella data per i
 * dipendenti che avrebbero diritto al buono pasto vanno sostituiti. Sono esclusi i giorni
 * in cui il 103 è insieme ad altri codici di assenza, che vanno valutati manualmente.</p>
 *
 * <p>La conversione completa richiede diversi minuti (ricalcolo di tutti i dipendenti
 * coinvolti), quindi viene eseguita in un {@link Job} asincrono. Una sola conversione per
 * volta: lo stato è mantenuto in memoria e vale per la singola istanza dell'applicazione.</p>
 */
@Slf4j
public class TeleworkMealTicketManager {

  /**
   * Prima data da cui convertire i codici 103 in 103BP.
   */
  public static final LocalDate MEAL_TICKET_BEGIN = new LocalDate(2026, 7, 2);

  /**
   * Minuti di lavoro giornalieri minimi previsti dall'orario per la conversione.
   */
  public static final int MIN_DAILY_WORKING_MINUTES = 360;

  private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
  private static volatile ConversionStatus status;

  private final AbsenceDao absenceDao;
  private final AbsenceTypeDao absenceTypeDao;
  private final ConsistencyManager consistencyManager;

  /**
   * Costruttore per l'injection.
   */
  @Inject
  public TeleworkMealTicketManager(AbsenceDao absenceDao, AbsenceTypeDao absenceTypeDao,
      ConsistencyManager consistencyManager) {
    this.absenceDao = absenceDao;
    this.absenceTypeDao = absenceTypeDao;
    this.consistencyManager = consistencyManager;
  }

  /**
   * Motivo per cui un codice 103 viene convertito o escluso dalla conversione.
   */
  @RequiredArgsConstructor
  public enum Outcome {
    TO_CONVERT("Da convertire in 103BP"),
    DUPLICATED("Codice 103 inserito più volte nel giorno"),
    OTHER_ABSENCES("Altri codici di assenza nel giorno"),
    NOT_HORIZONTAL("Orario non orizzontale"),
    LESS_THAN_SIX_HOURS("Orario con meno di 6 ore giornaliere"),
    NO_WORKING_TIME("Nessun orario di lavoro nel giorno");

    @Getter
    private final String label;
  }

  /**
   * Singolo codice 103 analizzato.
   */
  @Getter
  @RequiredArgsConstructor
  public static class AnalyzedAbsence {
    private final Absence absence;
    private final Outcome outcome;
    private final String workingTimeType;
    private final String otherCodes;

    public Person getPerson() {
      return absence.getPersonDay().getPerson();
    }

    public LocalDate getDate() {
      return absence.getPersonDay().getDate();
    }
  }

  /**
   * Riepilogo per dipendente, orario ed esito.
   */
  @Getter
  @RequiredArgsConstructor
  public static class Row {
    private final Person person;
    private final String workingTimeType;
    private final Outcome outcome;
    private final List<AnalyzedAbsence> absences;

    public int getDays() {
      return absences.size();
    }

    public LocalDate getFirstDate() {
      return absences.get(0).getDate();
    }

    public LocalDate getLastDate() {
      return absences.get(absences.size() - 1).getDate();
    }

    /**
     * Le date (con gli altri codici presenti, se ci sono) separate da virgola.
     */
    public String getDetails() {
      return absences.stream()
          .map(a -> a.getDate().toString("dd/MM/yyyy")
              + (a.getOtherCodes().isEmpty() ? "" : " (" + a.getOtherCodes() + ")"))
          .distinct()
          .collect(Collectors.joining(", "));
    }
  }

  /**
   * Esito dell'analisi.
   */
  @Getter
  @RequiredArgsConstructor
  public static class Report {
    private final LocalDate from;
    private final LocalDate to;
    private final List<Row> toConvert;
    private final List<Row> excluded;

    public int getDaysToConvert() {
      return toConvert.stream().mapToInt(Row::getDays).sum();
    }

    public long getPeopleToConvert() {
      return toConvert.stream().map(Row::getPerson).distinct().count();
    }

    public int getDaysExcluded() {
      return excluded.stream().mapToInt(Row::getDays).sum();
    }
  }

  /**
   * Stato dell'ultima conversione avviata (in corso o terminata).
   */
  @Getter
  public static class ConversionStatus {
    private final LocalDate from;
    private final LocalDate to;
    private final String office;
    private final LocalDateTime startedAt = LocalDateTime.now();
    private volatile LocalDateTime endedAt;
    private volatile int totalPeople;
    private volatile int processedPeople;
    private volatile int converted;
    private volatile int excluded;
    private volatile String error;
    private final List<String> failedPeople = Collections.synchronizedList(Lists.newArrayList());

    ConversionStatus(LocalDate from, LocalDate to, String office) {
      this.from = from;
      this.to = to;
      this.office = office;
    }

    public boolean isRunning() {
      return endedAt == null;
    }

    /**
     * Durata in minuti della conversione (fino ad ora se ancora in corso).
     */
    public long getMinutes() {
      LocalDateTime end = endedAt == null ? LocalDateTime.now() : endedAt;
      return (end.toDate().getTime() - startedAt.toDate().getTime()) / 60_000;
    }
  }

  /**
   * Lo stato dell'ultima conversione avviata dall'avvio dell'applicazione, se presente.
   */
  public static Optional<ConversionStatus> lastConversion() {
    return Optional.fromNullable(status);
  }

  /**
   * Avvia la conversione in un job asincrono.
   *
   * @param from data di inizio (non prima del 2 luglio 2026)
   * @param to data di fine (non oltre oggi)
   * @param office sede dei dipendenti da considerare, tutte se assente
   * @return false se c'è già una conversione in corso e quindi non ne è stata avviata un'altra
   */
  public boolean startConversion(LocalDate from, LocalDate to, Optional<Office> office) {
    if (!RUNNING.compareAndSet(false, true)) {
      return false;
    }
    final ConversionStatus current = new ConversionStatus(from, to,
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
            convert(from, to, jobOffice, current);
          } catch (RuntimeException ex) {
            log.error("Errore nella conversione dei codici 103 in 103BP", ex);
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
    log.info("Avviata in background la conversione dei codici 103 in 103BP dal {} al {} "
        + "per la sede {}", from, to, current.getOffice() == null ? "tutte" : current.getOffice());
    return true;
  }

  /**
   * Analizza i codici 103 nel periodo senza modificare nulla.
   *
   * @param from data di inizio (non prima del 2 luglio 2026)
   * @param to data di fine (non oltre oggi)
   * @param office sede dei dipendenti da considerare, tutte se assente
   * @return il report con i codici da convertire e quelli esclusi
   */
  public Report analyze(LocalDate from, LocalDate to, Optional<Office> office) {
    List<Absence> absences = absenceDao.getAbsenceByCodeInPeriod(Optional.absent(),
        Optional.of(DefaultAbsenceType.A_103.getCode()), from, to,
        Optional.absent(), false, true);
    log.info("Trovati {} codici 103 dal {} al {}", absences.size(), from, to);

    Map<Long, List<ContractWorkingTimeType>> cwttByPerson = Maps.newHashMap();
    List<AnalyzedAbsence> analyzed = absences.stream()
        .filter(a -> !office.isPresent()
            || office.get().equals(a.getPersonDay().getPerson()
                .getOfficeAtOrCurrent(a.getPersonDay().getDate())))
        .map(a -> analyze(a, cwttByPerson))
        .collect(Collectors.toList());

    // Raggruppamento per dipendente, orario ed esito mantenendo l'ordine delle date.
    Map<String, List<AnalyzedAbsence>> groups = Maps.newLinkedHashMap();
    for (AnalyzedAbsence a : analyzed) {
      String key = a.getPerson().id + "|" + a.getWorkingTimeType() + "|" + a.getOutcome();
      groups.computeIfAbsent(key, k -> Lists.newArrayList()).add(a);
    }
    List<Row> rows = groups.values().stream()
        .map(l -> new Row(l.get(0).getPerson(), l.get(0).getWorkingTimeType(),
            l.get(0).getOutcome(), l))
        .sorted(Comparator.comparing((Row r) -> r.getPerson().getSurname())
            .thenComparing(r -> r.getPerson().getName())
            .thenComparing(Row::getFirstDate))
        .collect(Collectors.toList());

    return new Report(from, to,
        rows.stream().filter(r -> r.getOutcome() == Outcome.TO_CONVERT)
            .collect(Collectors.toList()),
        rows.stream().filter(r -> r.getOutcome() != Outcome.TO_CONVERT)
            .collect(Collectors.toList()));
  }

  /**
   * Sostituisce con il 103BP i codici 103 convertibili nel periodo e ricalcola la situazione
   * dei dipendenti coinvolti. Ogni dipendente viene salvato in una transazione separata: un
   * errore su un dipendente annulla solo le sue modifiche e la conversione prosegue.
   */
  private void convert(LocalDate from, LocalDate to, Optional<Office> office,
      ConversionStatus current) {
    Report report = analyze(from, to, office);
    current.excluded = report.getDaysExcluded();
    AbsenceType mealTicketTelework = absenceTypeDao
        .getAbsenceTypeByCode(DefaultAbsenceType.A_103BP.getCode()).get();

    // Assenze da convertire raggruppate per dipendente.
    Map<Long, List<Long>> absencesByPerson = Maps.newLinkedHashMap();
    Map<Long, LocalDate> firstDateByPerson = Maps.newHashMap();
    Map<Long, String> names = Maps.newHashMap();
    for (Row row : report.getToConvert()) {
      names.put(row.getPerson().id, row.getPerson().getFullname());
      absencesByPerson.computeIfAbsent(row.getPerson().id, k -> Lists.newArrayList())
          .addAll(row.getAbsences().stream().map(a -> a.getAbsence().id)
              .collect(Collectors.toList()));
      firstDateByPerson.merge(row.getPerson().id, row.getFirstDate(),
          (d1, d2) -> d1.isBefore(d2) ? d1 : d2);
    }
    final Long mealTicketTeleworkId = mealTicketTelework.id;
    current.totalPeople = absencesByPerson.size();

    JPAPlugin.closeTx(false);
    for (Map.Entry<Long, List<Long>> entry : absencesByPerson.entrySet()) {
      JPAPlugin.startTx(false);
      try {
        AbsenceType newType = AbsenceType.findById(mealTicketTeleworkId);
        for (Long absenceId : entry.getValue()) {
          Absence absence = Absence.findById(absenceId);
          absence.setAbsenceType(newType);
          absence.save();
        }
        JPA.em().flush();
        consistencyManager.updatePersonSituation(entry.getKey(),
            firstDateByPerson.get(entry.getKey()));
        JPAPlugin.closeTx(false);
        current.converted += entry.getValue().size();
        log.info("Convertiti {} codici 103 in 103BP per {} dal {}",
            entry.getValue().size(), names.get(entry.getKey()),
            firstDateByPerson.get(entry.getKey()));
      } catch (RuntimeException ex) {
        log.error("Errore nella conversione dei codici 103 per {} (id={})",
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
    log.info("Convertiti {} codici 103 in 103BP per {} dipendenti, {} dipendenti in errore",
        current.converted, current.processedPeople - current.failedPeople.size(),
        current.failedPeople.size());
  }

  private AnalyzedAbsence analyze(Absence absence,
      Map<Long, List<ContractWorkingTimeType>> cwttByPerson) {
    String otherCodes = absence.getPersonDay().getAbsences().stream()
        .filter(a -> !a.equals(absence))
        .map(a -> a.getAbsenceType().getCode())
        .sorted()
        .collect(Collectors.joining(", "));

    Optional<WorkingTimeType> wtt = workingTimeType(absence.getPersonDay().getPerson(),
        absence.getPersonDay().getDate(), cwttByPerson);
    String wttDescription = wtt.isPresent() ? wtt.get().getDescription() : "";

    final Outcome outcome;
    boolean duplicated = absence.getPersonDay().getAbsences().stream()
        .anyMatch(a -> !a.equals(absence)
            && a.getAbsenceType().getCode().equals(absence.getAbsenceType().getCode()));
    if (duplicated) {
      outcome = Outcome.DUPLICATED;
    } else if (!otherCodes.isEmpty()) {
      outcome = Outcome.OTHER_ABSENCES;
    } else if (!wtt.isPresent()) {
      outcome = Outcome.NO_WORKING_TIME;
    } else if (!Boolean.TRUE.equals(wtt.get().getHorizontal())) {
      outcome = Outcome.NOT_HORIZONTAL;
    } else if (!hasMinimumDailyWorkingTime(wtt.get())) {
      outcome = Outcome.LESS_THAN_SIX_HOURS;
    } else {
      outcome = Outcome.TO_CONVERT;
    }
    return new AnalyzedAbsence(absence, outcome, wttDescription, otherCodes);
  }

  /**
   * L'orario di lavoro della persona nella data, cercato tra i periodi di orario dei suoi
   * contratti (caricati una sola volta per persona).
   */
  private static Optional<WorkingTimeType> workingTimeType(Person person, LocalDate date,
      Map<Long, List<ContractWorkingTimeType>> cwttByPerson) {
    List<ContractWorkingTimeType> cwtts = cwttByPerson.computeIfAbsent(person.id,
        id -> person.getContracts().stream()
            .flatMap(c -> c.getContractWorkingTimeType().stream())
            .collect(Collectors.toList()));
    return Optional.fromJavaUtil(cwtts.stream()
        .filter(cwtt -> !cwtt.getBeginDate().isAfter(date)
            && (cwtt.getEndDate() == null || !cwtt.getEndDate().isBefore(date)))
        .map(ContractWorkingTimeType::getWorkingTimeType)
        .findFirst());
  }

  /**
   * Verifica che tutti i giorni lavorativi dell'orario prevedano almeno 6 ore di lavoro.
   */
  private static boolean hasMinimumDailyWorkingTime(WorkingTimeType wtt) {
    return wtt.getWorkingTimeTypeDays().stream()
        .filter(d -> !d.isHoliday())
        .map(WorkingTimeTypeDay::getWorkingTime)
        .allMatch(minutes -> minutes != null && minutes >= MIN_DAILY_WORKING_MINUTES);
  }
}
