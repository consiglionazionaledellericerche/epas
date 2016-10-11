package manager.configurations;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import manager.configurations.EpasParam.EpasParamValueType.IpList;
import manager.configurations.EpasParam.EpasParamValueType.LocalTimeInterval;

import models.Office;
import models.Person;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.format.DateTimeFormat;
import org.testng.collections.Lists;

import java.util.List;

public enum EpasParam {

  //#######################################
  // GENERAL PARAMS

  ABSENCES_FOR_EMPLOYEE("absences_for_employee",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  OFF_SITE_STAMPING("off_site_stamping",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Person.class),

  DAY_OF_PATRON("dayOfPatron", 
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.DAY_MONTH,
      EpasParamValueType.formatValue(new MonthDay(1,1)),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS, 
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  WEB_STAMPING_ALLOWED("web_stamping_allowed",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  ADDRESSES_ALLOWED("addresses_allowed",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.IP_LIST,
      EpasParamValueType.formatValue(new IpList(Lists.<String>newArrayList())),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  NUMBER_OF_VIEWING_COUPLE("number_of_viewing_couple",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.INTEGER,
      EpasParamValueType.formatValue(2),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  DATE_START_MEAL_TICKET("date_start_meal_ticket",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.LOCALDATE,
      EpasParamValueType.formatValue(new LocalDate(2014, 7, 1)),
      Lists.newArrayList(RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  SEND_EMAIL("send_email",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  NEW_ATTESTATI("new_attestati",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),
      Office.class),
  
  /**
   * Di quanto indietro nel tempo effettuare il ricalcolo dei personDays.
   */
  RECOMPUTATION_LIMIT("recomputation_limit",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.INTEGER,
      EpasParamValueType.formatValue(6),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Viene utilizzato per popolare il campo replyTo delle mail inviate dal sistema. 
   */
  EMAIL_TO_CONTACT("email_to_contact",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.EMAIL,
      EpasParamValueType.formatValue(""),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  /**
   * Nuovo parametro per consentire/inibire la possibilità del dipendente di gestirsi 
   * l'orario di lavoro fuori sede.
   */
  WORKING_OFF_SITE("working_off_site",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.<RecomputationType>newArrayList(),  
      Office.class),


  //#######################################
  // YEARLY PARAMS

  EXPIRY_VACATION_PAST_YEAR("expiry_vacation_past_year",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.DAY_MONTH,
      EpasParamValueType.formatValue(new MonthDay(8,31)),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  MONTH_EXPIRY_RECOVERY_DAYS_13("month_expire_recovery_days_13",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.MONTH,
      EpasParamValueType.formatValue(0),
      Lists.newArrayList(RecomputationType.RESIDUAL_HOURS),
      Office.class),


  MONTH_EXPIRY_RECOVERY_DAYS_49("month_expire_recovery_days_49",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.MONTH,
      EpasParamValueType.formatValue(3),
      Lists.newArrayList(RecomputationType.RESIDUAL_HOURS),
      Office.class),

  MAX_RECOVERY_DAYS_13("max_recovery_days_13",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.INTEGER,
      EpasParamValueType.formatValue(22),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  MAX_RECOVERY_DAYS_49("max_recovery_days_49",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.INTEGER,
      EpasParamValueType.formatValue(0),
      Lists.<RecomputationType>newArrayList(),
      Office.class),

  //#######################################
  // PERIODIC PARAMS

  MATERNITY_PERIOD("maternity_period",
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.BOOLEAN,
      EpasParamValueType.formatValue(false),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS, 
          RecomputationType.RESIDUAL_MEALTICKETS),
      Person.class),

  HOUR_MAX_TO_CALCULATE_WORKTIME("hour_max_to_calculate_worktime",
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME,
      EpasParamValueType.formatValue(new LocalTime(5, 0)),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS, 
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  LUNCH_INTERVAL("lunch_interval",
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME_INTERVAL,
      EpasParamValueType
      .formatValue(new LocalTimeInterval(new LocalTime(12,0), new LocalTime(15,0))),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS, 
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class),

  WORK_INTERVAL("work_interval",
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME_INTERVAL,
      EpasParamValueType
      .formatValue(new LocalTimeInterval(new LocalTime(0,0), new LocalTime(23,59))),
      Lists.newArrayList(RecomputationType.DAYS, RecomputationType.RESIDUAL_HOURS, 
          RecomputationType.RESIDUAL_MEALTICKETS),
      Office.class);

  public final String name;
  public final EpasParamTimeType epasParamTimeType;
  public final EpasParamValueType epasParamValueType;
  public final List<RecomputationType> recomputationTypes;
  public final Object defaultValue;
  public final Class target;

  EpasParam(String name, EpasParamTimeType epasParamTimeType, EpasParamValueType epasParamValueType,
      Object defaultValue, List<RecomputationType> recomputationTypes, Class target) {
    this.name = name;
    this.epasParamTimeType = epasParamTimeType;
    this.epasParamValueType = epasParamValueType;
    this.defaultValue = defaultValue;
    this.recomputationTypes = recomputationTypes;
    this.target = target;
  }

  public boolean isYearly() {
    return this.epasParamTimeType.equals(EpasParamTimeType.YEARLY);
  }

  public boolean isGeneral() {
    return this.epasParamTimeType.equals(EpasParamTimeType.GENERAL);
  }


  public boolean isPeriodic() {
    return this.epasParamTimeType.equals(EpasParamTimeType.PERIODIC);
  }

  public enum EpasParamTimeType {
    GENERAL, YEARLY, PERIODIC;
  }

  public enum RecomputationType {
    DAYS, RESIDUAL_HOURS, RESIDUAL_MEALTICKETS;
  }

  /**
   * Enumerato con i tipi di valori che può assumere un parametro di configurazione.
   * @author alessandro
   *
   */
  public enum EpasParamValueType {

    LOCALTIME, LOCALTIME_INTERVAL, LOCALDATE,  DAY_MONTH, MONTH,  
    EMAIL, IP_LIST, INTEGER, BOOLEAN;

    public static class LocalTimeInterval {
      public LocalTime from;
      public LocalTime to;
      
      // TODO: validation
      public LocalTimeInterval(LocalTime from, LocalTime to) {
        this.from = from;
        this.to = to;
      }
      
      @Override
      public String toString() {
        return formatValue(this);
      }
    }

    public static class IpList {
      public List<String> ipList;
      // TODO: validation
      
      public IpList(List<String> ipList) { 
        this.ipList = ipList;
      }
      
      @Override
      public String toString() {
        return formatValue(this);
      }
    }

    public static final String DAY_MONTH_SEPARATOR = "/";
    public static final String LOCALTIME_INTERVAL_SEPARATOR = "-";
    public static final String LOCALTIME_FORMATTER = "HH:mm";
    public static final String IP_LIST_SEPARATOR = ", ";

    /**
     * Converte il tipo primitivo nella formattazione string.
     * @param value l'oggetto da convertire in stringa
     * @return la stringa rappresentante il valore passato
     */
    public static String formatValue(final Object value) {
      if (value instanceof String) {
        return value.toString();
      }

      if (value instanceof Boolean) {
        return value.toString();
      }

      if (value instanceof Integer) {
        return value.toString();
      }

      if (value instanceof LocalTime) {
        return ((LocalTime)value).toString(LOCALTIME_FORMATTER);
      }

      if (value instanceof LocalDate) {
        return ((LocalDate)value).toString();
      }

      if (value instanceof LocalTimeInterval) {
        return formatValue(((LocalTimeInterval)value).from) 
            + LOCALTIME_INTERVAL_SEPARATOR 
            + formatValue(((LocalTimeInterval)value).to);  
      }

      if (value instanceof MonthDay) {
        return ((MonthDay)value).getDayOfMonth() + DAY_MONTH_SEPARATOR 
            + ((MonthDay)value).getMonthOfYear();  
      }

      if (value instanceof IpList) {
        return Joiner.on(IP_LIST_SEPARATOR).join(((IpList)value).ipList);
      }

      return null;
    }

    /**
     * Converte il valore in oggetto.
     * @param type
     * @param value
     * @return
     */
    public static Object parseValue(final EpasParamValueType type, final String value) {
      try {
        switch (type) {
          case LOCALDATE:
            return new LocalDate(value);
          case LOCALTIME:
            return LocalTime.parse(value, DateTimeFormat.forPattern(LOCALTIME_FORMATTER));
          case LOCALTIME_INTERVAL:
            LocalTimeInterval interval = new LocalTimeInterval(
                (LocalTime)parseValue(
                    LOCALTIME, value.trim().split(LOCALTIME_INTERVAL_SEPARATOR)[0]),
                (LocalTime)parseValue(
                    LOCALTIME, value.trim().split(LOCALTIME_INTERVAL_SEPARATOR)[1]));
            if (interval.to.isBefore(interval.from)) {
              return null;
            } else {
              return interval;
            }
          case DAY_MONTH:
            return new MonthDay(
                new Integer(value.split(DAY_MONTH_SEPARATOR)[1]), 
                new Integer(value.split(DAY_MONTH_SEPARATOR)[0]));
          case MONTH:
            return new Integer(value);
          case EMAIL:
            return value;
          case IP_LIST:
            return new IpList(Splitter.on(IP_LIST_SEPARATOR).splitToList(value));
          case INTEGER:
            return new Integer(value);
          case BOOLEAN:
            return new Boolean(value);
        }
      } catch (Exception ex) {
        return null;
      }
      return null;
    }
  }




}
