package controllers;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.LocalDate;

import models.Contract;
import models.ContractWorkingTimeType;
import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import play.Logger;
import play.data.validation.Required;
import play.db.jpa.JPAPlugin;
import play.mvc.Controller;
import play.mvc.With;

@With( {Secure.class, RequestInit.class} )
public class WorkingTimes extends Controller{

	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void manageWorkingTime(){
		
		List<WorkingTimeType> wttDefault = WorkingTimeType.getDefaultWorkingTimeTypes();
		List<WorkingTimeType> wttAllowed = WorkingTimeType.getOfficesWorkingTimeTypes(Security.getOfficeAllowed()); 
		
		
		List<WorkingTimeType> wttList = WorkingTimeType.findAll();
				
	
		render(wttList, wttDefault, wttAllowed);
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void showContractWorkingTimeType(Long wttId) {
		
		WorkingTimeType wtt = WorkingTimeType.findById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile caricare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime();
		}
		List<Contract> contractList = wtt.getAssociatedActiveContract();
	
		render(wtt, contractList);
		
	}
	
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void insertWorkingTime(){
		List<WorkingTimeTypeDay> wttd = new LinkedList<WorkingTimeTypeDay>();
		WorkingTimeType wtt = new WorkingTimeType();
		for(int i = 1; i < 8; i++){
			WorkingTimeTypeDay w = new WorkingTimeTypeDay();
			wttd.add(w);
		}
		render(wtt, wttd);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void save(
			WorkingTimeType wtt, 
			WorkingTimeTypeDay wttd1,
			WorkingTimeTypeDay wttd2,
			WorkingTimeTypeDay wttd3,
			WorkingTimeTypeDay wttd4,
			WorkingTimeTypeDay wttd5,
			WorkingTimeTypeDay wttd6,
			WorkingTimeTypeDay wttd7){
			
			if(wtt.description == null || wtt.description.isEmpty())
			{
				flash.error("Il campo nome tipo orario è obbligatorio. Operazione annullata");
				WorkingTimes.manageWorkingTime();
			}
			if( WorkingTimeType.find("byDescription", wtt.description).first() != null)
			{
				flash.error("Il nome tipo orario è già esistente. Sceglierne un'altro. Operazione annullata");
				WorkingTimes.manageWorkingTime();
			}
			
			wtt.office = Security.getUser().person.office;
			
			wtt.save();
			
			wttd1.dayOfWeek = 1;
			wttd1.workingTimeType = wtt;
			wttd1.save();
			
			wttd2.dayOfWeek = 2;
			wttd2.workingTimeType = wtt;
			wttd2.save();
			
			wttd3.dayOfWeek = 3;
			wttd3.workingTimeType = wtt;
			wttd3.save();
			
			wttd4.dayOfWeek = 4;
			wttd4.workingTimeType = wtt;
			wttd4.save();
			
			wttd5.dayOfWeek = 5;
			wttd5.workingTimeType = wtt;
			wttd5.save();
			
			wttd6.dayOfWeek = 6;
			wttd6.workingTimeType = wtt;
			wttd6.save();
			
			wttd7.dayOfWeek = 7;
			wttd7.workingTimeType = wtt;
			wttd7.save();
		
			flash.success("Inserito nuovo orario di lavoro denominato %s.", wtt.description);
			WorkingTimes.manageWorkingTime();

	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void showWorkingTimeType(Long wttId) {
		
		WorkingTimeType wtt = WorkingTimeType.findById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile caricare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime();
		}
		
		render(wtt);
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void delete(Long wttId){

		WorkingTimeType wtt = WorkingTimeType.findById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime();
		}
		
		//Prima di cancellare il tipo orario controllo che non sia associato ad alcun contratto
		if( wtt.getAssociatedContract().size() > 0) {
			
			flash.error("Impossibile eliminare il tipo orario %s perchè associato ad almeno un contratto. Operazione annullata", wtt.description);
			WorkingTimes.manageWorkingTime();
		}
		
		for(WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays) {
			wttd.delete();
		}
		wtt.delete();
		
		flash.success("Eliminato orario di lavoro denominato %s.", wtt.description);
		WorkingTimes.manageWorkingTime();
		
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void toggleWorkingTimeTypeEnabled(Long wttId) {

		WorkingTimeType wtt = WorkingTimeType.findById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime();
			
		}

		//Prima di disattivarlo controllo che non sia associato ad alcun contratto attivo 
		if( wtt.disabled == false && wtt.getAssociatedActiveContract().size() > 0) {
		
			flash.error("Impossibile eliminare il tipo orario %s perchè attualmente associato ad almeno un contratto attivo. Operazione annullata", wtt.description);
			WorkingTimes.manageWorkingTime();
		}
		
		if( wtt.disabled ) {
			
			wtt.disabled = false;
			wtt.save();
			flash.success("Riattivato orario di lavoro denominato %s.", wtt.description);
			WorkingTimes.manageWorkingTime();
		}
		else {
			
			wtt.disabled = true;
			wtt.save();
			flash.success("Disattivato orario di lavoro denominato %s.", wtt.description);
			WorkingTimes.manageWorkingTime();
		}

	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void changeWorkingTimeTypeToAll(Long wttId) {
		
		WorkingTimeType wtt = WorkingTimeType.findById(wttId);
		if(wtt==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime();
			
		}
		
		List<WorkingTimeType> wttDefault = WorkingTimeType.getDefaultWorkingTimeTypes();
		List<WorkingTimeType> wttAllowed = WorkingTimeType.getOfficesWorkingTimeTypes(Security.getOfficeAllowed()); 
		
		List<WorkingTimeType> wttList = new ArrayList<WorkingTimeType>();
		wttList.addAll(wttDefault);
		wttList.addAll(wttAllowed);
		
		
		render(wtt, wttList);
	}
	
	@Check(Security.INSERT_AND_UPDATE_WORKINGTIME)
	public static void executeChangeWorkingTimeTypeToAll(Long wttId, Long wttId1,  String dateFrom, String dateTo) {
		
		int contractChanges = 0;
		int contractError = 0;
		
		JPAPlugin.startTx(false);

		WorkingTimeType wttOld = WorkingTimeType.findById(wttId);
		if(wttOld==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime();
			
		}

		
		WorkingTimeType wttNew = WorkingTimeType.findById(wttId1);
		if(wttNew==null) {
			
			flash.error("Impossibile trovare il tipo orario specificato. Riprovare o effettuare una segnalazione.");
			WorkingTimes.manageWorkingTime();
			
		}
		
		LocalDate begin = null;
		LocalDate end = null;
		try {
			begin = new LocalDate(dateFrom);
			if(dateTo!=null)
				end = new LocalDate(dateTo);
			
		} catch(Exception e) {
			flash.error("Fornire le date in un formato valido. Operazione annullata.");
			WorkingTimes.manageWorkingTime();
		}
		
		
		
		//Prendere tutti i contratti attivi da firstDay ad oggi
		List<Contract> contractInPeriod = Contract.getActiveContractInPeriod(begin, end);
		
		DateInterval period = new DateInterval(begin, end);

		JPAPlugin.closeTx(false);

		//Logica aggiornamento contratto
		for(Contract contract : contractInPeriod) {

			
			
			try {

				JPAPlugin.startTx(false);


				contract = Contract.findById(contract.id);
				wttOld = WorkingTimeType.findById(wttOld.id);
				wttNew = WorkingTimeType.findById(wttNew.id);

				boolean needChanges = false;

				for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) {	
					if(cwtt.workingTimeType.id.equals(wttOld.id) &&
							DateUtility.intervalIntersection(period, cwtt.getCwttDateInterval()) != null) {
						needChanges = true;
					}
				}

				if(needChanges) {

					Logger.info("need changes %s", contract.person.surname);

					List<ContractWorkingTimeType> newCwttList = new ArrayList<ContractWorkingTimeType>();

					for(ContractWorkingTimeType cwtt : contract.contractWorkingTimeType) { //requires ordinata per beginDate @OrderBy

						if(cwtt.workingTimeType.id.equals(wttOld.id) &&
								DateUtility.intervalIntersection(period, cwtt.getCwttDateInterval()) != null) {

							newCwttList.addAll( splitContractWorkingTimeType(cwtt, period, wttNew) );
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

					contract.recomputeContract(begin);
					
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
		WorkingTimes.manageWorkingTime();
		
		
		
	}
	
	private static List<ContractWorkingTimeType> splitContractWorkingTimeType(ContractWorkingTimeType cwtt, DateInterval period, WorkingTimeType wttNew) {
		
		List<ContractWorkingTimeType> newCwttList = new ArrayList<ContractWorkingTimeType>();
		
		ContractWorkingTimeType first = new ContractWorkingTimeType();
		first.workingTimeType = cwtt.workingTimeType;
		ContractWorkingTimeType middle = new ContractWorkingTimeType();
		middle.workingTimeType = wttNew;
		ContractWorkingTimeType last = new ContractWorkingTimeType();
		last.workingTimeType = cwtt.workingTimeType;
		
		DateInterval cwttInterval = cwtt.getCwttDateInterval();
		
		//caso1 cwtt inizia dopo e finisce prima (interamente contenuto)
		if(DateUtility.isIntervalIntoAnother(cwtt.getCwttDateInterval(), period)) {
			
			middle.beginDate = cwtt.beginDate;
			middle.endDate = cwtt.endDate;
			
			newCwttList.add(middle);
			
			return newCwttList;
		}
		
		//caso 2 cwtt inizia prima e finisce prima
		if( cwttInterval.getBegin().isBefore(period.getBegin()) && cwttInterval.getEnd().isBefore(period.getEnd()) ) {
			
			first.beginDate = cwtt.beginDate;
			first.endDate = period.getBegin().minusDays(1);
			
			middle.beginDate = period.getBegin();
			middle.endDate = cwtt.endDate;
			
			newCwttList.add(first);
			newCwttList.add(middle);
			
			return newCwttList;
		}
		
		
		//caso 3 cwtt inizia dopo e finisce dopo 
		if( cwttInterval.getBegin().isAfter(period.getBegin()) && cwttInterval.getEnd().isAfter(period.getEnd()) ) {
			
			last.beginDate = period.getEnd().plusDays(1);
			last.endDate = cwtt.endDate;
			
			middle.beginDate = cwtt.beginDate;
			middle.endDate = period.getEnd();
			
			newCwttList.add(middle);
			newCwttList.add(last);
			
			return newCwttList;
			
		}
		
		//caso 4 cwtt inizia prima e finisce dopo
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
