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

import com.google.common.util.concurrent.Striped;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import common.injection.AutoRegister;

/**
 * Fornisce l'accesso singleton ai lock dell'applicazione.
 *
 * @author Cristian Lucchesi
 *
 */
@AutoRegister
public class LockModule implements Module {
 
  private static int LOCK_POOL_SIZE = 1024;
  
  /**
   * Fornisce via injection uno StripedLock.
   */
  @Provides
  @Singleton
  public StripedLock getStripedLock() {
    return new StripedLock(Striped.lock(LOCK_POOL_SIZE));
  }
  
  @Override
  public void configure(Binder binder) {
    //Auto-generated method stub
  }

}
