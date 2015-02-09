package dao.wrapper;

import java.util.List;

import com.google.inject.Inject;

import manager.PersonManager;
import models.Contract;

/**
 * @author marco
 *
 */
public class WrapperContract implements IWrapperContract {

	private final Contract value;
	private final PersonManager personManager;

	@Inject
	WrapperContract(Contract contract, PersonManager personManager) {
		value = contract;
		this.personManager = personManager;
	}

	@Override
	public Contract getValue() {
		return value;
	}

	public boolean isLastInMonth(int month, int year) {
		List<Contract> contractInMonth = personManager
				.getMonthContracts(value.person, month, year);
		if (contractInMonth.size() == 0)
			return false;
		if (contractInMonth.get(contractInMonth.size()-1).id.equals(value.id))
			return true;
		else
			return false;
	}
}
