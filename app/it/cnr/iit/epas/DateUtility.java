package it.cnr.iit.epas;

import models.Configuration;
import models.Person;
import models.WorkingTimeType;

import org.joda.time.LocalDate;

import play.Logger;

public class DateUtility {

	public static boolean isHoliday(Person person, LocalDate date){
		Configuration config = Configuration.getConfiguration(date);	
		
		if(person.workingTimeType.getWorkingTimeFromWorkinTimeType(date.getDayOfWeek()).holiday)
			return true;
	
		LocalDate easter = findEaster(date.getYear());
		LocalDate easterMonday = easter.plusDays(1);

		if(date.getDayOfMonth() == easter.getDayOfMonth() && date.getMonthOfYear() == easter.getMonthOfYear())
			return true;
		if(date.getDayOfMonth() == easterMonday.getDayOfMonth() && date.getMonthOfYear() == easterMonday.getMonthOfYear())
			return true;
		
		//TODO: Mettere queste date in configurazione
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
}
