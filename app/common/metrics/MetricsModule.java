/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package common.metrics;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import common.injection.AutoRegister;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics;
import io.micrometer.core.instrument.binder.jpa.HibernateMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.lang.Nullable;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.time.Duration;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.sql.DataSource;
import lombok.val;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import play.Play;
import play.db.jpa.JPA;

/**
 * Configurazione del sistema di metriche.
 *
 * @author Marco Andreini
 # @see https://github.com/besmartbeopen/play1-base
 */
@AutoRegister
public class MetricsModule extends AbstractModule {

  /**
   * <p>Nome della chiave nella configurazione play per la soglia minima della durata
   * delle richieste che saranno registrate nei log.</p>
   * Il valore è espresso in millisecondi.
   */
  public static final String LOG_MIN_DURATION_REQUEST = "log_min_duration_request";

  /**
   * Durata minima predefinta: 0.7 secondi
   */
  public static final long DEFAULT_MIN_DURATION_REQUEST = 700_000_000L;

  /**
   * Fornisce il PrometheusMeterRegistry per l'injection.
   */
  @Singleton
  @Provides
  public PrometheusMeterRegistry registry() {
    return new PrometheusMeterRegistry(new PrometheusConfig() {

      @Override
      public Duration step() {
        return Duration.ofSeconds(10);
      }

      @Override
      @Nullable
      public String get(String key) {
        return null;
      }
    });
  }

  @SuppressWarnings("resource")
  @Provides
  public HibernateMetrics hibernateMetrics(Provider<EntityManager> emp) {
    return new HibernateMetrics(emp.get().getEntityManagerFactory().unwrap(SessionFactory.class),
        JPA.DEFAULT, ImmutableList.of());
  }

  /**
   * DataSource costruito tramite EntityManager.
   */
  public DataSource getDataSource(Provider<EntityManager> emp) {
    val entityManagerFactory = emp.get().getEntityManagerFactory();
    @SuppressWarnings("resource")
    ConnectionProvider cp = ((SessionFactory) entityManagerFactory).getSessionFactoryOptions()
        .getServiceRegistry()
        .getService(ConnectionProvider.class);
    return cp.unwrap(DataSource.class);
  }
  
  /**
   * Nome del db prelevato dalla configurazione.
   */
  public String getDatabaseName() {
    val databaseUrl = Splitter.on("/").splitToList(Play.configuration.getProperty("db"));
    return databaseUrl.get(databaseUrl.size() - 1);
  }
  
  /**
   * Fornisce l'istanza PostgreSQLDatabaseMetrics per l'injection.
   */
  @Provides
  public PostgreSQLDatabaseMetrics postgresqlMetrics(Provider<EntityManager> emp) {
    return new PostgreSQLDatabaseMetrics(getDataSource(emp), getDatabaseName());
  }
  
  /**
   * Fornisce una implementazione di IMinDurationCheck.
   */
  @Provides
  @Singleton
  public IMinDurationCheck checker() {
    return t -> t >= Optional
        .ofNullable(Play.configuration.getProperty(LOG_MIN_DURATION_REQUEST))
        .map(Long::parseLong).orElse(DEFAULT_MIN_DURATION_REQUEST);
  }

  @Override
  protected void configure() {
    bind(MeterRegistry.class).to(PrometheusMeterRegistry.class);
    val meterBinder = Multibinder.newSetBinder(binder(), MeterBinder.class);
    meterBinder.addBinding().to(ClassLoaderMetrics.class);
    meterBinder.addBinding().to(FileDescriptorMetrics.class);
    meterBinder.addBinding().to(HibernateMetrics.class);
    meterBinder.addBinding().to(JvmGcMetrics.class);
    meterBinder.addBinding().to(JvmMemoryMetrics.class);
    meterBinder.addBinding().to(JvmThreadMetrics.class);
    meterBinder.addBinding().to(ProcessorMetrics.class);
    //XXX: servono queste metriche?
    //meterBinder.addBinding().to(PostgreSQLDatabaseMetrics.class);
    meterBinder.addBinding().to(UptimeMetrics.class);
  }
}