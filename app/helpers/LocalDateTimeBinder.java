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
  private static final DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/YYYY HH:mm:ss");
  private static final DateTimeFormatter dtfNoSeconds = 
      DateTimeFormat.forPattern("dd/MM/YYYY HH:mm");
  
  private static final DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/YYYY");
  
  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      Class actualClass, Type genericType) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    try {
      if (value.length() == 10) {
        return LocalDate.parse(value, df).toDateTimeAtStartOfDay().toLocalDateTime();        
      }
      if (value.length() == 16) {
        return LocalDate.parse(value, dtfNoSeconds).toDateTimeAtStartOfDay().toLocalDateTime();
      }
      return LocalDateTime.parse(value, dtf);
    } catch (Exception ignored) {
      log.error("Exception during LocalDateTime binding", ignored);
    }

    return new LocalDateTime(DATE_BINDER.bind(name, annotations, value, actualClass, genericType));
  }
  
  public static void main(String[] args) {
    System.out.println(LocalDateTime.parse("03/07/2018 00:00:00", dtf));
  }
}
