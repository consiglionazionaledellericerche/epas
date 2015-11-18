package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import dao.ContractDao;
import dao.PersonChildrenDao;
import dao.PersonDao;
import dao.UserDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperContract;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.function.WrapperModelFunctionFactory;
import helpers.Web;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import lombok.extern.slf4j.Slf4j;
import manager.ConfGeneralManager;
import manager.ConsistencyManager;
import manager.ContractManager;
import manager.ContractStampProfileManager;
import manager.ContractWorkingTimeTypeManager;
import manager.EmailManager;
import manager.OfficeManager;
import manager.PersonManager;
import manager.SecureManager;
import manager.UserManager;
import models.Contract;
import models.ContractStampProfile;
import models.ContractWorkingTimeType;
import models.Person;
import models.WorkingTimeType;
import org.joda.time.LocalDate;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@With( {Resecure.class, RequestInit.class} )
public class Contracts extends Controller {

	@Inject
	private static UserManager userManager;
	@Inject
	private static EmailManager emailManager;
	@Inject
	private static SecureManager secureManager;
	@Inject
	private static OfficeManager officeManager;
	@Inject
	private static PersonDao personDao;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static ContractManager contractManager;
	@Inject
	private static SecurityRules rules;
	@Inject
	private static WorkingTimeTypeDao workingTimeTypeDao;
	@Inject
	private static PersonManager personManager;
	@Inject
	private static ContractDao contractDao;
	@Inject
	private static ContractStampProfileManager contractStampProfileManager;
	@Inject
	private static UserDao userDao;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static ConfGeneralManager confGeneralManager;
	@Inject
	private static ContractWorkingTimeTypeManager contractWorkingTimeTypeManager;
	@Inject
	private static PersonChildrenDao personChildrenDao;
	@Inject
	private static ConsistencyManager consistencyManager;

	public static void personContracts(final Long personId) {
		
		Person person = personDao.getPersonById(personId);
		notFoundIfNull(person);
		rules.checkIfPermitted(person.office);
		
		List<IWrapperContract> contractList = FluentIterable
				.from(contractDao.getPersonContractList(person))
				.transform(wrapperFunctionFactory.contract()).toList();

		List<ContractStampProfile> contractStampProfileList =
				contractDao.getPersonContractStampProfile(Optional.fromNullable(person), 
						Optional.<Contract>absent());

		
		render(person, contractList, contractStampProfileList);
	}
	
	public static void updateContractWorkingTimeType(Long id){

		Contract contract = contractDao.getContractById(id);
		notFoundIfNull(contract);
		
		rules.checkIfPermitted(contract.person.office);

		List<WorkingTimeType> wttList = wttList(contract);
		
		IWrapperContract wrappedContract = wrapperFactory.create(contract);
		
		render(wrappedContract, contract, wttList);
	}
	
	// FIXME è del wrappercontract
	private static List<WorkingTimeType> wttList(Contract contract) {
		
		//La lista dei tipi orario ammessi per la persona
		List<WorkingTimeType> wttDefault = workingTimeTypeDao.getDefaultWorkingTimeType();
		List<WorkingTimeType> wttAllowed = contract.person.office.getEnabledWorkingTimeType();
		List<WorkingTimeType> wttList = new ArrayList<WorkingTimeType>();
		wttList.addAll(wttDefault);
		wttList.addAll(wttAllowed);
		return wttList;
		
	}
	
	/**
	 * Divide il periodo tipo orario in due periodi.
	 * @param cwtt
	 * @param splitDate
	 */
	public static void splitContractWorkingTimeType(@Valid ContractWorkingTimeType cwtt, 
			@Required LocalDate splitDate) {
		
		notFoundIfNull(cwtt);
		
		rules.checkIfPermitted(cwtt.contract.person.office);

		Contract contract = cwtt.contract;
		IWrapperContract wrappedContract = wrapperFactory.create(cwtt.contract);
		List<WorkingTimeType> wttList = wttList(contract);

		if (!validation.hasErrors()) {
			
			// errori particolari
			
			if (!DateUtility.isDateIntoInterval(
					splitDate, new DateInterval(cwtt.beginDate, cwtt.endDate))) {
				validation.addError("splitDate", 
						"riechiesta entro l'intervallo");
			} else if (splitDate.isEqual(cwtt.beginDate)) {
				validation.addError("splitDate", 
						"non può essere il primo giorno dell'intervallo");
			}
		}
			
		if (validation.hasErrors()) {
			
			response.status = 400;
			flash.error(Web.msgHasErrors());
			
			log.warn("validation errors: {}", validation.errorsMap());
			
			render("@updateContractWorkingTimeType", wrappedContract, contract, wttList, splitDate);
		} 

		//agire
		contractWorkingTimeTypeManager.saveSplitContractWorkingTimeType(cwtt, splitDate);

		//flash.success("Orario di lavoro correttamente suddiviso in due sottoperiodi con tipo orario %s.", cwtt.workingTimeType.description);
		render("@updateContractWorkingTimeType", wrappedContract, contract, wttList);
	}

	
}
