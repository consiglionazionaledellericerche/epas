/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

package jobs;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import models.ShiftType;
import models.ShiftTypeMonth;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.Play;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Creazione degli ShiftTypeMonth per ogni attività di turno 
 * (shiftType) nel pregresso.
 *
 * @author Daniele Murgia
 * @since 09/06/17
 */
@Slf4j
@OnApplicationStart(async = true)
public class ShiftMonthStatus extends Job<Void> {

  /**
   * Crea gli ShiftTypeMonth per ogni attività di turno (shiftType) nel pregresso.
   */
  @Override
  public void doJob() {
    
    //in modo da inibire l'esecuzione dei job in base alla configurazione
    if (!"true".equals(Play.configuration.getProperty(Bootstrap.JOBS_CONF))) {
      log.info("{} interrotto. Disattivato dalla configurazione.", getClass().getName());
      return;
    }
    
    final List<ShiftType> shiftTypes = ShiftType.findAll();
    final YearMonth currentMonth = YearMonth.now();

    shiftTypes.forEach(activity -> {
      if (activity.getMonthsStatus().isEmpty()) {
        final Optional<LocalDate> oldestShift = activity.getPersonShiftDays().stream()
            .min(Comparator.comparing(shift -> shift.getDate()))
            .map(personShiftDay -> personShiftDay.getDate());
        if (!oldestShift.isPresent()) {
          log.debug("attività {} senza personShiftDays, monthStatus non creato.", activity);
          return;
        }
        YearMonth month = new YearMonth(oldestShift.get());
        do {

          ShiftTypeMonth monthStatus = new ShiftTypeMonth();
          monthStatus.setShiftType(activity);
          monthStatus.setYearMonth(month);
          monthStatus.setApproved(true);
          monthStatus.save();

          log.info("Creato nuovo {} per il turno {} nel mese {}",
              monthStatus.getClass().getSimpleName(), activity, month);
          month = month.plusMonths(1);
        } while (month.isBefore(currentMonth));
      }
    });
  }
}
