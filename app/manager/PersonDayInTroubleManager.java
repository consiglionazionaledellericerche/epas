package manager;

import models.PersonDay;
import models.PersonDayInTrouble;
import play.Logger;

public class PersonDayInTroubleManager {

	/**
	 * 
	 * @param pd
	 * @param cause
	 */
	public static void insertPersonDayInTrouble(PersonDay pd, String cause)
	{
		if(pd.troubles==null || pd.troubles.size()==0)
		{	
			//se non esiste lo creo
			Logger.info("Nuovo PersonDayInTrouble %s %s %s - %s - %s", pd.person.id, pd.person.name, pd.person.surname, pd.date, cause);
			PersonDayInTrouble trouble = new PersonDayInTrouble(pd, cause);
			trouble.save();
			pd.troubles.add(trouble);
			pd.save();
			return;
		}
		else
		{
			//se esiste lo setto fixed = false;
			pd.troubles.get(0).fixed = false;
			pd.troubles.get(0).cause = cause;
			pd.troubles.get(0).save();
			pd.save();
		}

	}
}
