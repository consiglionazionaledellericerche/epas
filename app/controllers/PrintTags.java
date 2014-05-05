package controllers;

import static play.modules.pdf.PDF.renderPDF;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.AbsenceType;
import models.ConfGeneral;
import models.Person;
import models.PersonDay;
import models.StampModificationType;
import models.StampType;
import models.enumerate.ConfigurationFields;
import models.rendering.PersonStampingDayRecap;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class} )
public class PrintTags extends Controller{
	
	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void showTag(Long personId){
		if(personId == null){
			flash.error("Malissimo! ci vuole un id! Seleziona una persona!");
			Application.indexAdmin();
		}
//		Lang.change("it");
//		Logger.debug("Il linguaggio attualmente è: %s", Lang.get());
//		Configuration confParameters = Configuration.getCurrentConfiguration();
		if(personId == -1){
			/**
			 * è il caso in cui ho chiesto la stampa cartellino di tutti...vediamo come gestirla in un secondo momento
			 */
		}
		Person person = Person.findById(personId);
		int month = params.get("month", Integer.class);
		int year = params.get("year", Integer.class);
		
	
		//Configuration conf = Configuration.getCurrentConfiguration();
		//ConfGeneral conf = ConfGeneral.getConfGeneral();
		int minInOutColumn = Integer.parseInt(ConfGeneral.getFieldValue(ConfigurationFields.NumberOfViewingCouple.description, person.office));
		//int minInOutColumn = conf.numberOfViewingCoupleColumn;
		int numberOfInOut = Math.max(minInOutColumn, PersonUtility.getMaximumCoupleOfStampings(person, year, month));

		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = PersonUtility.getTotalPersonDayInMonth(person, year, month);
		
		//Costruzione dati da renderizzare
		for(PersonDay pd : totalPersonDays)
		{
			pd.computeValidStampings(); //calcolo del valore valid per le stamping del mese (persistere??)
		}
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(PersonDay pd : totalPersonDays )
		{
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);
			daysRecap.add(dayRecap);
		}
		List<StampModificationType> stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;
		List<StampType> stampTypeList = PersonStampingDayRecap.stampTypeList;
		
		int numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month);
		int numberOfMealTicketToUse = PersonUtility.numberOfMealTicketToUse(person, year, month);
		int numberOfMealTicketToRender = PersonUtility.numberOfMealTicketToRender(person, year, month);
		int basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);
		Map<AbsenceType,Integer> absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);

		/*
		//RTODO il contratto attivo nel mese (quello più recente)
		Contract contract = person.getCurrentContract();
		CalcoloSituazioneAnnualePersona c = new CalcoloSituazioneAnnualePersona(contract, year, null);
		Mese mese = c.getMese(year, month);
		*/
		
		String titolo = "Situazione presenze mensile " +  DateUtility.fromIntToStringMonth(month) + " " + year + " di " + person.surname + " " + person.name;
		
		//Render
		renderPDF(person, year, month, numberOfInOut, numberOfCompensatoryRestUntilToday,numberOfMealTicketToUse,numberOfMealTicketToRender,
				daysRecap, stampModificationTypeList, stampTypeList, basedWorkingDays, absenceCodeMap, titolo);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void listPersonForPrintTags(int year, int month){
		LocalDate date = new LocalDate(year, month,1);
		List<Person> personList = Person.getActivePersonsInMonth(month, year, Security.getOfficeAllowed(), false);
		render(personList, date, year, month);
	}
	
	
	@Check(Security.VIEW_PERSONAL_SITUATION)
	public static void showPersonTag(Integer year, Integer month){
		
		//TODOUSER
		//if (Security.getPerson().username.equals("admin")) {
		//	Application.indexAdmin();
		//}
		Person person = Security.getUser().person;
		if(!person.isActiveInMonth(month, year))
		{
			flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
					"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			render("@redirectToIndex");
		}
	
		
		//Configuration conf = Configuration.getCurrentConfiguration();
		//ConfGeneral conf = ConfGeneral.getConfGeneral();
		int minInOutColumn = Integer.parseInt(ConfGeneral.getFieldValue(ConfigurationFields.NumberOfViewingCouple.description, person.office));
		//int minInOutColumn = conf.numberOfViewingCoupleColumn;
		int numberOfInOut = Math.max(minInOutColumn, PersonUtility.getMaximumCoupleOfStampings(person, year, month));

		//Lista person day contente tutti i giorni fisici del mese
		List<PersonDay> totalPersonDays = PersonUtility.getTotalPersonDayInMonth(person, year, month);
		
		//Costruzione dati da renderizzare
		for(PersonDay pd : totalPersonDays)
		{
			pd.computeValidStampings(); //calcolo del valore valid per le stamping del mese (persistere??)
		}
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(PersonDay pd : totalPersonDays )
		{
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);
			daysRecap.add(dayRecap);
		}
		List<StampModificationType> stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;
		List<StampType> stampTypeList = PersonStampingDayRecap.stampTypeList;
		
		int numberOfCompensatoryRestUntilToday = PersonUtility.numberOfCompensatoryRestUntilToday(person, year, month);
		int numberOfMealTicketToUse = PersonUtility.numberOfMealTicketToUse(person, year, month);
		int numberOfMealTicketToRender = PersonUtility.numberOfMealTicketToRender(person, year, month);
		int basedWorkingDays = PersonUtility.basedWorkingDays(totalPersonDays);
		Map<AbsenceType,Integer> absenceCodeMap = PersonUtility.getAllAbsenceCodeInMonth(totalPersonDays);

		String titolo = "Situazione presenze mensile " +  DateUtility.fromIntToStringMonth(month) + " " + year + " di " + person.surname + " " + person.name;
		
		//Render
		renderPDF(person, year, month, numberOfInOut, numberOfCompensatoryRestUntilToday,numberOfMealTicketToUse,numberOfMealTicketToRender,
				daysRecap, stampModificationTypeList, stampTypeList, basedWorkingDays, absenceCodeMap, titolo);
		//renderPDF(person, year, month, numberOfInOut, daysRecap,stampModificationTypeList, stampTypeList);
		
		
	}

}
