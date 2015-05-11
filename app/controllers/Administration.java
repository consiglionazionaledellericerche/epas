package controllers;

import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.ExportToYaml;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import jobs.RemoveInvalidStampingsJob;
import manager.ConsistencyManager;
import manager.recaps.residual.PersonResidualMonthRecap;
import manager.recaps.residual.PersonResidualYearRecap;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.Contract;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;


@With( {Resecure.class, RequestInit.class} )
public class Administration extends Controller {

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static ConsistencyManager consistencyManager;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static PersonResidualYearRecapFactory yearFactory;
	@Inject
	private static ExportToYaml exportToYaml;
	@Inject
	private static CompetenceUtility competenceUtility;

	public static void utilities(){

		final List<Person> personList = personDao.list(
				Optional.<String>absent(),officeDao.getOfficeAllowed(Security.getUser().get()), 
				false, LocalDate.now(), LocalDate.now(), true)
				.list();

		render(personList);
	}


	/**
	 * Ricalcolo della situazione di una persona dal mese e anno specificati ad oggi.
	 * @param personId l'id univoco della persona da fixare, -1 per fixare tutte le persone
	 * @param year l'anno dal quale far partire il fix
	 * @param month il mese dal quale far partire il fix
	 */
	public static void fixPersonSituation(Long personId, int year, int month){	
		LocalDate date = new LocalDate(year,month,1);
		Optional<Person> person = personId == -1 ? Optional.<Person>absent() : Optional.fromNullable(personDao.getPersonById(personId));
		consistencyManager.fixPersonSituation(person,Security.getUser(), date, false);
	}

	@Check(Security.INSERT_AND_UPDATE_COMPETENCES)
	public static void createOvertimeFile(int year) throws IOException{
		Logger.debug("Chiamo overtime in year...");
		Competences.getOvertimeInYear(year);

	}

	public static void showResidualSituation() {

		String name = null;
		//Prendo la lista delle persone attive oggi
		List<Person> personList = personDao.list(Optional.fromNullable(name),
				officeDao.getOfficeAllowed(Security.getUser().get()), false, LocalDate.now(),
				LocalDate.now(), false).list();

		//Calcolo i riepiloghi

		List<IWrapperPerson> wrapperPersonList = FluentIterable
				.from(personList)
				.transform(wrapperFunctionFactory.person()).toList();

		List<PersonResidualMonthRecap> monthRecapList = Lists.newArrayList();

		for(IWrapperPerson person : wrapperPersonList){

			Logger.debug("Persona %s", person.getValue().getFullname());
			PersonResidualYearRecap residual = 
					yearFactory.create(person.getCurrentContract().get(), LocalDate.now().getYear(), null);

			//monthRecapList.add(residual.getMese(LocalDate.now().getMonthOfYear()));
			monthRecapList.add(residual.getMese(2));

		}

		//Render
		render(monthRecapList);

	}

	public static void buildYaml(){
		//general
		exportToYaml.buildAbsenceTypesAndQualifications("conf/absenceTypesAndQualifications.yml");

		exportToYaml.buildCompetenceCodes("conf/competenceCodes.yml");

		exportToYaml.buildVacationCodes("conf/vacationCodes.yml");

	}

	public static void killclock()
	{
		Person person = Person.find("byName", "epas").first();


		//destroy person day in trouble
		List<PersonDay> pdList = PersonDay.find("select pd from PersonDay pd where pd.person = ?", person).fetch();
		for(PersonDay pd : pdList)
		{
			while(pd.troubles.size()>0)
			{
				PersonDayInTrouble pdt = pd.troubles.get(0);
				pd.troubles.remove(pdt);
				pdt.delete();
				pd.save();
			}
		}

		//destroy person day
		while(pdList.size()>0)
		{
			PersonDay pd = pdList.get(0);
			pdList.remove(pd);
			pd.delete();
		}

		//destroy contracts
		while(person.contracts.size()>0)
		{
			Contract c = person.contracts.get(0);
			person.contracts.remove(c);
			c.delete();
			person.save();
		}

		person.save();

		renderText(person.name);
	}

	public static void updateExceedeMinInCompetenceTable() {
		competenceUtility.updateExceedeMinInCompetenceTable();
		renderText("OK");
	}

	public static void deleteUncoupledStampings(@Required List<Long> peopleId,
			@Required LocalDate begin,LocalDate end){

		if (validation.hasErrors()){
			params.flash(); 
			utilities();
		}

		if(end == null){
			end = begin;
		}

		List<Person> people = Lists.newArrayList();

		for(Long id : peopleId){
			people.add(personDao.getPersonById(id));
		}

		for(Person person : people){
			new RemoveInvalidStampingsJob(person, begin, end).afterRequest();
		}

		flash.success("Avviati Job per la rimozione delle timbrature non valide per %s", people);
		utilities();
	}

}
