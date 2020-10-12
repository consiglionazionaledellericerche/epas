package helpers.rest;

import com.fatboyindustrial.gsonjodatime.Converters;
import com.google.gson.GsonBuilder;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import injection.AutoRegister;
import lombok.val;

/**
 * Registrazione delle utilità per la serializzazione / deserializzazione JSON.
 *
 */
@AutoRegister
public class GsonModule implements Module {

  /**
   * Fornisce una istanza configurata del GsonBuilder.
   */
  @Provides
  public GsonBuilder builderFactory() {
    val builder = new GsonBuilder(); 
    com.fatboyindustrial.gsonjavatime.Converters.registerAll(builder).serializeNulls();
    return Converters.registerAll(builder)
        .serializeNulls();
  }

  @Override
  public void configure(Binder binder) {
  }
}
