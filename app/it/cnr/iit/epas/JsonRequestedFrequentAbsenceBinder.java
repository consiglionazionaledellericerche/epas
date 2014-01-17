package it.cnr.iit.epas;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import models.exports.FrequentAbsenceCode;
import play.data.binding.TypeBinder;

public class JsonRequestedFrequentAbsenceBinder implements TypeBinder<FrequentAbsenceCode>{

	@Override
	public Object bind(String name, Annotation[] annotations, String value,
			Class actualClass, Type genericType) throws Exception {
		
		
		
		
		return null;
	}

}
