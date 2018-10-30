package common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

/**
 * Avvia il calcolo delle metriche registrando tutte quelle configurate.
 *
 * @author marco
 * @see Progetto github https://github.com/besmartbeopen/play1-base
 */
@OnApplicationStart
@Slf4j
public class StartMetrics extends Job<Void> {

  /**
   * Questa classe è utile subito.
   * @author marco
   *
   */
  public static class Starter {

    private final MeterRegistry registry;
    // per posticipare l'accesso appena dopo l'avvio:
    private final Provider<Set<MeterBinder>> meterBindersProvider;
    private final String instance;

    @Inject
    Starter(MeterRegistry registry,
        Provider<Set<MeterBinder>> meterBindersProvider,
        @Named("app.instance") String instance) {
      this.registry = registry;
      this.meterBindersProvider = meterBindersProvider;
      this.instance = instance;
    }

    public void run() {
      registry.config().commonTags("application", "epas", "deploy", instance);
      meterBindersProvider.get().forEach(mb -> mb.bindTo(registry));
      log.debug("meter-registry setup completed for instance \"{}\"", instance);
    }
  }

  @Inject
  static Starter starter;

  @Override
  public void doJob() {
    starter.run();
  }
}
