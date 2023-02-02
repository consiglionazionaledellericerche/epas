/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.PersonShiftDay;
import models.PersonShiftDayInTrouble;
import models.enumerate.ShiftTroubles;
import models.query.QPersonShiftDayInTrouble;

/**
 * DAO per i PersonShiftDayInTrouble.
 */
public class PersonShiftDayInTroubleDao extends DaoBase {

  @Inject
  PersonShiftDayInTroubleDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Il personShiftDayDao, se esiste, relativo ai parametri passati al metodo.
   *
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