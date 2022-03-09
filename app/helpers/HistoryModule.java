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
import common.injection.AutoRegister;
import javax.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

/**
 * Module which provides the AuditReader.
 *
 * @author Marco Andreini
 */
@AutoRegister
public class HistoryModule extends AbstractModule {

  /**
   * Fornisce tramite injection un AuditReader.
   */
  @Provides
  public AuditReader getAuditReader(EntityManager em) {
    return AuditReaderFactory.get(em);
  }

  @Override
  protected void configure() {
  }
}
