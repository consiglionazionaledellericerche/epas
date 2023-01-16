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
package dao;

import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.TimeVariation;
import models.query.QTimeVariation;
import org.joda.time.LocalDate;

/**
 * DAO per le TimeVariation.
 */
public class TimeVariationDao extends DaoBase {

  @Inject
  TimeVariationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Metodo che ritorna la lista delle variazioni temporali nell'arco di tempo specificato relative
   * alla persona passata come parametro.
   *
   * @param person la persona cui sono state assegnate le variazioni temporali
   * @param begin la data di inizio da cui cercare
   * @param end la data di fine da cui cercare
   * @return la lista delle variazioni temporali assegnate alla persona per recuperare i riposi
   *     compensativi per chiusura ente nell'intervallo temporale specificato.
   */
  public List<TimeVariation> getByPersonAndPeriod(Person person, LocalDate begin, LocalDate end) {
    final QTimeVariation timeVariation = QTimeVariation.timeVariation1;
    return getQueryFactory().selectFrom(timeVariation)
        .where(timeVariation.absence.personDay.person.eq(person)
            .and(timeVariation.dateVariation.between(begin, end)))
        .fetch();
  }

  /**
   * Metodo che ritorna la variazione oraria recuperata tramite l'id.
   *
   * @param timeVariationId l'identificativo della variazione oraria
   * @return la variazione oraria associata all'id passato come parametro.
   */
  public TimeVariation getById(long timeVariationId) {
    final QTimeVariation timeVariation = QTimeVariation.timeVariation1;
    return getQueryFactory().selectFrom(timeVariation)
        .where(timeVariation.id.eq(timeVariationId)).fetchOne();
  }
}