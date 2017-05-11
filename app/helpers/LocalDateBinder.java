package helpers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.joda.time.LocalDate;

import play.data.binding.Global;
import play.data.binding.TypeBinder;
import play.data.binding.types.DateBinder;

@Global
public class LocalDateBinder implements TypeBinder<LocalDate> {

  private static final DateBinder DATE_BINDER = new DateBinder();

  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
                     Class actualClass, Type genericType) throws Exception {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    return new LocalDate(DATE_BINDER.bind(name, annotations, value, actualClass, genericType));
  }
}
