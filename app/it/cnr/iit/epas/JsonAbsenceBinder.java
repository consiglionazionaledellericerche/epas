/**
 * 
 */
package it.cnr.iit.epas;

import helpers.deserializers.AbsenceFromClientDeserializer;
import injection.StaticInject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import models.exports.AbsenceFromClient;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.gson.GsonBuilder;


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