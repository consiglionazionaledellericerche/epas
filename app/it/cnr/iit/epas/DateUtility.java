package it.cnr.iit.epas;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import models.ConfGeneral;
import models.Person;
import models.WorkingTimeType;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.db.jpa.JPA;

public class DateUtility {

//	/**
//	 * Controlla che il giorno sia festivo o lavorativo per la persona sulla base delle Feste Generali e del piano di lavoro
//	 * @param person
//	 * @param date
//	 * @return
//	 */
//	public static boolean isHoliday(Person person, LocalDate date){	
//		
//		if(person.getCurrentContract() == null || date.isBefore(person.getCurrentContract().beginContract))
//			return false;
//		
//		if(isGeneralHoliday(date))
//			return true;
//		
//		return person.getWorkingTimeType(date).getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).holiday;
//
//	}

	/**
	 * 
	 * @param year
	 * @return il giorno in cui cade la pasqua
	 */
	private static final LocalDate findEaster(int year) {
	    if (year <= 1582) {
	      throw new IllegalArgumentException(
	          "Algorithm invalid before April 1583");
	    }
	    int golden, century, x, z, d, epact, n;
	    LocalDate easter = null;
	    golden = (year % 19) + 1; /* E1: metonic cycle */
	    century = (year / 100) + 1; /* E2: e.g. 1984 was in 20th C */
	    x = (3 * century / 4) - 12; /* E3: leap year correction */
	    z = ((8 * century + 5) / 25) - 5; /* E3: sync with moon's orbit */
	    d = (5 * year / 4) - x - 10;
	    epact = (11 * golden + 20 + z - x) % 30; /* E5: epact */
	    if ((epact == 25 && golden > 11) || epact == 24)
	      epact++;
	    n = 44 - epact;
	    n += 30 * (n < 21 ? 1 : 0); /* E6: */
	    n += 7 - ((d + n) % 7);
	    
	    if (n > 31) /* E7: */{
	    	easter = new LocalDate(year, 4 , n - 31);
	    	
	      return easter; /* April */
	    }
	    else{
	    	easter = new LocalDate(year, 3 , n);
	    	
	      return easter; /* March */
	    }
	}
	
