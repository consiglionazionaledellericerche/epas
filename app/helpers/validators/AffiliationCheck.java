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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import models.flows.Affiliation;
import play.data.validation.Check;

/**
 * Controlla che l'affiliazione di una persona sia corretta, 
 * cioè non si sovrapponga con una già presente per lo stesso
 * gruppo e che, in nessun giorno del suo periodo, la somma delle
 * percentuali delle afferenze attive della persona superi il 100%.
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
    if (affiliation.getPerson() == null || affiliation.getBeginDate() == null) {
      // persona e data di inizio obbligatorie sono verificate dagli altri vincoli
      return true;
    }
    if (affiliation.getPerson().getAffiliations().stream()
        .filter(a -> a != affiliation
            && (a.getId() == null || !a.getId().equals(affiliation.getId())))
        .anyMatch(aff -> aff.overlap(affiliation))) {
      setMessage("validation.affiliation.membershipAlreadyPresent");
      return false;
    }
    if (maxTotalPercentage(affiliation).compareTo(Affiliation.FULL_PERCENTAGE) > 0) {
      setMessage("validation.affiliation.percentageExceeded");
      return false;
    }
    return true;
  }

  /**
   * La somma massima giornaliera delle percentuali delle afferenze della persona nel periodo
   * dell'affiliazione, compresa l'affiliazione stessa. La somma cambia solo all'inizio di
   * un'afferenza, quindi basta valutarla nelle date di inizio che cadono nel periodo.
   */
  public static BigDecimal maxTotalPercentage(Affiliation affiliation) {
    final List<Affiliation> others = affiliation.getPerson().getAffiliations().stream()
        .filter(a -> a != affiliation
            && (a.getId() == null || !a.getId().equals(affiliation.getId())))
        .filter(a -> a.overlap(affiliation.getRange()))
        .collect(Collectors.toList());
    return Stream.concat(Stream.of(affiliation.getBeginDate()),
            others.stream().map(Affiliation::getBeginDate))
        .filter(affiliation::contains)
        .map(date -> totalAt(affiliation, others, date))
        .max(BigDecimal::compareTo)
        .orElse(affiliation.getEffectivePercentage());
  }

  private static BigDecimal totalAt(Affiliation affiliation, List<Affiliation> others,
      LocalDate date) {
    return others.stream().filter(a -> a.isActiveAt(date))
        .map(Affiliation::getEffectivePercentage)
        .reduce(affiliation.getEffectivePercentage(), BigDecimal::add);
  }

}
