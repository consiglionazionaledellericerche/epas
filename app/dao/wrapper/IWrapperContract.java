package dao.wrapper;

import models.Contract;

public interface IWrapperContract extends IWrapperModel<Contract> {

	boolean isLastInMonth(int month, int year);

}
