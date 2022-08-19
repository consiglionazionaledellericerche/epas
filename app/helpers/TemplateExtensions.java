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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.gson.Gson;
import common.injection.StaticInject;
import it.cnr.iit.epas.DateUtility;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import models.absences.JustifiedType;
import models.base.BaseModel;
import org.apache.commons.lang.WordUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.ReadablePeriod;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import play.db.jpa.GenericModel;
import play.i18n.Messages;
import play.libs.Crypto;
import play.libs.I18N;
import play.templates.JavaExtensions;

/**
 * Estensioni vari utilizzabili nei template, principamente formattatori di oggetti.
 *
 * @author Marco Andreini
 */
@StaticInject
public class TemplateExtensions extends JavaExtensions {

  private static final Joiner COMMAJ = Joiner.on(", ").skipNulls();

  private static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
      .appendYears()
      .appendSuffix(" anno", " anni")
      .appendSeparator(", ")
      .appendMonths()
      .appendSuffix(" mese", " mesi")
      .appendSeparator(", ")
      .appendWeeks()
      .appendSuffix(" settimana", " settimane")
      .appendSeparator(", ")
      .appendDays()
      .appendSuffix(" giorno", " giorni")
      .appendSeparator(", ")
      .appendHours()
      .appendSuffix(" ora", " ore")
      .appendSeparator(", ")
      .appendMinutes()
      .appendSuffix(" minuto", " minuti")
      .appendSeparator(", ")
      .printZeroRarelyLast()
      .appendSeconds()
      .appendSuffix(" secondo", " secondi")
      .toFormatter();

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormat
      .forPattern("dd/MM/yyyy HH:mm:ss");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm");
  private static final java.time.format.DateTimeFormatter DATE_TIME_JAVA_TIME_FORMATTER = 
      java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
  private static final java.time.format.DateTimeFormatter LOCALDATE_JAVA_FORMATTER = 
      java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();


  public static String format(ReadablePeriod period) {
    return PERIOD_FORMATTER.print(period);
  }

  public static String format(java.time.LocalDate date) {
    return date.format(LOCALDATE_JAVA_FORMATTER);
  }

  public static String format(java.time.LocalDateTime dateTime) {
    return dateTime.format(DATE_TIME_JAVA_TIME_FORMATTER);
  }

  public static String format(LocalDate date) {
    return format(date.toDate());
  }

  public static String format(LocalDate date, String format) {
    return format(date.toDate(), format, I18N.getDateFormat());
  }
  
  public static String format(LocalDateTime dt) {
    return DT_FORMATTER.print(dt);
  }

  public static String format(LocalDateTime dt, String format) {
    return DateTimeFormat.forPattern(format).print(dt);
  }
  
  public static String format(MonthDay md) {
    return md.toString("dd/MM");
  }

  public static String format(LocalTime time) {
    return time.toString("HH:mm");
  }

  /**
   * Metodo statico per la formattazione dell'oggetto passato.
   *
   * @param obj l'oggetto da formattare
   * @return la formattazione dell'oggetto passato.
   */
  public static String format(Object obj) {
    if (obj instanceof LocalDate) {
      return format((LocalDate) obj);
    } else {
      return obj.toString();
    }
  }

  public static String time(LocalDateTime dt) {
    return TIME_FORMATTER.print(dt);
  }

  public static String percentage(BigDecimal value) {
    return new DecimalFormat("##.### %").format(value);
  }

  /**
   * Ritorna l'applicazione del metodo getField sul campo del modello.
   *
   * @param <T> generico oggetto
   * @param models generico
   * @param fieldName il nome del campo
   * @return la stringa corrispondente al campo dell'oggetto.
   */
  public static <T extends GenericModel> String joinOnField(
      final Iterable<T> models, final String fieldName) {

    return COMMAJ.join(Iterables.transform(models, new Function<T, String>() {

      @Override
      public String apply(T model) {
        return getField(model, fieldName);
      }
    }));
  }

  /**
   * Trasforma i campi con i message.
   *
   * @param fields la lista di campi
   * @return l'applicazione del message sull'oggetto.
   */
  public static String i18nJoin(final Iterable<Enum<?>> fields) {
    return COMMAJ.join(Iterables.transform(fields, new Function<Enum<?>, String>() {

      @Override
      public String apply(Enum<?> field) {
        return Messages.get(field.toString());
      }
    }));
  }

