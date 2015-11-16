package manager.recaps.personStamping;

import manager.PersonDayManager;
import manager.cache.StampTypeManager;
import models.PersonDay;
import models.Stamping;

import javax.inject.Inject;

public class StampingTemplateFactory {

	private final PersonDayManager personDayManager;
	private final StampTypeManager stampTypeManager;

	@Inject
	StampingTemplateFactory(PersonDayManager personDayManager,
			StampTypeManager stampTypeManager) {
		this.personDayManager = personDayManager;
		this.stampTypeManager = stampTypeManager;
	}

	/**
	 * @param person
	 * @param year
	 * @param month
	 * @return il riepilogo mensile delle timbrature.
	 */
	public StampingTemplate create(Stamping stamping, int index,
			PersonDay pd, int pairId, String pairPosition) {

		return new StampingTemplate(personDayManager, stampTypeManager,
				stamping, index, pd, pairId, pairPosition);
	}

}
