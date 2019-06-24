package jobs;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import models.ShiftType;
import models.ShiftTypeMonth;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Creazione degli ShiftTypeMonth per ogni attività di turno 
 * (shiftType) nel pregresso.
 * 
 * @author daniele
 * @since 09/06/17.
 */
@Slf4j
@OnApplicationStart(async = true)
public class ShiftMonthStatus extends Job<Void> {

  /**
   * Crea gli ShiftTypeMonth per ogni attività di turno (shiftType) nel pregresso.
   */
  @Override
  public void doJob() {
    final List<ShiftType> shiftTypes = ShiftType.findAll();
    final YearMonth currentMonth = YearMonth.now();

    shiftTypes.forEach(activity -> {
      if (activity.monthsStatus.isEmpty()) {
        final Optional<LocalDate> oldestShift = activity.personShiftDays.stream()
            .min(Comparator.comparing(shift -> shift.date))
            .map(personShiftDay -> personShiftDay.date);
        if (!oldestShift.isPresent()) {
          log.info("activita {} senza personShiftDays, monthStatus non creato.", activity);
          return;
        }
        YearMonth month = new YearMonth(oldestShift.get());
        do {

          ShiftTypeMonth monthStatus = new ShiftTypeMonth();
          monthStatus.shiftType = activity;
          monthStatus.yearMonth = month;
          monthStatus.approved = true;
          monthStatus.save();

          log.info("Creato nuovo {} per il turno {} nel mese {}",
              monthStatus.getClass().getSimpleName(), activity, month);
          month = month.plusMonths(1);
        } while (month.isBefore(currentMonth));
      }
    });
  }
}
