package helpers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.attestati.service.CertificationService;
import manager.attestati.service.CertificationsComunication;
import manager.attestati.service.ICertificationService;

/**
 * @author daniele.
 */
@Slf4j
public class CacheModule extends AbstractModule {
  
 
  /**
   * Crea una istanza singleton della Cache.
   * 
   */
  @Provides 
  @Singleton
  public CacheValues getCacheValues(
      CertificationsComunication certification, ICertificationService certService,
      PersonDao personDao) {
    CacheValues cacheValues = 
        new CacheValues(certification, certService, personDao);
    log.info("Creata nuova cacheValues: {}", cacheValues);
    return cacheValues;
  }

  @Override
  public void configure() {
    bind(ICertificationService.class).to(CertificationService.class);
  }
}
