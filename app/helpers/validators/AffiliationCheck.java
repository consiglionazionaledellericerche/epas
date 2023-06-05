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

package helpers.validators;

import models.flows.Affiliation;
import play.data.validation.Check;

/**
 * Controlla che l'affiliazione di una persona sia corretta, 
 * cioè non si sovrapponga con una già presente per lo stesso
 * gruppo.
 *
 * @author Cristian Lucchesi
 */
public class AffiliationCheck extends Check {

  @Override
  public boolean isSatisfied(Object validatedObject, Object value) {
    if (!(validatedObject instanceof Affiliation)) {
      return false;
    }
    final Affiliation affiliation = (Affiliation) validatedObject;
    if (affiliation.getPerson().getAffiliations().stream()
        .filter(a -> !a.id.equals(affiliation.getId()))
        .anyMatch(aff -> aff.overlap(affiliation))) {
      setMessage("validation.affiliation.membershipAlreadyPresent");
      return false;
    }
    return true;
  }

}
