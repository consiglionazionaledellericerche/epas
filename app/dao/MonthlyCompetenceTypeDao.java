package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.MonthlyCompetenceType;
import models.query.QMonthlyCompetenceType;

public class MonthlyCompetenceTypeDao extends DaoBase {

  @Inject
  MonthlyCompetenceTypeDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }
  
  /**
   * Metodo che ritorna la lista di tutti i tipi di competenza mensile.
   * @return la lista di tutti i tipi di competenza mensile.
   */
  public List<MonthlyCompetenceType> listTypes() {
    final QMonthlyCompetenceType monthlyType = QMonthlyCompetenceType.monthlyCompetenceType;
    return getQueryFactory().selectFrom(monthlyType).fetch();
  }
}
