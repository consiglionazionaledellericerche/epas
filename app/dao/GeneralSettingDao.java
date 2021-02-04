/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.Optional;
import javax.persistence.EntityManager;
import models.GeneralSetting;
import models.query.QGeneralSetting;

/**
 * DAO per le impostazioni generali di ePAS.
 * 
 * @author Cristian Lucchesi
 *
 */
public class GeneralSettingDao extends DaoBase {

  @Inject
  GeneralSettingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * In caso non siano ancora mai state salvate, le restituisce nuove.
   *
   * @return le impostazioni generali.
   */
  public GeneralSetting generalSetting() {
    return Optional.ofNullable(queryFactory
        .selectFrom(QGeneralSetting.generalSetting).fetchOne())
        .orElseGet(GeneralSetting::new);
  }
}