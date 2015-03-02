package manager.recaps.personStamping;

import javax.inject.Inject;

import manager.PersonDayManager;
import models.PersonDay;


public class PersonStampingDayRecapFactory {

	private final PersonDayManager personDayManager;
	private final StampingTemplateFactory stampingTemplateFactory;
	
	@Inject
	PersonStampingDayRecapFactory(PersonDayManager personDayManager,
			StampingTemplateFactory stampingTemplateFactory) {
		this.personDayManager = personDayManager;
		this.stampingTemplateFactory = stampingTemplateFactory;
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
				stampingTemplateFactory, personDay, numberOfInOut);
	}
	
}
