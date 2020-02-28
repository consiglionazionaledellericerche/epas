package helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import play.data.binding.TypeBinder;
import play.data.binding.types.DateBinder;

@Slf4j
public class LocalTimeBinder implements TypeBinder<LocalTime> {
  
  private static final DateBinder TIME_BINDER = new DateBinder();
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("HH:mm");

  @Override
  public Object bind(String name, Annotation[] annotations, String value, Class actualClass,
      Type genericType) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalTime.parse(value, dtf);
    } catch (Exception ignored) {
      log.info("Exception during LocalDate binding, value = {}", value);
    }
    return new LocalTime(TIME_BINDER.bind(name, annotations, value, actualClass, genericType));
  }

}
