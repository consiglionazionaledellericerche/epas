package helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

@Slf4j
@Global
public class LocalDateJavaBinder implements TypeBinder<LocalDate> {

  //Questo formato è utilizzato nelle form HTML
  static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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
      log.debug("Exception during java LocalDate binding, value={}", value);
    }
    //Nei metodi REST le date vengono passate nel formato ISO
    return LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(value));
  }
}
