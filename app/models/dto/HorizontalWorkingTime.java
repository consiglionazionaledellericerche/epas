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

package models.dto;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.List;
import models.Office;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import play.data.validation.Max;
import play.data.validation.Min;
import play.data.validation.Required;


/**
 * Rappresenta le informazioni di un'orario di lavoro di tipo orizzontale.
 */
public class HorizontalWorkingTime {

  /**
   * Ore lavorative.
   */
  @Min(1)
  @Max(DateTimeConstants.HOURS_PER_DAY - 1)
  public int workingTimeHour = 7;

  /**
   * Frazione orario di minuti lavorativi da sommare alle ore lavorative FIXME: perché non fare un
   * campo unico con i minuti lavorativi del giorno?.
   */
  @Min(0)
  @Max(DateTimeConstants.MINUTES_PER_HOUR - 1)
  public int workingTimeMinute = 12;

  public List<String> holidays;

  public boolean mealTicketEnabled = true;
  
  public boolean reproportionAbsenceCodesEnabled = true;

  @Min(1)
  @Max(23)
  public int mealTicketTimeHour = 6;
  @Min(0)
  @Max(59)
  public int mealTicketTimeMinute = 0;
  @Min(30)
  public int breakTicketTime = 30;

  public boolean afternoonThresholdEnabled = false;

  @Min(1)
  @Max(23)
  public int ticketAfternoonThresholdHour = 13;
  @Min(0)
  @Max(59)
  public int ticketAfternoonThresholdMinute = 30;
  @Min(0)
  public int ticketAfternoonWorkingTime = 1;

  @Required
  public String name;
  public String externalId;
  
  /**
   * Costruisce il pattern di default per la costruzione di un nuovo tipo orario orizzontale.
   */
  public HorizontalWorkingTime() {
    this.holidays = Lists.newArrayList();
    this.holidays.add(LocalDate.now().withDayOfWeek(
            DateTimeConstants.SATURDAY).dayOfWeek().getAsText());
    this.holidays.add(LocalDate.now().withDayOfWeek(
            DateTimeConstants.SUNDAY).dayOfWeek().getAsText());
  }

  /**
   * Dal tipo orario ricava il pattern originario.
   */
  public HorizontalWorkingTime(final WorkingTimeType wtt) {

    this.name = wtt.description;
    this.holidays = Lists.newArrayList();
    this.reproportionAbsenceCodesEnabled = wtt.enableAdjustmentForQuantity;

    for (WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays) {

      if (wttd.holiday) {
        this.holidays.add(holidayName(wttd.dayOfWeek));
        continue;
      }

      this.workingTimeHour =
              wttd.workingTime / DateTimeConstants.SECONDS_PER_MINUTE;
      this.workingTimeMinute =
              wttd.workingTime % DateTimeConstants.SECONDS_PER_MINUTE;

      if (wttd.mealTicketTime > 0) {
        this.mealTicketEnabled = true;
        this.mealTicketTimeHour =
                wttd.mealTicketTime / DateTimeConstants.SECONDS_PER_MINUTE;
        this.mealTicketTimeMinute =
                wttd.mealTicketTime % DateTimeConstants.SECONDS_PER_MINUTE;
        this.breakTicketTime = wttd.breakTicketTime;
      } else {
        this.mealTicketEnabled = false;
      }

      if (wttd.ticketAfternoonThreshold > 0) {
        this.afternoonThresholdEnabled = true;
        this.ticketAfternoonThresholdHour =
                wttd.ticketAfternoonThreshold
                        /
                        DateTimeConstants.SECONDS_PER_MINUTE;

        this.ticketAfternoonThresholdMinute =
                wttd.ticketAfternoonThreshold
                        %
                        DateTimeConstants.SECONDS_PER_MINUTE;
        this.ticketAfternoonWorkingTime =
                wttd.ticketAfternoonWorkingTime;
      } else {
        this.afternoonThresholdEnabled = false;
      }
    }
  }

  private static final String holidayName(final int dayOfWeek) {

    return LocalDate.now().withDayOfWeek(dayOfWeek).dayOfWeek().getAsText();
  }

  /**
   * Dal pattern orizzontale costruisce il tipo orario con ogni giorno di lavoro e persiste i dati.
   */
  public final void buildWorkingTimeType(final Office office) {

    WorkingTimeType wtt = new WorkingTimeType();

    wtt.horizontal = true;
    wtt.description = this.name;
    wtt.office = office;
    wtt.disabled = false;
    wtt.externalId = this.externalId;
    wtt.enableAdjustmentForQuantity = this.reproportionAbsenceCodesEnabled;

    wtt.save();

    for (int i = 0; i < DateTimeConstants.DAYS_PER_WEEK; i++) {

      WorkingTimeTypeDay wttd = new WorkingTimeTypeDay();
      wttd.dayOfWeek = i + 1;
      wttd.workingTime =
              this.workingTimeHour * DateTimeConstants.SECONDS_PER_MINUTE
                      + this.workingTimeMinute;
      wttd.holiday = isHoliday(wttd);

      if (this.mealTicketEnabled) {
        wttd.mealTicketTime =
                this.mealTicketTimeHour
                        *
                        DateTimeConstants.SECONDS_PER_MINUTE
                        +
                        this.mealTicketTimeMinute;
        wttd.breakTicketTime = this.breakTicketTime;

        if (this.afternoonThresholdEnabled) {
          wttd.ticketAfternoonThreshold =
                  this.ticketAfternoonThresholdHour
                          *
                          DateTimeConstants.SECONDS_PER_MINUTE
                          +
                          this.ticketAfternoonThresholdMinute;
          wttd.ticketAfternoonWorkingTime =
                  this.ticketAfternoonWorkingTime;
        }
      }

      wttd.workingTimeType = wtt;
      wttd.save();

    }
  }

  private final boolean isHoliday(final WorkingTimeTypeDay wttd) {

    return this.holidays.contains(LocalDate.now()
            .withDayOfWeek(wttd.dayOfWeek).dayOfWeek().getAsText());
  }

  /**
   * TODO: Impostare un global binder.
   */
  public final void setHolidays(final String value) {
    this.holidays = Lists.newArrayList((Splitter.on(",")
            .trimResults()
            .omitEmptyStrings()
            .split(value)));
  }


}
