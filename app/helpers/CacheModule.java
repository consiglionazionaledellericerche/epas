package helpers;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import dao.PersonDao;

import lombok.extern.slf4j.Slf4j;

import manager.attestati.service.CertificationService;
import manager.attestati.service.CertificationsComunication;
import manager.attestati.service.ICertificationService;

import javax.inject.Inject;

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
