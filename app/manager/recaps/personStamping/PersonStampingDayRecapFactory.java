package manager.recaps.personStamping;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.PersonDayManager;
import models.PersonDay;
import dao.StampingDao;
import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;


public class PersonStampingDayRecapFactory {

	private final WorkingTimeTypeDao workingTimeTypeDao;	
	public final IWrapperFactory wrapperFactory;
	private final PersonDayManager personDayManager;
	private final StampingTemplateFactory stampingTemplateFactory;
	public final StampingDao stampingDao;
	private final ConfGeneralManager confGeneralManager;

	@Inject
	PersonStampingDayRecapFactory(PersonDayManager personDayManager,
			StampingTemplateFactory stampingTemplateFactory,
			StampingDao stampingDao, IWrapperFactory wrapperFactory,
			WorkingTimeTypeDao workingTimeTypeDao,
			ConfGeneralManager confGeneralManager) {
		this.personDayManager = personDayManager;
		this.stampingTemplateFactory = stampingTemplateFactory;
		this.stampingDao = stampingDao;
		this.wrapperFactory = wrapperFactory;
		this.workingTimeTypeDao = workingTimeTypeDao;
		this.confGeneralManager = confGeneralManager;
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
				workingTimeTypeDao,personDay, numberOfInOut,confGeneralManager);
	}

}
