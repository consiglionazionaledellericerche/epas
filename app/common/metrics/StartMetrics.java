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
 * @author Marco Andreini
 * @see Progetto github https://github.com/besmartbeopen/play1-base
 */
@OnApplicationStart
@Slf4j
public class StartMetrics extends Job<Void> {

  /**
   * Questa classe è utile subito per avviare la configurazione delle metriche.
   *
   * @author Marco Andreini
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

    /**
     * Avvio del job per la configurazione delle metriche.
     */
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
