package injection;

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
 * @author marco
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
