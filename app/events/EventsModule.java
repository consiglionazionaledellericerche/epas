package events;

import com.google.common.eventbus.EventBus;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * @author daniele
 * @since 10/06/17.
 */
public class EventsModule implements Module {

  @Provides
  @Singleton
  public EventBus getEventBus() {
    return new EventBus();
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(ShiftEventsListener.class).asEagerSingleton();
  }
}
