package controllers;

import it.cnr.iit.epas.ExportToYaml;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

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
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import controllers.Resecure.NoCheck;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;


@With( {Resecure.class, RequestInit.class} )
public class Administration extends Controller {
	
	@Inject
	static OfficeDao officeDao;
	
	@Inject
	static PersonDao personDao;
	
	@Inject
	static ConsistencyManager consistencyManager;
	
	@Inject
	static PersonResidualYearRecapFactory yearFactory;
	
	@Inject
	static WrapperModelFunctionFactory wrapperFunctionFactory; 
	
	@Inject
	static IWrapperFactory wrapperFactory;
	
	public static void utilities(){

		final List<Person> personList = PersonDao.list(
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
		Optional<Person> person = personId == -1 ? Optional.<Person>absent() : Optional.fromNullable(PersonDao.getPersonById(personId));
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
	
	public static void buildYaml()
	{
		//general
		ExportToYaml.buildAbsenceTypesAndQualifications("conf/absenceTypesAndQualifications.yml");
		
		ExportToYaml.buildCompetenceCodes("conf/competenceCodes.yml");
		
		ExportToYaml.buildVacationCodes("conf/vacationCodes.yml");
		
		
		//person
		/*
		Person person = Person.findById(146l);
		ExportToYaml.buildPerson(person, "test/dataTest/persons/lucchesi.yml");
		
		//test stampings
		ExportToYaml.buildPersonMonth(person, 2013,  9, "test/dataTest/stampings/lucchesiStampingsSettembre2013.yml");
		ExportToYaml.buildPersonMonth(person, 2013, 10, "test/dataTest/stampings/lucchesiStampingsOttobre2013.yml");
		
		//test vacations
		ExportToYaml.buildYearlyAbsences(person, 2012, "test/dataTest/absences/lucchesiAbsences2012.yml");
		ExportToYaml.buildYearlyAbsences(person, 2013, "test/dataTest/absences/lucchesiAbsences2013.yml");
		*/
		
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
		
		//destroy contact_data
//		if(person.contactData!=null)
//			person.contactData.delete();
		
		//destroy locations
//		if(person.location!=null)
//			person.location.delete();
//		
		person.save();
		
		renderText(person.name);
	}
   
}
