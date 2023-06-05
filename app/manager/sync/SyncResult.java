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

package manager.sync;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contiene le informazioni relative ad una richiesta di sincronizzazione.
 *
 * @author Cristian Lucchesi
 */
@Data
@NoArgsConstructor
public class SyncResult {

  private boolean success = true;
  private List<String> messages = Lists.newArrayList();
  
  /**
   * Combina i risultati delle due sincronizzazioni.
   * La sincronizzazione è success solo se lo sono entrambe.
   */
  public SyncResult add(SyncResult other) {
    success = success && other.success;
    messages.addAll(other.getMessages());
    return this;
  }
  
  public SyncResult add(String message) {
    messages.add(message);
    return this;
  }

  public SyncResult setFailed() {
    success = false;
    return this;
  }  
  
  @Override
  public String toString() {
    return String.format("Sicronizzazione %s. %s",
        success ? "avvenuta con successo" : "fallita",
        Joiner.on(" ").join(messages));
  }
}