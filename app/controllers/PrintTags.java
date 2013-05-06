package controllers;

import java.util.List;

import org.joda.time.LocalDate;

import models.Configuration;
import models.Person;
import models.PersonDay;
import models.PersonMonth;
import play.mvc.Controller;

import static play.modules.pdf.PDF.*;

public class PrintTags extends Controller{
	
	public static void showTag(Long personId){
		if(personId == null){
			flash.error("Malissimo! ci vuole un id! Seleziona una persona!");
			Application.indexAdmin();
		}
		
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
		int numberOfInOut = Math.min(confParameters.numberOfViewingCoupleColumn, (int)pm.getMaximumCoupleOfStampings());
				
		renderPDF(pdList, person, year, month, pm, numberOfInOut);
		
		
	}
	
	public static void listPersonForPrintTags(){
		LocalDate date = new LocalDate();
		List<Person> personList = Person.getActivePersons(date);
		render(personList, date);
	}

}
