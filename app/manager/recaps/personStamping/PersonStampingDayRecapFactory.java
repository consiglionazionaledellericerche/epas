package manager.recaps.personStamping;

import java.util.List;

import javax.inject.Inject;

import manager.ConfGeneralManager;
import manager.PersonDayManager;
import manager.cache.StampTypeManager;
import models.Contract;
import models.PersonDay;

import com.google.common.base.Optional;

import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;


public class PersonStampingDayRecapFactory {

	private final WorkingTimeTypeDao workingTimeTypeDao;	
	public final IWrapperFactory wrapperFactory;
	private final PersonDayManager personDayManager;
	private final StampingTemplateFactory stampingTemplateFactory;
	public final StampTypeManager stampTypeManager;
	private final ConfGeneralManager confGeneralManager;

	@Inject
	PersonStampingDayRecapFactory(PersonDayManager personDayManager,
			StampingTemplateFactory stampingTemplateFactory,
			StampTypeManager stampTypeManager, IWrapperFactory wrapperFactory,
			WorkingTimeTypeDao workingTimeTypeDao,
			ConfGeneralManager confGeneralManager) {
		this.personDayManager = personDayManager;
		this.stampingTemplateFactory = stampingTemplateFactory;
		this.stampTypeManager = stampTypeManager;
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
	public PersonStampingDayRecap create(PersonDay personDay, int numberOfInOut,
			Optional<List<Contract>> monthContracts) {

		return new PersonStampingDayRecap(personDayManager, 
				stampingTemplateFactory, stampTypeManager, wrapperFactory,
				workingTimeTypeDao, confGeneralManager, 
				personDay, numberOfInOut, monthContracts);
	}

}
