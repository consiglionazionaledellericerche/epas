package helpers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

/**
 * Module which provides the AuditReader.
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
