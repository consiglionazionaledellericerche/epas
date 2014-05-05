package models.enumerate;

/**
 * 
 * @author dario
 * enumerato per la definizione dei campi delle tabelle di configurazione
 */
public enum ConfigurationFields {
	InitUseProgram("init_use_program"),
	InstituteName("institute_name"),
	EmailToContact("email_to_contact"),
	SeatCode("seat_code"),
	UrlToPresence("url_to_presence"),
	UserToPresence("user_to_presence"),
	PasswordToPresence("password_to_presence"),
	NumberOfViewingCouple("number_of_viewing_couple"),
	MonthOfPatron("month_of_patron"),
	DayOfPatron("day_of_patron"),
	WebStampingAllowed("web_stamping_allowed"),
	MealTimeStartHour("meal_time_start_hour"),
	MealTimeStartMinute("meal_time_start_minute"),
	MealTimeEndHour("meal_time_end_hour"),
	MealTimeEndMinute("meal_time_end_minute"),
	MonthExpiryVacationPastYear("month_expiry_vacation_past_year"),
	DayExpiryVacationPastYear("day_expiry_vacation_past_year"),
	MonthExpireRecoveryDays13("month_expire_recovery_days_13"),
	MonthExpireRecoveryDays49("month_expire_recovery_days_49"),
	MaxRecoveryDays13("max_recovery_days_13"),
	MaxRecoveryDays49("max_recovery_days_49"),
	HourMaxToCalculateWorkTime("hour_max_to_calculate_worktime");
	
	public String description;
	
	private ConfigurationFields(String description){
		this.description = description;
	}
	
	public static ConfigurationFields getByDescription(String description){
		if(description.equals("init_use_program"))
			return ConfigurationFields.InitUseProgram;
		if(description.equals("institute_name"))
			return ConfigurationFields.InstituteName;
		if(description.equals("email_to_contact"))
			return ConfigurationFields.EmailToContact;
		if(description.equals("seat_code"))
			return ConfigurationFields.SeatCode;
		if(description.equals("url_to_presence"))
			return ConfigurationFields.UrlToPresence;
		if(description.equals("user_to_presence"))
			return ConfigurationFields.UserToPresence;
		if(description.equals("password_to-presence"))
			return ConfigurationFields.PasswordToPresence;
		if(description.equals("number_of_viewing_couple"))
			return ConfigurationFields.NumberOfViewingCouple;
		if(description.equals("month_of_patron"))
			return ConfigurationFields.MonthOfPatron;
		if(description.equals("day_of_patron"))
			return ConfigurationFields.DayOfPatron;
		if(description.equals("web_stamping_allowed"))
			return ConfigurationFields.WebStampingAllowed;
		if(description.equals("meal_time_start_hour"))
			return ConfigurationFields.MealTimeStartHour;
		if(description.equals("meal_time_start_minute"))
			return ConfigurationFields.MealTimeStartMinute;
		if(description.equals("meal_time_end_hour"))
			return ConfigurationFields.MealTimeEndHour;
		if(description.equals("meal_time_end_minute"))
			return ConfigurationFields.MealTimeEndMinute;
		if(description.equals("month_expiry_vacation_past_year"))
			return ConfigurationFields.MonthExpiryVacationPastYear;
		if(description.equals("day_expiry_vacation_past_year"))
			return ConfigurationFields.DayExpiryVacationPastYear;
		if(description.equals("month_expire_recovery_days_13"))
			return ConfigurationFields.MonthExpireRecoveryDays13;
		if(description.equals("month_expire_recovery_days_49"))
			return ConfigurationFields.MonthExpireRecoveryDays49;
		if(description.equals("max_recovery_days_13"))
			return ConfigurationFields.MaxRecoveryDays13;
		if(description.equals("max_recovery_days_49"))
			return ConfigurationFields.MaxRecoveryDays49;
		if(description.equals("hour_max_to_calculate_worktime"))
			return ConfigurationFields.HourMaxToCalculateWorkTime;
		return null;
	}
}
