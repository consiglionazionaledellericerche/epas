package helpers;

import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * @author marco
 */
public class HistoryModule extends AbstractModule {

  @Provides
  public AuditReader getAuditReader(EntityManager em) {
    return AuditReaderFactory.get(em);
  }

  @Override
  protected void configure() {
  }
}
