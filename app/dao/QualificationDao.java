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

import static com.querydsl.core.group.GroupBy.groupBy;

import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Provider;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Qualification;
import models.absences.query.QAbsenceType;
import models.enumerate.QualificationMapping;
import models.query.QQualification;

/**
 * Dao per l'accesso alle informazioni delle Qualification.
 *
 * @author Dario Tagliaferri
 */
public class QualificationDao extends DaoBase {

  @Inject
  QualificationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * La lista delle qualifiche a seconda dei parametri passati.
   *
   * @param qualification (opzionale) la qualifica da cercare
   * @param idQualification (opzionale) l'id della qualifica da cercare
   * @param findAll se voglio cercare tutti
   * @return la lista di qualifiche a seconda dei parametri passati: nel caso in cui il booleano sia
   *     "true" viene ritornata l'intera lista di qualifiche. Nel caso sia presente la qualifica 
   *     che si vuole ritornare, viene ritornata sempre una lista, ma con un solo elemento, 
   *     corrispondente al criterio di ricerca. Nel caso invece in cui si voglia una lista di 
   *     elementi sulla base dell'id, si controllerà il parametro idQualification, se presente, 
   *     che determinerà una lista di un solo elemento corrispondente ai criteri di ricerca. 
   *     Ritorna null nel caso che non dovesse essere soddisfatta alcuna delle opzioni di chiamata.
   */
  public List<Qualification> getQualification(
      Optional<Integer> qualification, Optional<Long> idQualification, boolean findAll) {
    final BooleanBuilder condition = new BooleanBuilder();
    QQualification qual = QQualification.qualification1;
    final JPQLQuery<Qualification> query = getQueryFactory().selectFrom(qual);
    if (findAll) {
      return query.fetch();
    }
    if (qualification.isPresent()) {
      condition.and(qual.qualification.eq(qualification.get()));
      query.where(condition);
      return query.fetch();
    }
    if (idQualification.isPresent()) {
      condition.and(qual.id.eq(idQualification.get()));
      query.where(condition);
      return query.fetch();
    }
    return null;

  }

  /**
   * La qualifica, se esiste, del livello passato come parametro.
   *
   * @param qualification il livello della qualifica da cercare.
   * @return la qualificica corrispondente al livello indicato.
   */
  public Optional<Qualification> byQualification(Integer qualification) {

    Preconditions.checkNotNull(qualification);

    QQualification qual = QQualification.qualification1;
    final Qualification result = getQueryFactory().selectFrom(qual)
        .where(qual.qualification.eq(qualification)).fetchOne();

    return Optional.fromNullable(result);

  }

  /**
   * La qualifica, se esiste, dal id passato come parametro.
   *
   * @param id della qualifica da cercare.
   * @return la qualificica corrispondente al livello indicato.
   */
  public Optional<Qualification> byId(Long id) {

    Preconditions.checkNotNull(id);

    QQualification qual = QQualification.qualification1;
    final Qualification result = getQueryFactory().selectFrom(qual)
        .where(qual.id.eq(id)).fetchOne();

    return Optional.fromNullable(result);

  }
  /**
   * Ritorna tutte le qualifiche presenti sul db.
   *
   * @return tutte le qualifiche presenti nel sistema.
   */
  public List<Qualification> findAll() {
    final QQualification qual = QQualification.qualification1;
    return getQueryFactory().selectFrom(qual).fetch();
  }

  /**
   * Ritorna la mappa di intero/qualifica.
   *
   * @return Tutte le qualifiche epas come mappa qualification.qualification -> qualification.
   */
  public Map<Integer, Qualification> allQualificationMap() {
    final QQualification qualification = QQualification.qualification1;
    return getQueryFactory().from(qualification)
        .transform(groupBy(qualification.qualification).as(qualification));
  }

  /**
   * Le qualifiche con quel mapping.
   *
   * @param mapping mapping
   * @return qualifiche.
   */
  public List<Qualification> getByQualificationMapping(QualificationMapping mapping) {
    QQualification qual = QQualification.qualification1;
    return getQueryFactory()
        .selectFrom(qual)
        .where(qual.qualification.goe(mapping.getRange().lowerEndpoint())
            .and(qual.qualification.loe(mapping.getRange().upperEndpoint())))
        .fetch();
  }
  
  /**
   * Verifica se esiste una qualifica con valore maggiore di 10.
   *
   * 10 è il valore massimo previsto per gli enti di ricerca. 
   */
  public Boolean qualificationGreaterThan10Exist() {
    QQualification qual = QQualification.qualification1;
    return getQueryFactory()
        .selectFrom(qual)
        .where(qual.qualification.gt(10))
        .select(qual.count()).fetchOne() > 0;
  }
  
  /**
   * Restituise la qualifica con il numero più alto.
   */
  public int getMaxQualification() {
    QQualification qual = QQualification.qualification1;
    return getQueryFactory()
        .selectFrom(qual)
        .select(qual.qualification.max()).fetchOne();
  }
}