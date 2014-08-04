package helpers;

import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import play.db.jpa.JPA;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * @author marco
 *
 */
public class HistoryModule extends AbstractModule {

	@Provides
	public EntityManager getEntityManager() {
		return JPA.em();
	}

	@Provides
	public AuditReader getAuditReader(EntityManager em) {
		return AuditReaderFactory.get(em);
	}

	@Override
	protected void configure() {
	}
}
