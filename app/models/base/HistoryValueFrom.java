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

package models.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotazione per dirimere se si tratta dell'ultima revisione di 
 * un'entity (current) o di una specifica revisione nel passato (revision).
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HistoryValueFrom {

  /**
   * Tipologia di valore (current o revision) da associare all'entity
   * dove è usata questa annotazione.
   */
  HistoryFrom value();

  /**
   * Ultima versione (current) o versione ad una revisione specifica (revision).
   */
  public enum HistoryFrom {
    REVISION, CURRENT;
  }
}
