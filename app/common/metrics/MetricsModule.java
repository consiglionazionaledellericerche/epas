package common.metrics;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import injection.AutoRegister;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jpa.HibernateMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.lang.Nullable;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.time.Duration;
import java.util.Optional;
import javax.persistence.EntityManager;
import lombok.val;
import play.Play;
import play.db.jpa.JPA;

/**
 * Configurazione del sistema di metriche.
 * 
 * @author marco
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
   * Durata minima predefinta: 0.5 secondi
   */
  public static final long DEFAULT_MIN_DURATION_REQUEST = 500_000_000L;

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

  @Provides
  public HibernateMetrics hibernateMetrics(Provider<EntityManager> emp) {
    return new HibernateMetrics(emp.get().getEntityManagerFactory(),
        JPA.DEFAULT, ImmutableList.of());
  }

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
    //meterBinder.addBinding().to(HibernateMetrics.class);
    meterBinder.addBinding().to(UptimeMetrics.class);
    meterBinder.addBinding().to(ProcessorMetrics.class);
    meterBinder.addBinding().to(JvmGcMetrics.class);
    meterBinder.addBinding().to(JvmMemoryMetrics.class);
    meterBinder.addBinding().to(ClassLoaderMetrics.class);
  }
}
