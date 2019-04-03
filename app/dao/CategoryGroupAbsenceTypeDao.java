package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.absences.CategoryGroupAbsenceType;
import models.absences.query.QCategoryGroupAbsenceType;

/**
 * DAO per le categorie di gruppi di assenza.
 *
 * @author cristian
 */
public class CategoryGroupAbsenceTypeDao extends DaoBase {

  @Inject
  CategoryGroupAbsenceTypeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Tutte le categorie di tipi di assenza.
   *
   * @return la lista delle categorie di tipi di assenza.
   */
  public List<CategoryGroupAbsenceType> all() {
    QCategoryGroupAbsenceType categoryGroupAbsenceType =
        QCategoryGroupAbsenceType.categoryGroupAbsenceType;

    return getQueryFactory().selectFrom(categoryGroupAbsenceType)
        .orderBy(categoryGroupAbsenceType.name.asc()).fetch();
  }
}
