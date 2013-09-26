package it.cnr.iit.epas;

import javax.persistence.EntityManager;

import models.Configuration;
import models.Person;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import play.Logger;
import play.db.jpa.JPA;

public class DateUtility {

	public static boolean isHoliday(Person person, LocalDate date){
		Configuration config = Configuration.getConfiguration(date.toDate());	
		
		Logger.trace("configurazione: %s con localdate: %s", config, date);
		
		if(person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).holiday)
			return true;
		if(isGeneralHoliday(date))
			return true;
		else
			return false;

	}

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
		
		Configuration config = Configuration.getConfiguration(date.toDate());
		LocalDate easter = findEaster(date.getYear());
		LocalDate easterMonday = easter.plusDays(1);
		if(date.getDayOfMonth() == easter.getDayOfMonth() && date.getMonthOfYear() == easter.getMonthOfYear())
			return true;
		if(date.getDayOfMonth() == easterMonday.getDayOfMonth() && date.getMonthOfYear() == easterMonday.getMonthOfYear())
			return true;
		if((date.getDayOfWeek() == 7)||(date.getDayOfWeek() == 6))
			return true;		
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
		if((date.getMonthOfYear() == config.monthOfPatron && date.getDayOfMonth() == config.dayOfPatron))
			return true;
		/**
		 * ricorrenza centocinquantenario dell'unità d'Italia
		 */
		if(date.isEqual(new LocalDate(2011,3,17)))
			return true;
			
		return false;
	}

	public static boolean isFebruary29th(int month, int day)
	{
		return (month==2 && day==29);
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
}
