package manager.recaps.personStamping;

import manager.PersonDayManager;
import manager.cache.StampTypeManager;

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
   * Costruisce l'oggetto che rappresenta una timbratura da visualizzare nel tabellone timbrature.
   * @param stamping timbratura del BaseModel
   * @param position la posizione all'interno della sua coppia.
   * @return la timbratura.
   */
  public StampingTemplate create(Stamping stamping, String position) {

    return new StampingTemplate(personDayManager, stampTypeManager,
            stamping, position);
  }

}
