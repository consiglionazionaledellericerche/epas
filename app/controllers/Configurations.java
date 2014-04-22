package controllers;


import java.util.List;

import models.ConfGeneral;
import models.ConfYear;

import models.Office;
import models.enumerate.ConfigurationFields;


import org.joda.time.LocalDate;

import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, NavigationMenu.class} )
public class Configurations extends Controller{



	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void showConfGeneral(Long officeId){
		Office office = null;
		List<Office> offices = Security.getOfficeAllowed();
		if(officeId != null){
			office = Office.findById(officeId);
		}
		else{
			office = Security.getUser().person.office;
		}
		ConfGeneral initUseProgram = ConfGeneral.getConfGeneralByField(ConfigurationFields.InitUseProgram.description, office);
		ConfGeneral instituteName = ConfGeneral.getConfGeneralByField(ConfigurationFields.InstituteName.description, office);
		ConfGeneral seatCode = ConfGeneral.getConfGeneralByField(ConfigurationFields.SeatCode.description, office);
		ConfGeneral dayOfPatron = ConfGeneral.getConfGeneralByField(ConfigurationFields.DayOfPatron.description, office);
		ConfGeneral monthOfPatron = ConfGeneral.getConfGeneralByField(ConfigurationFields.MonthOfPatron.description, office);
		ConfGeneral webStampingAllowed = ConfGeneral.getConfGeneralByField(ConfigurationFields.WebStampingAllowed.description, office);
		ConfGeneral urlToPresence = ConfGeneral.find("Select c from ConfGeneral c where c.field = ? and c.office = ?", ConfigurationFields.UrlToPresence.description, office).first();

		ConfGeneral userToPresence = ConfGeneral.getConfGeneralByField(ConfigurationFields.UserToPresence.description, office);
		ConfGeneral passwordToPresence = ConfGeneral.getConfGeneralByField(ConfigurationFields.PasswordToPresence.description, office);
		ConfGeneral numberOfViewingCouple = ConfGeneral.getConfGeneralByField(ConfigurationFields.NumberOfViewingCouple.description, office);

		render(initUseProgram, instituteName, seatCode, dayOfPatron, monthOfPatron, webStampingAllowed, urlToPresence, userToPresence,
				passwordToPresence, numberOfViewingCouple, offices);

	}

	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void showConfYear(Long officeId){

		Office office = null;
		List<Office> offices = Security.getOfficeAllowed();
		if(officeId != null){
			office = Office.findById(officeId);
		}
		else{
			office = Security.getUser().person.office;
		}
		LocalDate date = new LocalDate();
		//		Office office = Security.getUser().person.office;

		//last year (non modificabile)
		ConfYear lastConfYear = ConfYear.getConfYear(new LocalDate().getYear()-1);
		Integer lastYearDayExpiryVacationPastYear = ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, date.getYear()-1, office);
		Integer lastYearMonthExpiryVacationPastYear = ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, date.getYear()-1, office);
		Integer lastYearMonthExpireRecoveryDaysOneThree = ConfYear.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays13.description, date.getYear()-1, office);
		Integer lastYearMonthExpireRecoveryDaysFourNine = ConfYear.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays49.description, date.getYear()-1, office);
		Integer lastYearMaxRecoveryDaysOneThree = ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays13.description, date.getYear()-1, office);
		Integer lastYearMaxRecoveryDaysFourNine = ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays49.description, date.getYear()-1, office);
		Integer lastYearHourMaxToCalculateWorkTime = ConfYear.getFieldValue(ConfigurationFields.HourMaxToCalculateWorkTime.description, date.getYear()-1, office);

		//current year (modificabile)
		ConfYear confYear = ConfYear.getConfYear(new LocalDate().getYear());
		ConfYear dayExpiryVacationPastYear = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.DayExpiryVacationPastYear.description, date.getYear(), office);
		ConfYear monthExpiryVacationPastYear = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpiryVacationPastYear.description, date.getYear(), office);
		ConfYear monthExpireRecoveryDaysOneThree = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpireRecoveryDays13.description, date.getYear(), office);
		ConfYear monthExpireRecoveryDaysFourNine = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpireRecoveryDays49.description, date.getYear(), office);
		ConfYear maxRecoveryDaysOneThree = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MaxRecoveryDays13.description, date.getYear(), office);
		ConfYear maxRecoveryDaysFourNine = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MaxRecoveryDays49.description, date.getYear(), office);
		ConfYear hourMaxToCalculateWorkTime = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.HourMaxToCalculateWorkTime.description, date.getYear(), office);

		Integer nextYear = new LocalDate().getYear()+1;

		render(lastYearDayExpiryVacationPastYear, lastYearMonthExpiryVacationPastYear, lastYearMonthExpireRecoveryDaysOneThree,
				lastYearMonthExpireRecoveryDaysFourNine, lastYearMaxRecoveryDaysOneThree, lastYearMaxRecoveryDaysFourNine,
				lastYearHourMaxToCalculateWorkTime, lastConfYear, dayExpiryVacationPastYear, monthExpiryVacationPastYear,
				monthExpireRecoveryDaysOneThree, monthExpireRecoveryDaysFourNine, monthExpireRecoveryDaysFourNine, maxRecoveryDaysOneThree,
				maxRecoveryDaysFourNine, hourMaxToCalculateWorkTime, confYear, nextYear, offices);

	}

	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void saveConfGeneral(String pk, String value){

		try
		{
			ConfGeneral conf = ConfGeneral.findById(Long.parseLong(pk));
			conf.fieldValue = value;
			conf.save();
			Cache.set(conf.field+conf.office.name, conf.fieldValue);
		}		
		catch(Exception e)
		{
			response.status = 500;
			renderText("Bad request");
		}

	}

	@Check(Security.INSERT_AND_UPDATE_CONFIGURATION)
	public static void saveConfYear(String pk, String value){
		Integer year = new LocalDate().getYear();

		try
		{
			ConfYear conf = ConfYear.findById(Long.parseLong(pk));

			if(conf.field.equals(ConfigurationFields.DayExpiryVacationPastYear.description)){
				Integer month = ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, year, conf.office);
				try{
					new LocalDate(year, month, Integer.parseInt(value));
				}
				catch(Exception e){
					response.status = 500;
					renderText(Integer.parseInt(value)+"/"+ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, year, Security.getUser().person.office)+"/"+year+" data non valida. Settare correttamente i parametri.");
				}
			}

			if(conf.field.equals(ConfigurationFields.MonthExpiryVacationPastYear.description)){
				Integer day  = ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, year, conf.office);
				try{
					LocalDate date = new LocalDate(year, Integer.parseInt(value), day);
				}
				catch(Exception e){
					response.status = 500;
					renderText(ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, year, Security.getUser().person.office)+"/"+Integer.parseInt(value)+"/"+year+" data non valida. Settare correttamente i parametri.");
				}
				
			}
			
			if(conf.field.equals(ConfigurationFields.MonthExpireRecoveryDays13.description)){
				if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 12){
					response.status = 500;
					renderText("Bad request");
				}
			}
			
			if(conf.field.equals(ConfigurationFields.MonthExpireRecoveryDays49.description)){
				if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 12){
					response.status = 500;
					renderText("Bad request");
				}
			}
			
			if(conf.field.equals(ConfigurationFields.MaxRecoveryDays13.description)){
				if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 31){
					response.status = 500;
					renderText("Bad request");
				}
			}
			
			if(conf.field.equals(ConfigurationFields.MaxRecoveryDays49.description)){
				if(Integer.parseInt(value) < 0 || Integer.parseInt(value) > 31){
					response.status = 500;
					renderText("Bad request");
				}
			}
			
			conf.fieldValue = Integer.parseInt(value);
			conf.save();
			Cache.set(conf.field+conf.office.name+conf.year, conf.fieldValue);
		
		}
		catch(Exception e)
		{
			response.status = 500;
			renderText("Bad request");
		}	

	}

}
