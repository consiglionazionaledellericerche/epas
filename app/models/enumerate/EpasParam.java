package models.enumerate;

import com.google.common.base.Joiner;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.testng.collections.Lists;

import java.util.List;

public enum EpasParam {
  
  //#######################################
  // GENERAL PARAMS
  
  DAY_OF_PATRON("dayOfPatron", 
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.DAY_MONTH,
      Lists.newArrayList(RecomputationType.DAYS),
      convertDayMonth(1,1)),
  
  WEB_STAMPING_ALLOWED("web_stamping_allowed",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      Lists.newArrayList(RecomputationType.NONE),
      convertValue(false)),
  
  ADDRESSES_ALLOWED("addresses_allowed",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.IP_LIST,
      Lists.newArrayList(RecomputationType.NONE),
      convertIpList(Lists.<String>newArrayList())),
  
  NUMBER_OF_VIEWING_COUPLE("number_of_viewing_couple",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.INTEGER,
      Lists.newArrayList(RecomputationType.NONE),
      convertValue(2)),
  
  DATE_START_MEAL_TICKET("date_start_meal_ticket",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.LOCALDATE,
      Lists.newArrayList(RecomputationType.RESIDUAL_MEALTICKETS),
      convertValue(new LocalDate(2014, 7, 1))),
  
  SEND_EMAIL("send_email",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      Lists.newArrayList(RecomputationType.NONE),
      convertValue(false)),
  
  /**
   * Viene utilizzato per popolare il campo replyTo delle mail inviate dal sistema. 
   */
  EMAIL_TO_CONTACT("email_to_contact",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.EMAIL,
      Lists.newArrayList(RecomputationType.NONE),
      convertValue("")),
  
  //#######################################
  // YEARLY PARAMS
  
  EXPIRY_VACATION_PAST_YEAR("expiry_vacation_past_year",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.DAY_MONTH,
      Lists.newArrayList(RecomputationType.NONE),
      convertDayMonth(8,31)),

  MONTH_EXPIRY_RECOVERY_DAYS_13("month_expire_recovery_days_13",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.MONTH,
      Lists.newArrayList(RecomputationType.RESIDUAL_HOURS),
      convertValue(0)),
  
  MONTH_EXPIRY_RECOVERY_DAYS_49("month_expire_recovery_days_49",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.MONTH,
      Lists.newArrayList(RecomputationType.RESIDUAL_HOURS),
      convertValue(3)),

  MAX_RECOVERY_DAYS_13("max_recovery_days_13",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.INTEGER,
      Lists.newArrayList(RecomputationType.NONE),
      convertValue(22)),
  
  MAX_RECOVERY_DAYS_49("max_recovery_days_49",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.INTEGER,
      Lists.newArrayList(RecomputationType.NONE),
      convertValue(0)),

  //#######################################
  // PERIODIC PARAMS
  
  HOUR_MAX_TO_CALCULATE_WORKTIME("hour_max_to_calculate_worktime",
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME,
      Lists.newArrayList(RecomputationType.DAYS),
      convertValue(new LocalTime(5, 0))),
  
  LUNCH_INTERVAL("lunch_interval",
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.LOCALTIME_INTERVAL,
      Lists.newArrayList(RecomputationType.DAYS),
      convertLocalTimeInterval(new LocalTime(12,0), new LocalTime(15,0)));

  public final String name;
  public final EpasParamTimeType epasParamTimeType;
  public final EpasParamValueType epasParamValueType;
  public final List<RecomputationType> recomputationTypes;
  public final Object defaultValue;
  
  EpasParam(String name, EpasParamTimeType epasParamTimeType, EpasParamValueType epasParamValueType,
      List<RecomputationType> recomputationTypes, Object defaultValue) {
    this.name = name;
    this.epasParamTimeType = epasParamTimeType;
    this.epasParamValueType = epasParamValueType;
    this.recomputationTypes = recomputationTypes;
    this.defaultValue = defaultValue;
  }

  public enum EpasParamTimeType {
    GENERAL, YEARLY, PERIODIC;
  }
  
  public enum RecomputationType {
    DAYS, RESIDUAL_HOURS, RESIDUAL_MEALTICKETS, NONE;
  }
  
  public enum EpasParamValueType {
    LOCALTIME, LOCALTIME_INTERVAL, LOCALDATE,  DAY_MONTH, MONTH,  
    EMAIL, IP_LIST, INTEGER, BOOLEAN;
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
  
  // ##############################################################
  // Parte static
  
  /**
   * Converte il tipo primitivo nella formattazione string.
   * @param valueType
   * @param value
   * @return
   */
  public static String convertValue(Object value) {
    if (value instanceof String) {
      return (String)value;
    }

    if (value instanceof Boolean) {
      return "" + value;
    }
    
    if (value instanceof Integer) {
      return "" + value;
    }
    
    if (value instanceof LocalTime) {
      return ((LocalTime)value).getHourOfDay() + ":" + ((LocalTime)value).getMinuteOfHour();
    }
    
    if (value instanceof LocalDate) {
      return ((LocalDate)value).toString();
    }
    
    return null;
  }
  
  public static String convertLocalTimeInterval(LocalTime from, LocalTime to) {
    return from.getHourOfDay() + ":" + from.getMinuteOfHour() + "-" 
        + to.getHourOfDay() + ":" + to.getMinuteOfHour();
  }
  
  public static String convertDayMonth(int day, int month) {
    return day + "-" + month;
  }
  
  public static String convertIpList(List<String> ipList) {
    return Joiner.on("-").join(ipList);
  }
  
}
