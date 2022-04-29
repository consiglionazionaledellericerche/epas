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

package common.injection;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import play.Play;
import play.PlayPlugin;
import play.inject.BeanSource;

/**
 * Plugin di injection.
 *
 * @author Marco Andreini
 *
 */
@Slf4j
public class InjectionPlugin extends PlayPlugin implements BeanSource {

  private Injector injector;
  private final Set<Module> modules = Sets.newHashSet();

  @Override
  public void onApplicationStart() {
    log.info("Starting Injection modules scanning");
    try {
      modules.clear();
      log.debug("Injection modules cleared");
      for (final Class<?> cls : Play.classloader.getAnnotatedClasses(AutoRegister.class)) {
        if (Module.class.isAssignableFrom(cls)) {
          log.debug("Add injection for module {}", cls.getName());
          modules.add((Module) cls.newInstance());
        } else {
          log.warn("{} isn't a Module, skipped.", cls);
        }
      }
      injector = Guice.createInjector(modules);
    } catch (InstantiationException e) {
      log.error("injection error", e);
    } catch (IllegalAccessException e) {
      log.error("injection error", e);
    }
    play.inject.Injector.inject(this);
  }

  /**
   * Classe staticInject estende abstractModule.
   *
   * @author Marco
   *
   */
  @AutoRegister
  public static class StaticInjectModule extends AbstractModule {

    @Override
    protected void configure() {
      for (Class<?> cls : Play.classloader.getAnnotatedClasses(StaticInject.class)) {
        log.debug("static injection for {}", cls.getName());
        binder().requestStaticInjection(cls);
      }
    }
  }

  @Override
  public <T> T getBeanOfType(Class<T> clazz) {
    return injector.getInstance(clazz);
  }
}
