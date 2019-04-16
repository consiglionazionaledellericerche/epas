package dao;

import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;

/**
 * Base dao which provides the JPQLQueryFactory and the EntityManager.
 *
 * @author marco
 */
public abstract class DaoBase {

  protected final JPQLQueryFactory queryFactory;
  protected final Provider<EntityManager> emp;

  protected DaoBase(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    this.emp = emp;
    this.queryFactory = new JPAQueryFactory(this.emp);
  }

  protected JPQLQueryFactory getQueryFactory() {
    return queryFactory;
  }

  protected EntityManager getEntityManager() {
    return emp.get();
  }
}
