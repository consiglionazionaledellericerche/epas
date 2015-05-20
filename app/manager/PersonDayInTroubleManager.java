package manager;

import models.PersonDay;
import models.PersonDayInTrouble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersonDayInTroubleManager {

	private final static Logger log = LoggerFactory.getLogger(PersonDayInTroubleManager.class);
	/**
	 * 
	 * @param pd
	 * @param cause
	 */
	public void insertPersonDayInTrouble(PersonDay pd, String cause)
	{
		if(pd.troubles==null || pd.troubles.size()==0)
		{	
			//se non esiste lo creo
			log.info("Nuovo PersonDayInTrouble {} - {} - {}", 
					new Object[]{pd.person.getFullname(), pd.date, cause});
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
