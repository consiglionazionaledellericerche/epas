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
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import common.injection.AutoRegister;


/**
 * Impostazioni del mapper Jackson.
 *
 * @author Marco Andreini
 */
@AutoRegister
public class JacksonModule implements Module {

  public static final String FILTER = "filter";

  public static final SimpleBeanPropertyFilter BASE_OBJECT_FILTER =
          SimpleBeanPropertyFilter.serializeAllExcept("entityId", "persistent");

  public static FilterProvider filterProviderFor(PropertyFilter filter) {
    return new SimpleFilterProvider().addFilter(JacksonModule.FILTER,
            filter);
  }

  /**
   * Configura l'objectMapper e lo restiuisce.
   *
   * @return l'objectMapper configurato opportunamente.
   */
  @Provides
  public ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JodaModule())
            .registerModule(new AfterburnerModule())
            .registerModule(new Hibernate5Module())
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
