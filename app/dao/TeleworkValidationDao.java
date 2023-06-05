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

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Person;
import models.TeleworkValidation;
import models.query.QTeleworkValidation;

/**
 * Dao per i controlli sul telelavoro.
 *
 * @author dario
 *
 */
public class TeleworkValidationDao extends DaoBase {

  @Inject
  TeleworkValidationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna la lista delle richieste di telelavoro approvate.
   *
   * @param person la persona di cui cercare le richieste
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return la lista delle richiest di telelavoro approvate.
   */
  public Optional<TeleworkValidation> byPersonYearAndMonth(Person person, int year, int month) {
    final QTeleworkValidation teleworkValidation = QTeleworkValidation.teleworkValidation;

    final JPQLQuery<TeleworkValidation> query = getQueryFactory(
        ).selectFrom(teleworkValidation).where(teleworkValidation.person.eq(person)
            .and(teleworkValidation.year.eq(year).and(teleworkValidation.month.eq(month)
                .and(teleworkValidation.approved.isTrue()
                    .and(teleworkValidation.approvationDate.isNotNull())))));
    return Optional.fromNullable(query.fetchFirst());
  }

  /**
   * Ritorna le validazioni precedenti all'anno/mese passato come parametro.
   *
   * @param person la persona di cui si cercano le validazioni
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return le validazioni precedenti all'anno/mese passato come parametro.
   */
  public List<TeleworkValidation> previousValidations(Person person, int year, int month) {
    final QTeleworkValidation teleworkValidation = QTeleworkValidation.teleworkValidation;

    final JPQLQuery<TeleworkValidation> query = getQueryFactory()
        .selectFrom(teleworkValidation).where(teleworkValidation.person.eq(person)
            .and(teleworkValidation.year.eq(year).and(teleworkValidation.month.loe(month)
                .and(teleworkValidation.approved.isTrue()
                    .and(teleworkValidation.approvationDate.isNotNull())))));
    return query.fetch();
  }
  
  /**
   * Ritorna la lista di tutte le validazioni di telelavoro relative alla persona passata.
   *
   * @param person la persona di cui si richiedono le validazioni passate
   * @return la lista di tutte le validazioni di telelavoro relative alla persona passata.
   */
  public List<TeleworkValidation> allPersonValidations(Person person) {
    final QTeleworkValidation teleworkValidation = QTeleworkValidation.teleworkValidation;
    
    final JPQLQuery<TeleworkValidation> query = getQueryFactory()
        .selectFrom(teleworkValidation).where(teleworkValidation.person.eq(person))
        .orderBy(teleworkValidation.year.desc(), teleworkValidation.month.desc());
    return query.fetch();
  }
  
  /**
   * Ritorna la validazione, se esiste, con id passato come parametro.
   *
   * @param id l'identificativo della validazione da ricercare
   * @return la validazione, se esiste, con id passato come parametro.
   */
  public Optional<TeleworkValidation> getValidationById(Long id) {
    final QTeleworkValidation teleworkValidation = QTeleworkValidation.teleworkValidation;
    return Optional.fromNullable(getQueryFactory()
        .selectFrom(teleworkValidation).where(teleworkValidation.id.eq(id)).fetchFirst());
  }
}
