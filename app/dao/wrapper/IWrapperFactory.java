package dao.wrapper;

import models.CompetenceCode;
import models.Contract;
import models.Office;
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
	IWrapperCompetenceCode create(CompetenceCode cc);
	IWrapperOffice create(Office office);
}
