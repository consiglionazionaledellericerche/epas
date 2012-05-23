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
	
//	public static String toCalendarTime(LocalDateTime ldt) {
//		Number hour = ldt.getHourOfDay();
//		Number minute = ldt.getMinuteOfHour();
//		
//		Number second = ldt.getSecondOfMinute();
//		
//		String format = "HH:mm";
//		Time tempo = new Time(hour.intValue(),minute.intValue(),second.intValue());
//		
//	    return new SimpleDateFormat(format).format(tempo);
//	  
//	}
//	
//	public static String toHourTime(int minutes){
//		Number hour = (int)minutes/60;
//		Number minute = minutes%60;
//		String format = "HH:mm";
//		Time tempo = new Time(hour.intValue(),minute.intValue(),0);
//		
//		return new SimpleDateFormat(format).format(tempo);
//	}

	public static String toCalendarTime(LocalDateTime ldt) {
        Number hour = ldt.getHourOfDay();
        Number minute = ldt.getMinuteOfHour();
        return String.format("%02d:%02d", hour, minute);          
	}
        
	public static String toHourTime(Integer minutes) {
		int min =  Math.abs(minutes%60);
		int hour= Math.abs((int)minutes/60);
		if((minutes.intValue()<0))
			
			return String.format("-%02d:%02d", hour, min);
        return String.format("%02d:%02d", hour, min);
	}
	
	
	

}
