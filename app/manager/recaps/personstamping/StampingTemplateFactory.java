package manager.recaps.personstamping;

import manager.cache.StampTypeManager;

import models.Stamping;

import javax.inject.Inject;

public class StampingTemplateFactory {

  private final StampTypeManager stampTypeManager;

  @Inject
  StampingTemplateFactory(StampTypeManager stampTypeManager) {
    this.stampTypeManager = stampTypeManager;
  }

  /**
   * Costruisce l'oggetto che rappresenta una timbratura da visualizzare nel tabellone timbrature.
   *
   * @param stamping timbratura del BaseModel
   * @param position la posizione all'interno della sua coppia.
   * @return la timbratura.
   */
  public StampingTemplate create(Stamping stamping, String position) {

    return new StampingTemplate(stampTypeManager, stamping, position);
  }

}
