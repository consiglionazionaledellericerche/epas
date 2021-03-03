/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package helpers.jpa;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import play.Play;
import play.PlayPlugin;
import play.data.binding.Binder;
import play.data.binding.Global;
import play.data.binding.RootParamNode;
import play.data.binding.TypeUnbinder;
import play.db.jpa.JPA;
import play.db.jpa.JPABase;

/**
 * Plugin per il bind/unbind degli oggetti JPA.
 *
 * @author Marco Andreini
 *
 */
@Slf4j
public class JpaReferencePlugin extends PlayPlugin {

  private static Map<Class<?>, Class<TypeUnbinder<?>>> unbinders;

  @SuppressWarnings("unchecked")
  @Override
  public void onApplicationStart() {
    log.info("Registrato JPAReference plugin");
    unbinders = Maps.newHashMap();
    for (Class<TypeUnbinder<?>> c : Play.classloader
        .getAssignableClasses(TypeUnbinder.class)) {
      if (c.isAnnotationPresent(Global.class)) {
        final Type forType = ((ParameterizedType) c
            .getGenericInterfaces()[0]).getActualTypeArguments()[0];
        final Class<?> clsType;
        if (forType instanceof ParameterizedType) {
          clsType = (Class<?>) ((ParameterizedType) forType).getRawType();
        } else {
          clsType = (Class<?>) forType;
        }
        unbinders.put(clsType, c);
      }
    }
  }

  /*
   * Find global unbinder registered for `src` object.
   */
  @Override
  public Map<String, Object> unBind(Object src, String name) {
    if (src == null) {
      return null;
    }
    final Class<?> clazz = src.getClass();
    for (Class<?> cls : ImmutableList.of(clazz, clazz.getSuperclass())) {
      if (unbinders.containsKey(cls)) {
        final Class<TypeUnbinder<?>> unbinder = unbinders.get(cls);
        final Map<String, Object> result = Maps.newHashMap();
        try {
          if (unbinder.newInstance().unBind(result, src, clazz, name,
              new Annotation[]{})) {
            return result;
          }
        } catch (Exception e) {
          log.error("unbind error", e);
        }
      }
    }
    return null;
  }

  @Override
  public Object bind(RootParamNode rootParamNode, String name, 
      @SuppressWarnings("rawtypes") Class clazz,
      Type type, Annotation[] annotations) {

    if (Optional.class.isAssignableFrom(clazz)) {
      if (rootParamNode.getChild(name) == null) {
        return Optional.absent();
      } else {
        final Class<?> cls = (Class<?>) ((ParameterizedType) type)
            .getActualTypeArguments()[0];
        // casi a parte per i non nullable...
        if (JPABase.class.isAssignableFrom(cls)) {
          final String value = rootParamNode.getChild(name)
              .getFirstValue(Long.class);
          if (Strings.isNullOrEmpty(value)) {
            return Optional.absent();
          } else {
            try {
              // si passa come riferimento, senza .id
              long key = (Long) Binder.directBind(value, long.class);
              return Optional.of(JPA.em().getReference(cls, key));
            } catch (Exception e) {
              return null;
            }
          }
        } else {
          return Optional.fromNullable(Binder.bind(rootParamNode,
              name, cls, cls, annotations));
        }
      }
    }
    return null;
  }
}
