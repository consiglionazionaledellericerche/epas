package dao.wrapper;

import models.Contract;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * @author marco
 *
 */
public class WrapperContract implements IWrapperContract {

	private final Contract value;

	@Inject
	WrapperContract(@Assisted Contract contract) {
		value = contract;
	}

	@Override
	public Contract getValue() {
		return value;
	}

}
