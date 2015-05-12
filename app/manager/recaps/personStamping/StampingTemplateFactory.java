package manager.recaps.personStamping;

import javax.inject.Inject;

import manager.PersonDayManager;
import models.PersonDay;
import models.Stamping;
import dao.StampingDao;

public class StampingTemplateFactory {

	private final PersonDayManager personDayManager;
	private final StampingDao stampingDao;

	@Inject
	StampingTemplateFactory(PersonDayManager personDayManager,
			StampingDao stampingDao) {
		this.personDayManager = personDayManager;
		this.stampingDao = stampingDao;
	}

	/**
	 * @param person
	 * @param year
	 * @param month
	 * @return il riepilogo mensile delle timbrature.
	 */
	public StampingTemplate create(Stamping stamping, int index,
			PersonDay pd, int pairId, String pairPosition) {

		return new StampingTemplate(personDayManager, stampingDao,
				stamping, index, pd, pairId, pairPosition);
	}

}
