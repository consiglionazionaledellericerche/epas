package manager;

import models.WorkingTimeType;
import models.WorkingTimeTypeDay;

public class WorkingTimeTypeManager {

	/**
	 * 
	 * @param wttd
	 * @param wtt
	 * @param dayOfWeek
	 */
	public void saveWorkingTimeType(WorkingTimeTypeDay wttd,
			WorkingTimeType wtt, int dayOfWeek){
		
		wttd.dayOfWeek = dayOfWeek;
		wttd.workingTimeType = wtt;
		wttd.save();
	}
	
		
}
