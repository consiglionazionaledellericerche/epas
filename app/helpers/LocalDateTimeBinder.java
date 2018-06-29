package helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.data.binding.Global;
import play.data.binding.TypeBinder;
import play.data.binding.types.DateBinder;

@Slf4j
@Global
public class LocalDateTimeBinder implements TypeBinder<LocalDateTime> {

  private static final DateBinder DATE_BINDER = new DateBinder();
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("YYYY-MM-dd hh:mm");

  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      Class actualClass, Type genericType) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      return LocalDate.parse(value, dtf);
    } catch (Exception ignored) {
      log.error("Exception during LocalDateTime binding", ignored);
    }

    return new LocalDateTime(DATE_BINDER.bind(name, annotations, value, actualClass, genericType));
  }
}
