package helpers.jpa;

import com.google.common.base.Predicates;
import com.google.common.base.Throwables;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import javassist.ClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import models.base.HistoryValueFrom;

import org.apache.commons.lang.WordUtils;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class HistoryViews {

  private static final Logger log = LoggerFactory.getLogger(HistoryViews.class);
  private static final CtClass[] NO_ARGS = {};
  private static final Map<String, Class<?>> map = Maps.newHashMap();

  static <T> Class<? extends T> compose(Class<T> orig) throws Exception {

    final ClassPool pool = ClassPool.getDefault();
    pool.appendClassPath(new LoaderClassPath(orig.getClassLoader()));
    pool.appendClassPath(new ApplicationClassesClasspath());
    final CtClass ctOriginal = pool.get(orig.getName());
    final CtClass ctLocalDateTime = pool.get(LocalDateTime.class.getName());

    CtClass cls = pool.makeClass("models.history." + orig.getSimpleName());
    cls.setSuperclass(ctOriginal);
    cls.addField(new CtField(ctOriginal, "_current", cls));
    cls.addField(new CtField(ctOriginal, "_history", cls));
    cls.addField(new CtField(ctLocalDateTime, "_datetime", cls));

    // add public constructor method to class
    CtConstructor cons = new CtConstructor(new CtClass[]{ctOriginal,
        ctOriginal, ctLocalDateTime}, cls);
    cons.setBody("{_current = $1; _history = $2; _datetime = $3;}");
    cls.addConstructor(cons);

    for (CtField origField : ctOriginal.getFields()) {
      // in play1.x jpa models: attributes are public.
      if (Modifier.isFinal(origField.getModifiers()) 
          || !Modifier.isPublic(origField.getModifiers())) {
        continue;
      }
      final String fieldName = origField.getName();
      final String getterName = "get" + WordUtils.capitalize(fieldName);
      CtClass returnType = origField.getType();
      CtMethod ctMethod = new CtMethod(returnType, getterName, NO_ARGS, cls);

      try {
        /*
         * Only fields are checked.
         */
        final CtField ctField = ctOriginal.getField(fieldName);
        final FluentIterable<Object> annotations =
            FluentIterable.from(ImmutableList
                .copyOf(ctField.getAnnotations()));

        if (annotations.anyMatch(Predicates.instanceOf(NotAudited.class))) {
          // not audited:

          if (ctMethod.getReturnType().equals(ctLocalDateTime)
              && annotations.anyMatch(Predicates
                  .instanceOf(HistoryValueFrom.class))) {
            // choosing by annoation:
            final HistoryValueFrom annotation = (HistoryValueFrom)
                annotations.firstMatch(Predicates
                    .instanceOf(HistoryValueFrom.class)).get();
            if (annotation.value() == HistoryValueFrom.HistoryFrom.CURRENT) {
              ctMethod.setBody("{return _current." + fieldName + ";}");
            } else { // si assume che venga dalla revisione.
              ctMethod.setBody("{return _datetime;}");
            }
          } else {
            /*
             * default by current object.
             */
            ctMethod.setBody("{return _current." + fieldName + ";}");
          }
        } else {
          /*
           * audited are in _history.
           */
          ctMethod.setBody("{ return _history." + fieldName + ";}");
        }
      } catch (NotFoundException ex) {
        /*
         * XXX: if not found, try in _history (is transient?)
         */
        ctMethod.setBody("{return _history." + fieldName + ";}");
      }
      cls.addMethod(ctMethod);
    }
    @SuppressWarnings("unchecked")
    Class<T> result = cls.toClass();
    cls.detach();
    return result;
  }

  static <T> Class<? extends T> historicalModel(Class<T> cls) {

    @SuppressWarnings("unchecked")
    Class<? extends T> result = (Class<? extends T>) map.get(cls.getName());
    if (result == null) {
      try {
        result = compose(cls);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      map.put(cls.getName(), result);
    }
    return result;
  }

  public static <T> T historicalViewOf(Class<T> cls, T current, T history,
      LocalDateTime revisionDateTime) {
    final Class<? extends T> model = historicalModel(cls);
    Constructor<? extends T> cons;
    try {
      cons = model.getDeclaredConstructor(new Class<?>[]{cls, cls,
        LocalDateTime.class});
    } catch (SecurityException se) {
      throw Throwables.propagate(se);
    } catch (NoSuchMethodException ex) {
      throw Throwables.propagate(ex);
    }
    try {
      return cons.newInstance(current, history, revisionDateTime);
    } catch (Exception ex) {
      throw Throwables.propagate(ex);
    }
  }

  public static class ApplicationClassesClasspath implements ClassPath {

    @Override
    public InputStream openClassfile(String className) throws NotFoundException {

      if (Play.usePrecompiled) {
        try {
          final File file = Play.getFile("precompiled/java/"
               + className.replace(".", "/") + ".class");
          return new FileInputStream(file);
        } catch (Exception ex) {
          log.error("missing class {}", className);
        }
      }
      ApplicationClass appClass = Play.classes.getApplicationClass(className);

      Verify.verifyNotNull(appClass.enhancedByteCode);
      return new ByteArrayInputStream(appClass.enhancedByteCode);
    }

    @Override
    public URL find(String className) {
      if (Play.classes.getApplicationClass(className) != null) {
        String cname = className.replace('.', '/') + ".class";
        try {
          // return new File(cname).toURL();
          return new URL("file:/ApplicationClassesClasspath/" + cname);
        } catch (MalformedURLException ex) {
        }
      }
      return null;
    }

    @Override
    public void close() {
    }
  }
}
