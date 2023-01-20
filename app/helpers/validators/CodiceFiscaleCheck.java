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
 * Controllo per il codice fiscale Italiano e l'unicità
 * per le persone presenti in ePAS.
 *
 * @author Marco Andreini
 * @author Cristian Lucchesi
 */
@StaticInject
public class CodiceFiscaleCheck extends Check {

  private static final int[] DISPONIBILI = {
      1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20,
      11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23 };

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
    final String value = ((String) v).toUpperCase();

    if (value.isEmpty()) {
      return true;
    }

    if (validatedObject instanceof Person) {
      val person = (Person) validatedObject;
      val other = personDao.byFiscalCode(value);

      if (other.isPresent() 
          && !Objects.equals(person.getId(), other.get().getId())) {
        setMessage("validation.codiceFiscale.alreadyPresent");
        return false;
      }
    }

    if (value.length() != 16)  {
      setMessage("validation.codiceFiscale.length");
      return false;
    }

    for (int i = 0; i < 16; i++) {

      char c = value.charAt(i);
      if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'Z')) {
        setMessage("validation.codiceFiscale.char");
        return false;
      }
    }
    int sum = 0;
    for (int i = 1; i <= 13; i += 2) {

      char c = value.charAt(i);
      if (c >= '0' && c <= '9') {
        sum += c - '0';
      } else {
        sum += c - 'A';
      }
    }
    for (int i = 0; i <= 14; i += 2) {

      int c = value.charAt(i);
      if (c >= '0' && c <= '9') {
        c = c - '0' + 'A';
      }
      sum += DISPONIBILI[c - 'A'];
    }
    if (sum % 26 + 'A' != value.charAt(15)) {
      setMessage("validation.codiceFiscale.check");
      return false;
    }
    return true;
  }
}
