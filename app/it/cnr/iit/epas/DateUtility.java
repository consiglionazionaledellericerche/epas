package it.cnr.iit.epas;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTimeFieldType;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.Months;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateUtility {

  public static final int DECEMBER = 12;
  public static final int JANUARY = 1;
  static final int MINUTE_IN_HOUR = 60;
  static final LocalDate MAX_DATE = new LocalDate(9999, 12, 1);

  /**
   * @return il giorno in cui cade la pasqua.
   */
  private static final LocalDate findEaster(int year) {
    if (year <= 1582) {
      throw new IllegalArgumentException("Algorithm invalid before April 1583");
    }
    int golden, century, x, z, d, epact, n;
    LocalDate easter = null;
    golden = (year % 19) + 1; /* E1: metonic cycle */
    century = (year / 100) + 1; /* E2: e.g. 1984 was in 20th C */
    x = (3 * century / 4) - 12; /* E3: leap year correction */
    z = ((8 * century + 5) / 25) - 5; /* E3: sync with moon's orbit */
    d = (5 * year / 4) - x - 10;
    epact = (11 * golden + 20 + z - x) % 30; /* E5: epact */
    if ((epact == 25 && golden > 11) || epact == 24) {
      epact++;
    }
    n = 44 - epact;
    n += 30 * (n < 21 ? 1 : 0); /* E6: */
    n += 7 - ((d + n) % 7);

    if (n > 31) /* E7: */ {
      easter = new LocalDate(year, 4, n - 31);

      return easter; /* April */
    } else {
      easter = new LocalDate(year, 3, n);

      return easter; /* March */
    }
  }

  /**
   * Festività generale.
   * @param officePatron giorno del patrono
   * @param date data da verificare
   * @return esito
   */
  public static boolean isGeneralHoliday(
      final Optional<MonthDay> officePatron, final LocalDate date) {

    LocalDate easter = findEaster(date.getYear());
    LocalDate easterMonday = easter.plusDays(1);
    if (date.getDayOfMonth() == easter.getDayOfMonth()
            && date.getMonthOfYear() == easter.getMonthOfYear()) {
      return true;
    }
    if (date.getDayOfMonth() == easterMonday.getDayOfMonth()
            && date.getMonthOfYear() == easterMonday.getMonthOfYear()) {
      return true;
    }
    // if((date.getDayOfWeek() == 7)||(date.getDayOfWeek() == 6))
    // return true;
    if ((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 25)) {
      return true;
    }
    if ((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 26)) {
      return true;
    }
    if ((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 8)) {
      return true;
    }
    if ((date.getMonthOfYear() == 6) && (date.getDayOfMonth() == 2)) {
      return true;
    }
    if ((date.getMonthOfYear() == 4) && (date.getDayOfMonth() == 25)) {
      return true;
    }
    if ((date.getMonthOfYear() == 5) && (date.getDayOfMonth() == 1)) {
      return true;
    }
    if ((date.getMonthOfYear() == 8) && (date.getDayOfMonth() == 15)) {
      return true;
    }
    if ((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 1)) {
      return true;
    }
    if ((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 6)) {
      return true;
    }
    if ((date.getMonthOfYear() == 11) && (date.getDayOfMonth() == 1)) {
      return true;
    }

    if (officePatron.isPresent()) {

      return (date.getMonthOfYear() == officePatron.get().getMonthOfYear()
              && date.getDayOfMonth() == officePatron.get().getDayOfMonth());
    }

    /**
     * ricorrenza centocinquantenario dell'unità d'Italia.
     */
    if (date.isEqual(new LocalDate(2011, 3, 17))) {
      return true;
    }

    return false;
  }

  /**
   * @param begin data iniziale.
   * @param end   data finale
   * @return lista di tutti i giorni fisici contenuti nell'intervallo [begin,end] estremi compresi,
   *     escluse le general holiday
   */
  public static List<LocalDate> getGeneralWorkingDays(final LocalDate begin, final LocalDate end) {

    LocalDate day = begin;
    List<LocalDate> generalWorkingDays = new ArrayList<LocalDate>();
    while (!day.isAfter(end)) {
      if (!DateUtility.isGeneralHoliday(Optional.<MonthDay>absent(), day)) {
        generalWorkingDays.add(day);
      }
      day = day.plusDays(1);
    }
    return generalWorkingDays;
  }


  /**
   * Se la data è contenuta nell'intervallo.
   * @param date     data
   * @param interval intervallo
   * @return true se la data ricade nell'intervallo estremi compresi
   */
  public static boolean isDateIntoInterval(final LocalDate date, final DateInterval interval) {
    
    if (interval == null) {
      return false;
    }
    LocalDate dateToCheck = date;
    if (dateToCheck == null) {
      dateToCheck = MAX_DATE;
    }

    if (dateToCheck.isBefore(interval.getBegin()) || dateToCheck.isAfter(interval.getEnd())) {
      return false;
    }
    return true;
  }

  /**
   * L'intervallo contenente l'intersezione fra inter1 e inter2.
   * @param inter1 primo intervallo
   * @param inter2 secondo intervallo
   * @return l'intervallo contenente l'intersezione fra inter1 e inter2, null in caso di
   *         intersezione vuota.
   */
  public static DateInterval intervalIntersection(final DateInterval inter1, 
      final DateInterval inter2) {
  
    if (inter1 == null || inter2 == null) {
      return null;
    }
    
    // un intervallo contenuto nell'altro
    if (isIntervalIntoAnother(inter1, inter2)) {
      return new DateInterval(inter1.getBegin(), inter1.getEnd());
    }

    if (isIntervalIntoAnother(inter2, inter1)) {
      return new DateInterval(inter2.getBegin(), inter2.getEnd());
    }

    DateInterval copy1 = new DateInterval(inter1.getBegin(), inter1.getEnd());
    DateInterval copy2 = new DateInterval(inter2.getBegin(), inter2.getEnd());

    // ordino
    if (!inter1.getBegin().isBefore(inter2.getBegin())) {
      DateInterval aux = new DateInterval(inter1.getBegin(), inter1.getEnd());
      copy1 = inter2;
      copy2 = aux;
    }
 
    // fine di inter1 si interseca con inizio di inter2
    if (copy1.getEnd().isBefore(copy2.getBegin())) {
      return null;
    } else {
      return new DateInterval(copy2.getBegin(), copy1.getEnd());
    }
  }
  
  /**
   * Conta il numero di giorni appartenenti all'intervallo estremi compresi.
   * @param inter l'intervallo
   * @return numero di giorni
   */
  public static int daysInInterval(final DateInterval inter) {

    int days = Days.daysBetween(inter.getBegin(), inter.getEnd()).getDays() + 1;

    //controllo compatibilità con vecchio algoritmo.
    if (inter.getBegin().getYear() == inter.getEnd().getYear()) {
      int oldDays = inter.getEnd().getDayOfYear() - inter.getBegin().getDayOfYear() + 1;
      Preconditions.checkState(days == oldDays);
    }

    return days;

  }

  /**
   * Conta il numero di mesi appartenenti all'intervallo, estremi compresi.
   * @param inter intervallo
   * @return numero di mesi
   */
  public static int monthsInInterval(final DateInterval inter) {
    return Months.monthsBetween(inter.getBegin(), inter.getEnd()).getMonths() + 1;
  }

  /**
   * Se il primo intervallo è contenuto nel secondo intervallo.
   * @param first  il primo intervallo
   * @param second il secondo intervallo
   * @return se il primo interllallo è contenuto nel secondo intervallo.
   */
  public static boolean isIntervalIntoAnother(final DateInterval first, final DateInterval second) {

    if (first.getBegin().isBefore(second.getBegin()) 
        || first.getEnd().isAfter(second.getEnd())) {
      return false;
    }
    return true;
  }
  
  /**
   * Se i due inervalli coincidono.
   * @param first first
   * @param second second
   * @return esito
   */
  public static boolean areIntervalsEquals(final DateInterval first, final DateInterval second) {
    if (first.getBegin().isEqual(second.getBegin()) 
        && first.getEnd().isEqual(second.getEnd())) {
      return true;
    }
    return false;
  }

  /**
   * La data massima che equivale a infinito.
   * @return la data infinito
   */
  public static LocalDate setInfinity() {
    return MAX_DATE;
  }

  /**
   * @param date la data da confrontare
   * @return se la data è molto molto lontana...
   */
  public static boolean isInfinity(final LocalDate date) {
    return date.equals(MAX_DATE);
  }
  
  /**
   * L'intervallo dell'anno.
   * @param year anno
   * @return l'intervallo
   */
  public static DateInterval getYearInterval(int year) {
    return new DateInterval(new LocalDate(year, 1, 1), new LocalDate(year, 12, 31));
  }


  /**
   * @param monthNumber mese da formattare.
   * @return il nome del mese con valore monthNumber, null in caso di argomento non valido
   */
  public static String fromIntToStringMonth(final Integer monthNumber) {
    LocalDate date = new LocalDate().withMonthOfYear(monthNumber);
    return date.monthOfYear().getAsText();
  }

  /**
   * @param minute minuti da formattare.
   * @return stringa contente la formattazione -?HH:MM
   */
  public static String fromMinuteToHourMinute(final int minute) {
    if (minute == 0) {
      return "00:00";
    }
    String string = "";
    int positiveMinute = minute;
    if (minute < 0) {
      string = string + "-";
      positiveMinute = minute * -1;
    }
    int hour = positiveMinute / MINUTE_IN_HOUR;
    int min = positiveMinute % MINUTE_IN_HOUR;

    if (hour < 10) {
      string = string + "0" + hour;
    } else {
      string = string + hour;
    }
    string = string + ":";
    if (min < 10) {
      string = string + "0" + min;
    } else {
      string = string + min;
    }
    return string;
  }


  /**
   * @param date data.
   * @param pattern : default dd/MM
   * @return effettua il parsing di una stringa che contiene solo giorno e Mese
   */
  public static LocalDate dayMonth(final String date, final Optional<String> pattern) {

    DateTimeFormatter dtf;
    if (pattern.isPresent()) {
      dtf = DateTimeFormat.forPattern(pattern.get());
    } else {
      dtf = DateTimeFormat.forPattern("dd/MM");
    }
    return LocalDate.parse(date, dtf);
  }

  /**
   * @param yearMonth il mese da considerare.
   * @return il primo giorno del mese da considerare formato LocalDate.
   */
  public static LocalDate getMonthFirstDay(final YearMonth yearMonth) {
    return new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1);
  }

  /**
   * @param yearMonth il mese da considerare.
   * @return l'ultimo giorno del mese da considerare formato LocalDate.
   */
  public static LocalDate getMonthLastDay(final YearMonth yearMonth) {
    return new LocalDate(yearMonth.getYear(), yearMonth.getMonthOfYear(), 1).dayOfMonth()
            .withMaximumValue();
  }

  /**
   * @param time ora.
   * @return il numero di minuti trascorsi dall'inizio del giorno all'ora.
   */
  public static int toMinute(final LocalDateTime time) {
    return toMinute(time.toLocalTime());
  }
  
  
  /**
   * Il tempo dalla mezzanotte. 
   * @param time orario
   * @return tempo
   */
  public static int toMinute(final LocalTime time) {
    int dateToMinute = 0;
    if (time != null) {
      int hour = time.get(DateTimeFieldType.hourOfDay());
      int minute = time.get(DateTimeFieldType.minuteOfHour());
      dateToMinute = (MINUTE_IN_HOUR * hour) + minute;
    }
    return dateToMinute;
  }

  /**
   * @param begin orario di ingresso.
   * @param end   orario di uscita.
   * @return minuti lavorati.
   */
  public static Integer getDifferenceBetweenLocalTime(final LocalTime begin, final LocalTime end) {

    int timeToMinute = 0;
    if (end != null && begin != null) {
      int hourBegin = begin.getHourOfDay();
      int minuteBegin = begin.getMinuteOfHour();
      int hourEnd = end.getHourOfDay();
      int minuteEnd = end.getMinuteOfHour();
      timeToMinute =
              ((MINUTE_IN_HOUR * hourEnd + minuteEnd) - (MINUTE_IN_HOUR * hourBegin + minuteBegin));
    }

    return timeToMinute;
  }
}
