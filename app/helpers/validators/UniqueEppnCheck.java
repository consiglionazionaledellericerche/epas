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

package helpers.validators;

import com.google.inject.Inject;
import common.injection.StaticInject;
import dao.PersonDao;
import java.util.Objects;
import lombok.val;
import models.Person;
import play.data.validation.Check;

/**
 * Controlla l'unicità del codice ePPN.
 *
 * @author Cristian Lucchesi
 */
@StaticInject
public class UniqueEppnCheck extends Check {

	@Inject
	static PersonDao personDao;

	@Override
	public boolean isSatisfied(Object validatedObject, Object v) {
		if (v == null) {
			return true;
		}
		if (!(v instanceof String)) {
			return false;
		}
		final String value = (String) v;

		if (value.isEmpty()) {
			return true;
		}

		if (validatedObject instanceof Person) {
		  val person = (Person) validatedObject;
		  val other = personDao.byEppn(value);

		  if (other.isPresent() 
		      && !Objects.equals(person.getId(), other.get().getId())) {
        setMessage("validation.eppn.alreadyPresent");
        return false;
		  }
		}

			return true;
	}
}
