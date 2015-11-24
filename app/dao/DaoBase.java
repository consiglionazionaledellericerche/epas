package dao;

import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import javax.persistence.EntityManager;

/**
 * @author marco
 *
 */
public abstract class DaoBase {

	protected final JPQLQueryFactory queryFactory;
	protected final Provider<EntityManager> emp;

	DaoBase(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
		this.emp = emp;
		this.queryFactory = new JPAQueryFactory(this.emp);
	}

	protected JPQLQueryFactory getQueryFactory() {
		return this.queryFactory;
	}

	protected EntityManager getEntityManager() {
		return emp.get();
	}
}
