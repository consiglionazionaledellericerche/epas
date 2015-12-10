package manager.recaps.competence;

import javax.inject.Inject;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.wrapper.IWrapperFactory;
import models.Contract;

public class PersonMonthCompetenceRecapFactory {

  private final CompetenceCodeDao competenceCodeDao;
  private final CompetenceDao competenceDao;
  private final IWrapperFactory wrapperFactory;

  @Inject
  PersonMonthCompetenceRecapFactory(CompetenceCodeDao competenceCodeDao,
                                    CompetenceDao competenceDao, IWrapperFactory wrapperFactory) {
    this.competenceCodeDao = competenceCodeDao;
    this.competenceDao = competenceDao;
    this.wrapperFactory = wrapperFactory;
  }

  /**
   * Il riepilogo competenze per il dipendente.
   *
   * @param contract requires not null.
   */
  public PersonMonthCompetenceRecap create(Contract contract, int month,
                                           int year) {

    return new PersonMonthCompetenceRecap(competenceCodeDao,
            competenceDao, wrapperFactory, contract, month, year);
  }

}
