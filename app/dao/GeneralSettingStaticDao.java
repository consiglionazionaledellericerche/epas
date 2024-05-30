/*
 * Copyright (C) 2024  Consiglio Nazionale delle Ricerche
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

import java.util.Optional;

import javax.inject.Inject;

import com.querydsl.jpa.JPQLQueryFactory;

import common.injection.StaticInject;
import models.GeneralSetting;
import models.query.QGeneralSetting;

/**
 * DAO per le impostazioni generali di ePAS con metodi statici utilizzabili nelle drools.
 *
 * @author Cristian Lucchesi
 *
 */
@StaticInject
public class GeneralSettingStaticDao {

  @Inject
  private static JPQLQueryFactory queryFactory;

  //Utilizzato nelle drools per verificare se gli amministrativi possono
  //inserire il personale nell'anagrafica di ePAS
  public static boolean isPersonCreationEnabled() {
    return Optional.ofNullable(queryFactory
        .selectFrom(QGeneralSetting.generalSetting).fetchOne())
        .orElseGet(GeneralSetting::new).isPersonCreationEnabled();
  }
}