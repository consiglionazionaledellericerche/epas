package controllers;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import models.Configuration;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import models.StampModificationType;
import models.StampType;
import models.rendering.PersonStampingDayRecap;
import play.Logger;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.With;
import static play.modules.pdf.PDF.*;

@With( {Secure.class, NavigationMenu.class} )
public class PrintTags extends Controller{
	
	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void showTag(Long personId){
		if(personId == null){
			flash.error("Malissimo! ci vuole un id! Seleziona una persona!");
			Application.indexAdmin();
		}
//		Lang.change("it");
//		Logger.debug("Il linguaggio attualmente è: %s", Lang.get());
		Configuration confParameters = Configuration.getCurrentConfiguration();
		if(personId == -1){
			/**
			 * è il caso in cui ho chiesto la stampa cartellino di tutti...vediamo come gestirla in un secondo momento
			 */
		}
		Person person = Person.findById(personId);
		int month = params.get("month", Integer.class);
		int year = params.get("year", Integer.class);
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?",
				person, new LocalDate(year,month,1), new LocalDate(year,month,1).dayOfMonth().withMaximumValue()).fetch();
		
		PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
				person, month, year).first();
		
		PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?",
				person, month, year).first();

		//int numberOfInOut = Math.min(confParameters.numberOfViewingCoupleColumn, (int)pm.getMaximumCoupleOfStampings());
		int numberOfInOut = Math.max(0, (int)personMonth.getMaximumCoupleOfStampings());
				
		//TODO 18/10 usare metodo in models.person getMonthContract() per implementare questo controllo
		if (personMonth == null) {
			/**
			 * se il personMonth che viene richiesto, è situato nel tempo prima dell'inizio del contratto della persona oppure successivamente 
			 * ad esso, se quest'ultimo è a tempo determinato (expireContract != null), si rimanda alla pagina iniziale perchè si tenta di accedere
			 * a un periodo fuori dall'intervallo temporale in cui questa persona ha un contratto attivo
			 */
			if(new LocalDate(year, month, 1).dayOfMonth().withMaximumValue().isBefore(person.getCurrentContract().beginContract)
					|| (person.getCurrentContract().expireContract != null && new LocalDate(year, month, 1).isAfter(person.getCurrentContract().expireContract))){
				flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
						"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
				render("@redirectToIndex");
			}
			personMonth = new PersonMonth(person, year, month);
			personMonth.create();

		}		
		
		PersonStampingDayRecap.stampModificationTypeList = new ArrayList<StampModificationType>();	
		PersonStampingDayRecap.stampTypeList = new ArrayList<StampType>();							

		List<PersonStampingDayRecap> daysRecap = new ArrayList<PersonStampingDayRecap>();
		for(PersonDay pd : personMonth.days)
		{
			PersonStampingDayRecap dayRecap = new PersonStampingDayRecap(pd,numberOfInOut);
			daysRecap.add(dayRecap);
		}
		List<StampModificationType> stampModificationTypeList = PersonStampingDayRecap.stampModificationTypeList;
		List<StampType> stampTypeList = PersonStampingDayRecap.stampTypeList;
		
			
		renderPDF(pdList, person, year, month, pm, numberOfInOut, daysRecap, personMonth, stampModificationTypeList, stampTypeList);
		
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_STAMPING)
	public static void listPersonForPrintTags(int year, int month){
		//int month = params.get("month", Integer.class);
		//int year = params.get("year", Integer.class);
		LocalDate date = new LocalDate(year, month,1);
		List<Person> personList = Person.getActivePersons(date);
		render(personList, date);
	}

}
