/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import java.time.LocalDate;
import models.Person;
import models.flows.Affiliation;
import models.flows.query.QAffiliation;
import models.flows.query.QGroup;

/**
 * Condizioni riusabili sulle afferenze ai gruppi, coerenti con
 * {@link Person#getApprovalGroups(LocalDate)}: un'afferenza conta se la data è compresa nel suo
 * periodo (estremi inclusi) e il gruppo è attivo; tra quelle attive di una persona contano solo
 * quelle con la percentuale più alta.
 */
public final class AffiliationFilter {

  private AffiliationFilter() {
  }

  /**
   * Sottoquery con gli id delle persone per cui il responsabile indicato gestisce, alla data,
   * uno dei gruppi di riferimento (quelli con la percentuale di afferenza più alta).
   */
  public static JPQLQuery<Long> personIdsInChargeOf(Person manager, LocalDate date) {
    final QAffiliation affiliation = new QAffiliation("affiliationInCharge");
    final QGroup group = new QGroup("groupInCharge");
    final QAffiliation other = new QAffiliation("otherAffiliation");
    final QGroup otherGroup = new QGroup("otherGroup");
    return JPAExpressions.select(affiliation.person.id)
        .from(affiliation).join(affiliation.group, group)
        .where(group.manager.eq(manager),
            activeAt(affiliation, group, date),
            percentage(affiliation).goe(
                JPAExpressions.select(percentage(other).max())
                    .from(other).join(other.group, otherGroup)
                    .where(other.person.eq(affiliation.person),
                        activeAt(other, otherGroup, date))));
  }

  /**
   * Condizione vera se l'afferenza è attiva alla data e il suo gruppo è attivo.
   */
  public static BooleanExpression activeAt(QAffiliation affiliation, QGroup group,
      LocalDate date) {
    return affiliation.beginDate.loe(date)
        .and(affiliation.endDate.isNull().or(affiliation.endDate.goe(date)))
        .and(group.endDate.isNull().or(group.endDate.after(date)));
  }

  private static NumberExpression<java.math.BigDecimal> percentage(QAffiliation affiliation) {
    return Expressions.numberTemplate(java.math.BigDecimal.class, "coalesce({0}, {1})",
        affiliation.percentage, Affiliation.FULL_PERCENTAGE);
  }
}
