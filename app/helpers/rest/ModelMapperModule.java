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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import common.injection.AutoRegister;
import helpers.JodaConverters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.val;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

/**
 * Modulo guice per fornire un ModelMapper configurato per
 * convertire le date in formato joda in quelle java.time.
 */
@AutoRegister
public class ModelMapperModule extends AbstractModule {

  /**
   * Fornisce una istanza singleton di un model mapper configurata
   * per convertire le date in formato joda in quelle java.time.
   */
  @Provides
  public ModelMapper modelMapper() {
    val modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    modelMapper.addConverter(jodaToJavaLocalDateConverter());
    modelMapper.addConverter(jodaToJavaLocalTimeConverter());
    modelMapper.addConverter(jodaToJavaLocalDateTimeConverter());
    modelMapper.addConverter(javaToJodaLocalDateConverter());
    modelMapper.addConverter(javaToJodaLocalTimeConverter());
    modelMapper.addConverter(javaToJodaLocalDateTimeConverter());
    return modelMapper;
  }
  
  
  /**
   * Converter per il ModelMapper per convertire org.joda.time.LocalDate in java.time.LocalDate.
   */
  public Converter<org.joda.time.LocalDate, LocalDate> jodaToJavaLocalDateConverter() {
    return new AbstractConverter<org.joda.time.LocalDate, LocalDate>() {
      protected LocalDate convert(org.joda.time.LocalDate source) {
        return JodaConverters.jodaToJavaLocalDate(source);
      }
    };
  }
  
  /**
   * Converter per il ModelMapper per convertire org.joda.time.LocalTime in java.time.LocalTime.
   */
  public Converter<org.joda.time.LocalTime, LocalTime> jodaToJavaLocalTimeConverter() {
    return new AbstractConverter<org.joda.time.LocalTime, LocalTime>() {
      protected LocalTime convert(org.joda.time.LocalTime source) {
        return JodaConverters.jodaToJavaLocalTime(source);
      }
    };
  }

  /**
   * Converter per il ModelMapper per convertire org.joda.time.LocalDateTime in 
   * java.time.LocalDateTime.
   */
  public Converter<org.joda.time.LocalDateTime, LocalDateTime> 
      jodaToJavaLocalDateTimeConverter() {
    return new AbstractConverter<org.joda.time.LocalDateTime, LocalDateTime>() {
      protected LocalDateTime convert(org.joda.time.LocalDateTime source) {
        return JodaConverters.jodaToJavaLocalDateTime(source);
      }
    };
  }

  /**
   * Converter per il ModelMapper per convertire java.time.LocalDate in org.joda.time.LocalDate.
   */
  public Converter<LocalDate, org.joda.time.LocalDate> javaToJodaLocalDateConverter() {
    return new AbstractConverter<LocalDate, org.joda.time.LocalDate>() {
      protected org.joda.time.LocalDate convert(LocalDate source) {
        return JodaConverters.javaToJodaLocalDate(source);
      }
    };
  }

  /**
   * Converter per il ModelMapper per convertire java.time.LocalTime in org.joda.time.LocalTime.
   */
  public Converter<LocalTime, org.joda.time.LocalTime> javaToJodaLocalTimeConverter() {
    return new AbstractConverter<LocalTime, org.joda.time.LocalTime>() {
      protected org.joda.time.LocalTime convert(LocalTime source) {
        return JodaConverters.javaToJodaLocalTime(source);
      }
    };
  }

  /**
   * Converter per il ModelMapper per convertire java.time.LocalDateTime in 
   * org.joda.time.LocalDateTime.  
   */
  public Converter<LocalDateTime, org.joda.time.LocalDateTime> 
      javaToJodaLocalDateTimeConverter() {
    return new AbstractConverter<LocalDateTime, org.joda.time.LocalDateTime>() {
      protected org.joda.time.LocalDateTime convert(LocalDateTime source) {
        return JodaConverters.javaToJodaLocalDateTime(source);
      }
    };
  }

}
