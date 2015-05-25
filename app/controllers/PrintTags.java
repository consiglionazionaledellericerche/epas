package controllers;

import static play.modules.pdf.PDF.renderPDF;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import manager.PersonManager;
import manager.recaps.personStamping.PersonStampingRecap;
import manager.recaps.personStamping.PersonStampingRecapFactory;
import models.Person;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;

import dao.OfficeDao;
import dao.PersonDao;

@With( {Resecure.class, RequestInit.class} )
public class PrintTags extends Controller{
	
	@Inject
	private static PersonDao personDao;
	@Inject
	private static SecurityRules rules;
	@Inject
	private static PersonStampingRecapFactory stampingsRecapFactory;
	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonManager personManager;

	public static void showTag(Long personId, int month, int year){

		if(personId == null) {
			flash.error("Malissimo! ci vuole un id! Seleziona una persona!");
			Application.indexAdmin();
		}

		Person person = personDao.getPersonById(personId);

		rules.checkIfPermitted(person.office);

		PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month);

		String titolo = "Situazione presenze mensile " +  
				DateUtility.fromIntToStringMonth(month) + " " + year + " di " + 
				person.surname + " " + person.name;

		renderPDF(psDto, titolo) ;		
	}

	public static void listPersonForPrintTags(int year, int month){

		rules.checkIfPermitted(Security.getUser().get().person.office);

		LocalDate date = new LocalDate(year, month,1);

		List<Person> personList = personDao.list(Optional.<String>absent(), 
				officeDao.getOfficeAllowed(Security.getUser().get()), false, 
				date, date.dayOfMonth().withMaximumValue(), true).list();

		render(personList, date, year, month);
	}

	public static void showPersonTag(Integer year, Integer month){

		Person person = Security.getUser().get().person;

		if(!personManager.isActiveInMonth(person, month, year, false)) {

			flash.error("Si è cercato di accedere a un mese al di fuori del contratto valido per %s %s. " +
					"Non esiste situazione mensile per il mese di %s", person.name, person.surname, DateUtility.fromIntToStringMonth(month));
			render("@redirectToIndex");
		}

		PersonStampingRecap psDto = stampingsRecapFactory.create(person, year, month);

		String titolo = "Situazione presenze mensile " +  
				DateUtility.fromIntToStringMonth(month) + " " + year + " di " + 
				person.surname + " " + person.name;

		renderPDF(psDto, titolo) ;

	}

}
