package manager;

import models.WorkingTimeTypeDay;

public class WorkingTimeTypeDayManager {

	/**
	 * 
	 * @param workingTime
	 * @param wttd
	 * setta il tempo di lavoro di un certo WorkingTimeTypeDay
	 */
	public static void setWorkingTime(Integer workingTime, WorkingTimeTypeDay wttd)
	{
		if(workingTime==null)
			wttd.workingTime = 0;
		else
			wttd.workingTime = workingTime;
	}
	
	/**
	 * 
	 * @param breakTicketTime
	 * @param wttd
	 * setta il tempo di pausa pranzo per un certo WorkingTimeTypeDay
	 */
	public static void setBreakTicketTime(Integer breakTicketTime, WorkingTimeTypeDay wttd)
	{
		if(breakTicketTime==null)
			wttd.breakTicketTime = 0;
		else
			wttd.breakTicketTime = breakTicketTime;
		
		if(wttd.breakTicketTime < 30)
			wttd.breakTicketTime = 30;
	}
	
	/**
	 * 
	 * @param mealTicketTime
	 * @param wttd
	 * setta il tempo per avere il buono pasto per un certo WorkingTimeTypeDay
	 */
	public static void setMealTicketTime(Integer mealTicketTime, WorkingTimeTypeDay wttd)
	{
		if(mealTicketTime==null)
			wttd.mealTicketTime = 0;
		else
			wttd.mealTicketTime = mealTicketTime;
	}
	
	/**
	 * 
	 * @param holiday
	 * @param wttd
	 */
	public static void setHoliday(String holiday, WorkingTimeTypeDay wttd)
	{
		if(holiday != null && holiday.equals("true"))
			wttd.holiday = true;
		else
			wttd.holiday = false;
	}
	
	/**
	 * True se è ammesso il calcolo del buono pasto per la persona, false altrimenti (il campo mealTicketTime
	 *  che rappresenta il tempo minimo di lavoro per avere diritto al buono pasto è pari a zero)
	 * @return
	 */
	public static boolean mealTicketEnabled(WorkingTimeTypeDay wttd) {
		if( wttd.holiday )
			return false;
		
		if( wttd.mealTicketTime > 0)
			return true;
		else 
			return false;
		
	}
}
