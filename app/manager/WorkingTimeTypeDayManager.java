package manager;

import models.WorkingTimeTypeDay;

public class WorkingTimeTypeDayManager {

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
