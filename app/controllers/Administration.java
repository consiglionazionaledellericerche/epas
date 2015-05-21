package controllers;

import it.cnr.iit.epas.CompetenceUtility;
import it.cnr.iit.epas.ExportToYaml;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import jobs.RemoveInvalidStampingsJob;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.ContractMonthRecapManager;
import manager.ContractYearRecapManager;
import manager.recaps.residual.PersonResidualMonthRecap;
import manager.recaps.residual.PersonResidualYearRecap;
import manager.recaps.residual.PersonResidualYearRecapFactory;
import models.Contract;
import models.ContractMonthRecap;
import models.Person;
import models.PersonDay;
import models.PersonDayInTrouble;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.data.validation.Required;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class} )
public class Administration extends Controller {

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static PersonDao personDao;
	@Inject 
	private static ContractDao contractDao;
	@Inject
	private static ConsistencyManager consistencyManager;
	@Inject
	private static PersonResidualYearRecapFactory yearFactory;
	@Inject
	private static ExportToYaml exportToYaml;
	@Inject
	private static CompetenceUtility competenceUtility;
	@Inject
	private static ContractMonthRecapManager contractMonthRecapManager;
	@Inject
	private static ContractYearRecapManager contractYearRecapManager;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static IWrapperFactory wrapperFactory;
	
	private final static Logger log = LoggerFactory.getLogger(Administration.class);

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

	/**
	 * Metodo di sviluppo per testing nuova costruzione riepiloghi mensili.S
	 * @param id
	 */
	
	@SuppressWarnings("deprecation")
	public static void showPersonResidualSituation(Long id) {

		List<Person> personList = Lists.newArrayList();
		
		if (id != null) { 
			Person person = personDao.getPersonById(id);
			personList.add(person);
		} else { 
			//Prendo la lista delle persone attive oggi
			personList = personDao.list(Optional.<String>absent(),
					officeDao.getOfficeAllowed(Security.getUser().get()), false, LocalDate.now(),
					LocalDate.now(), false).list();
		}
		
		//Sampling
		List<Person> sampling = Lists.newArrayList();
		if(personList.size() == 1) {
			sampling = personList;
		}
		else {
			for ( int i = 0; i<personList.size(); i++) {
				if ( i%20 == 0) {
					sampling.add(personList.get(i));
				}
			}
		}

		JPAPlugin.closeTx(false);
		
		for(Person person : sampling) {

			JPAPlugin.startTx(false);
			
			person = Person.findById(person.id);
			
			IWrapperPerson wperson = wrapperFactory.create(person);
			Contract contract = wperson.getCurrentContract().get();

			//lista ContractMonthRecap
			contractMonthRecapManager.populateContractMonthRecap(contract, Optional.<YearMonth>absent());

			List<ContractMonthRecap> contractMonthRecaps = Lists.newArrayList();

			YearMonth first = null;
			YearMonth last = null;

			int actualYear = 2013;

			while (actualYear <= 2015) {
				int month = 1;
				while (month <= 12) {
					YearMonth yearMonth = new YearMonth(actualYear, month);
					Optional<ContractMonthRecap> cmr = contractMonthRecapManager.getContractMonthRecap(contract, yearMonth, false);
					if (cmr.isPresent()) {
						if( first == null ) {
							first = yearMonth;
						}
						contractMonthRecaps.add(cmr.get());
						last = yearMonth;
					}
					month++;
				}

				actualYear++;
			}

			//lista PersonResidualMonthRecap
			List<PersonResidualMonthRecap> personResidualMonthRecaps = Lists.newArrayList();
			
			//Ricalcolo dei residui per anno
			List<Contract> contractList = contractDao.getPersonContractList(person);
			for(Contract c : contractList) {
				contractYearRecapManager.buildContractYearRecap(c);
			}

			actualYear = first.getYear();

			while (actualYear <= 2015) {

				PersonResidualYearRecap residual = 
						yearFactory.create(contract, actualYear, null);

				int month = 1;
				while (month <= 12) {

					YearMonth yearMonth = new YearMonth(actualYear, month);

					if( ! (yearMonth.isBefore(first) || yearMonth.isAfter(last)) ) {

						PersonResidualMonthRecap prmr = residual.getMese(month);
						if (prmr != null) {
							personResidualMonthRecaps.add(prmr);
						}
					}

					month++;
				}

				actualYear++;
			}

			int maxArrayIndex = contractMonthRecaps.size() - 1;
			
			boolean error = false;
			
			if(personResidualMonthRecaps.get(maxArrayIndex).buoniPastoResidui 
					!= contractMonthRecaps.get(maxArrayIndex).remainingMealTickets.intValue())
				error = true;
			
			if(personResidualMonthRecaps.get(maxArrayIndex).monteOreAnnoCorrente
					!= contractMonthRecaps.get(maxArrayIndex).remainingMinutesCurrentYear.intValue())
				error = true;
			if(personResidualMonthRecaps.get(maxArrayIndex).monteOreAnnoPassato 
						!= contractMonthRecaps.get(maxArrayIndex).remainingMinutesLastYear.intValue())
				error = true;
			
			if(error)
				log.info("Check Persona={} id={}, Esito={}", person.fullName(), person.id, "NOK");
			else 
				log.info("Check Persona={} id={}, Esito={}", person.fullName(), person.id, "OK");
			
			JPAPlugin.closeTx(false);
			
			if(personList.size() == 1) {
				render(contractMonthRecaps, personResidualMonthRecaps, maxArrayIndex);
			}
			

		}

		renderText("Fine");

	}
	
	/**
	 * Metodo di sviluppo per creare nuovi riepiloghi mensili.
	 * 
	 */
	@SuppressWarnings("deprecation")
	public static void buildActualContractMonthRecap() {

		//Prendo la lista delle persone attive oggi
		List<Person> personList = personDao.list(Optional.<String>absent(),
				officeDao.getOfficeAllowed(Security.getUser().get()), false, LocalDate.now(),
				LocalDate.now(), false).list();

		for (Person person : personList) {
			JPAPlugin.startTx(false);
			Contract contract = wrapperFactory.create(person).getCurrentContract().get();
			// detached
			Contract c = contractDao.getContractById(contract.id);
			
			log.debug("Costruzione Persona={} id={}", person.fullName(), person.id);
			
			contractMonthRecapManager.populateContractMonthRecap(c, Optional.<YearMonth>absent());
			JPAPlugin.closeTx(false);
		}
		renderText("Concluso Job");
	}


	public static void showResidualSituation() {
		
		String name = null;
		//Prendo la lista delle persone attive oggi
		List<Person> personList = personDao.list(Optional.fromNullable(name),
					officeDao.getOfficeAllowed(Security.getUser().get()), false, LocalDate.now(),
					LocalDate.now(), false).list();
		

		//Sampling
		List<Person> sampling = Lists.newArrayList();
		for ( int i = 0; i<personList.size(); i++) {
			
			if ( i%20 == 0) {
				sampling.add(personList.get(i));
			}
		}
		
		
		//Calcolo i riepiloghi
		
		List<IWrapperPerson> wrapperPersonList = FluentIterable
				.from(sampling)
				.transform(wrapperFunctionFactory.person()).toList();
		
		List<PersonResidualMonthRecap> monthRecapList = Lists.newArrayList();
		
		for(IWrapperPerson person : wrapperPersonList){
			
			log.debug("Persona {}", person.getValue().getFullname());
			
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
