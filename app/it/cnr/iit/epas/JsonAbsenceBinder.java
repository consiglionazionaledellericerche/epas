/**
 * 
 */
package it.cnr.iit.epas;

import injection.StaticInject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.inject.Inject;

import models.BadgeReader;
import models.Person;
import models.StampType;
import models.exports.AbsenceFromClient;
import models.exports.StampingFromClient;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.binding.Global;
import play.data.binding.TypeBinder;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dao.BadgeReaderDao;
import dao.PersonDao;
import dao.StampingDao;


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
		
		return new Gson().fromJson(value, AbsenceFromClient.class);
	}
}