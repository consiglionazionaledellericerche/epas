package helpers.jpa;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import models.base.BaseModel;
import play.data.binding.TypeBinder;
import play.db.jpa.JPA;

/**
 * Binder per le collezioni di oggetti JPA.
 * @author marco
 *
 */
public class JpaReferenceBinder implements TypeBinder<Collection<? extends BaseModel>> {

  @SuppressWarnings("unchecked")
  @Override
  public Object bind(String name, Annotation[] annotations, String value,
      @SuppressWarnings("rawtypes") Class actualClass, Type genericType) throws Exception {
    final Long id = Long.parseLong(value);
    return JPA.em().find(actualClass, id);
  }
}
