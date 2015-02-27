package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import manager.ContractManager;
import manager.WorkingTimeTypeManager;
import models.Contract;
import models.ContractWorkingTimeType;
import models.Office;
import models.User;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;

import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;
import security.SecurityRules;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

import dao.ContractDao;
import dao.OfficeDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperWorkingTimeType;
import dao.wrapper.function.WrapperModelFunctionFactory;

@With( {Secure.class, RequestInit.class} )
public class WorkingTimes extends Controller{

	
	@Inject
	static SecurityRules rules;
	
	@Inject
	static IWrapperFactory wrapperFactory;
	
	@Inject
	static WrapperModelFunctionFactory wrapperFunctionFactory; 
	
	@Inject
	static OfficeDao officeDao;
	
	public static void manageWorkingTime(Office office){
		
		Set<Office> offices = officeDao.getOfficeAllowed(Optional.<User>absent());
		if(office == null || office.id == null) {
			//TODO se offices è vuota capire come comportarsi
			office = offices.iterator().next();
		}
		
		rules.checkIfPermitted(office);
		
		List<IWrapperWorkingTimeType> wttDefault = FluentIterable
				.from(WorkingTimeTypeDao.getDefaultWorkingTimeType())
				.transform(wrapperFunctionFactory.workingTimeType()).toList();
	
		List<IWrapperWorkingTimeType> wttAllowed = FluentIterable 
				.from(office.workingTimeType)
				.transform(wrapperFunctionFactory.workingTimeType()).toList();
				
		List<IWrapperWorkingTimeType> wttList = FluentIterable 
				.from(WorkingTimeTypeDao.getAllWorkingTimeType())
				.transform(wrapperFunctionFactory.workingTimeType()).toList(); 
			
		render(wttList, wttDefault, wttAllowed, office, offices);
	}
	
	public static void showContract(Long wttId, Long officeId) {
		
		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile caricare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		rules.checkIfPermitted(wtt.office);
		
		List<Contract> contractList = wrapperFactory.create(wtt).getAssociatedActiveContract(officeId);
			
		render(wtt, contractList);
		
	}
	
