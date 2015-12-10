package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.google.common.base.Strings;

import play.data.binding.TypeBinder;

public class NullStringBinder implements TypeBinder<String> {

  @Override
  public Object bind(String name, Annotation[] annotations, String value,
                     Class actualClass, Type genericType) throws Exception {
    if (Strings.isNullOrEmpty(value)) {
      return null;
    }
    return value;
  }
}
