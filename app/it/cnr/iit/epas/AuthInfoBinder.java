package it.cnr.iit.epas;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import lombok.extern.slf4j.Slf4j;

import models.exports.AuthInfo;

import play.data.binding.Global;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Binder per le informazioni sul auth info (username, password).
 *
 * @author cristian
 */
@Slf4j
@Global
public class AuthInfoBinder implements TypeBinder<AuthInfo> {

  /**
   * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[],
   * java.lang.String, java.lang.Class, java.lang.reflect.Type)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object bind(
      String name, Annotation[] annotations, String value,
      Class actualClass, Type genericType) throws Exception {

    log.trace("binding AuthInfo: {}, {}, {}, {}, {}",
        name, annotations, value, actualClass, genericType);
    JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();

    return new AuthInfo(
        jsonObject.get("username").getAsString(), jsonObject.get("password").getAsString());

  }

}