	public static void showContractWorkingTimeType(Long wttId, Long officeId) {
		
		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeById(wttId);
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
		
		if(!officeDao.isSeat(office)) {
			
			flash.error("E' possibile definire tipi orario solo a livello sede. Operazione annullata.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		rules.checkIfPermitted(office);
		
		List<WorkingTimeTypeDay> wttd = new LinkedList<WorkingTimeTypeDay>();
		WorkingTimeType wtt = new WorkingTimeType();
		for(int i = 1; i < 8; i++){
			WorkingTimeTypeDay w = new WorkingTimeTypeDay();
			wttd.add(w);
		}
		render(office, wtt, wttd);
		
	}
	
	
	public static void save(
			
			Office office,
			WorkingTimeType wtt, 
			WorkingTimeTypeDay wttd1,
			WorkingTimeTypeDay wttd2,
			WorkingTimeTypeDay wttd3,
			WorkingTimeTypeDay wttd4,
			WorkingTimeTypeDay wttd5,
			WorkingTimeTypeDay wttd6,
			WorkingTimeTypeDay wttd7){
		
		if(office == null || office.id == null) {
			
			flash.error("Per inserire un tipo orario è necessario fornire una sede esistente. Operazione annullata.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		if(!officeDao.isSeat(office)) {
			
			flash.error("E' possibile definire tipi orario solo a livello sede. Operazione annullata.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		rules.checkIfPermitted(office);
		
		if(wtt.description == null || wtt.description.isEmpty()) {
			flash.error("Il campo nome tipo orario è obbligatorio. Operazione annullata");
			WorkingTimes.manageWorkingTime(wtt.office);
		}
		if(WorkingTimeTypeDao.getWorkingTimeTypeByDescription(wtt.description) != null) {
			flash.error("Il nome tipo orario è già esistente. Sceglierne un'altro. Operazione annullata");
			WorkingTimes.manageWorkingTime(wtt.office);
		}
		
		wtt.office = office;

		wtt.save();

		WorkingTimeTypeManager.saveWorkingTimeType(wttd1, wtt, 1);
		WorkingTimeTypeManager.saveWorkingTimeType(wttd2, wtt, 2);
		WorkingTimeTypeManager.saveWorkingTimeType(wttd3, wtt, 3);
		WorkingTimeTypeManager.saveWorkingTimeType(wttd4, wtt, 4);
		WorkingTimeTypeManager.saveWorkingTimeType(wttd5, wtt, 5);
		WorkingTimeTypeManager.saveWorkingTimeType(wttd6, wtt, 6);
		WorkingTimeTypeManager.saveWorkingTimeType(wttd7, wtt, 7);

		flash.success("Inserito nuovo orario di lavoro denominato %s per la sede %s.", wtt.description, office.name);
		
		manageWorkingTime(null);		//FIXME vorrei passargli office ma non funziona!!!
		
	}
	
	public static void showWorkingTimeType(Long wttId) {
		
		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile caricare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		rules.checkIfPermitted(wtt.office);
		
		render(wtt);
		
	}
	
	public static void delete(Long wttId){

		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		rules.checkIfPermitted(wtt.office);
		
		//Prima di cancellare il tipo orario controllo che non sia associato ad alcun contratto
		if(ContractDao.getAssociatedContract(wtt).size() > 0){
			
			flash.error("Impossibile eliminare il tipo orario %s perchè associato ad almeno un contratto. Operazione annullata", wtt.description);
			WorkingTimes.manageWorkingTime(wtt.office);
		}
		
		for(WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays) {
			wttd.delete();
		}
		wtt.delete();
		
		flash.success("Eliminato orario di lavoro denominato %s.", wtt.description);
		WorkingTimes.manageWorkingTime(null);	//FIXME vorrei metterci wtt.office
		
	}
	
	public static void toggleWorkingTimeTypeEnabled(Long wttId) {

		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}
		rules.checkIfPermitted(wtt.office);

		IWrapperWorkingTimeType wwtt = wrapperFactory.create(wtt);
		
		
		//Prima di disattivarlo controllo che non sia associato ad alcun contratto attivo 
		if( wtt.disabled == false && wwtt.getAssociatedActiveContract(wtt.office.id).size() > 0) {
				
			flash.error("Impossibile eliminare il tipo orario %s perchè attualmente associato ad almeno un contratto attivo. Operazione annullata", wtt.description);
			WorkingTimes.manageWorkingTime(wtt.office);
		}
		
		if( wtt.disabled ) {
			
			wtt.disabled = false;
			wtt.save();
			flash.success("Riattivato orario di lavoro denominato %s.", wtt.description);
			WorkingTimes.manageWorkingTime(null);	//FIXME vorrei passare wtt.office
		}
		else {
			
			wtt.disabled = true;
			wtt.save();
			flash.success("Disattivato orario di lavoro denominato %s.", wtt.description);
			WorkingTimes.manageWorkingTime(null); //FIXME vorrei passare wtt.office
		}

	}
	

	public static void changeWorkingTimeTypeToAll(Long wttId, Long officeId) {
		
		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		Office office = officeDao.getOfficeById(officeId);
		if(office == null) {
			
			flash.error("La sede inerente il cambio di orario è obbligatoria. Operazione annullata.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		rules.checkIfPermitted(office);
		
		List<WorkingTimeType> wttDefault = WorkingTimeTypeDao.getDefaultWorkingTimeType();
		List<WorkingTimeType> wttAllowed = office.getEnabledWorkingTimeType(); 

		List<WorkingTimeType> wttList = new ArrayList<WorkingTimeType>();
		wttList.addAll(wttDefault);
		wttList.addAll(wttAllowed);
		
		wttList.remove(wtt);
		
		render(wtt, wttList, office);
	}
	
	/**
	 * NB nelle drools questa action è considerata editPerson!!!
	 * @param wttId
	 * @param wttId1
	 * @param dateFrom
	 * @param dateTo
	 */
	public static void executeChangeWorkingTimeTypeToAll(Long wttId, Long wttId1, Long officeId, String dateFrom, String dateTo) {
		
		int contractChanges = 0;
		int contractError = 0;
		
		JPAPlugin.startTx(false);
		
		Office office = officeDao.getOfficeById(officeId);
		if(office == null) {
			
			flash.error("Fornire la sede interessata per il cambio di orario. Operazione annullata.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		rules.checkIfPermitted(office);
		
		WorkingTimeType wttOld = WorkingTimeTypeDao.getWorkingTimeTypeById(wttId);
		if(wttOld==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		WorkingTimeType wttNew = WorkingTimeTypeDao.getWorkingTimeTypeById(wttId1);
		if(wttNew==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		//L'operazione deve interessare tipi orario della stessa sede
		if(wttOld.office != null && wttNew.office != null 
				&& ! wttOld.office.id.equals(wttNew.office.id)) {
			
			flash.error("L'operazione di cambio orario a tutti deve coinvolgere tipi orario definiti per la stessa sede.");
			WorkingTimes.manageWorkingTime(null);
		}
		
		LocalDate inputBegin = null;
		LocalDate inputEnd = null;
		try {
			inputBegin = new LocalDate(dateFrom);
			if(dateTo!=null && !dateTo.equals(""))
				inputEnd = new LocalDate(dateTo);
			
		} catch(Exception e) {
			flash.error("Fornire le date in un formato valido. Operazione annullata.");
			WorkingTimes.manageWorkingTime(null);
		}	
		
		//Prendere tutti i contratti attivi da firstDay ad oggi
		List<Contract> contractInPeriod = ContractManager.getActiveContractInPeriod(inputBegin, inputEnd);
		JPAPlugin.closeTx(false);

		//Logica aggiornamento contratto
		for(Contract contract : contractInPeriod) {
			
			DateInterval contractPeriod = new DateInterval(inputBegin, inputEnd);
			
			try {

				JPAPlugin.startTx(false);

				contract = ContractDao.getContractById(contract.id);
				wttOld = WorkingTimeTypeDao.getWorkingTimeTypeById(wttOld.id);
				wttNew = WorkingTimeTypeDao.getWorkingTimeTypeById(wttNew.id);
				
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
						
						DateInterval intersection = DateUtility.intervalIntersection(contractPeriod, new DateInterval(cwtt.beginDate, cwtt.endDate));
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

					ContractManager.recomputeContract(contract, inputBegin, null);
					
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
		WorkingTimes.manageWorkingTime(null);
		
		
		
	}
	
	private static List<ContractWorkingTimeType> splitContractWorkingTimeType(ContractWorkingTimeType cwtt, DateInterval period, WorkingTimeType wttNew) {
		
		List<ContractWorkingTimeType> newCwttList = new ArrayList<ContractWorkingTimeType>();
		
		ContractWorkingTimeType first = new ContractWorkingTimeType();
		first.workingTimeType = cwtt.workingTimeType;
		ContractWorkingTimeType middle = new ContractWorkingTimeType();
		middle.workingTimeType = wttNew;
		ContractWorkingTimeType last = new ContractWorkingTimeType();
		last.workingTimeType = cwtt.workingTimeType;
		
		DateInterval cwttInterval = new DateInterval(cwtt.beginDate, cwtt.endDate);
		
		//caso1 cwtt inizia dopo e finisce prima (interamente contenuto)	Risultato dello split: MIDDLE (new)
		if(DateUtility.isIntervalIntoAnother(new DateInterval(cwtt.beginDate, cwtt.endDate), period)) {
			
			middle.beginDate = cwtt.beginDate;
			middle.endDate = cwtt.endDate;
			
			newCwttList.add(middle);
			
			return newCwttList;
		}
		
		//caso 2 cwtt inizia prima e finisce prima (o uguale)				Risultato dello split: FIRST (old) MIDDLE (new)
		if( cwttInterval.getBegin().isBefore(period.getBegin()) && !cwttInterval.getEnd().isAfter(period.getEnd()) ) {
			
			first.beginDate = cwtt.beginDate;
			first.endDate = period.getBegin().minusDays(1);
			
			middle.beginDate = period.getBegin();
			middle.endDate = cwtt.endDate;
			
			newCwttList.add(first);
			newCwttList.add(middle);
			
			return newCwttList;
		}
		
		
		//caso 3 cwtt inizia dopo (o uguale) e finisce dopo 				Risultato dello split: MIDDLE (new) LAST (old)
		if( !cwttInterval.getBegin().isBefore(period.getBegin()) && cwttInterval.getEnd().isAfter(period.getEnd()) ) {
			
			middle.beginDate = cwtt.beginDate;
			middle.endDate = period.getEnd();
			
			last.beginDate = period.getEnd().plusDays(1);
			last.endDate = cwtt.endDate;
			
			
			newCwttList.add(middle);
			newCwttList.add(last);
			
			return newCwttList;
			
		}
		
		//caso 4 cwtt inizia prima e finisce dopo							Risultato dello split: FIRST (old) MIDDLE (new) LAST (old)
		if( cwttInterval.getBegin().isBefore(period.getBegin()) && cwttInterval.getEnd().isAfter(period.getEnd()) ) {
			
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
