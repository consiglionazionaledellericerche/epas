package dao.wrapper;

import models.Contract;
import models.Person;

/**
 * @author marco
 *
 */
public interface IWrapperFactory {

	WrapperPerson create(Person person);
	WrapperContract create(Contract contract);
}
