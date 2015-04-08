package manager.recaps.personStamping;

import javax.inject.Inject;

import manager.PersonDayManager;
import models.PersonDay;
import dao.StampingDao;
import dao.wrapper.IWrapperFactory;


public class PersonStampingDayRecapFactory {

	private final PersonDayManager personDayManager;
	private final StampingTemplateFactory stampingTemplateFactory;
	private final StampingDao stampingDao;
	private final IWrapperFactory wrapperFactory;
	
	@Inject
	PersonStampingDayRecapFactory(PersonDayManager personDayManager,
			StampingTemplateFactory stampingTemplateFactory,
			StampingDao stampingDao, IWrapperFactory wrapperFactory) {
		this.personDayManager = personDayManager;
		this.stampingTemplateFactory = stampingTemplateFactory;
		this.stampingDao = stampingDao;
		this.wrapperFactory = wrapperFactory;
	}
	
	/**
	 * Costruisce 
	 * @param person
	 * @param year
	 * @param month
	 * @return il riepilogo mensile delle timbrature. 
	 */
	public PersonStampingDayRecap create(PersonDay personDay, int numberOfInOut) {
		
		return new PersonStampingDayRecap(personDayManager, 
				stampingTemplateFactory, stampingDao, wrapperFactory,
				personDay, numberOfInOut);
	}
	
}
