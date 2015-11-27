package models.enumerate;

import org.joda.time.LocalDate;

public enum Parameter {

	/* GENERAL PARAMETER */

  INIT_USE_PROGRAM("general", "init_use_program", LocalDate.now().toString()),

  MONTH_OF_PATRON("general", "month_of_patron", "1"),
  DAY_OF_PATRON("general", "day_of_patron", "1"),

  WEB_STAMPING_ALLOWED("general", "web_stamping_allowed", "false"),
  ADDRESSES_ALLOWED("general", "addresses_allowed", ""),

  NUMBER_OF_VIEWING_COUPLE("general", "number_of_viewing_couple", "2"),

  USER_TO_PRESENCE("general", "user_to_presence", ""),
  PASSWORD_TO_PRESENCE("general", "password_to_presence", ""),
  URL_TO_PRESENCE("general", "url_to_presence", "https://attestati.rm.cnr.it/attestati/"),

  DATE_START_MEAL_TICKET("general", "date_start_meal_ticket", ""),

  EMAIL_TO_CONTACT("general", "email_to_contact", ""),
//	EMAIL_TO_CONTACT viene utilizzato per popolare il campo replyTo delle
//	mail inviate dal sistema in base all'ufficio del destinatario

  SEND_EMAIL("general", "send_email", "false"),            // attiva/disattiva l'invio delle mail dai job

  MEAL_TIME_START_HOUR("general", "meal_time_start_hour", "1"),
  MEAL_TIME_START_MINUTE("general", "meal_time_start_minute", "0"),
  MEAL_TIME_END_HOUR("general", "meal_time_end_hour", "23"),
  MEAL_TIME_END_MINUTE("general", "meal_time_end_minute", "0"),
	
	/* YEARLY PARAMETER */

  MONTH_EXPIRY_VACATION_PAST_YEAR("yearly", "month_expiry_vacation_past_year", "8"),
  DAY_EXPIRY_VACATION_PAST_YEAR("yearly", "day_expiry_vacation_past_year", "31"),
  MONTH_EXPIRY_RECOVERY_DAYS_13("yearly", "month_expire_recovery_days_13", "0"),
  MONTH_EXPIRY_RECOVERY_DAYS_49("yearly", "month_expire_recovery_days_49", "4"),
  MAX_RECOVERY_DAYS_13("yearly", "max_recovery_days_13", "22"),
  MAX_RECOVERY_DAYS_49("yearly", "max_recovery_days_49", "0"),
  HOUR_MAX_TO_CALCULATE_WORKTIME("yearly", "hour_max_to_calculate_worktime", "5");

  public final String description;
  public final String type;
  private final String defaultValue;
  Parameter(String type, String description,
            String defaultValue) {
    this.description = description;
    this.type = type;
    this.defaultValue = defaultValue;
  }

  public static Parameter getByDescription(String description) {
    for (Parameter param : values()) {
      if (param.description.equalsIgnoreCase(description)) {
        return param;
      }
    }
    return null;
  }

  public boolean isGeneral() {
    return this.type.equals("general");
  }

  public boolean isYearly() {
    return this.type.equals("yearly");
  }

  public String getDefaultValue() {

    //mettere qui i valori del file configurazioni quando ci sarà...

    return this.defaultValue;
  }

}
