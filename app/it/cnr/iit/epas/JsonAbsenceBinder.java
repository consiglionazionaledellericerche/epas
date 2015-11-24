/**
 * 
 */
package it.cnr.iit.epas;

import com.google.gson.GsonBuilder;
import helpers.deserializers.AbsenceFromClientDeserializer;
import injection.StaticInject;
import models.exports.AbsenceFromClient;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * @author cristian
 *
 */
@Global
@StaticInject
public class JsonAbsenceBinder implements TypeBinder<AbsenceFromClient> {

	/**
	 * @see play.data.binding.TypeBinder#bind(java.lang.String, java.lang.annotation.Annotation[], 
	 * java.lang.String, java.lang.Class, java.lang.reflect.Type)
	 */
	@Override
	public Object bind(String name, Annotation[] annotations, String value,	
			Class actualClass, Type genericType) throws Exception {
		
		return new GsonBuilder().registerTypeAdapter(AbsenceFromClient.class, 
				new AbsenceFromClientDeserializer()).create()
				.fromJson(value, AbsenceFromClient.class);
	}
}