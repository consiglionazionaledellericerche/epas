package controllers;

import play.data.validation.Error;
import helpers.ValidationHelper;
import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import manager.ContractManager;
import manager.SecureManager;
import manager.WorkingTimeTypeManager;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import models.dto.HorizontalWorkingTime;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;

import dao.ContractDao;
import dao.OfficeDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperOffice;
import dao.wrapper.IWrapperWorkingTimeType;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Resecure.class, RequestInit.class} )
public class WorkingTimes extends Controller{

	@Inject 
	private static OfficeDao officeDao;
	@Inject
	private static SecureManager secureManager;
	@Inject
	private static SecurityRules rules;
	@Inject
	private static WorkingTimeTypeDao workingTimeTypeDao;
	@Inject
	private static WrapperModelFunctionFactory wrapperFunctionFactory;
	@Inject
	private static ContractDao contractDao;
	@Inject
	private static IWrapperFactory wrapperFactory;
	@Inject
	private static WorkingTimeTypeManager workingTimeTypeManager;
	@Inject
	private static ContractManager contractManager;

	public static void manageWorkingTime(Long officeId){
		
		Office office = null;
		if(officeId != null){
			office = officeDao.getOfficeById(officeId);
		}
		
		Set<Office> offices = secureManager.officesReadAllowed(Security.getUser().get());
		if(office == null) {
			//TODO se offices è vuota capire come comportarsi
			office = offices.iterator().next();
		}

		rules.checkIfPermitted(office);

		List<IWrapperWorkingTimeType> wttDefault = FluentIterable
				.from(workingTimeTypeDao.getDefaultWorkingTimeType())
				.transform(wrapperFunctionFactory.workingTimeType()).toList();

		List<IWrapperWorkingTimeType> wttAllowed = FluentIterable 
				.from(office.workingTimeType)
				.transform(wrapperFunctionFactory.workingTimeType()).toList();

		 List<IWrapperWorkingTimeType> wttAllowedEnabled = Lists.newArrayList();
		 List<IWrapperWorkingTimeType> wttAllowedDisabled = Lists.newArrayList();
		 for (IWrapperWorkingTimeType wtt : wttAllowed ){
			 if (wtt.getValue().disabled) {
				 wttAllowedDisabled.add(wtt);
			 } else {
				 wttAllowedEnabled.add(wtt);
			 }
		 }

		render(wttDefault, wttAllowedEnabled, wttAllowedDisabled, office, offices);
	}

