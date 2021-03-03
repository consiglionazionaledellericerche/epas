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

import com.google.common.base.Verify;
import com.google.common.util.concurrent.Striped;
import java.util.concurrent.locks.Lock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Wrapper per gli Striped Lock perché non funziona
 * la @Inject nei controller di un oggetto con i generic.
 *
 * @author Cristian Lucchesi
 *
 */
@Getter 
@RequiredArgsConstructor
public class StripedLock {

  private final Striped<Lock> lock;
  
  public Lock get(Object lockId) {
    Verify.verifyNotNull(lock);
    return lock.get(lockId);
  }
}
