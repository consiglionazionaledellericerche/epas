package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import javax.persistence.EntityManager;
import models.PersonShiftDay;
import models.PersonShiftDayInTrouble;
import models.enumerate.ShiftTroubles;
import models.query.QPersonShiftDayInTrouble;

public class PersonShiftDayInTroubleDao extends DaoBase {

  @Inject
  PersonShiftDayInTroubleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Il personShiftDayDao, se esiste, relativo ai parametri passati al metodo.
   * @param pd il personShiftDay per cui si ricerca il trouble
   * @param trouble la causa per cui si ricerca il trouble
   * @return il personShiftDayInTrouble, se esiste, relativo ai parametri passati al metodo.
   */
  public Optional<PersonShiftDayInTrouble> getPersonShiftDayInTroubleByType(
      PersonShiftDay pd, ShiftTroubles trouble) {
    final QPersonShiftDayInTrouble pdit = QPersonShiftDayInTrouble.personShiftDayInTrouble;
    final PersonShiftDayInTrouble result = getQueryFactory()
        .selectFrom(pdit)
        .where(pdit.personShiftDay.eq(pd).and(pdit.cause.eq(trouble))).fetchOne();
    return Optional.fromNullable(result);
  }
}
