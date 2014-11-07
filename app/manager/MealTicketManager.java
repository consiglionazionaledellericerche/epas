package manager;

import java.util.List;

import models.MealTicket;
import play.Logger;

import com.google.common.collect.Lists;

/**
 * Manager per MealTicket
 * @author alessandro
 *
 */
public class MealTicketManager {
	
	/**
	 * Genera la lista di MealTicket appartenenti al blocco identificato dal codice codeBlock
	 * @param codeBlock il codice del blocco di meal ticket
	 * @param dimBlock la dimensione del blocco di meal ticket
	 * @return la lista di MealTicket appartenenti al blocco.
	 */
	public static List<MealTicket> buildBlockMealTicket(Integer codeBlock, Integer dimBlock) {

		List<MealTicket> mealTicketList = Lists.newArrayList();

		for(int i=1; i<=dimBlock; i++) {

			MealTicket mealTicket = new MealTicket();
			mealTicket.block = codeBlock;
			mealTicket.number = i;

			if(i<10) 
				mealTicket.code = codeBlock + "0" + i;
			else
				mealTicket.code = "" + codeBlock + i;

			mealTicketList.add(mealTicket);

			Logger.info(mealTicket.code);
		}

		return  mealTicketList;
	}

}
