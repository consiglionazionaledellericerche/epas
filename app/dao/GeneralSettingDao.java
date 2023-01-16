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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.Optional;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import models.GeneralSetting;
import models.query.QGeneralSetting;

/**
 * DAO per le impostazioni generali di ePAS.
 *
 * @author Cristian Lucchesi
 *
 */
@Singleton
@Slf4j
public class GeneralSettingDao extends DaoBase {

  LoadingCache<String, GeneralSetting> generalSettingCache;
  private static final String cacheKey = "gs";
  
  @Inject
  GeneralSettingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
    this.generalSettingCache = CacheBuilder.newBuilder().build(
        new CacheLoader<String, GeneralSetting>() {
          public GeneralSetting load(String key) {
          return Optional.ofNullable(queryFactory
              .selectFrom(QGeneralSetting.generalSetting).fetchOne())
              .orElseGet(GeneralSetting::new);
          }
        });
  }

  /**
   * In caso non siano ancora mai state salvate, le restituisce nuove.
   *
   * @return le impostazioni generali.
   */
  public GeneralSetting generalSetting()  {
    return generalSettingCache.getUnchecked(cacheKey);
  }
  
  /**
   * Invalida la cache sui GeneralSetting.
   */
  public void generalSettingInvalidate() {
    log.info("Cache invalidata");
    generalSettingCache.invalidate(cacheKey);
  }
}