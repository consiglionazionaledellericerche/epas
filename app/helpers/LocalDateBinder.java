package helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import play.data.binding.Global;
import play.data.binding.TypeBinder;
import play.data.binding.types.DateBinder;

@Slf4j
@Global
public class LocalDateBinder implements TypeBinder<LocalDate> {

  private static final DateBinder DATE_BINDER = new DateBinder();
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYYY");

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
      log.debug("Exception during binding LocalDate {} format dd/MM/YYYY", value, ignored);
    }
    try {
      return LocalDate.parse(value, DateTimeFormat.forPattern("YYYY-mm-dd"));
    } catch (Exception e) {
      log.debug("Exception during binding LocalDate {} as ISO", value);
    }

    return new LocalDate(DATE_BINDER.bind(name, annotations, value, actualClass, genericType));
  }
}