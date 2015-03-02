package manager.recaps.personStamping;

import javax.inject.Inject;

import dao.StampingDao;
import manager.PersonDayManager;
import models.PersonDay;


public class PersonStampingDayRecapFactory {

	private final PersonDayManager personDayManager;
	private final StampingTemplateFactory stampingTemplateFactory;
	private final StampingDao stampingDao;
	
	@Inject
	PersonStampingDayRecapFactory(PersonDayManager personDayManager,
			StampingTemplateFactory stampingTemplateFactory,
			StampingDao stampingDao) {
		this.personDayManager = personDayManager;
		this.stampingTemplateFactory = stampingTemplateFactory;
		this.stampingDao = stampingDao;
	}
	
	/**
	 * Costruisce il riepilogo mensile delle timbrature. 
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public PersonStampingDayRecap create(PersonDay personDay, int numberOfInOut) {
		
		return new PersonStampingDayRecap(personDayManager, 
				stampingTemplateFactory, stampingDao, 
				personDay, numberOfInOut);
	}
	
}
