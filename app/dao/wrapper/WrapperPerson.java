package dao.wrapper;

import manager.ContractManager;
import models.Contract;
import models.ContractStampProfile;
import models.Person;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import dao.ContractDao;

/**
 * @author marco
 *
 */
public class WrapperPerson implements IWrapperPerson {

	private final Person value;
	private final ContractManager contractManager;
	private final ContractDao contractDao;
	private Optional<Contract> currentContract;

	@Inject
	WrapperPerson(@Assisted Person person,	ContractManager contractManager,
			ContractDao contractDao) {
		this.value = person;
		this.contractManager = contractManager;
		this.contractDao = contractDao;
	}

	@Override
	public Person getValue() {
		return value;
	}

	public Optional<Contract> getCurrentContract() {
		if (this.currentContract == null) {
			this.currentContract = Optional.fromNullable(contractDao.getContract(LocalDate.now(), value));
		}
		return this.currentContract;
	}

	public ContractStampProfile getCurrentContractStampProfile() {
		final Optional<Contract> ocontract = getCurrentContract();
		if (ocontract.isPresent()) {
			return contractManager.getContractStampProfileFromDate(ocontract.get(),
				LocalDate.now());
		}
		return null;
	}
}
