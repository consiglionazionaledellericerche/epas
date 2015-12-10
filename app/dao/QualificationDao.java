package dao;

import com.google.common.base.Optional;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Provider;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.AbsenceType;
import models.Qualification;
import models.query.QQualification;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

/**
 * @author dario
 */
public class QualificationDao extends DaoBase {

  @Inject
  QualificationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * @return la lista di qualifiche a seconda dei parametri passati: nel caso in cui il booleano sia
   * "true" viene ritornata l'intera lista di qualifiche. Nel caso sia presente la qualifica che si
   * vuole ritornare, viene ritornata sempre una lista, ma con un solo elemento, corrispondente al
   * criterio di ricerca. Nel caso invece in cui si voglia una lista di elementi sulla base dell'id,
   * si controllerà il parametro idQualification, se presente, che determinerà una lista di un solo
   * elemento corrispondente ai criteri di ricerca. Ritorna null nel caso in cui non dovesse essere
   * soddisfatta alcuna delle opzioni di chiamata
   */
  public List<Qualification> getQualification(Optional<Integer> qualification, Optional<Long> idQualification, boolean findAll) {
    final BooleanBuilder condition = new BooleanBuilder();
    QQualification qual = QQualification.qualification1;
    final JPQLQuery query = getQueryFactory().from(qual);
    if (findAll) {
      return query.list(qual);
    }
    if (qualification.isPresent()) {
      condition.and(qual.qualification.eq(qualification.get()));
      query.where(condition);
      return query.list(qual);
    }
    if (idQualification.isPresent()) {
      condition.and(qual.id.eq(idQualification.get()));
      query.where(condition);
      return query.list(qual);
    }
    return null;

  }

  /**
   *
   * @param qualification
   * @return
   */
  public Optional<Qualification> byQualification(Integer qualification) {

    Preconditions.checkNotNull(qualification);

    QQualification qual = QQualification.qualification1;
    final JPQLQuery query = getQueryFactory().from(qual)
            .where(qual.qualification.eq(qualification));

    return Optional.fromNullable(query.singleResult(qual));

  }

  /**
   *
   * @return
   */
  public List<Qualification> findAll() {

    QQualification qual = QQualification.qualification1;
    final JPQLQuery query = getQueryFactory().from(qual);
    return query.list(qual);
  }

  /**
   * @return la lista di qualifiche che possono usufruire del codice di assenza abt
   */
  public List<Qualification> getQualificationByAbsenceTypeLinked(AbsenceType abt) {
    QQualification qual = QQualification.qualification1;
    final JPQLQuery query = getQueryFactory().from(qual)
            .where(qual.absenceTypes.contains(abt));
    return query.list(qual);
  }


  /**
   * @return la lista di qualifiche superiori o uguali al limite passato come parametro (da usare,
   * ad esempio, per ritornare la lista delle qualifiche dei tecnici che hanno qualifica superiore a
   * 3)
   */
  public List<Qualification> getQualificationGreaterThan(Integer limit) {
    QQualification qual = QQualification.qualification1;
    final JPQLQuery query = getQueryFactory().from(qual)
            .where(qual.qualification.goe(limit));

    return query.list(qual);
  }
}
