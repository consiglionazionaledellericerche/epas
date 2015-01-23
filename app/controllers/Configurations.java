package controllers;


import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.ConfigurationsManager;
import manager.ConfigurationsManager.MessageResult;
import models.ConfGeneral;
import models.ConfYear;
import models.Office;
import models.enumerate.ConfigurationFields;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import dao.ConfGeneralDao;
import dao.ConfYearDao;
import dao.OfficeDao;
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
			office = OfficeDao.getOfficeById(officeId);			
		}
		else{
			office = Security.getUser().get().person.office;
		}
		ConfGeneral initUseProgram = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.InitUseProgram.description, office);

		ConfGeneral dayOfPatron = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.DayOfPatron.description, office);
		ConfGeneral monthOfPatron = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.MonthOfPatron.description, office);
		ConfGeneral webStampingAllowed = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.WebStampingAllowed.description, office);
		ConfGeneral urlToPresence = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.UrlToPresence.description, office);

		ConfGeneral userToPresence = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.UserToPresence.description, office);
		ConfGeneral passwordToPresence = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.PasswordToPresence.description, office);
		ConfGeneral numberOfViewingCouple = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.NumberOfViewingCouple.description, office);

		ConfGeneral dateStartMealTicket = ConfGeneralManager.getConfGeneralByField(ConfigurationFields.DateStartMealTicket.description, office);

		render(initUseProgram, dayOfPatron, monthOfPatron, webStampingAllowed, urlToPresence, userToPresence,
				passwordToPresence, numberOfViewingCouple, dateStartMealTicket, offices, office);

	}

	public static void showConfYear(Long officeId){

		Office office = null;
		List<Office> offices = Security.getOfficeAllowed();
		if(officeId != null){
			office = OfficeDao.getOfficeById(officeId);

		}
		else{
			office = Security.getUser().get().person.office;
		}
		LocalDate date = new LocalDate();
		Integer lastYearDayExpiryVacationPastYear = null;
		Integer lastYearMonthExpiryVacationPastYear = null;
		Integer lastYearMonthExpireRecoveryDaysOneThree = null;
		Integer lastYearMonthExpireRecoveryDaysFourNine = null;
		Integer lastYearMaxRecoveryDaysOneThree = null;
		Integer lastYearMaxRecoveryDaysFourNine = null;
		Integer lastYearHourMaxToCalculateWorkTime = null;
		List<ConfYear> confLastYear = ConfYearDao.getConfByYear(Optional.fromNullable(office), date.getYear()-1);

		if(confLastYear.size()==0){
			/**
			 * controllo se esiste una configurazione dell'anno passato, nel caso non ci sia ne creo una con valori piuttosto
			 * arbitrari di default di modo che non ci siano problemi nei successivi calcoli di inserimento e visualizzazione dati
			 */
			ConfigurationsManager.saveNewValueField(ConfigurationFields.DayExpiryVacationPastYear.description, "31", office, date.getYear()-1);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpiryVacationPastYear.description, "8", office, date.getYear()-1);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpireRecoveryDays13.description, "0", office, date.getYear()-1);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpireRecoveryDays49.description, "4", office, date.getYear()-1);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MaxRecoveryDays13.description, "22", office, date.getYear()-1);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MaxRecoveryDays49.description, "0", office, date.getYear()-1);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.HourMaxToCalculateWorkTime.description, "5", office, date.getYear()-1);
		}

		lastYearDayExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.DayExpiryVacationPastYear.description, date.getYear()-1, office));
		lastYearMonthExpiryVacationPastYear = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.MonthExpiryVacationPastYear.description, date.getYear()-1, office));
		lastYearMonthExpireRecoveryDaysOneThree = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays13.description, date.getYear()-1, office));
		lastYearMonthExpireRecoveryDaysFourNine = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.MonthExpireRecoveryDays49.description, date.getYear()-1, office));
		lastYearMaxRecoveryDaysOneThree = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.MaxRecoveryDays13.description, date.getYear()-1, office));
		lastYearMaxRecoveryDaysFourNine = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.MaxRecoveryDays49.description, date.getYear()-1, office));
		lastYearHourMaxToCalculateWorkTime = Integer.parseInt(ConfYearManager.getFieldValue(ConfigurationFields.HourMaxToCalculateWorkTime.description, date.getYear()-1, office));
		
		ConfYear dayExpiryVacationPastYear = null;
		ConfYear monthExpiryVacationPastYear = null;
		ConfYear monthExpireRecoveryDaysOneThree = null;
		ConfYear monthExpireRecoveryDaysFourNine = null;
		ConfYear maxRecoveryDaysOneThree = null;
		ConfYear maxRecoveryDaysFourNine = null;
		ConfYear hourMaxToCalculateWorkTime = null;
		List<ConfYear> confYear = ConfYearDao.getConfByYear(Optional.fromNullable(office), date.getYear());
		//List<ConfYear> confYear = ConfYear.find("Select c from ConfYear c where c.year = ? and c.office = ?", date.getYear(), office).fetch();
		if(confYear.size()==0){
			/**
			 * se non esiste una configurazione annuale dell'anno in corso per l'ufficio office, ne creo una di default 
			 * con valori fittizi
			 */
			ConfigurationsManager.saveNewValueField(ConfigurationFields.DayExpiryVacationPastYear.description, "1", office, date.getYear());
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpiryVacationPastYear.description, "1", office, date.getYear());
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpireRecoveryDays13.description, "1", office, date.getYear());
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpireRecoveryDays49.description, "1", office, date.getYear());
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MaxRecoveryDays13.description, "1", office, date.getYear());
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MaxRecoveryDays49.description, "1", office, date.getYear());
			ConfigurationsManager.saveNewValueField(ConfigurationFields.HourMaxToCalculateWorkTime.description, "1", office, date.getYear());
		}
		else{
			dayExpiryVacationPastYear = ConfYearManager.getConfGeneralByFieldAndYear(ConfigurationFields.DayExpiryVacationPastYear.description, date.getYear(), office);
			monthExpiryVacationPastYear = ConfYearManager.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpiryVacationPastYear.description, date.getYear(), office);
			monthExpireRecoveryDaysOneThree = ConfYearManager.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpireRecoveryDays13.description, date.getYear(), office);
			monthExpireRecoveryDaysFourNine = ConfYearManager.getConfGeneralByFieldAndYear(ConfigurationFields.MonthExpireRecoveryDays49.description, date.getYear(), office);
			maxRecoveryDaysOneThree = ConfYearManager.getConfGeneralByFieldAndYear(ConfigurationFields.MaxRecoveryDays13.description, date.getYear(), office);
			maxRecoveryDaysFourNine = ConfYearManager.getConfGeneralByFieldAndYear(ConfigurationFields.MaxRecoveryDays49.description, date.getYear(), office);
			hourMaxToCalculateWorkTime = ConfYearManager.getConfGeneralByFieldAndYear(ConfigurationFields.HourMaxToCalculateWorkTime.description, date.getYear(), office);

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

			ConfGeneral conf = ConfGeneralDao.getConfGeneralById(Long.parseLong(pk));

			rules.checkIfPermitted(conf.office);

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
		ConfYear conf = ConfYearDao.getConfYearById(Long.parseLong(pk));
		MessageResult message = ConfigurationsManager.persistConfYear(conf, year, value);
		if(message.result == false){
			response.status = 500;
			renderText(message.message);
		}


	}


	public static void insertNewConfYear(Long id){
		Office office = null;

		if(id != null){
			office = OfficeDao.getOfficeById(id);
		}
		else{
			office = Security.getUser().get().person.office;
		}
		rules.checkIfPermitted(office);		

		int year = LocalDate.now().getYear()+1;
		List<ConfYear> confList = ConfYearDao.getConfByYear(Optional.fromNullable(office), year);
		Optional<ConfYear> conf = null;
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
			conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.DayExpiryVacationPastYear.description);
			if(conf.isPresent())
				dayExpiryVacationPastYear = conf.get();
			conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MonthExpiryVacationPastYear.description);
			if(conf.isPresent())
				monthExpiryVacationPastYear = conf.get();
			conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MonthExpireRecoveryDays13.description);
			if(conf.isPresent())
				monthExpireRecoveryDaysOneThree = conf.get(); 
			conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MonthExpireRecoveryDays49.description);
			if(conf.isPresent())
				monthExpireRecoveryDaysFourNine = conf.get();
			conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MaxRecoveryDays13.description);
			if(conf.isPresent())
				maxRecoveryDaysOneThree = conf.get(); 
			conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.MaxRecoveryDays49.description);
			if(conf.isPresent())
				maxRecoveryDaysFourNine = conf.get(); 
			conf = ConfYearDao.getConfYearField(Optional.fromNullable(office), year, ConfigurationFields.HourMaxToCalculateWorkTime.description);
			if(conf.isPresent())
				hourMaxToCalculateWorkTime = conf.get(); 
		}		

		List<String> mesi = ConfigurationsManager.populateMonths();

		render(year, office, dayExpiryVacationPastYear, monthExpiryVacationPastYear, monthExpireRecoveryDaysOneThree,
				monthExpireRecoveryDaysFourNine,maxRecoveryDaysOneThree, maxRecoveryDaysFourNine, hourMaxToCalculateWorkTime, mesi, message);
	}

	public static void saveNewConfYear(String giornoMassimoFerieAnnoPrecedente, String residuiAnnoPrecedente13, String residuiAnnoPrecedente49,
			String giorniRecupero13, String giorniRecupero49, String oreTimbraturaNotturna, int year, Long id){

		Office office = null;

		if(id != null){
			office = OfficeDao.getOfficeById(id);			
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
			ConfigurationsManager.saveConfigurationNextYear(confList, giornoMassimoFerieAnnoPrecedente, 
					residuiAnnoPrecedente13, residuiAnnoPrecedente49, giorniRecupero13, giorniRecupero49, 
					oreTimbraturaNotturna, year, office);

			flash.success("Modificati i valori precedentemente impostati per la configurazione dell'anno %s", year);
			Configurations.showConfYear(office.id);
		}
		else{
			ConfigurationsManager.saveNewValueField(ConfigurationFields.DayExpiryVacationPastYear.description, new Integer(DateUtility.dayMonth(giornoMassimoFerieAnnoPrecedente, Optional.<String>absent()).getDayOfMonth()).toString(), office, year);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpiryVacationPastYear.description, new Integer(DateUtility.dayMonth(giornoMassimoFerieAnnoPrecedente, Optional.<String>absent()).getMonthOfYear()).toString(), office, year);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpireRecoveryDays13.description, (new Integer(DateUtility.fromStringToIntMonth(residuiAnnoPrecedente13))).toString(), office, year);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MonthExpireRecoveryDays49.description, (new Integer(DateUtility.fromStringToIntMonth(residuiAnnoPrecedente49))).toString(), office, year);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MaxRecoveryDays13.description, giorniRecupero13, office, year);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.MaxRecoveryDays49.description, giorniRecupero49, office, year);
			ConfigurationsManager.saveNewValueField(ConfigurationFields.HourMaxToCalculateWorkTime.description, oreTimbraturaNotturna, office, year);


			flash.success("Aggiunta nuova configurazione per l'anno %s", year);
			Configurations.showConfYear(office.id);
		}

	}
}