	public static boolean isGeneralHoliday(LocalDate date){
		
		//Configuration config = Configuration.getConfiguration(date);
		ConfGeneral confGeneral = ConfGeneral.getConfGeneral();
		
		LocalDate easter = findEaster(date.getYear());
		LocalDate easterMonday = easter.plusDays(1);
		if(date.getDayOfMonth() == easter.getDayOfMonth() && date.getMonthOfYear() == easter.getMonthOfYear())
			return true;
		if(date.getDayOfMonth() == easterMonday.getDayOfMonth() && date.getMonthOfYear() == easterMonday.getMonthOfYear())
			return true;
		//if((date.getDayOfWeek() == 7)||(date.getDayOfWeek() == 6))
		//	return true;		
		if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 25))
			return true;
		if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 26))
			return true;
		if((date.getMonthOfYear() == 12) && (date.getDayOfMonth() == 8))
			return true;
		if((date.getMonthOfYear() == 6) && (date.getDayOfMonth() == 2))
			return true;
		if((date.getMonthOfYear() == 4) && (date.getDayOfMonth() == 25))
			return true;
		if((date.getMonthOfYear() == 5) && (date.getDayOfMonth() == 1))
			return true;
		if((date.getMonthOfYear() == 8) && (date.getDayOfMonth() == 15))
			return true;
		if((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 1))
			return true;
		if((date.getMonthOfYear() == 1) && (date.getDayOfMonth() == 6))
			return true;
		if((date.getMonthOfYear() == 11) && (date.getDayOfMonth() == 1))
			return true;
		if((date.getMonthOfYear() == confGeneral.monthOfPatron && date.getDayOfMonth() == confGeneral.dayOfPatron))
			return true;
		/**
		 * ricorrenza centocinquantenario dell'unità d'Italia
		 */
		if(date.isEqual(new LocalDate(2011,3,17)))
			return true;
			
		return false;
	}

	/**
	 * 
	 * @param month
	 * @param day
	 * @return
	 */
	public static boolean isFebruary29th(int month, int day)
	{
		return (month==2 && day==29);
	}


	/**
	 *  * La lista di tutti i giorni fisici contenuti nell'intervallo [begin,end] estremi compresi, escluse le general holiday
	 * @param begin
	 * @param end
	 * @return
	 */
	public static List<LocalDate> getGeneralWorkingDays(LocalDate begin, LocalDate end)
	{
		LocalDate day = begin;
		List<LocalDate> generalWorkingDays = new ArrayList<LocalDate>();
		while(!day.isAfter(end))
		{
			if( ! DateUtility.isGeneralHoliday(day) )
				generalWorkingDays.add(day);
			day = day.plusDays(1);
		}
		return generalWorkingDays;
	}
	
	/**
	 * 
	 * @param date
	 * @param interval
	 * @return true se date ricade nell'intervallo estremi compresi
	 */
	public static boolean isDateIntoInterval(LocalDate date, DateInterval interval)
	{
		if(date==null)
			date = setInfinity();
		
		if(date.isBefore(interval.getBegin()) || date.isAfter(interval.getEnd()))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @param inter1
	 * @param inter2
	 * @return l'intervallo contenente l'intersezione fra inter1 e inter2,
	 * null in caso di intersezione vuota
	 */
	public static DateInterval intervalIntersection(DateInterval inter1, DateInterval inter2)
	{
		if(inter1==null || inter2==null)
			return null;
		//ordino
		if(!inter1.getBegin().isBefore(inter2.getBegin()))
		{
			DateInterval aux = new DateInterval(inter1.getBegin(), inter1.getEnd());
			inter1 = inter2;
			inter2 = aux;
		}
		
		
		//un intervallo contenuto nell'altro
		if(isIntervalIntoAnother(inter1, inter2) )
		{
			return inter1;
		}
		
		if(isIntervalIntoAnother(inter2, inter1) )
		{
			return inter2;
		}
		
		//fine di inter1 si interseca con inizio di inter2
		if(inter1.getEnd().isBefore(inter2.getBegin()))
		{
			return null;
		}
		else
		{
			return new DateInterval(inter2.getBegin(), inter1.getEnd());
		}
		
	}
	
	/**
	 * 
	 * @param inter
	 * @return conta il numero di giorni appartenenti all'intervallo estremi compresi
	 */
	public static int daysInInterval(DateInterval inter)
	{
		return inter.getEnd().getDayOfYear() - inter.getBegin().getDayOfYear();
	}
	
	/**
	 * 
	 * @param inter
	 * @param another
	 * @return true se l'intervallo inter e' contenuto nell'intervallo another (estremi compresi), false altrimenti
	 */
	public static boolean isIntervalIntoAnother(DateInterval inter, DateInterval another)
	{
		
		if(inter.getBegin().isBefore(another.getBegin()) || inter.getEnd().isAfter(another.getEnd()) )
		{
			return false;
		}
		return true;
	}
	
	/**
	 * 
	 * @return la data infinito
	 */
	public static LocalDate setInfinity()
	{
		return new LocalDate(9999,12,31);
	}
	
	public static boolean isInfinity(LocalDate date)
	{
		LocalDate infinity = new LocalDate(9999,12,31);
		if(date.equals(infinity))
			return true;
		else
			return false;
	}

	/**
	 * 
	 * @param monthNumber
	 * @return il nome del mese con valore monthNumber
	 * 			null in caso di argomento non valido 
	 */
	public static String fromIntToStringMonth(Integer monthNumber)
	{
		LocalDate date = new LocalDate().withMonthOfYear(monthNumber);
		return date.monthOfYear().getAsText();
	}
	
	public static String getName(int mese)
	{
		if(mese==1)
			return "Gennaio";
		if(mese==2)
			return "Febbraio";
		if(mese==3)
			return "Marzo";
		if(mese==4)
			return "Aprile";
		if(mese==5)
			return "Maggio";
		if(mese==6)
			return "Giugno";
		if(mese==7)
			return "Luglio";
		if(mese==8)
			return "Agosto";
		if(mese==9)
			return "Settembre";
		if(mese==10)
			return "Ottobre";
		if(mese==11)
			return "Novembre";
		if(mese==12)
			return "Dicembre";
		else
			return null;
	}

	/**
	 * 
	 * @param minute
	 * @return
	 */
	public static String fromMinuteToHourMinute(int minute)
	{
		if(minute==0)
			return "00:00";
		
		String s = "";
		if(minute<0)
		{
			s = s + "-";
			minute = minute * -1;
		}
		int hour = minute / 60;
		int min  = minute % 60;
		
		if(hour<10)
		{
			s = s + "0" + hour;
		}
		else
		{
			s = s + hour;
		}
		s = s + ":";
		if(min<10)
		{
			s = s + "0" + min;
		}
		else
		{
			s = s + min;
		}
		return s;
	}
	
	public static String fromLocalDateTimeHourTime(LocalDateTime time)
	{
		int min = time.getMinuteOfHour();
		int hour = time.getHourOfDay();
		return String.format("%02d:%02d", hour, min);    
	}
}
