package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import javax.persistence.EntityManager;
import models.flows.Affiliation;
import models.flows.query.QAffiliation;

public class AffiliationDao extends DaoBase {

  @Inject
  AffiliationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Affiliazione prelevata per id.
   */
  public Affiliation byId(Long id) {
    QAffiliation affilation = QAffiliation.affiliation;
    return (Affiliation) getQueryFactory().from(affilation)
        .where(affilation.id.eq(id)).fetchOne();
  }
}
