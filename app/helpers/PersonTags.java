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

package helpers;

import java.util.Date;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.templates.JavaExtensions;

/**
 * Classe di utilità per la gestione di date e orari.
 *
 * @author dario
 *
 */
public class PersonTags extends JavaExtensions {


  public static String toDateTime(LocalDate localDate) {
    return String.format("%1$td %1$tB %1$tY", localDate.toDate());
  }

  /**
   * Ritorna la stringa per la data con ora passata come parametro.
   *
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
   *
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
   *
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
   *
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