  /**
   * Traduce l'enumerato.
   *
   * @param item l'enumerato da tradurre
   * @return la traduzione dei valori di un enum è composta da NomeSempliceEnum.valore.
   */
  public static String label(Enum<?> item) {
    return Messages.get(item.getClass().getSimpleName() + "." + item.name());
  }

  /**
   * Formatta l'oggetto in formato stringa.
   *
   * @param obj l'oggetto da considerare
   * @return la formattazione in stringa dell'oggetto passato.
   */
  public static String label(Object obj) {
    if (obj instanceof JustifiedType) {
      return Messages.get(obj.toString());
    }
    if (obj instanceof BaseModel) {
      return ((BaseModel) obj).getLabel();
    }
    if (obj instanceof LocalDate) {
      return format((LocalDate) obj);
    }
    if (obj instanceof LocalTime) {
      return format((LocalTime) obj);
    }
    if (obj instanceof MonthDay) {
      return format((MonthDay) obj);
    }
    if (obj instanceof Boolean) {
      if ((Boolean) obj) {
        return Messages.get("views.common.yes_or_no.true");
      } else {
        return Messages.get("views.common.yes_or_no.false");
      }
    }
    return obj.toString();
  }

  /**
   * Traduce il range passato come parametro.
   *
   * @param obj il range da tradurre
   * @return la traduzione in stringa del range passato come parametro.
   */
  public static String label(Range<?> obj) {
    if (obj.isEmpty()) {
      return Messages.get("range.empty");
    } else {
      if (obj.hasLowerBound() && obj.hasUpperBound()) {
        return Messages.get("range.from_to", format(obj.lowerEndpoint()),
            format(obj.upperEndpoint()));
      } else if (obj.hasLowerBound()) {
        return Messages.get("range.from", format(obj.lowerEndpoint()));
      } else if (obj.hasUpperBound()) {
        return Messages.get("range.to", format(obj.upperEndpoint()));
      } else {
        return Messages.get("range.full");
      }
    }
  }

  public static Object label(String label) {
    return label(label, new Object[]{});
  }

  /**
   * Traduce un oggetto.
   *
   * @param label la label da tradurre
   * @param args la lista di argomenti
   * @return l'oggetto tradotto
   */
  public static Object label(String label, Object... args) {
    if (label.contains("%")) {
      return label;
    }
    return raw(Messages.get(label, args));
  }

  public static Iterable<String> commaSplit(String value) {
    return COMMA_SPLITTER.split(value);
  }

  /**
   * Ritorna la stringa cryptata con aes e chiave play predefinita.
   *
   * @param value la stringa da criptare
   * @return la stringa cryptata con aes e chiave play predefinita.
   */
  public static String encrypt(String value) {
    return Crypto.encryptAES(value);
  }

  public static String toJson(Object obj) {
    return new Gson().toJson(obj);
  }

  public static String escapeAttribute(String str) {
    return str.replace("\"", "&quot;");
  }

  public static String[] toStringItems(Iterable<Object> iterable) {
    return Iterables.toArray(
        Iterables.transform(iterable, Functions.toStringFunction()), String.class);
  }

  public static String value(LocalDate date) {
    return date.toString("dd/MM/yyyy");
  }

  public static String value(String string) {
    return string;
  }

  public static String shortDayName(LocalDate date) {
    final DateTimeFormatter fmt = DateTimeFormat.forPattern("dd E");
    return date.toString(fmt);
  }

  private static String getField(GenericModel model, String fieldName) {
    try {
      final Object obj = model.getClass().getField(fieldName).get(model);
      return obj != null ? obj.toString() : null;
    } catch (Throwable throwable) {
      // TODO logging
      throw new RuntimeException(throwable);
    }
  }

  /**
   * Minuti in formato HH:MM.
   *
   * @param minutes minuti
   * @return stringa formattata
   */
  public static String printHourMinute(Integer minutes) {
    return DateUtility.fromMinuteToHourMinute(minutes);
  }

  public static String dayOfWeek(Integer day) {
    return WordUtils.capitalize(LocalDate.now().withDayOfWeek(day).dayOfWeek().getAsText());
  }

  /**
   * Ritorna la stringa dell'anno mese passato come parametro.
   *
   * @param month Yearmoth da formattare
   * @return La Stringa in formato Mese(nome) Anno
   */
  public static String asText(YearMonth month) {
    return WordUtils.capitalize(month.monthOfYear().getAsText()) + " " + month.getYear();
  }

}
