package helpers;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.templates.JavaExtensions;

import java.util.Date;

public class PersonTags extends JavaExtensions {


  public static String toDateTime(LocalDate localDate) {
    return String.format("%1$td %1$tB %1$tY", localDate.toDate());
  }

  public static String toCalendarTime(LocalDateTime ldt) {
    Number hour = ldt.getHourOfDay();
    Number minute = ldt.getMinuteOfHour();
    return String.format("%02d:%02d", hour, minute);
  }

  public static String toHourTime(Integer minutes) {
    int min = Math.abs(minutes % 60);
    int hour = Math.abs(minutes / 60);
    if ((minutes.intValue() < 0)) {
      return String.format("-%02d:%02d", hour, min);
    }
    return String.format("%02d:%02d", hour, min);
  }

  public static String toHourTimeWithPlus(Integer minutes) {
    if (minutes < 0) {
      return toHourTime(minutes);
    }
    return "+" + toHourTime(minutes);
  }

  public static String toHourTimeWithMinus(Integer minutes) {
    if (minutes < 0) {
      return toHourTime(minutes);
    }
    return "-" + toHourTime(minutes);
  }

  public static String toHour(Integer minutes) {
    int hour = Math.abs(minutes / 60);
    return String.format("%d", hour);
  }

  public static LocalDate convertToLocalDate(Date date) {
    return new LocalDate(date);
  }

}
