package controllers;


import java.util.List;

import javax.inject.Inject;

import models.ConfGeneral;
import models.ConfYear;
import models.Office;
import models.enumerate.ConfigurationFields;

import org.joda.time.LocalDate;

import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

@With( {Resecure.class, RequestInit.class} )
public class Configurations extends Controller{

	@Inject
	static SecurityRules rules;

	public static void showConfGeneral(Long officeId){
		Office office = null;
		List<Office> offices = Security.getOfficeAllowed();
		if(officeId != null){
			office = Office.findById(officeId);
		}
		else{
			office = Security.getUser().get().person.office;
		}
		ConfGeneral initUseProgram = ConfGeneral.getConfGeneralByField(ConfigurationFields.InitUseProgram.description, office);

		ConfGeneral dayOfPatron = ConfGeneral.getConfGeneralByField(ConfigurationFields.DayOfPatron.description, office);
		ConfGeneral monthOfPatron = ConfGeneral.getConfGeneralByField(ConfigurationFields.MonthOfPatron.description, office);
		ConfGeneral webStampingAllowed = ConfGeneral.getConfGeneralByField(ConfigurationFields.WebStampingAllowed.description, office);
		ConfGeneral urlToPresence = ConfGeneral.getConfGeneralByField(ConfigurationFields.UrlToPresence.description, office);

		ConfGeneral userToPresence = ConfGeneral.getConfGeneralByField(ConfigurationFields.UserToPresence.description, office);
		ConfGeneral passwordToPresence = ConfGeneral.getConfGeneralByField(ConfigurationFields.PasswordToPresence.description, office);
		ConfGeneral numberOfViewingCouple = ConfGeneral.getConfGeneralByField(ConfigurationFields.NumberOfViewingCouple.description, office);

		ConfGeneral dateStartMealTicket = ConfGeneral.getConfGeneralByField(ConfigurationFields.DateStartMealTicket.description, office);
		
		render(initUseProgram, dayOfPatron, monthOfPatron, webStampingAllowed, urlToPresence, userToPresence,
				passwordToPresence, numberOfViewingCouple, dateStartMealTicket, offices, office);

	}

