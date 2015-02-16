package dao.wrapper;

import models.Contract;
import models.Person;
import models.WorkingTimeType;

/**
 * @author marco
 *
 */
public interface IWrapperFactory {

	IWrapperPerson create(Person person);
	IWrapperContract create(Contract contract);
	IWrapperWorkingTimeType create(WorkingTimeType wtt);
}
