package injection;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.PlayPlugin;
import play.inject.BeanSource;

/**
 * Injection Plugin.
 *
 * @author marco
 */
public class InjectionPlugin extends PlayPlugin implements BeanSource {

  private static final Logger LOG = LoggerFactory.getLogger(InjectionPlugin.class);
  private final Set<Module> modules = Sets.newHashSet();
  private Injector injector;

  @Override
  public void onApplicationStart() {
    LOG.info("Starting Injection modules scanning");
    try {
      modules.clear();
      LOG.debug("Injection modules cleared");
      for (final Class<?> cls : Play.classloader.getAssignableClasses(Module.class)) {
        LOG.debug("Add injection for module {}", cls.getName());
        modules.add((Module) cls.newInstance());
      }
      injector = Guice.createInjector(modules);
    } catch (InstantiationException ex) {
      LOG.error("injection error", ex);
    } catch (IllegalAccessException ex) {
      LOG.error("injection error", ex);
    }
    play.inject.Injector.inject(this);
  }

  @Override
  public <T> T getBeanOfType(Class<T> clazz) {
    return injector.getInstance(clazz);
  }

  public static class StaticInjectModule extends AbstractModule {

    @Override
    protected void configure() {
      for (Class<?> cls : Play.classloader.getAnnotatedClasses(StaticInject.class)) {
        LOG.debug("static injection for {}", cls.getName());
        binder().requestStaticInjection(cls);
      }
    }
  }
}
