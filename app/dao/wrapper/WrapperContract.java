package dao.wrapper;

import it.cnr.iit.epas.DateInterval;

import java.util.List;

import manager.PersonManager;
import models.Contract;
import models.ContractWorkingTimeType;
import models.VacationPeriod;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.VacationPeriodDao;

/**
 * @author marco
 *
 */
public class WrapperContract implements IWrapperContract {

	private final PersonManager personManager;
	private final Contract value;
	private final VacationPeriodDao vacationPeriodDao;

	@Inject
	WrapperContract(@Assisted Contract contract, PersonManager personManager
			,VacationPeriodDao vacationPeriodDao) {
		value = contract;
		this.personManager = personManager;
		this.vacationPeriodDao = vacationPeriodDao;
	}

	@Override
	public Contract getValue() {
		return value;
	}

	/**
	 * True se il contratto è l'ultimo contratto per mese e anno selezionati.
	 * @param month
	 * @param year
	 * @return
	 */
	@Override
	public boolean isLastInMonth(int month, int year) {
		
		List<Contract> contractInMonth = personManager.getMonthContracts(this.value.person, month, year);
		if (contractInMonth.size() == 0) {
			return false;
		}
		if (contractInMonth.get(contractInMonth.size()-1).id.equals(this.value.id)){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * La lista dei VacationPeriod associati al contratto in ordine crescente per data di inizio periodo.
	 * @param contract
	 * @return
	 */
	public List<VacationPeriod> getContractVacationPeriods() {

		List<VacationPeriod> vpList = vacationPeriodDao.getVacationPeriodByContract(this.value);
		return vpList;
	}

	/**
	 * True se il contratto è a tempo determinato.
	 * 
	 * @return
	 */
	public boolean isDefined() {

		return this.value.expireContract != null;
	}

	/**
	 * Conversione della lista dei contractWorkingtimeType da Set a List
	 * @param contract
	 * * @return
	 */
	public List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList() {
		return Lists.newArrayList(this.value.contractWorkingTimeType);
	}

	/**
	 * FIXME ha una dipendenza con DateUtility, capire se può rimanere nel modello.
	 * Utilizza la libreria DateUtils per costruire l'intervallo attivo per il contratto.
	 * @return
	 */
	public DateInterval getContractDateInterval(){
		DateInterval contractInterval;
		if(value.endContract!=null)
			contractInterval = new DateInterval(value.beginContract, value.endContract);
		else
			contractInterval = new DateInterval(value.beginContract, value.expireContract);
		return contractInterval;
	}

}
