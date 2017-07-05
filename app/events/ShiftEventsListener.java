package events;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import dao.ShiftDao;
import dao.ShiftTypeMonthDao;
import dao.history.HistoricalDao;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import manager.ShiftManager2;
import models.PersonShiftDay;
import models.ShiftType;
import models.ShiftTypeMonth;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.YearMonth;
import play.jobs.Job;

/**
 * @author daniele
 * @since 10/06/17.
 */
@Slf4j
public class ShiftEventsListener {

  private final ShiftTypeMonthDao shiftTypeMonthDao;
  private final ShiftDao shiftDao;
  private final ShiftManager2 shiftManager2;

  @Inject
  public ShiftEventsListener(EventBus bus, ShiftTypeMonthDao shiftTypeMonthDao,
      ShiftDao shiftDao, ShiftManager2 shiftManager2) {
    this.shiftDao = shiftDao;
    this.shiftTypeMonthDao = shiftTypeMonthDao;
    this.shiftManager2 = shiftManager2;
    bus.register(this);
  }

  @Subscribe
  public void shiftChanged(PersonShiftDay shift) {

    final long shiftTypeId = shift.shiftType.id;
    final long shiftId = shift.id;
    final LocalDate date = shift.date;

    // Il Job evita le chiamate ricorsive (il motivo non l'ho ancora capito)
    // FIXME le modifiche fatte dal job non hanno un utente nell'history
    new Job<Void>() {

      @Override
      public void doJob() {
        final Optional<ShiftType> shiftType = shiftDao.getShiftTypeById(shiftTypeId);

        // Aggiornamento dello ShiftTypeMonth
        if (shiftType.isPresent()) {

          PersonShiftDay psd = PersonShiftDay.findById(shiftId);
          // Ricalcoli sul turno
          if (psd != null && psd.isPersistent()) {
            shiftManager2.checkShiftValid(psd);
          }

          // Ricalcoli sui giorni coinvolti dalle modifiche
          HistoricalDao.lastRevisionsOf(PersonShiftDay.class, shiftId)
              .stream().limit(2).map(historyValue -> {
            PersonShiftDay pd = (PersonShiftDay) historyValue.value;
            return pd.date;
          }).filter(Objects::nonNull).distinct()
              .forEach(localDate -> {
                shiftManager2.checkShiftDayValid(localDate, shiftType.get());
              });

          final Optional<ShiftTypeMonth> monthStatus = shiftTypeMonthDao
              .byShiftTypeAndDate(shiftType.get(), date);

          ShiftTypeMonth newStatus;

          if (monthStatus.isPresent()) {
            newStatus = monthStatus.get();
            newStatus.updatedAt = LocalDateTime.now();
          } else {
            newStatus = new ShiftTypeMonth();
            newStatus.yearMonth = new YearMonth(date);
            newStatus.shiftType = shiftType.get();
          }
          newStatus.save();
        }
      }
    }.now();
  }
}
