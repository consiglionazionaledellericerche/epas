package models;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.sql.Time;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import org.joda.time.LocalDateTime;

import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

public class PersonTags extends JavaExtensions{
	
	public static String toCalendarTime(LocalDateTime ldt) {
		Number hour = ldt.getHourOfDay();
		Number minute = ldt.getMinuteOfHour();
		
		Number second = ldt.getSecondOfMinute();
		
		String format = "HH:mm:ss";
		Time tempo = new Time(hour.intValue(),minute.intValue(),second.intValue());
		
	    return new SimpleDateFormat(format).format(tempo);
	  //  return new DecimalFormat(format).format(ldt);
	}

	

}
