package manager.recaps.competence;

import com.google.common.base.Optional;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.wrapper.IWrapperFactory;

import javax.inject.Inject;

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
  public Optional<PersonMonthCompetenceRecap> create(Contract contract, int month, int year) {

    try {
      return Optional.fromNullable(new PersonMonthCompetenceRecap(competenceCodeDao,
          competenceDao, wrapperFactory, contract, month, year));

    } catch (Exception ex) {
      
      //impossibile costruire il recap... inizializzazione mancante.
      return Optional.<PersonMonthCompetenceRecap>absent();
    }
  }

}
