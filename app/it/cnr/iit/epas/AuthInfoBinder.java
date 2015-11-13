/**
 * 
 */
package it.cnr.iit.epas;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import models.exports.AuthInfo;
import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * @author cristian
 *
 */
@Global
public class AuthInfoBinder implements TypeBinder<AuthInfo> {

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	Class actualClass, Type genericType) throws Exception {
		
		Logger.trace("binding AuthInfo: %s, %s, %s, %s, %s", name, annotations, value, actualClass, genericType);
		JsonObject jsonObject = new JsonParser().parse(value).getAsJsonObject();
		
		return new AuthInfo(jsonObject.get("username").getAsString(), jsonObject.get("password").getAsString());
		
	}
	
}