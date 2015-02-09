package manager;

import models.WorkingTimeType;
import models.WorkingTimeTypeDay;
import dao.WorkingTimeTypeDayDao;

public class WorkingTimeTypeManager {

	/**
	 * 
	 * @return se il tale giorno è di riposo o meno a seconda del workingtimetype
	 */
	public static boolean getHolidayFromWorkinTimeType(int dayOfWeek, WorkingTimeType wtt){
		boolean holiday = false;
		WorkingTimeTypeDay wttd = WorkingTimeTypeDayDao.getWorkingTimeTypeDayByDayOfWeek(wtt, dayOfWeek);

		holiday = wttd.holiday;
		return holiday;
	}
	
	
	/**
	 * 
	 * @param dayOfWeek il giorno della settimana
	 * @return il WorkingTimeTypeDay in quel giorno della settimana
	 */
	public static WorkingTimeTypeDay getWorkingTimeTypeDayFromDayOfWeek(int dayOfWeek, WorkingTimeType wtt){
		
		return wtt.workingTimeTypeDays.get(dayOfWeek-1);
	}
	
	

	/**
	 * 
	 * @param dayOfWeek
	 * @param wtt
	 * @return il numero di minuti minimo di lavoro per poter usufruire della pausa pranzo
	 */
	public int getMinimalTimeForLunch(int dayOfWeek, WorkingTimeType wtt){
		int minTimeForLunch = 0;
		WorkingTimeTypeDay wttd = WorkingTimeTypeDayDao.getWorkingTimeTypeDayByDayOfWeek(wtt, dayOfWeek);

		minTimeForLunch = wttd.mealTicketTime;
		return minTimeForLunch;
	}
	

	
	/**
	 * 
	 * @param dayOfWeek
	 * @param wtt
	 * @return il numero di minuti di pausa pranzo per quel giorno per quell'orario
	 */
	public int getBreakTime(int dayOfWeek, WorkingTimeType wtt){
		int breakTime = 0;
		WorkingTimeTypeDay wttd = WorkingTimeTypeDayDao.getWorkingTimeTypeDayByDayOfWeek(wtt, dayOfWeek);

		breakTime = wttd.breakTicketTime;
		
		return breakTime;
	}

	/**
	 * 
	 * @param wttd
	 * @param wtt
	 * @param dayOfWeek
	 */
	public static void saveWorkingTimeType(WorkingTimeTypeDay wttd, WorkingTimeType wtt, int dayOfWeek){
		
		wttd.dayOfWeek = dayOfWeek;
		wttd.workingTimeType = wtt;
		wttd.save();
	}
	
		
}
