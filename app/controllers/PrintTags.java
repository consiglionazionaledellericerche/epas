package controllers;

import static play.modules.pdf.PDF.renderPDF;
import it.cnr.iit.epas.DateUtility;

import java.util.List;

import javax.inject.Inject;

import manager.recaps.personStamping.PersonStampingRecap;
import manager.recaps.personStamping.PersonStampingRecapFactory;
import models.Person;
import models.User;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;

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
	private static IWrapperFactory wrapperFactory;

	public static void showTag(Long personId, int month, int year){

		Preconditions.checkNotNull(personId);
		Person person = personDao.getPersonById(personId);

		Preconditions.checkNotNull(person);
		
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

		Optional<User> currentUser = Security.getUser();
		Preconditions.checkState(currentUser.isPresent());
		Preconditions.checkNotNull(currentUser.get().person);
		
		IWrapperPerson person = wrapperFactory
				.create(Security.getUser().get().person);
		
		
		if(! person.isActiveInMonth(new YearMonth(year,month)) ) {

			flash.error("La persona %s non ha contratto attivo nel mese selezionato",
					person.getValue().fullName());
			render("@redirectToIndex");
		}

		PersonStampingRecap psDto = stampingsRecapFactory
				.create(person.getValue(), year, month);

		// FIXME: spostare nel template
		String titolo = "Situazione presenze mensile " +  
				DateUtility.fromIntToStringMonth(month) + " " + year + " di " + 
				person.getValue().fullName();

		renderPDF(psDto, titolo) ;

	}

}
