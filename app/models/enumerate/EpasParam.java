package models.enumerate;

import org.testng.collections.Lists;

import java.util.List;

public enum EpasParam {
  
  //#######################################
  // GENERAL PARAMS
  
  DAY_OF_PATRON("dayOfPatron", 
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.DAY_MONTH,
      Lists.newArrayList(RecomputationType.DAYS),
      "1-1"),
  
  WEB_STAMPING_ALLOWED("web_stamping_allowed",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      Lists.newArrayList(RecomputationType.NONE),
      (Object)new Boolean(false)),
  
  ADDRESSES_ALLOWED("",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.IP_LIST,
      Lists.newArrayList(RecomputationType.NONE),
      ""),
  
  NUMBER_OF_VIEWING_COUPLE("number_of_viewing_couple",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.INTEGER,
      Lists.newArrayList(RecomputationType.NONE),
      new Integer(2)),
  
  DATE_START_MEAL_TICKET("date_start_meal_ticket",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.DATE,
      Lists.newArrayList(RecomputationType.RESIDUAL_MEALTICKETS),
      null),
  
  SEND_EMAIL("send_email",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.BOOLEAN,
      Lists.newArrayList(RecomputationType.NONE),
      new Boolean(false)),
  
  /**
   * Viene utilizzato per popolare il campo replyTo delle mail inviate dal sistema. 
   */
  EMAIL_TO_CONTACT("email_to_contact",
      EpasParamTimeType.GENERAL,
      EpasParamValueType.EMAIL,
      Lists.newArrayList(RecomputationType.NONE),
      ""),
  
  //#######################################
  // YEARLY PARAMS
  
  EXPIRY_VACATION_PAST_YEAR("expiry_vacation_past_year",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.DAY_MONTH,
      Lists.newArrayList(RecomputationType.NONE),
      "8-31"),

  MONTH_EXPIRY_RECOVERY_DAYS_13("month_expire_recovery_days_13",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.MONTH,
      Lists.newArrayList(RecomputationType.RESIDUAL_HOURS),
      "0"),
  
  MONTH_EXPIRY_RECOVERY_DAYS_49("month_expire_recovery_days_13",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.MONTH,
      Lists.newArrayList(RecomputationType.RESIDUAL_HOURS),
      "3"),

  MAX_RECOVERY_DAYS_13("max_recovery_days_13",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.INTEGER,
      Lists.newArrayList(RecomputationType.NONE),
      "22"),
  
  MAX_RECOVERY_DAYS_49("max_recovery_days_49",
      EpasParamTimeType.YEARLY,
      EpasParamValueType.INTEGER,
      Lists.newArrayList(RecomputationType.NONE),
      "0"),

  //#######################################
  // PERIODIC PARAMS
  
  HOUR_MAX_TO_CALCULATE_WORKTIME("hour_max_to_calculate_worktime",
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.HOUR_MINUTE,
      Lists.newArrayList(RecomputationType.DAYS),
      "5:00"),
  
  LUNCH_INTERVAL("lunch_interval",
      EpasParamTimeType.PERIODIC,
      EpasParamValueType.HOUR_MINUTE_INTERVAL,
      Lists.newArrayList(RecomputationType.DAYS),
      "12:00-15:00");

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
    DAY_MONTH, DATE, MONTH, HOUR_MINUTE, HOUR_MINUTE_INTERVAL, 
    EMAIL, IP_LIST, INTEGER, BOOLEAN;
  }

}
