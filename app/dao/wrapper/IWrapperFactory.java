package dao.wrapper;

import models.CompetenceCode;
import models.Contract;
import models.ContractMonthRecap;
import models.ContractWorkingTimeType;
import models.Office;
import models.Person;
import models.PersonDay;
import models.TimeSlot;
import models.WorkingTimeType;

/**
 * Interfaccia per generico factory di alcuni componenti di ePAS.
 *
 * @author marco
 */
public interface IWrapperFactory {

  IWrapperPerson create(Person person);

  IWrapperContract create(Contract contract);

  IWrapperWorkingTimeType create(WorkingTimeType wtt);
  
  IWrapperTimeSlot create(TimeSlot ts);

  IWrapperCompetenceCode create(CompetenceCode cc);

  IWrapperOffice create(Office office);

  IWrapperPersonDay create(PersonDay pd);

  IWrapperContractMonthRecap create(ContractMonthRecap cmr);

  IWrapperContractWorkingTimeType create(ContractWorkingTimeType cwtt);
}
