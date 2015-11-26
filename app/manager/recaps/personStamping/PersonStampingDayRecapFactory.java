package manager.recaps.personStamping;

import com.google.common.base.Optional;

import dao.WorkingTimeTypeDao;
import dao.wrapper.IWrapperFactory;

import manager.ConfGeneralManager;
import manager.PersonDayManager;
import manager.PersonManager;
import manager.cache.StampTypeManager;

import models.Contract;
import models.PersonDay;

import java.util.List;

import javax.inject.Inject;


public class PersonStampingDayRecapFactory {

  public final IWrapperFactory wrapperFactory;
  public final StampTypeManager stampTypeManager;
  private final WorkingTimeTypeDao workingTimeTypeDao;
  private final PersonDayManager personDayManager;
  private final StampingTemplateFactory stampingTemplateFactory;
  private final ConfGeneralManager confGeneralManager;
  private PersonManager personManager;

  @Inject
  PersonStampingDayRecapFactory(PersonDayManager personDayManager,
                                PersonManager personManager,
                                StampingTemplateFactory stampingTemplateFactory,
                                StampTypeManager stampTypeManager, IWrapperFactory wrapperFactory,
                                WorkingTimeTypeDao workingTimeTypeDao,
                                ConfGeneralManager confGeneralManager) {
    this.personDayManager = personDayManager;
    this.personManager = personManager;
    this.stampingTemplateFactory = stampingTemplateFactory;
    this.stampTypeManager = stampTypeManager;
    this.wrapperFactory = wrapperFactory;
    this.workingTimeTypeDao = workingTimeTypeDao;
    this.confGeneralManager = confGeneralManager;
  }

  /**
   * Costruisce
   *
   * @return il riepilogo mensile delle timbrature.
   */
  public PersonStampingDayRecap create(PersonDay personDay, int numberOfInOut,
                                       Optional<List<Contract>> monthContracts) {

    return new PersonStampingDayRecap(personDayManager, personManager,
            stampingTemplateFactory, stampTypeManager, wrapperFactory,
            workingTimeTypeDao, confGeneralManager,
            personDay, numberOfInOut, monthContracts);
  }

}
