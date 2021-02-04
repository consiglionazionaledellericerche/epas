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

package dao.filter;

import com.google.common.base.Optional;
import com.querydsl.core.BooleanBuilder;
import models.query.QPerson;

/**
 * Filtri per le query.
 */
public class QFilters {

  /**
   * Filtro sul nome del QPerson.
   */
  public BooleanBuilder filterNameFromPerson(QPerson person, Optional<String> name) {

    BooleanBuilder condition = new BooleanBuilder();

    if (name.isPresent() && !name.get().trim().isEmpty()) {
      condition.andAnyOf(person.name.startsWithIgnoreCase(name.get()),
          person.surname.startsWithIgnoreCase(name.get()));
    }

    return condition;
  }
}
