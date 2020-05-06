package helpers;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.GsonBuilder;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import injection.AutoRegister;

/**
 * Registrazione delle utilità per la serializzazione / deserializzazione JSON.
 *
 */
@AutoRegister
public class GsonModule implements Module {

  @Provides
  public GsonBuilder builderFactory() {
    return Converters.registerAll(new GsonBuilder())
        .serializeNulls();
  }

  @Override
  public void configure(Binder binder) {
  }
}
