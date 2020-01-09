package dao;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;

public class CompetenceRequestDao extends DaoBase {

  @Inject
  CompetenceRequestDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }
}
