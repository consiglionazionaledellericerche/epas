package dao.wrapper;

import java.util.List;

import models.Contract;
import models.VacationPeriod;

public interface IWrapperContract extends IWrapperModel<Contract> {

	boolean isLastInMonth(int month, int year);

	List<VacationPeriod> getContractVacationPeriods();

}
