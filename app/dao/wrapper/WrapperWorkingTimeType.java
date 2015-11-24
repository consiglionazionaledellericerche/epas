package dao.wrapper;

import java.util.ArrayList;
import java.util.List;

import manager.ContractManager;
import models.Contract;
import models.ContractWorkingTimeType;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;

/**
 * @author alessandro
 *
 */
public class WrapperWorkingTimeType implements IWrapperWorkingTimeType {

	private final WorkingTimeType value;
	private final ContractManager contractManager;
	private final ContractDao contractDao;

	@Inject
	WrapperWorkingTimeType(@Assisted WorkingTimeType wtt,
			ContractManager contractManager, ContractDao contractDao) {
		this.value = wtt;
		this.contractManager = contractManager;
		this.contractDao = contractDao;
	}

	@Override
	public WorkingTimeType getValue() {
		return value;
	}

	/**
	 * I contratti attivi che attualmente hanno impostato il WorkingTimeType.
	 * @param officeId
	 * @return 
	 */
	@Override
	public List<Contract> getAssociatedActiveContract(Long officeId) {

		List<Contract> contractList = new ArrayList<Contract>();

		LocalDate today = new LocalDate();

		List<Contract> activeContract = 
				contractDao.getActiveContractsInPeriod(today, Optional.fromNullable(today));

		for(Contract contract : activeContract) {

			if( !contract.person.office.id.equals(officeId))
				continue;

			ContractWorkingTimeType current = contractManager
					.getContractWorkingTimeTypeFromDate(contract, today);
			if(current.workingTimeType.id.equals(value.id))
				contractList.add(contract);
		}

		return contractList;
	}

	/**
	 * Ritorna i periodi di orario associati ai contratti attualmente attivi.
	 * @param officeId
	 * @return 
	 */
	@Override
	public List<ContractWorkingTimeType> getAssociatedPeriodInActiveContract(Long officeId) {

		List<ContractWorkingTimeType> cwttList = new ArrayList<ContractWorkingTimeType>();

		LocalDate today = new LocalDate();

		List<Contract> activeContract = 
				contractDao.getActiveContractsInPeriod(today, Optional.fromNullable(today));

		for(Contract contract : activeContract) {

			//TODO 	questa restrizione andrebbe fatta dentro activeContract
			if( !contract.person.office.id.equals(officeId))	
				continue;

			for(ContractWorkingTimeType cwtt: contract.contractWorkingTimeType) {

				if(cwtt.workingTimeType.id.equals(value.id))
					cwttList.add(cwtt);	
			}
		}

		return cwttList;
	}

	@Override
	public List<Contract> getAssociatedContract() {

		//TODO
		//PER la delete quindi per adesso permettiamo l'eliminazione 
		//solo di contratti particolari di office bisogna controllare 
		//che this non sia default ma abbia l'associazione con office

		List<Contract> contractList = contractDao.getContractListByWorkingTimeType(value);

		return contractList;
	}
}
