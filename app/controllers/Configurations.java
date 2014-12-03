package controllers;


import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import models.ConfGeneral;
import models.ConfYear;
import models.Office;
import models.enumerate.ConfigurationFields;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.ConfYearDao;
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
	
	
	public static void insertNewConfYear(Long id){
		Office office = null;

		if(id != null){
			office = Office.findById(id);
		}
		else{
			office = Security.getUser().get().person.office;
		}
		rules.checkIfPermitted(office);
		
		
		int year = LocalDate.now().getYear()+1;
		List<ConfYear> confList = ConfYearDao.getConfByYear(Optional.fromNullable(office), year);
		ConfYear dayExpiryVacationPastYear = new ConfYear();
		ConfYear monthExpiryVacationPastYear = new ConfYear();
		ConfYear monthExpireRecoveryDaysOneThree = new ConfYear();
		ConfYear monthExpireRecoveryDaysFourNine = new ConfYear();
		ConfYear maxRecoveryDaysOneThree = new ConfYear();
		ConfYear maxRecoveryDaysFourNine = new ConfYear();
		ConfYear hourMaxToCalculateWorkTime = new ConfYear(); 
		String message = "";
		if(confList.size() > 0){
			message="Attenzione! attualmente il database contiene già una configurazione per l'anno richiesto. Continuando si sovrascriverà tale configurazione.";
			dayExpiryVacationPastYear = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.DayExpiryVacationPastYear.description);
			monthExpiryVacationPastYear = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MonthExpiryVacationPastYear.description);
			monthExpireRecoveryDaysOneThree = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MonthExpireRecoveryDays13.description);
			monthExpireRecoveryDaysFourNine = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MonthExpireRecoveryDays49.description);
			maxRecoveryDaysOneThree = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MaxRecoveryDays13.description);
			maxRecoveryDaysFourNine = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MaxRecoveryDays49.description);
			hourMaxToCalculateWorkTime = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.HourMaxToCalculateWorkTime.description);
		}
		
		
		List<String> mesi = Lists.newArrayList();
		mesi.add("Nessuno");
		for(int i = 1; i < 13; i++){
			mesi.add(DateUtility.getName(i));
		}
		
		render(year, office, dayExpiryVacationPastYear, monthExpiryVacationPastYear, monthExpireRecoveryDaysOneThree,
				monthExpireRecoveryDaysFourNine,maxRecoveryDaysOneThree, maxRecoveryDaysFourNine, hourMaxToCalculateWorkTime, mesi, message);
	}

	public static void saveNewConfYear(String giornoMassimoFerieAnnoPrecedente, String residuiAnnoPrecedente13, String residuiAnnoPrecedente49,
			String giorniRecupero13, String giorniRecupero49, String oreTimbraturaNotturna, int year, Long id){
		
		Office office = null;

		if(id != null){
			office = Office.findById(id);
		}
		else{
			office = Security.getUser().get().person.office;
		}
		rules.checkIfPermitted(office);
		
		
		
		if(validation.hasErrors()) {
			if(request.isAjax()) error("Parametri incompleti");
			Configurations.showConfYear(office.id);
		}
		
		List<ConfYear> confList = ConfYearDao.getConfByYear(Optional.fromNullable(office), year);
		if(confList.size() > 0){
		
			for(ConfYear conf : confList){
				if(conf.field.equals(ConfigurationFields.DayExpiryVacationPastYear.description)){
					conf.fieldValue = (new Integer(DateUtility.dayMonth(giornoMassimoFerieAnnoPrecedente, Optional.<String>absent()).getDayOfMonth())).toString();
					conf.save();
				}
				if(conf.field.equals(ConfigurationFields.MonthExpiryVacationPastYear.description)){
					conf.fieldValue = (new Integer(DateUtility.dayMonth(giornoMassimoFerieAnnoPrecedente, Optional.<String>absent()).getMonthOfYear())).toString();
					conf.save();
				}
				if(conf.field.equals(ConfigurationFields.MonthExpireRecoveryDays13.description)){
					conf.fieldValue = (new Integer(DateUtility.fromStringToIntMonth(residuiAnnoPrecedente13))).toString();
					conf.save();
				}
				if(conf.field.equals(ConfigurationFields.MonthExpireRecoveryDays49.description)){
					conf.fieldValue = (new Integer(DateUtility.fromStringToIntMonth(residuiAnnoPrecedente49))).toString();
					conf.save();
				}
				if(conf.field.equals(ConfigurationFields.MaxRecoveryDays13.description)){
					conf.fieldValue = giorniRecupero13;
					conf.save();
				}
				if(conf.field.equals(ConfigurationFields.MaxRecoveryDays49.description)){
					conf.fieldValue = giorniRecupero49;
					conf.save();
				}
				if(conf.field.equals(ConfigurationFields.HourMaxToCalculateWorkTime.description)){
					conf.fieldValue = oreTimbraturaNotturna;
					conf.save();
				}
			}
			flash.success("Modificati i valori precedentemente impostati per la configurazione dell'anno %s", year);
			Configurations.showConfYear(office.id);
		}
		ConfYear giornoFerieAP = new ConfYear();
		giornoFerieAP.field = ConfigurationFields.DayExpiryVacationPastYear.description;
		giornoFerieAP.fieldValue = (new Integer(DateUtility.dayMonth(giornoMassimoFerieAnnoPrecedente, Optional.<String>absent()).getDayOfMonth())).toString(); 
		giornoFerieAP.office = Security.getUser().get().person.office;
		giornoFerieAP.year = year;
		giornoFerieAP.save();
		
		ConfYear meseFerieAP = new ConfYear();
		meseFerieAP.field = ConfigurationFields.MonthExpiryVacationPastYear.description;
		meseFerieAP.fieldValue = (new Integer(DateUtility.dayMonth(giornoMassimoFerieAnnoPrecedente, Optional.<String>absent()).getMonthOfYear())).toString();; 
		meseFerieAP.office = Security.getUser().get().person.office;
		meseFerieAP.year = year;
		meseFerieAP.save();
		
		ConfYear resAnnoPrec13month = new ConfYear();
		resAnnoPrec13month.field = ConfigurationFields.MonthExpireRecoveryDays13.description;
		resAnnoPrec13month.fieldValue = (new Integer(DateUtility.fromStringToIntMonth(residuiAnnoPrecedente13))).toString();
		resAnnoPrec13month.office = Security.getUser().get().person.office;
		resAnnoPrec13month.year = year;
		resAnnoPrec13month.save();
		
		ConfYear resAnnoPrec49month = new ConfYear();
		resAnnoPrec49month.field = ConfigurationFields.MonthExpireRecoveryDays49.description;
		resAnnoPrec49month.fieldValue = (new Integer(DateUtility.fromStringToIntMonth(residuiAnnoPrecedente49))).toString();;
		resAnnoPrec49month.office = Security.getUser().get().person.office;
		resAnnoPrec49month.year = year;
		resAnnoPrec49month.save();
		
		ConfYear recuperi13 = new ConfYear();
		recuperi13.field = ConfigurationFields.MaxRecoveryDays13.description;
		recuperi13.fieldValue = giorniRecupero13;
		recuperi13.office = Security.getUser().get().person.office;
		recuperi13.year = year;
		recuperi13.save();
		
		ConfYear recuperi49 = new ConfYear();
		recuperi49.field = ConfigurationFields.MaxRecoveryDays49.description;
		recuperi49.fieldValue = giorniRecupero49;
		recuperi49.office = Security.getUser().get().person.office;
		recuperi49.year = year;
		recuperi49.save();
		
		ConfYear oreTimbrNotturna = new ConfYear();
		oreTimbrNotturna.field = ConfigurationFields.HourMaxToCalculateWorkTime.description;
		oreTimbrNotturna.fieldValue = oreTimbraturaNotturna;
		oreTimbrNotturna.office = Security.getUser().get().person.office;
		oreTimbrNotturna.year = year;
		oreTimbrNotturna.save();
		
		flash.success("Aggiunta nuova configurazione per l'anno %s", year);
		Configurations.showConfYear(office.id);
		
	}
}
