package manager.recaps.personstamping;

import com.google.common.base.Optional;

import dao.wrapper.IWrapperFactory;

import manager.PersonDayManager;
import manager.cache.StampTypeManager;
import manager.configurations.ConfigurationManager;

import models.Contract;
import models.PersonDay;

import java.util.List;

import javax.inject.Inject;


public class PersonStampingDayRecapFactory {

  public final IWrapperFactory wrapperFactory;
  public final StampTypeManager stampTypeManager;
  private final PersonDayManager personDayManager;
  private final StampingTemplateFactory stampingTemplateFactory;
  private final ConfigurationManager configurationManager;

  @Inject
  PersonStampingDayRecapFactory(PersonDayManager personDayManager,
      StampingTemplateFactory stampingTemplateFactory,
      StampTypeManager stampTypeManager, IWrapperFactory wrapperFactory,
      ConfigurationManager configurationManager) {
    this.personDayManager = personDayManager;
    this.stampingTemplateFactory = stampingTemplateFactory;
    this.stampTypeManager = stampTypeManager;
    this.wrapperFactory = wrapperFactory;
    this.configurationManager = configurationManager;
  }

  /**
   * Costruisce l'oggetto che rappresenta un giorno nel tabellone timbrature.
   *
   * @param personDay          personDay
   * @param numberOfInOut      numero di colonne del tabellone a livello mensile.
   * @param considerExitingNow se considerare nel calcolo l'uscita in questo momento
   * @param monthContracts     riepiloghi mensili (servono a capire se il giorno è da considere).
   * @return personStampingDayRecap
   */
  public PersonStampingDayRecap create(PersonDay personDay, int numberOfInOut,
      boolean considerExitingNow, Optional<List<Contract>> monthContracts) {

    return new PersonStampingDayRecap(personDayManager, stampingTemplateFactory, stampTypeManager,
        wrapperFactory, configurationManager,
        personDay, numberOfInOut, considerExitingNow, monthContracts);
  }

}
