package helpers;

import javax.inject.Provider;
import javax.persistence.EntityManager;

import com.mysema.query.jpa.JPQLQueryFactory;
import com.mysema.query.jpa.impl.JPAQueryFactory;

import play.db.jpa.JPA;
import play.db.jpa.JPQL;

/**
 * @author marco
 *
 */
public final class ModelQuery {
	
	private static JPAQueryFactory factory = 
			new JPAQueryFactory(new Provider<EntityManager>() {

		@Override
		public EntityManager get() {
			return JPA.em();
		}
	});

	private ModelQuery() {}
	
	/**
	 * @return un query factory per il querydsl
	 */
	public static JPQLQueryFactory queryFactory() {
		return factory;
	}
}
