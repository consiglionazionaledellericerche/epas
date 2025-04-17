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


package dao;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.Office;
import models.Person;
import models.PersonsOffices;
import models.absences.Absence;
import models.query.QPersonsOffices;
import org.joda.time.LocalDate;

/**
 * Dao per le query sull'affiliazione persona/ufficio.
 * 
 */
public class PersonsOfficesDao extends DaoBase {

  private final IWrapperFactory factory;

  @Inject
  PersonsOfficesDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory factory) {
    super(queryFactory, emp);
    this.factory = factory;
  }
  
  /**
   * Ritorna la lista delle affiliazioni della persona in ordine decrescente.
   * 
   * @param person la persona di cui si richiedono le affiliazioni alle sedi
   * @return la lista delle affiliazioni della persona in ordine decrescente.
   */
  public List<PersonsOffices> listByPerson(Person person) {
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    return getQueryFactory().selectFrom(personsOffices)
        .where(personsOffices.person.eq(person))
        .orderBy(personsOffices.beginDate.desc()).fetch();
  }
  
  /**
   * Ritorna la lista delle affiliazioni alla sede in ordine decrescente.
   * 
   * @param office la sede di cui si richiedono le affiliazioni di personale
   * @return la lista delle affiliazioni alla sede office.
   */
  public List<PersonsOffices> listByOffice(Office office) {
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    return getQueryFactory().selectFrom(personsOffices)
        .where(personsOffices.office.eq(office))
        .orderBy(personsOffices.beginDate.desc()).fetch();
  }
  
  /**
   * L'affiliazione della persona alla sede nell'anno/mese passati come parametro.
   * @param person la persona da cercare
   * @param office la sede su cui cercare
   * @param year l'anno di riferimento
   * @param month il mese di riferimento
   * @return l'associazione periodica della persona alla sede se esiste
   */
  public Optional<PersonsOffices> affiliation(Person person, Office office, int year, int month) {
    LocalDate beginMonth = new LocalDate(year, month, 1);
    LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    return Optional.fromNullable(getQueryFactory().selectFrom(personsOffices)
        .where(personsOffices.person.eq(person)
            .and(personsOffices.office.eq(office)).and(personsOffices.beginDate.loe(endMonth))
            .andAnyOf(personsOffices.endDate.isNull(), personsOffices.endDate.goe(beginMonth)))
        .fetchFirst());
  }
  
  /**
   * Ritorna l'affiliazione della persona nel periodo compreso tra begin e end.
   * 
   * @param person la persona di cui cercare l'affiliazione
   * @param begin l'inizio del periodo
   * @param end la fine del periodo
   * @return l'affiliazione di una persona nel periodo compreso tra begin e end.
   */
  public Optional<PersonsOffices> affiliationByPeriod(Person person, LocalDate begin, 
      LocalDate end) {
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    return Optional.fromNullable(getQueryFactory().selectFrom(personsOffices)
        .where(personsOffices.person.eq(person)
            .and(personsOffices.beginDate.loe(begin)
                .andAnyOf(personsOffices.endDate.isNull(), personsOffices.endDate.goe(end))))
        .fetchFirst());
  }
  
  /**
   * La lista delle affiliazioni alla sede passata come parametro in questo momento.
   * 
   * @param office la sede di cui si vogliono le affiliazioni
   * @return la lista delle affiliazioni alla sede passata come parametro in questo momento.
   */
  public List<PersonsOffices> affiliationByCurrentOffice(List<Office> officeList) {
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    return getQueryFactory().selectFrom(personsOffices).where(personsOffices.office.in(officeList)
        .and(personsOffices.beginDate.before(LocalDate.now())
            .andAnyOf(personsOffices.endDate.isNull(), personsOffices.endDate.after(LocalDate.now())))).fetch();
  }
  
  /**
   * Ritorna la lista delle affiliazioni di una persona nell'anno/mese passati come parametro.
   * 
   * @param person la persona di cui si cerca l'affiliazione
   * @param year l'anno di riferimento
   * @param month il mese di riferimento 
   * @return la lista delle affiliazioni della persona nell'anno/mese passati come parametro.
   */
  public List<PersonsOffices> monthlyAffiliations(Person person, int year, int month) {
    final QPersonsOffices personsOffices = QPersonsOffices.personsOffices;
    LocalDate beginMonth = new LocalDate(year, month, 1);
    LocalDate endMonth = beginMonth.dayOfMonth().withMaximumValue();
    return getQueryFactory().selectFrom(personsOffices)
        .where(personsOffices.person.eq(person)
            .and(personsOffices.beginDate.loe(endMonth))
            .andAnyOf(personsOffices.endDate.isNull(), personsOffices.endDate.goe(beginMonth))).fetch();
  }
}
