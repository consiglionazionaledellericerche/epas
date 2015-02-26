package dao;

import javax.persistence.EntityManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

/**
 * @author marco
 *
 */
public class DaoBase {

	protected final JPQLQueryFactory queryFactory;
	protected final Provider<EntityManager> emp;

	@Inject
	DaoBase(/*JPQLQueryFactory queryFactory, */Provider<EntityManager> emp) {
		/*this.queryFactory = queryFactory;*/
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