	public static void showContract(Long wttId, Long officeId) {

		WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {

			flash.error("Impossibile caricare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}

		rules.checkIfPermitted(wtt.office);

		List<Contract> contractList = wrapperFactory.create(wtt).getAssociatedActiveContract(officeId);

		render(wtt, contractList);

	}

	public static void showContractWorkingTimeType(Long wttId, Long officeId) {

		WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {

			flash.error("Impossibile caricare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}

		rules.checkIfPermitted(wtt.office);

		IWrapperWorkingTimeType wwtt = wrapperFactory.create(wtt); 

		List<ContractWorkingTimeType> cwttList = wwtt.getAssociatedPeriodInActiveContract(officeId);

		render(wtt, cwttList);

	}


	public static void insertWorkingTime(Long officeId){

		Office office = officeDao.getOfficeById(officeId);

		if(office == null) {

			flash.error("Sede non trovata. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}

		rules.checkIfPermitted(office);

		HorizontalWorkingTime horizontalPattern = new HorizontalWorkingTime();
			
		boolean horizontal = true;
		
		render(office, horizontalPattern, horizontal);

	}
	
	public static void saveHorizontal(@Valid HorizontalWorkingTime horizontalPattern,
			@Required Office office) {

		// TODO: la creazione dell'orario default ha office null.
		notFoundIfNull(office);
		
		rules.checkIfPermitted(office);
		
		WorkingTimeType wtt = workingTimeTypeDao.workingTypeTypeByDescription(
				horizontalPattern.name, Optional.fromNullable(office));
		if(wtt != null) {
			validation.addError("horizontalPattern.name", 
					"nome già presente", horizontalPattern.name);
		}
		
		if (validation.hasErrors()) {
			boolean horizontal = true;
   		    render("@insertWorkingTime", horizontalPattern, horizontal, office);
		}
		
		horizontalPattern.buildWorkingTimeType(office);

		flash.success("Orario creato con successo.");
		
		manageWorkingTime(office.id);
		
	}

	public static void save(@Valid WorkingTimeType wtt, WorkingTimeTypeDay wttd1,
			WorkingTimeTypeDay wttd2,WorkingTimeTypeDay wttd3,
			WorkingTimeTypeDay wttd4,WorkingTimeTypeDay wttd5,
			WorkingTimeTypeDay wttd6,WorkingTimeTypeDay wttd7){

		if (validation.hasErrors()){
			flash.error(ValidationHelper.errorsMessages(validation.errors()));
			manageWorkingTime(wtt.office.id);
		}
		

		rules.checkIfPermitted(wtt.office);

		wtt.save();

		workingTimeTypeManager.saveWorkingTimeType(wttd1, wtt, 1);
		workingTimeTypeManager.saveWorkingTimeType(wttd2, wtt, 2);
		workingTimeTypeManager.saveWorkingTimeType(wttd3, wtt, 3);
		workingTimeTypeManager.saveWorkingTimeType(wttd4, wtt, 4);
		workingTimeTypeManager.saveWorkingTimeType(wttd5, wtt, 5);
		workingTimeTypeManager.saveWorkingTimeType(wttd6, wtt, 6);
		workingTimeTypeManager.saveWorkingTimeType(wttd7, wtt, 7);

		flash.success("Inserito nuovo orario di lavoro '%s' per la sede %s.", wtt.description, wtt.office.name);

		manageWorkingTime(wtt.office.id);

	}
	
	public static void showHorizontal(Long wttId) {
		
		WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
		notFoundIfNull(wtt);
		
		// se non è horizontal ho sbagliato action
		Preconditions.checkState(wtt.horizontal);
		
		if (wtt.office != null) {
			rules.checkIfPermitted(wtt.office);
		}
		
		HorizontalWorkingTime horizontalPattern = new HorizontalWorkingTime(wtt);
		
		Office office = wtt.office;
		
		render(horizontalPattern, office);
	}
	
	public static void showWorkingTimeType(Long wttId) {

		WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {

			flash.error("Impossibile caricare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}

		rules.checkIfPermitted(wtt.office);

		render(wtt);
	}

	public static void delete(Long wttId){

		WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {

			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			manageWorkingTime(null);
		}

		rules.checkIfPermitted(wtt.office);

		//Prima di cancellare il tipo orario controllo che non sia associato ad alcun contratto
		if(contractDao.getAssociatedContract(wtt).size() > 0){

			flash.error("Impossibile eliminare il tipo orario %s perchè associato ad almeno un contratto. Operazione annullata", wtt.description);
			WorkingTimes.manageWorkingTime(wtt.office.id);
		}

		for(WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays) {
			wttd.delete();
		}
		wtt.delete();

		flash.success("Eliminato orario di lavoro denominato %s.", wtt.description);
		WorkingTimes.manageWorkingTime(wtt.office.id);	

	}

	public static void toggleWorkingTimeTypeEnabled(Long wttId) {

		WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {

			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			manageWorkingTime(null);
		}
		rules.checkIfPermitted(wtt.office);

		IWrapperWorkingTimeType wwtt = wrapperFactory.create(wtt);


		//Prima di disattivarlo controllo che non sia associato ad alcun contratto attivo 
		if( wtt.disabled == false && wwtt.getAssociatedActiveContract(wtt.office.id).size() > 0) {

			flash.error("Impossibile eliminare il tipo orario %s perchè attualmente associato ad almeno un contratto attivo. Operazione annullata", wtt.description);
			manageWorkingTime(wtt.office.id);
		}

		if( wtt.disabled ) {

			wtt.disabled = false;
			wtt.save();
			flash.success("Riattivato orario di lavoro denominato %s.", wtt.description);
			manageWorkingTime(wtt.office.id);
		}
		else {

			wtt.disabled = true;
			wtt.save();
			flash.success("Disattivato orario di lavoro denominato %s.", wtt.description);
			manageWorkingTime(wtt.office.id);
		}

	}


	public static void changeWorkingTimeTypeToAll(Long wttId, Long officeId) {

		WorkingTimeType wtt = workingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {

			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			manageWorkingTime(null);
		}

		Office office = officeDao.getOfficeById(officeId);
		if(office == null) {

			flash.error("La sede inerente il cambio di orario è obbligatoria. Operazione annullata.");
			manageWorkingTime(null);
		}

		rules.checkIfPermitted(office);

		List<WorkingTimeType> wttDefault = workingTimeTypeDao.getDefaultWorkingTimeType();
		List<WorkingTimeType> wttAllowed = office.getEnabledWorkingTimeType(); 

		List<WorkingTimeType> wttList = new ArrayList<WorkingTimeType>();
		wttList.addAll(wttDefault);
		wttList.addAll(wttAllowed);

		wttList.remove(wtt);

		render(wtt, wttList, office);
	}

	/**
	 * @param wttId
	 * @param wttId1
	 * @param dateFrom
	 * @param dateTo
	 */
	public static void executeChangeWorkingTimeTypeToAll(WorkingTimeType wttOld, 
			WorkingTimeType wttNew, Long officeId, LocalDate dateFrom, LocalDate dateTo) {
		
		Office office = officeDao.getOfficeById(officeId);
		
		if(dateFrom.isAfter(dateTo)){
			flash.error("Intervallo date non Valido");
			manageWorkingTime(office.id);
		}
		
		int contractChanges = 0;
		int contractError = 0;

		rules.checkIfPermitted(office);

		//L'operazione deve interessare tipi orario della stessa sede
		if(wttOld.office != null && wttNew.office != null 
				&& ! wttOld.office.id.equals(wttNew.office.id)) {

			flash.error("L'operazione di cambio orario a tutti deve coinvolgere tipi orario definiti per la stessa sede.");
			manageWorkingTime(office.id);
		}

		//Prendere tutti i contratti attivi da firstDay ad oggi
		List<Contract> contractInPeriod = 
				contractDao.getActiveContractsInPeriod(dateFrom, Optional.fromNullable(dateTo));
		JPAPlugin.closeTx(false);
		JPAPlugin.startTx(false);

		//Logica aggiornamento contratto
		for(Contract contract : contractInPeriod) {

			DateInterval contractPeriod = new DateInterval(dateFrom, dateTo);

			try {

				JPAPlugin.startTx(false);

				contract = contractDao.getContractById(contract.id);
				wttOld = workingTimeTypeDao.getWorkingTimeTypeById(wttOld.id);
				wttNew = workingTimeTypeDao.getWorkingTimeTypeById(wttNew.id);

				boolean needChanges = false;

				for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {	
					if(cwtt.workingTimeType.id.equals(wttOld.id) &&
							DateUtility.intervalIntersection(contractPeriod, new DateInterval(cwtt.beginDate, cwtt.endDate)) != null) {
						needChanges = true;
					}
				}

				if(needChanges) {

					Logger.info("need changes %s", contract.person.surname);

					List<ContractWorkingTimeType> newCwttList = new ArrayList<ContractWorkingTimeType>();

					for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) { //requires ordinata per beginDate @OrderBy

						// FIXME: secondo me la requires che siano ordinati non serve più
						// verificare.
						
						DateInterval intersection = DateUtility
								.intervalIntersection(contractPeriod, 
										new DateInterval(cwtt.beginDate, cwtt.endDate));
						
						if(cwtt.workingTimeType.id.equals(wttOld.id) && intersection != null) {

							newCwttList.addAll( splitContractWorkingTimeType(cwtt, intersection, wttNew) );
						}
						else {

							ContractWorkingTimeType copy = new ContractWorkingTimeType();
							copy.beginDate = cwtt.beginDate;
							copy.endDate = cwtt.endDate;
							copy.workingTimeType = cwtt.workingTimeType;
							newCwttList.add(copy);
						}
					}
					Logger.info("clean");
					List<ContractWorkingTimeType> newCwttListClean = cleanContractWorkingTimeType(newCwttList);
					Logger.info("replace");
					replaceContractWorkingTimeTypeList(contract, newCwttListClean);
					Logger.info("recompute");

					contractManager.recomputeContract(contract, Optional.fromNullable(dateFrom), false);

					contractChanges++;

				}

				JPAPlugin.closeTx(false);

			}
			catch (Exception e) {

				contractError++;
			}

		}

		JPAPlugin.startTx(false);
		if(contractError == 0) {
			flash.success("Operazione completata con successo. Correttamente aggiornati %s contratti.", contractChanges);
		}
		else {
			flash.error("Aggiornati correttamente %s contratti. Si sono verificati errori per %s contratti. "
					+ "Riprovare o effettuare una segnalazione.", contractChanges, contractError);
		}

		//TODO capire quale office deve essere ritornato
		manageWorkingTime(office.id);

	}

	private static List<ContractWorkingTimeType> splitContractWorkingTimeType(
			ContractWorkingTimeType cwtt, DateInterval period, WorkingTimeType wttNew) {

		List<ContractWorkingTimeType> newCwttList = new ArrayList<ContractWorkingTimeType>();

		ContractWorkingTimeType first = new ContractWorkingTimeType();
		first.workingTimeType = cwtt.workingTimeType;
		ContractWorkingTimeType middle = new ContractWorkingTimeType();
		middle.workingTimeType = wttNew;
		ContractWorkingTimeType last = new ContractWorkingTimeType();
		last.workingTimeType = cwtt.workingTimeType;

		DateInterval cwttInterval = new DateInterval(cwtt.beginDate, cwtt.endDate);

		//caso1 cwtt inizia dopo e finisce prima (interamente contenuto)	
		// Risultato dello split: MIDDLE (new)
		if(DateUtility.isIntervalIntoAnother(
				new DateInterval(cwtt.beginDate, cwtt.endDate), period)) {

			middle.beginDate = cwtt.beginDate;
			middle.endDate = cwtt.endDate;

			newCwttList.add(middle);

			return newCwttList;
		}

		//caso 2 cwtt inizia prima e finisce prima (o uguale)				
		// Risultato dello split: FIRST (old) MIDDLE (new)
		if( cwttInterval.getBegin().isBefore(period.getBegin()) 
				&& !cwttInterval.getEnd().isAfter(period.getEnd()) ) {

			first.beginDate = cwtt.beginDate;
			first.endDate = period.getBegin().minusDays(1);

			middle.beginDate = period.getBegin();
			middle.endDate = cwtt.endDate;

			newCwttList.add(first);
			newCwttList.add(middle);

			return newCwttList;
		}


		//caso 3 cwtt inizia dopo (o uguale) e finisce dopo 				
		// Risultato dello split: MIDDLE (new) LAST (old)
		if( !cwttInterval.getBegin().isBefore(period.getBegin()) 
				&& cwttInterval.getEnd().isAfter(period.getEnd()) ) {

			middle.beginDate = cwtt.beginDate;
			middle.endDate = period.getEnd();

			last.beginDate = period.getEnd().plusDays(1);
			last.endDate = cwtt.endDate;


			newCwttList.add(middle);
			newCwttList.add(last);

			return newCwttList;

		}

		//caso 4 cwtt inizia prima e finisce dopo							
		// Risultato dello split: FIRST (old) MIDDLE (new) LAST (old)
		if( cwttInterval.getBegin().isBefore(period.getBegin()) 
				&& cwttInterval.getEnd().isAfter(period.getEnd()) ) {

			first.beginDate = cwtt.beginDate;
			first.endDate = period.getBegin().minusDays(1);

			middle.beginDate = period.getBegin();
			middle.endDate = period.getEnd();

			last.beginDate = period.getEnd().plusDays(1);
			last.endDate = cwtt.endDate;

			newCwttList.add(first);
			newCwttList.add(middle);
			newCwttList.add(last);

			return newCwttList;

		}

		return newCwttList;

	}

	/**
	 * Fonde insieme due periodi consecutivi con lo stesso tipo orario
	 * @require cwttList ordinato per beginDate
	 */
	public static List<ContractWorkingTimeType> cleanContractWorkingTimeType(List<ContractWorkingTimeType> cwttList) {

		Collections.sort(cwttList);

		List<ContractWorkingTimeType> cwttListClean = new ArrayList<ContractWorkingTimeType>();

		ContractWorkingTimeType previousCwtt = null;

		boolean hasFusion = true;

		while(hasFusion) {

			hasFusion = false;

			for(ContractWorkingTimeType cwtt : cwttList) {

				if(previousCwtt==null) {

					previousCwtt = cwtt;
					continue;
				}

				if( ! previousCwtt.workingTimeType.id.equals(cwtt.workingTimeType.id)) {

					cwttListClean.add(previousCwtt);
					previousCwtt = cwtt;
				}
				else {

					hasFusion = true;

					//fusione
					ContractWorkingTimeType cwttClean = new ContractWorkingTimeType();
					cwttClean.beginDate = previousCwtt.beginDate;
					cwttClean.endDate = cwtt.endDate;
					cwttClean.workingTimeType = previousCwtt.workingTimeType;
					cwttListClean.add(cwttClean);

					previousCwtt = null;
				}
			}

			if(previousCwtt != null) {

				cwttListClean.add(previousCwtt);
			}

			previousCwtt = null;
			cwttList = cwttListClean;
			cwttListClean = new ArrayList<ContractWorkingTimeType>();

		}

		return cwttList;
	}

	/**
	 * Elimina gli esistenti ContractWorkingTimeType del contratto e li sostituisce con cwttList
	 * @param contract
	 * @param cwttList
	 */
	private static void replaceContractWorkingTimeTypeList(Contract contract, List<ContractWorkingTimeType> cwttList) {

		List<ContractWorkingTimeType> toDelete = new ArrayList<ContractWorkingTimeType>();
		for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {
			toDelete.add(cwtt);
		}

		for(ContractWorkingTimeType cwtt : toDelete) {
			cwtt.delete();
			contract.contractWorkingTimeType.remove(cwtt);
			contract.save();
		}

		for(ContractWorkingTimeType cwtt : cwttList) {

			cwtt.contract = contract;
			cwtt.save();
			contract.contractWorkingTimeType.add(cwtt);
			contract.save();
		}		

	}	

}
