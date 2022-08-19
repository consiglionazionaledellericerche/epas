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

package helpers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import common.injection.AutoRegister;
import dao.PersonDao;
import lombok.extern.slf4j.Slf4j;
import manager.attestati.service.CertificationService;
import manager.attestati.service.CertificationsComunication;
import manager.attestati.service.ICertificationService;

/**
 * Modulo per la gestione dei valori nella cache.
 *
 * @author Daniele Murgia
 */
@AutoRegister
@Slf4j
public class CacheModule extends AbstractModule {
  
 
  /**
   * Crea una istanza singleton della Cache.
   */
  @Provides 
  @Singleton
  public CacheValues getCacheValues(
      CertificationsComunication certification, ICertificationService certService,
      PersonDao personDao) {
    CacheValues cacheValues = 
        new CacheValues(certification, certService, personDao);
    log.info("Creata nuova cacheValues: {}", cacheValues);
    return cacheValues;
  }

  /**
   * Autoconfigura questo modulo.
   */
  @Override
  public void configure() {
    bind(ICertificationService.class).to(CertificationService.class);
  }
}
