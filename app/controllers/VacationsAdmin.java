package controllers;

import helpers.ModelQuery.SimpleResults;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import manager.VacationManager;
import manager.recaps.vacation.VacationsRecap;
import manager.recaps.vacation.VacationsRecapFactory;
import models.Contract;
import models.Office;
import models.Person;

import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import exceptions.EpasExceptionNoSourceData;

@With( {Secure.class, RequestInit.class} )
public class VacationsAdmin extends Controller{

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static VacationsRecapFactory vacationsFactory;
	@Inject
	private static VacationManager vacationManager;
	@Inject
	private static SecurityRules rules;


	public static void list(Integer year, String name, Integer page){

		if(page==null)
			page = 0;

		SimpleResults<Person> simpleResults = personDao.list(Optional.fromNullable(name), 
				officeDao.getOfficeAllowed(Security.getUser().get()),
				false, LocalDate.now(), LocalDate.now(), true);

		List<Person> personList = simpleResults.paginated(page).getResults();

		List<VacationsRecap> vacationsList = new ArrayList<VacationsRecap>();

		List<Person> personsWithVacationsProblems = new ArrayList<Person>();

		for(Person person: personList) {

			Optional<Contract> contract = wrapperFactory
					.create(person).getCurrentContract();

			try {
				VacationsRecap vr = vacationsFactory.create(
						year, contract.get(), LocalDate.now(), true);
				vacationsList.add(vr);

			} catch (EpasExceptionNoSourceData e) {
				personsWithVacationsProblems.add(person);
			}
		}

		Office office = Security.getUser().get().person.office;

		LocalDate expireDate =  vacationManager
				.vacationsLastYearExpireDate(year, office);

		boolean isVacationLastYearExpired = vacationManager
				.isVacationsLastYearExpired(year, expireDate);

		render(vacationsList, isVacationLastYearExpired, 
				personsWithVacationsProblems, year, simpleResults, name);
	}



	public static void vacationsCurrentYear(Long personId, Integer anno){

		Person person = personDao.getPersonById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}

		rules.checkIfPermitted(person.office);

		//FIXME tutti questi metodi con parametro anno vanno applicati sul
		//contratto lastContractInYear, e non sul currentContract. Inoltre 
		//potrebbe essere necessario renderizzare tutti i contratti attivi nell
		//anno. 
		Optional<Contract> contract = wrapperFactory
				.create(person).getCurrentContract();

		Preconditions.checkState(contract.isPresent());

		try { 
			VacationsRecap vacationsRecap = vacationsFactory
					.create(anno, contract.get(), LocalDate.now(), true);

			renderTemplate("Vacations/vacationsCurrentYear.html", vacationsRecap);

		} catch(EpasExceptionNoSourceData e) {
			flash.error("Mancano i dati di inizializzazione per " 
					+ contract.get().person.fullName());
			renderTemplate("Application/indexAdmin.html");
		}

	}



	public static void vacationsLastYear(Long personId, Integer anno){

		Person person = personDao.getPersonById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}

		rules.checkIfPermitted(person.office);

		Optional<Contract> contract = wrapperFactory
				.create(person).getCurrentContract();

		Preconditions.checkState(contract.isPresent());

		try { 
			VacationsRecap vacationsRecap = vacationsFactory
					.create(anno, contract.get(), LocalDate.now(), true);

			renderTemplate("Vacations/vacationsLastYear.html", vacationsRecap);

		} catch(EpasExceptionNoSourceData e) {
			flash.error("Mancano i dati di inizializzazione per " 
					+ contract.get().person.fullName());
			renderTemplate("Application/indexAdmin.html");
		}

	}


	public static void permissionCurrentYear(Long personId, Integer anno){

		Person person = personDao.getPersonById(personId);
		if( person == null ) {
			error();	/* send a 500 error */
		}
		rules.checkIfPermitted(person.office);

		Optional<Contract> contract = wrapperFactory
				.create(person).getCurrentContract();

		Preconditions.checkState(contract.isPresent());

		try { 
			VacationsRecap vacationsRecap = vacationsFactory
					.create(anno, contract.get(), LocalDate.now(), true);

			renderTemplate("Vacations/permissionCurrentYear.html", vacationsRecap);

		} catch(EpasExceptionNoSourceData e) {
			flash.error("Mancano i dati di inizializzazione per " 
					+ contract.get().person.fullName());
			renderTemplate("Application/indexAdmin.html");
		}

	}

}