	public static void showConfYear(Long officeId){

		Office office = null;
		List<Office> offices = Security.getOfficeAllowed();
		if(officeId != null){
			office = Office.findById(officeId);
		}
		else{
			office = Security.getUser().get().person.office;
		}
		LocalDate date = new LocalDate();
		//		Office office = Security.getUser().person.office;

		//last year (non modificabile)
		//ConfYear lastConfYear = ConfYear.getConfYear(new LocalDate().getYear()-1);
		Integer lastYearDayExpiryVacationPastYear = null;
		Integer lastYearMonthExpiryVacationPastYear = null;
		Integer lastYearMonthExpireRecoveryDaysOneThree = null;
		Integer lastYearMonthExpireRecoveryDaysFourNine = null;
		Integer lastYearMaxRecoveryDaysOneThree = null;
		Integer lastYearMaxRecoveryDaysFourNine = null;
		Integer lastYearHourMaxToCalculateWorkTime = null;
		List<ConfYear> confLastYear = ConfYear.find("Select c from ConfYear c where c.year = ? and c.office = ?", date.getYear()-1, office).fetch();
		if(confLastYear.size()==0){
			/**
			 * controllo se esiste una configurazione dell'anno passato, nel caso non ci sia ne creo una con valori piuttosto
			 * arbitrari di default di modo che non ci siano problemi nei successivi calcoli di inserimento e visualizzazione dati
			 */
			ConfYear dayExpiryVacationPastYear = new ConfYear();
			dayExpiryVacationPastYear.field = "day_expiry_vacation_past_year";
			dayExpiryVacationPastYear.fieldValue = "31";
			dayExpiryVacationPastYear.office = office;
			dayExpiryVacationPastYear.year = date.getYear()-1;
			dayExpiryVacationPastYear.save();
			
			ConfYear monthExpiryVacationPastYear = new ConfYear();
			monthExpiryVacationPastYear.field ="month_expiry_vacation_past_year";
			monthExpiryVacationPastYear.fieldValue ="8";
			monthExpiryVacationPastYear.office = office;
			monthExpiryVacationPastYear.year = date.getYear()-1;
			monthExpiryVacationPastYear.save();
			
			ConfYear monthRecoveryDaysOneThree = new ConfYear();
			monthRecoveryDaysOneThree.field="month_expire_recovery_days_13";
			monthRecoveryDaysOneThree.fieldValue="0";
			monthRecoveryDaysOneThree.office = office;
			monthRecoveryDaysOneThree.year = date.getYear()-1;
			monthRecoveryDaysOneThree.save();			
			
			ConfYear monthExpireRecoveryDayFourNine = new ConfYear();
			monthExpireRecoveryDayFourNine.field="month_expire_recovery_days_49";
			monthExpireRecoveryDayFourNine.fieldValue="4";
			monthExpireRecoveryDayFourNine.office=office;
			monthExpireRecoveryDayFourNine.year=date.getYear()-1;
			monthExpireRecoveryDayFourNine.save();
			
			ConfYear maxRecoveryDaysOneThree = new ConfYear();
			maxRecoveryDaysOneThree.field="max_recovery_days_13";
			maxRecoveryDaysOneThree.fieldValue="22";
			maxRecoveryDaysOneThree.office=office;
			maxRecoveryDaysOneThree.year=date.getYear()-1;
			maxRecoveryDaysOneThree.save();
			
			ConfYear maxRecoveryDaysFourNine = new ConfYear();
			maxRecoveryDaysFourNine.field="max_recovery_days_49";
			maxRecoveryDaysFourNine.fieldValue="0";
			maxRecoveryDaysFourNine.office=office;
			maxRecoveryDaysFourNine.year=date.getYear()-1;
			maxRecoveryDaysFourNine.save();
			
			ConfYear hourMax = new ConfYear();
			hourMax.field="hour_max_to_calculate_worktime";
			hourMax.fieldValue="5";
			hourMax.office=office;
			hourMax.year=date.getYear()-1;
			hourMax.save();

		}

		lastYearDayExpiryVacationPastYear = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, date.getYear()-1, office));
		lastYearMonthExpiryVacationPastYear = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, date.getYear()-1, office));
		lastYearMonthExpireRecoveryDaysOneThree = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays13.description, date.getYear()-1, office));
		lastYearMonthExpireRecoveryDaysFourNine = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays49.description, date.getYear()-1, office));
		lastYearMaxRecoveryDaysOneThree = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays13.description, date.getYear()-1, office));
		lastYearMaxRecoveryDaysFourNine = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.MaxRecoveryDays49.description, date.getYear()-1, office));
		lastYearHourMaxToCalculateWorkTime = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.HourMaxToCalculateWorkTime.description, date.getYear()-1, office));



		//current year (modificabile)
		//ConfYear confYear = ConfYear.getConfYear(new LocalDate().getYear());
		ConfYear dayExpiryVacationPastYear = null;
		ConfYear monthExpiryVacationPastYear = null;
		ConfYear monthExpireRecoveryDaysOneThree = null;
		ConfYear monthExpireRecoveryDaysFourNine = null;
		ConfYear maxRecoveryDaysOneThree = null;
		ConfYear maxRecoveryDaysFourNine = null;
		ConfYear hourMaxToCalculateWorkTime = null;
		List<ConfYear> confYear = ConfYear.find("Select c from ConfYear c where c.year = ? and c.office = ?", date.getYear(), office).fetch();
		if(confYear.size()==0){
			/**
			 * se non esiste una configurazione annuale dell'anno in corso per l'ufficio office, ne creo una di default 
			 * con valori fittizi
			 */
			dayExpiryVacationPastYear = new ConfYear();
			dayExpiryVacationPastYear.office = office;
			dayExpiryVacationPastYear.field = "day_expiry_vacation_past_year";
			dayExpiryVacationPastYear.fieldValue = "1";
			dayExpiryVacationPastYear.year = date.getYear();
			dayExpiryVacationPastYear.save();

			monthExpiryVacationPastYear = new ConfYear();
			monthExpiryVacationPastYear.office = office;
			monthExpiryVacationPastYear.field = "month_expiry_vacation_past_year";
			monthExpiryVacationPastYear.fieldValue = "1";
			monthExpiryVacationPastYear.year = date.getYear();
			monthExpiryVacationPastYear.save();

			monthExpireRecoveryDaysOneThree = new ConfYear();
			monthExpireRecoveryDaysOneThree.office = office;
			monthExpireRecoveryDaysOneThree.field = "month_expire_recovery_days_13";
			monthExpireRecoveryDaysOneThree.fieldValue = "1";
			monthExpireRecoveryDaysOneThree.year = date.getYear();
			monthExpireRecoveryDaysOneThree.save();

			monthExpireRecoveryDaysFourNine = new ConfYear();
			monthExpireRecoveryDaysFourNine.office = office;
			monthExpireRecoveryDaysFourNine.field = "month_expire_recovery_days_49";
			monthExpireRecoveryDaysFourNine.fieldValue = "1";
			monthExpireRecoveryDaysFourNine.year = date.getYear();
			monthExpireRecoveryDaysFourNine.save();

			maxRecoveryDaysOneThree = new ConfYear();
			maxRecoveryDaysOneThree.office = office;
			maxRecoveryDaysOneThree.field = "max_recovery_days_13";
			maxRecoveryDaysOneThree.fieldValue = "1";
			maxRecoveryDaysOneThree.year = date.getYear();
			maxRecoveryDaysOneThree.save();

			maxRecoveryDaysFourNine = new ConfYear();
			maxRecoveryDaysFourNine.office = office;
			maxRecoveryDaysFourNine.field = "max_recovery_days_49";
			maxRecoveryDaysFourNine.fieldValue = "1";
			maxRecoveryDaysFourNine.year = date.getYear();
			maxRecoveryDaysFourNine.save();

			hourMaxToCalculateWorkTime = new ConfYear();
			hourMaxToCalculateWorkTime.office = office;
			hourMaxToCalculateWorkTime.field = "hour_max_to_calculate_worktime";
			hourMaxToCalculateWorkTime.fieldValue = "1";
			hourMaxToCalculateWorkTime.year = date.getYear();
			hourMaxToCalculateWorkTime.save();
		}
		else{
			dayExpiryVacationPastYear = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.DayExpiryVacationPastYear.description, date.getYear(), office);
			monthExpiryVacationPastYear = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpiryVacationPastYear.description, date.getYear(), office);
			monthExpireRecoveryDaysOneThree = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpireRecoveryDays13.description, date.getYear(), office);
			monthExpireRecoveryDaysFourNine = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpireRecoveryDays49.description, date.getYear(), office);
			maxRecoveryDaysOneThree = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MaxRecoveryDays13.description, date.getYear(), office);
			maxRecoveryDaysFourNine = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.MaxRecoveryDays49.description, date.getYear(), office);
			hourMaxToCalculateWorkTime = ConfYear.getConfGeneralByFieldAndYear(ConfigurationFields.HourMaxToCalculateWorkTime.description, date.getYear(), office);

		}

		Integer year = new LocalDate().getYear();
		Integer nextYear = new LocalDate().getYear()+1;

		render(lastYearDayExpiryVacationPastYear, lastYearMonthExpiryVacationPastYear, lastYearMonthExpireRecoveryDaysOneThree,
				lastYearMonthExpireRecoveryDaysFourNine, lastYearMaxRecoveryDaysOneThree, lastYearMaxRecoveryDaysFourNine,
				lastYearHourMaxToCalculateWorkTime, dayExpiryVacationPastYear, monthExpiryVacationPastYear,
				monthExpireRecoveryDaysOneThree, monthExpireRecoveryDaysFourNine, monthExpireRecoveryDaysFourNine, maxRecoveryDaysOneThree,
				maxRecoveryDaysFourNine, hourMaxToCalculateWorkTime, nextYear, offices, office, year);

	}


	public static void saveConfGeneral(String pk, String value){

		try  {
			ConfGeneral conf = ConfGeneral.findById(Long.parseLong(pk));
			
			rules.checkIfPermitted(conf.office);

			/*
			if(conf.field.equals(ConfigurationFields.DateStartMealTicket.description)){
				
				if(value.equals("")){
					value = null;
				} else {
					//Controllo validità parametro
					new LocalDate(value);
				}
			}
			*/
			
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

	
	public static void saveConfYear(String pk, String value){
		Integer year = new LocalDate().getYear();

		try
		{
			ConfYear conf = ConfYear.findById(Long.parseLong(pk));

			if(conf.field.equals(ConfigurationFields.DayExpiryVacationPastYear.description)){
				Integer month = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, year, conf.office));
				try{
					new LocalDate(year, month, Integer.parseInt(value));
				}
				catch(Exception e){
					response.status = 500;
					renderText(Integer.parseInt(value)+"/"+ConfYear.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, year, Security.getUser().get().person.office)+"/"+year+" data non valida. Settare correttamente i parametri.");
				}
			}

			if(conf.field.equals(ConfigurationFields.MonthExpiryVacationPastYear.description)){
				Integer day  = Integer.parseInt(ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, year, conf.office));
				try{
					new LocalDate(year, Integer.parseInt(value), day);
				}
				catch(Exception e){
					response.status = 500;
					renderText(ConfYear.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, year, Security.getUser().get().person.office)+"/"+Integer.parseInt(value)+"/"+year+" data non valida. Settare correttamente i parametri.");
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

			conf.fieldValue = value;
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
