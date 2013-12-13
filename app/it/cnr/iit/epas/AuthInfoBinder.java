/**
 * 
 */
package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import models.Person;
import models.PersonReperibilityType;
import models.exports.AuthInfo;
import models.exports.ReperibilityPeriod;
import models.exports.ReperibilityPeriods;

import org.joda.time.LocalDate;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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