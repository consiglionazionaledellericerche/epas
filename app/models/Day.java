package models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.joda.time.LocalDate;


public class Day {

	private Person person;
	
	private boolean mealTicket;
	
	private List<AbsenceType> absenceTypeList;
	
	private List<Stamping> stampingList;
	
	private String workingTimeType;
	
	private int timeAtWork;
	
	private int difference;
	
	private int progressive;
		
	
	public List<AbsenceType> absenceList(Person person, Date date){
		List<AbsenceType> listaAssenze = new ArrayList<AbsenceType>();
		listaAssenze = AbsenceType.find("Select abs, at from Absence abs, AbsenceType at where " +
				"abs.AbsenceType = at and abs.person = ? and abs.date = ? ", person, date).fetch();
		return listaAssenze;
		
	}	
	
	public int mealTicketToUse(){
		return 0;
		
	}
	
	public int mealTicketToReturn(){
		return 0;
	}
	
	public int officeWorkingDay(){
		return 0;
	}
	
	/**
	 * 
	 * @param date
	 * @return calcola il numero di minuti di cui è composta la data passata come parametro (di cui considera solo
	 * ora e minuti
	 */
	@SuppressWarnings("deprecation")
	public int toMinute(Date date){
		int dateToMinute = 0;
		if (date!=null){
			int hour = date.getHours();
			int minute = date.getMinutes();
			dateToMinute = (60*hour)+minute;
		}
		return dateToMinute;
	}
	
	/**
	 * 
	 * @param data
	 * @return true se il giorno in questione è un giorno di festa. False altrimenti
	 */
	public boolean isHoliday(Date data){
		if (data!=null){
			
			LocalDate date = new LocalDate(); 
			date.fromDateFields(data);
			if(date.getDayOfWeek() == 7)
				return true;
			else{
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
			}
			
		}
		return false;
	}
	
	/**
	 * 
	 * @param date
	 * @param person
	 * @return true se per quella data, quella persona aveva un giorno di lavoro. False altrimenti
	 */
	public boolean isWorkingDay(Date date, Person person){
		
		WorkingTimeTypeDay wttd = (WorkingTimeTypeDay) WorkingTimeTypeDay.find("Select wttd from WorkingTimeType w," +
				"WorkingTimeTypeDay wttd where w.WorkingTimeTypeDay = wttd and w.person = ? and w.date = ? ",person, date).fetch();
		if(wttd.holiday == true){
			return true;
		}
		else{
			return false;
		}
	
	}
	
	/**
	 * 
	 * @param date
	 * @return numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	public int timeAtWork(Date date){
		
		List<Stamping> listStamp = Stamping.find("select s, st from Stamping s " +
			    "where s.person = ? and s.date = ? order by date", person, date).fetch();
		
		int size = listStamp.size();
		timeAtWork = 0;
		if(size%2 != 0){
			/**
			 * vuol dire che ci sono più timbrature di ingresso rispetto alle uscite o viceversa,
			 * come devo gestire questa cosa???
			 */
		}
		else{
			
			Iterator<Stamping> iter = listStamp.iterator();
			while(iter.hasNext()){
				Stamping s = iter.next();
				if(s.way == Stamping.WayType.in){
					timeAtWork -= toMinute(s.date);					
				}
				if(s.way == Stamping.WayType.out){
					timeAtWork += toMinute(s.date);
				}
			}
		}
		return timeAtWork;
		
	}
	
	/**
	 * 
	 * @param date
	 * @return la differenza in minuti tra l'orario giornaliero e quello effettivamente lavorato dalla persona
	 */
	public int difference(Date date){
		if(date != null){
			LocalDate data = new LocalDate();
			data.fromDateFields(date);
			if((data.getDayOfMonth()==1) && (data.getDayOfWeek()==6 || data.getDayOfWeek()==7)){
				return 0;
			}
			else{
				int minutiDiLavoro = timeAtWork(date);
				int orarioGiornaliero = 432; //valore in minuti di una giornata lavorativa
				difference = orarioGiornaliero-minutiDiLavoro;
			}
			
		}
		return difference;
	}
	
	/**
	 * 
	 * @param date
	 * @return il progressivo delle ore in più o in meno rispetto al normale orario previsto per quella data
	 */
	public int progressive(Date date){
		if(date != null){
			LocalDate data = new LocalDate();
			data.fromDateFields(date);
			if((data.getDayOfMonth()==1) && (data.getDayOfWeek()==6 || data.getDayOfWeek()==7))
				return 0;			
			if((data.getDayOfMonth()==2) && (data.getDayOfWeek()==7))
				return 0;
			else{
				progressive = progressive+difference;
			}
		}
		return progressive;
	}
	
	/**
	 * 
	 * @param person
	 * @return il nome del tipo di orario per quella persona
	 */
	public String workingTimeType(Person person){
		WorkingTimeType wtt = (WorkingTimeType) WorkingTimeType.find("Select wtt from WorkingTimeType where wtt.person = ? ", person).fetch();
		workingTimeType = wtt.description;
		
		return workingTimeType;
		
	}
	
	/**
	 * 
	 * @param date
	 * @param timeAtWork
	 * @param person
	 * @return se la persona può usufruire del buono pasto per quella data
	 */
	public boolean mealTicket(Date date, int timeAtWork, Person person){
		if(timeAtWork >= 432)
			return true;
		else
			return false;
	}
}
