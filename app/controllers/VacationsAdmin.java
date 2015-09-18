package controllers;

import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.SecureManager;
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
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gdata.util.common.base.Preconditions;

import dao.ContractDao;
import dao.OfficeDao;
import dao.PersonDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;

@With( {Secure.class, RequestInit.class} )
public class VacationsAdmin extends Controller{

	@Inject
	private static OfficeDao officeDao;
	@Inject
	private static SecureManager secureManager;
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
	@Inject
	private static ContractDao contractDao;

	public static void list(Integer year, String name, Integer page, Long officeId){

		Set<Office> offices = secureManager.officesReadAllowed(Security.getUser().get());
		if(officeId == null) {
			if(offices.size() == 0) {
				flash.error("L'user non dispone di alcun diritto di visione "
						+ "delle sedi. Operazione annullata.");
				Application.indexAdmin();
			}
			officeId = offices.iterator().next().id;
		}
		Office office = officeDao.getOfficeById(officeId);
		notFoundIfNull(office);
		rules.checkIfPermitted(office);

		if(page==null) {
			page = 0;
		}

		LocalDate beginYear = new LocalDate(year, 1, 1);
		LocalDate endYear = new LocalDate(year, 12, 31);
		DateInterval yearInterval = new DateInterval(beginYear, endYear);

		SimpleResults<Person> simpleResults = personDao.list(Optional.fromNullable(name), 
				Sets.newHashSet(office), false, beginYear, endYear, true);

		List<Person> personList = simpleResults.paginated(page).getResults();

		List<VacationsRecap> vacationsList = Lists.newArrayList();

		List<Contract> contractsWithVacationsProblems = Lists.newArrayList();

		for(Person person : personList) {
			
			for(Contract contract : person.contracts) {
				
				IWrapperContract c = wrapperFactory.create(contract);
				if (DateUtility.intervalIntersection(c.getContractDateInterval(), 
						yearInterval) == null) {
					
					//Questo evento andrebbe segnalato... la list dovrebbe caricare
					// nello heap solo i contratti attivi nel periodo specificato.
					continue;
				}
				
				Optional<VacationsRecap> vr = vacationsFactory.create(year, 
						contract, LocalDate.now(), true);
				
				if(vr.isPresent()) {
					vacationsList.add(vr.get());
					
				} else {
					contractsWithVacationsProblems.add(contract);
				}
			}
		}

		LocalDate expireDate =  vacationManager
				.vacationsLastYearExpireDate(year, office);

		boolean isVacationLastYearExpired = vacationManager
				.isVacationsLastYearExpired(year, expireDate);

		render(vacationsList, isVacationLastYearExpired, 
				contractsWithVacationsProblems, year, simpleResults, name, offices, office);
	}

	public static void vacationsCurrentYear(Long contractId, Integer anno){

		Contract contract = contractDao.getContractById(contractId);
		if( contract == null ) {
			error();	/* send a 500 error */
		}

		rules.checkIfPermitted(contract.person.office);

		Optional<VacationsRecap> vr = vacationsFactory
				.create(anno, contract, LocalDate.now(), true);


		Preconditions.checkState(vr.isPresent());
		
		VacationsRecap vacationsRecap = vr.get();
		
		renderTemplate("Vacations/vacationsCurrentYear.html", vacationsRecap);
	}

	public static void vacationsLastYear(Long contractId, Integer anno){

		Contract contract = contractDao.getContractById(contractId);
		if( contract == null ) {
			error();	/* send a 500 error */
		}

		rules.checkIfPermitted(contract.person.office);

		Optional<VacationsRecap> vr = vacationsFactory
				.create(anno, contract, LocalDate.now(), true);

		Preconditions.checkState(vr.isPresent());
		
		VacationsRecap vacationsRecap = vr.get();
		
		renderTemplate("Vacations/vacationsLastYear.html", vacationsRecap);
	}


	public static void permissionCurrentYear(Long contractId, Integer anno){

		Contract contract = contractDao.getContractById(contractId);
		if( contract == null ) {
			error();	/* send a 500 error */
		}

		rules.checkIfPermitted(contract.person.office);

		Optional<VacationsRecap> vr = vacationsFactory
				.create(anno, contract, LocalDate.now(), true);

		
		Preconditions.checkState(vr.isPresent());
		
		VacationsRecap vacationsRecap = vr.get();

		renderTemplate("Vacations/permissionCurrentYear.html", vacationsRecap);
	}

}
