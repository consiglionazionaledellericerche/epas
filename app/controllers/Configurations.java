package controllers;


import java.util.Set;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.ConfYearManager;
import manager.ConfigurationsManager;
import manager.ConfigurationsManager.MessageResult;
import models.ConfGeneral;
import models.ConfYear;
import models.Office;
import models.enumerate.Parameter;

import org.joda.time.LocalDate;

import play.cache.Cache;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Preconditions;

import dao.ConfGeneralDao;
import dao.ConfYearDao;
import dao.OfficeDao;

@With( {Resecure.class, RequestInit.class} )
public class Configurations extends Controller{

	@Inject
	static SecurityRules rules;
	
	@Inject
	static OfficeDao officeDao;

	/**
	 * Visualizza la pagina di configurazione generale dell'office.
	 * 
	 * @param officeId
	 */
	public static void showConfGeneral(Long officeId) {
		
		Office office = null;

		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());
		if(officeId != null) {
			office = officeDao.getOfficeById(officeId);			
		}
		else{
			office = Security.getUser().get().person.office;
		}

		ConfGeneral initUseProgram = ConfGeneralManager.getByField(Parameter.INIT_USE_PROGRAM,  office);

		ConfGeneral dayOfPatron = ConfGeneralManager.getByField(Parameter.DAY_OF_PATRON, office);
		ConfGeneral monthOfPatron = ConfGeneralManager.getByField(Parameter.MONTH_OF_PATRON, office);
		
		ConfGeneral webStampingAllowed = ConfGeneralManager.getByField(Parameter.WEB_STAMPING_ALLOWED, office);
		ConfGeneral addressesAllowed = ConfGeneralManager.getByField(Parameter.ADDRESSES_ALLOWED, office);
		
		ConfGeneral urlToPresence = ConfGeneralManager.getByField(Parameter.URL_TO_PRESENCE, office);
		ConfGeneral userToPresence = ConfGeneralManager.getByField(Parameter.USER_TO_PRESENCE, office);
		ConfGeneral passwordToPresence = ConfGeneralManager.getByField(Parameter.PASSWORD_TO_PRESENCE, office);
		
		ConfGeneral numberOfViewingCouple = ConfGeneralManager.getByField(Parameter.NUMBER_OF_VIEWING_COUPLE, office);

		ConfGeneral dateStartMealTicket = ConfGeneralManager.getByField(Parameter.DATE_START_MEAL_TICKET, office);
		ConfGeneral sendEmail = ConfGeneralManager.getByField(Parameter.SEND_EMAIL, office);

		render(initUseProgram, dayOfPatron, monthOfPatron, webStampingAllowed, addressesAllowed, urlToPresence, userToPresence,
				passwordToPresence, numberOfViewingCouple, dateStartMealTicket,sendEmail, offices, office);

	}
	
	/**
	 * Salva il nuovo valore per il field name. (Chiamata via ajax tramite X-editable)
	 * 
	 * @param pk
	 * @param name
	 * @param value
	 */
	public static void saveConfGeneral(Long pk, String name, String value){
		
		Office office = officeDao.getOfficeById(pk);
		
		rules.checkIfPermitted(office);
		
		ConfGeneral conf =  ConfGeneralDao.getByFieldName(name, office)
								.or(new ConfGeneral(office, name, value));
		
		conf.fieldValue = value;
		conf.save();
		
		Cache.set(conf.field+conf.office.name, conf.fieldValue);
	}

	/**
	 * Visualizza la pagina di configurazione annuale dell'office.
	 * 
	 * @param officeId
	 */
	public static void showConfYear(Long officeId){

		Office office = null;
		Set<Office> offices = officeDao.getOfficeAllowed(Security.getUser().get());
		if(officeId != null){
			office = officeDao.getOfficeById(officeId);
		}
		else{
			office = Security.getUser().get().person.office;
		}
		
		Integer currentYear = LocalDate.now().getYear();
		Integer previousYear = currentYear - 1;

		//Parametri configurazione anno passato
		ConfYear lastYearDayExpiryVacationPastYear = ConfYearManager.getByField(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, previousYear);
		ConfYear lastYearMonthExpiryVacationPastYear = ConfYearManager.getByField(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, previousYear);
		ConfYear lastYearMonthExpireRecoveryDaysOneThree = ConfYearManager.getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13, office, previousYear);
		ConfYear lastYearMonthExpireRecoveryDaysFourNine = ConfYearManager.getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49, office, previousYear);
		ConfYear lastYearMaxRecoveryDaysOneThree = ConfYearManager.getByField(Parameter.MAX_RECOVERY_DAYS_13, office, previousYear);
		ConfYear lastYearMaxRecoveryDaysFourNine = ConfYearManager.getByField(Parameter.MAX_RECOVERY_DAYS_49, office, previousYear);
		ConfYear lastYearHourMaxToCalculateWorkTime = ConfYearManager.getByField(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, office, previousYear);

		//Parametri configurazione anno corrente
		ConfYear dayExpiryVacationPastYear = ConfYearManager.getByField(Parameter.DAY_EXPIRY_VACATION_PAST_YEAR, office, currentYear);
		ConfYear monthExpiryVacationPastYear = ConfYearManager.getByField(Parameter.MONTH_EXPIRY_VACATION_PAST_YEAR, office, currentYear);
		ConfYear monthExpireRecoveryDaysOneThree = ConfYearManager.getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_13, office, currentYear);
		ConfYear monthExpireRecoveryDaysFourNine = ConfYearManager.getByField(Parameter.MONTH_EXPIRY_RECOVERY_DAYS_49, office, currentYear);
		ConfYear maxRecoveryDaysOneThree = ConfYearManager.getByField(Parameter.MAX_RECOVERY_DAYS_13, office, currentYear);
		ConfYear maxRecoveryDaysFourNine = ConfYearManager.getByField(Parameter.MAX_RECOVERY_DAYS_49, office, currentYear);
		ConfYear hourMaxToCalculateWorkTime = ConfYearManager.getByField(Parameter.HOUR_MAX_TO_CALCULATE_WORKTIME, office, currentYear);

		render(currentYear, previousYear, lastYearDayExpiryVacationPastYear, lastYearMonthExpiryVacationPastYear, lastYearMonthExpireRecoveryDaysOneThree,
				lastYearMonthExpireRecoveryDaysFourNine, lastYearMaxRecoveryDaysOneThree, lastYearMaxRecoveryDaysFourNine,
				lastYearHourMaxToCalculateWorkTime, dayExpiryVacationPastYear, monthExpiryVacationPastYear,
				monthExpireRecoveryDaysOneThree, monthExpireRecoveryDaysFourNine, monthExpireRecoveryDaysFourNine, maxRecoveryDaysOneThree,
				maxRecoveryDaysFourNine, hourMaxToCalculateWorkTime, offices, office);

	}

	/**
	 *  Salva il nuovo valore per il field name. (Chiamata via ajax tramite X-editable)
	 * 
	 * @param pk
	 * @param value
	 */
	public static void saveConfYear(String pk, String value){
		
		ConfYear conf = ConfYearDao.getById(Long.parseLong(pk));
		
		Preconditions.checkNotNull(conf);
		
		MessageResult message = ConfigurationsManager.persistConfYear(conf, value);
		
		if(message.result == false){
			response.status = 500;
			renderText(message.message);
		}
	}
	
}
