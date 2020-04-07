package helpers;

import java.util.Date;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.templates.JavaExtensions;

public class PersonTags extends JavaExtensions {


  public static String toDateTime(LocalDate localDate) {
    return String.format("%1$td %1$tB %1$tY", localDate.toDate());
  }

  /**
   * Ritorna la stringa per la data con ora passata come parametro.
   * @param ldt la data con orario
   * @return la data in formato stringa nel format specificato.
   */
  public static String toCalendarTime(LocalDateTime ldt) {
    Number hour = ldt.getHourOfDay();
    Number minute = ldt.getMinuteOfHour();
    return String.format("%02d:%02d", hour, minute);
  }

  /**
   * Costruisce ore e minuti a partire dal numero dei minuti.
   * @param minutes il numero dei minuti.
   * @return la stringa contenente ore e minuti a partire dai minuti.
   */
  public static String toHourTime(Integer minutes) {
    int min = Math.abs(minutes % 60);
    int hour = Math.abs(minutes / 60);
    if ((minutes.intValue() < 0)) {
      return String.format("-%02d:%02d", hour, min);
    }
    return String.format("%02d:%02d", hour, min);
  }

  /**
   * La stringa contenente ore e minuti col "+" davanti se positivo.
   * @param minutes il numero dei minuti
   * @return il numero di ore e miunti col "+" davanti se positivo.
   */
  public static String toHourTimeWithPlus(Integer minutes) {
    if (minutes < 0) {
      return toHourTime(minutes);
    }
    return "+" + toHourTime(minutes);
  }

  /**
   * La stringa contenente ore e minuti col "-" davanti se negativo.
   * @param minutes il numero dei minuti
   * @return il numero di ore e miunti col "-" davanti se negativo.
   */
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
