package helpers.rest;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import helpers.JodaConverters;
import injection.AutoRegister;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.val;
import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;

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
}
