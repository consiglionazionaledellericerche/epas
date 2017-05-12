package it.cnr.iit.epas;

import com.google.common.base.Strings;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import play.data.binding.TypeBinder;

public class NullStringBinder implements TypeBinder<String> {

  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
                     Class actualClass, Type genericType) throws Exception {
    if (Strings.isNullOrEmpty(value)) {
      return null;
    }
    return value;
  }
}
