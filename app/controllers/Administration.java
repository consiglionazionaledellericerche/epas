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
import manager.PersonDayManager;
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
import dao.PersonDayDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPerson;
import dao.wrapper.function.WrapperModelFunctionFactory;
import exceptions.EpasExceptionNoSourceData;


@With( {Resecure.class, RequestInit.class} )
public class Administration extends Controller {
	
	@Inject
	static OfficeDao officeDao;
	
	@Inject
	static PersonDao personDao;
	
	@Inject 
	static ContractDao contractDao;
	
	@Inject
	static PersonDayDao personDayDao;
	
	@Inject
	static PersonDayManager personDayManager;
	
	@Inject
	static ConsistencyManager consistencyManager;
	
	@Inject
	static PersonResidualYearRecapFactory yearFactory;
	
	@Inject
	static WrapperModelFunctionFactory wrapperFunctionFactory; 
	
	@Inject
	static IWrapperFactory wrapperFactory;
	
	@Inject
	static ContractManager contractManager;
	
	@Inject
	static ContractMonthRecapManager contractMonthRecapManager;
	
	@Inject
	static ContractYearRecapManager contractYearRecapManager;
	
	private final static Logger log = LoggerFactory.getLogger(Administration.class);
	
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
		log.debug("Chiamo overtime in year...");
		Competences.getOvertimeInYear(year);
		
	}
	
	/**
	 * Metodo di sviluppo per testing nuova costruzione riepiloghi mensili.S
	 * @param id
	 * @throws EpasExceptionNoSourceData
	 */
	public static void showPersonResidualSituation(Long id) throws EpasExceptionNoSourceData {

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

		for(Person person : sampling) {

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
					Optional<ContractMonthRecap> cmr = contractManager.getContractMonthRecap(contract, yearMonth);
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
			List<Contract> contractList = ContractDao.getPersonContractList(person);
			for(Contract c : contractList) {
				try {
					contractYearRecapManager.buildContractYearRecap(c);
				} catch (EpasExceptionNoSourceData e) {
						log.warn("Manca l'inizializzazione per il contratto {} di {}",
								new Object[]{contract.id, contract.person.getFullname()});
				}
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
					== contractMonthRecaps.get(maxArrayIndex).remainingMealTickets)
				error = true;
			
			if(personResidualMonthRecaps.get(maxArrayIndex).monteOreAnnoCorrente
					== contractMonthRecaps.get(maxArrayIndex).remainingMinutesCurrentYear)
				error = true;
			if(personResidualMonthRecaps.get(maxArrayIndex).monteOreAnnoPassato 
						== contractMonthRecaps.get(maxArrayIndex).remainingMinutesLastYear)
				error = true;
			
			if(error)
				log.info("Check Persona={} id={}, Esito={}", person.fullName(), person.id, "NOK");
			else 
				log.info("Check Persona={} id={}, Esito={}", person.fullName(), person.id, "OK");
			
			if(personList.size() == 1) {
				render(contractMonthRecaps, personResidualMonthRecaps, maxArrayIndex);
			}

		}

		renderText("Fine");

	}
	
	/**
	 * Metodo di sviluppo per creare nuovi riepiloghi mensili.
	 * 
	 * @throws EpasExceptionNoSourceData
	 */
	@SuppressWarnings("deprecation")
	public static void buildActualContractMonthRecap() throws EpasExceptionNoSourceData {

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
	
	public static void updateExceedeMinInCompetenceTable() {
		CompetenceUtility.updateExceedeMinInCompetenceTable();
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
			people.add(PersonDao.getPersonById(id));
		}
		
		for(Person person : people){
			new RemoveInvalidStampingsJob(person, begin, end).afterRequest();
		}
		
		flash.success("Avviati Job per la rimozione delle timbrature non valide per %s", people);
		utilities();
	}
   
}
