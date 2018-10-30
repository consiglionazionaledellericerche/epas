package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.PersonShiftDay;
import models.PersonShiftDayInTrouble;
import models.enumerate.ShiftTroubles;
import models.query.QPersonShiftDayInTrouble;

import javax.persistence.EntityManager;

public class PersonShiftDayInTroubleDao extends DaoBase{

  @Inject
  PersonShiftDayInTroubleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  
  /**
   * 
   * @param pd il personShiftDay per cui si ricerca il trouble
   * @param trouble la causa per cui si ricerca il trouble
   * @return il personShiftDayInTrouble, se esiste, relativo ai parametri passati al metodo.
   */
  public Optional<PersonShiftDayInTrouble> getPersonShiftDayInTroubleByType(
      PersonShiftDay pd, ShiftTroubles trouble) {
    QPersonShiftDayInTrouble pdit = QPersonShiftDayInTrouble.personShiftDayInTrouble;
    final JPQLQuery query = getQueryFactory()
        .from(pdit)
        .where(pdit.personShiftDay.eq(pd).and(pdit.cause.eq(trouble)));
    return Optional.fromNullable(query.singleResult(pdit));
  }
}
