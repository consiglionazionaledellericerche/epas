package helpers;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.gson.Gson;

import it.cnr.iit.epas.DateUtility;

import models.base.BaseModel;

import org.apache.commons.lang.WordUtils;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.ReadablePeriod;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import injection.StaticInject;
import play.db.jpa.GenericModel;
import play.i18n.Messages;
import play.libs.Crypto;
import play.templates.JavaExtensions;

import java.math.BigDecimal;
import java.text.DecimalFormat;

/**
 * Estensioni vari utilizzabili nei template, principamente formattatori di oggetti.
 *
 * @author marco
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
  private static final Splitter COMMA_SPLITTER = Splitter.on(',').omitEmptyStrings().trimResults();

    
  public static String format(ReadablePeriod period) {
    return PERIOD_FORMATTER.print(period);
  }

  public static String format(LocalDate date) {
    return format(date.toDate());
  }

  public static String format(LocalDateTime dt) {
    return DT_FORMATTER.print(dt);
  }
  
  public static String format(MonthDay md) {
    return md.toString("dd/MM");
  }
   
  public static String format(LocalTime time) {
    return time.toString("HH:mm"); 
  }

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

  public static <T extends GenericModel> String joinOnField(
      final Iterable<T> models, final String fieldName) {

    return COMMAJ.join(Iterables.transform(models, new Function<T, String>() {

      @Override
      public String apply(T model) {
        return getField(model, fieldName);
      }
    }));
  }

  public static String i18nJoin(final Iterable<Enum<?>> fields) {
    return COMMAJ.join(Iterables.transform(fields, new Function<Enum<?>, String>() {

      @Override
      public String apply(Enum<?> field) {
        return Messages.get(field.toString());
      }
    }));
  }

  /**
   * @return la traduzione dei valori di un enum è composta da NomeSempliceEnum.valore
   */
  public static String label(Enum<?> item) {
    return Messages.get(item.getClass().getSimpleName() + "." + item.name());
  }

  public static String label(Object obj) {
    if (obj instanceof BaseModel) {
      return ((BaseModel)obj).getLabel();
    }
    if (obj instanceof LocalDate) {
      return format((LocalDate)obj);
    }
    if (obj instanceof LocalTime) {
      return format((LocalTime)obj);
    }
    if (obj instanceof MonthDay) {
      return format((MonthDay)obj);
    }
    if (obj instanceof Boolean) {
      if ((Boolean)obj) {
        return Messages.get("views.common.yes_or_no.true");
      } else {
        return Messages.get("views.common.yes_or_no.false");
      }
    }
    return obj.toString();
  }

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

  public static Object label(String label, Object... args) {
    if (label.contains("%")){
      return label;
    }
    return raw(Messages.get(label, args));
  }

  public static Iterable<String> commaSplit(String value) {
    return COMMA_SPLITTER.split(value);
  }

  /**
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
      throw Throwables.propagate(throwable);
    }
  }
  
  public static String printHourMinute(Integer i) {
    if (i > 0) {
      return DateUtility.fromMinuteToHourMinute(i);
    } else {
      return DateUtility.fromMinuteToHourMinute(i);
    }
  }
  
  public static String dayOfWeek(Integer day) {
    return WordUtils.capitalize(LocalDate.now().withDayOfWeek(day).dayOfWeek().getAsText());
  }
}
