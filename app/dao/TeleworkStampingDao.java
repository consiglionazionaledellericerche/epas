package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import javax.persistence.EntityManager;
import models.Stamping;
import models.TeleworkStamping;
import models.query.QStamping;
import models.query.QTeleworkStamping;

public class TeleworkStampingDao extends DaoBase {

  @Inject
  TeleworkStampingDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
  
  /**
   * Preleva una timbratura tramite il suo id.
   *
   * @param id l'id associato alla Timbratura sul db.
   * @return la timbratura corrispondente all'id passato come parametro.
   */
  public TeleworkStamping getStampingById(Long id) {
    final QTeleworkStamping stamping = QTeleworkStamping.teleworkStamping;
    return getQueryFactory().selectFrom(stamping)
        .where(stamping.id.eq(id)).fetchOne();
  }
}
