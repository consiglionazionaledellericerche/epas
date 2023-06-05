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

import java.time.LocalDate;

/**
 * Converte date da java.time a org.joda.time e viceversa.
 *
 * @author Cristian Lucchesi
 */
public class JodaConverters {

  private static int milliToNanoConst = 1000000;

  /**
   * Converte una org.joda.time.LocalDate in una java.time.LocalDate
   *
   * @param source Joda-Time LocalDate
   * @return Java 8 LocalDate
   */
  public static LocalDate jodaToJavaLocalDate(org.joda.time.LocalDate source) {
    return source == null ? null : 
      LocalDate.of(
        source.getYear(), source.getMonthOfYear(), source.getDayOfMonth());
  }
  
  /**
   * Converte una org.joda.time.LocalTime in una java.time.LocalTime.
   *
   * @param source Joda-Time LocalTime
   * @return Java 8 LocalTime
   */
  public static java.time.LocalTime jodaToJavaLocalTime(org.joda.time.LocalTime source) {
    return source == null ? null :
      java.time.LocalTime.of(
          source.getHourOfDay(), source.getMinuteOfHour(),
          source.getSecondOfMinute(), source.getMillisOfSecond() * milliToNanoConst);
  }
  
  /**
   * Converts Joda-Time LocalDateTime to Java 8 equivalent.
   *
   * @param source Joda-Time LocalDateTime
   * @return Java 8 LocalDateTime
   */
  public static java.time.LocalDateTime jodaToJavaLocalDateTime(
      org.joda.time.LocalDateTime source) {
    return source == null ? null :
      java.time.LocalDateTime.of(
        source.getYear(), source.getMonthOfYear(),
        source.getDayOfMonth(), 
        source.getHourOfDay(), source.getMinuteOfHour(),
        source.getSecondOfMinute(), source.getMillisOfSecond() * milliToNanoConst);
  }

  /**
   * Converts Java 8 LocalDate to Joda-Time equivalent.
   *
   * @param source Java 8 LocalDate
   * @return Joda-Time LocalDate
   */
  public static org.joda.time.LocalDate javaToJodaLocalDate(LocalDate source) {
    return source == null ? null :
      new org.joda.time.LocalDate(
          source.getYear(), source.getMonthValue(), source.getDayOfMonth());
  }
  
  /**
   * Converts Java 8 LocalTime to Joda-Time equivalent.
   * <p>
   * This is a potentially lossy operation. Any time info below millis unit are deleted.
   * </p>
   *
   * @param localTime Java 8 LocalTime
   * @return Joda-Time LocalTime
   */
  public static org.joda.time.LocalTime javaToJodaLocalTime(java.time.LocalTime localTime) {
    return localTime == null ? null :
      new org.joda.time.LocalTime(
        localTime.getHour(),
        localTime.getMinute(),
        localTime.getSecond(),
        localTime.getNano() / milliToNanoConst);
  }

  /**
   * Converts Java 8 LocalDateTime to Joda-Time equivalent.
   * <p>
   * This is a potentially lossy operation. Any time info below millis unit are lost.
   * </p>
   *
   * @param localDateTime Java 8 LocalDateTime
   * @return Joda-Time LocalDateTime
   */
  public static org.joda.time.LocalDateTime javaToJodaLocalDateTime(
      java.time.LocalDateTime localDateTime) {
    return localDateTime == null ? null :
      new org.joda.time.LocalDateTime(
        localDateTime.getYear(),
        localDateTime.getMonthValue(),
        localDateTime.getDayOfMonth(),
        localDateTime.getHour(),
        localDateTime.getMinute(),
        localDateTime.getSecond(),
        localDateTime.getNano() / milliToNanoConst);
  }
}