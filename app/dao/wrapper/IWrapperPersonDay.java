package dao.wrapper;

import models.Contract;
import models.PersonDay;

/**
 * @author alessandro
 *
 */
public interface IWrapperPersonDay extends IWrapperModel<PersonDay> {

	Contract getPersonDayContract();

	boolean isHoliday();

	boolean isFixedTimeAtWork();

}
