package it.cnr.iit.epas;

import com.google.common.base.Optional;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.MonthDay;
import org.joda.time.YearMonth;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class DateUtility {

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
	
	public static boolean isGeneralHoliday(Optional<MonthDay> officePatron, LocalDate date){
		 
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
		
		if(officePatron.isPresent()){
			
			return (date.getMonthOfYear() == officePatron.get().getMonthOfYear()  && 
					date.getDayOfMonth() == officePatron.get().getDayOfMonth());
		}

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
	public static boolean isFebruary29th(int month, int day){
		
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
			if( !DateUtility.isGeneralHoliday(Optional.<MonthDay>absent(), day) )
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
		return inter.getEnd().getDayOfYear() - inter.getBegin().getDayOfYear() + 1;
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
	
	/**
	 * 
	 * @param month
	 * @return il numero corrispondente al mese passato come parametro
	 */
	public static int fromStringToIntMonth(String month){
		if(month.equals("Gennaio"))
			return 1;
		if(month.equals("Febbraio"))
			return 2;
		if(month.equals("Marzo"))
			return 3;
		if(month.equals("Aprile"))
			return 4;
		if(month.equals("Maggio"))
			return 5;
		if(month.equals("Giugno"))
			return 6;
		if(month.equals("Luglio"))
			return 7;
		if(month.equals("Agosto"))
			return 8;
		if(month.equals("Settembre"))
			return 9;
		if(month.equals("Ottobre"))
			return 10;
		if(month.equals("Novembre"))
			return 11;
		if(month.equals("Dicembre"))
			return 12;
		else
			return 0;
		
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
	
	public static String fromLocalDateTimeHourTime(LocalDateTime time){
		return time.toString("HH:mm");
	}
	

	/**
	 * @param date
	 * @param pattern : default dd/MM
	 * @return Effettua il parsing di una Stringa che contiene solo giorno e Mese
	 * 
	 */
	public static LocalDate dayMonth(String date,Optional<String> pattern){
		DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern.isPresent() ? pattern.get() : "dd/MM");

		return LocalDate.parse(date,dtf);
	}
	
	public static LocalDate getMonthFirstDay(YearMonth yearMonth){
		return yearMonth.toInterval().getStart().toLocalDate();
	}
	
	public static LocalDate getMonthLastDay(YearMonth yearMonth){
		return yearMonth.toInterval().getEnd().minusDays(1).toLocalDate();
	}
	
	/**
	 * Calcola il numero di minuti trascorsi dall'inizio del giorno 
	 * all'ora presente nella data.
	 * @param date
	 * @return
	 */
	public static int toMinute(LocalDateTime date){
		int dateToMinute = 0;
		if (date!=null) {
			int hour = date.get(DateTimeFieldType.hourOfDay());
			int minute = date.get(DateTimeFieldType.minuteOfHour());
			dateToMinute = (60 * hour) + minute;
		}
		return dateToMinute;
	}
	
	/*
	 * @param begin: orario di ingresso
	 * @param end: orario di uscita
	 * @return minuti lavorati
	 */
	 public static Integer getDifferenceBetweenLocalTime(LocalTime begin, LocalTime end) {
		int timeToMinute = 0;
		if (end != null && begin != null)
		{
			int hourBegin = begin.getHourOfDay();
			int minuteBegin = begin.getMinuteOfHour();
			int hourEnd = end.getHourOfDay();
			int minuteEnd = end.getMinuteOfHour();
			timeToMinute = ((60 * hourEnd + minuteEnd) - (60 * hourBegin + minuteBegin));
		} 
		
		return timeToMinute;
	}
}
