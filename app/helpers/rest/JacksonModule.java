package helpers.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;

//import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * @author marco
 */
public class JacksonModule implements Module {

  public final static String FILTER = "filter";

  public final static SimpleBeanPropertyFilter BASE_OBJECT_FILTER =
          SimpleBeanPropertyFilter.serializeAllExcept("entityId", "persistent");

  public static FilterProvider filterProviderFor(PropertyFilter filter) {
    return new SimpleFilterProvider().addFilter(JacksonModule.FILTER,
            filter);
  }

  @Provides
  public ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JodaModule())
            .registerModule(new AfterburnerModule())
            .registerModule(new Hibernate4Module())
            .setVisibility(PropertyAccessor.FIELD, Visibility.PUBLIC_ONLY)
            .setVisibility(PropertyAccessor.GETTER, Visibility.PUBLIC_ONLY)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    // .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }

  @Override
  public void configure(Binder binder) {
  }
}
