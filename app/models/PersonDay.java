/**
 * 
 */
package models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import lombok.Data;
import models.Stamping.WayType;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.db.jpa.Model;

/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo di una persona.
 *  
 * @author cristian
 *
 */
public class PersonDay extends Model {

	private final Person person;
	
	public final LocalDate date;
	
	public final LocalDateTime startOfDay;
	public final LocalDateTime endOfDay;
	
	private List<Stamping> stampings = null;
	
	private AbsenceType absenceType = null;
	
	private Absence absence = null;	
			
	/**
	 * Totale del tempo lavorato nel giorno in minuti 
	 */
	private Integer timeAtWork;

	private static int progressive = 0;

	public int difference;
	
	public PersonDay(Person person, LocalDate date) {
		this.person = person;
		this.date = date;
		this.startOfDay = new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0);
		this.endOfDay = new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 23, 59);
	}
	
	/**	 
	 * Calcola se un giorno è lavorativo o meno. L'informazione viene calcolata a partire
	 * dal giorno e dal WorkingTimeType corrente della persona
	 * 
	 * @return true se il giorno corrente è un giorno lavorativo, false altrimenti.
	 */
	public boolean isWorkingDay() {
		boolean isWorkingDay = false;
	
		WorkingTimeTypeDay wttd2 = WorkingTimeTypeDay.find("Select wttd from WorkingTimeType wtt, WorkingTimeTypeDay wttd, Person p " +
				"where p.workingTimeType = wtt and wttd.workingTimeType = wtt and p = ? and wttd.dayOfWeek = ? ", person, date.getDayOfWeek()).first();
		Logger.warn("In isWorkingDay il giorno chiamato è: " +date.getDayOfWeek() +" mentre la persona è: " +person.id);
		isWorkingDay = wttd2.holiday;
		return isWorkingDay;
	}
	
	/**
	 * 
	 * @return true nel caso in cui la persona sia stata assente
	 */
	public boolean isAbsent() {
		if (getAbsence() != null) {
			if (!stampings.isEmpty()) {
				Logger.warn("Attenzione il giorno %s è presente un'assenza (%s) (Person = %s), però ci sono anche %d timbrature.", 
					absence, absence.person, stampings.size());
			}
		}
		return absence != null;
	}
	
	/**
	 * 
	 * @return l'assenza relativa a quella persona in quella data
	 */
	public Absence getAbsence(){
		if(absence == null){
			absence = Absence.find("Select abs from Absence abs where abs.person = ? and abs.date = ?", person, date).first();
		}
		return absence;
		
	}
	/**
	 * 
	 * @return l'absenceType relativo alla persona in quella data
	 */
	public AbsenceType getAbsenceType() {
		if (absenceType == null) {
			
			absenceType = AbsenceType.find("SELECT abt FROM Absence abs, AbsenceType abt, Person p WHERE abt = abs.absenceType AND " +
					"abs.person = p AND p = ? AND abs.date = ? ", person, date).first();
			
		}
		return absenceType;
	}
	
	/**
	 * 
	 * @return la lista di timbrature giornaliere
	 */
//	public List<Stamping> getStampings() {
//		if (stampings == null) {
//			
//			stampings = Stamping.find("SELECT s FROM Stamping s " +
//					"WHERE s.person = ? and date between ? and ? " +
//					"ORDER BY date", person, startOfDay, endOfDay).fetch();		
////			if(stampings.size()%2 != 0){
////				int hour= stampings.get(0).date.getHourOfDay();
////				int minute=stampings.get(0).date.getMinuteOfHour();
////				for(int i = 1; i<stampings.size();i++){
////					LocalDateTime ldt = stampings.get(i).date;
////					if(hour-ldt.getHourOfDay()<1 && minute-ldt.getMinuteOfHour()<30){
////						
////					}
////					
////					
////				}			
////				
////			}
//			
//		}
//		return stampings;
//	}
	
	public List<Stamping> getStampings() {
        
        if (stampings == null) {
            stampings = new LinkedList<Stamping>();
            List<Stamping> dbStamping = Stamping.find("SELECT s FROM Stamping s " +
                            "WHERE s.person = ? and date between ? and ? " +
                            "ORDER BY date", person, startOfDay, endOfDay).fetch();
            
            if(dbStamping.size()/2==1 && dbStamping.size()%2==1){
            	int i = 0;
            	Stamping s = dbStamping.get(i+1);
            	if(s.way == dbStamping.get(i).way && s.way == WayType.in){
        			stampings.add(0, dbStamping.get(i));
        			stampings.add(1, null);
        			stampings.add(2, dbStamping.get(i+1));
        			stampings.add(3, dbStamping.get(i+2));
        		}
            	if(s.way == dbStamping.get(i+2).way && s.way == WayType.out){
            		stampings.add(0, dbStamping.get(i));
        			stampings.add(1, dbStamping.get(i+1));
        			stampings.add(2, null);
        			stampings.add(3, dbStamping.get(i+2));
            	}
            	                    
            }
            else{
            	for(int i = 0; i < dbStamping.size(); i++)
            		stampings.add(i, dbStamping.get(i));
            }
                
        }
        return stampings;
	}
	
	
	/**
	 * 
	 * @param data
	 * @return true se il giorno in questione è un giorno di festa. False altrimenti
	 */
	public boolean isHoliday(){
		if (date!=null){
			WorkingTimeType wtt = WorkingTimeType.findById(person.workingTimeType.id);
			
			if(wtt.description.equals("normale-mod")){
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
			}
						
		}
		return false;
	}
	
	/**
	 * 
	 * @param date
	 * @return numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	public int timeAtWork(){
		
		if(stampings == null){
			stampings = getStampings();
			
		}		
		if(stampings.contains(null)){
			/**
			 * in questo caso si guarda quale posizione della linkedList è null per stabilire se sia mancante un ingresso o un'uscita
			 */
			if(stampings.get(0)==null){
				/**
				 * è mancante la prima entrata, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla seconda 
				 * entrata alla seconda uscita
				 */
				Stamping enter = stampings.get(2);
				Stamping exit = stampings.get(3);
				timeAtWork = toMinute(exit.date)-toMinute(enter.date);
				
			}
			if(stampings.get(1)==null){
				/**
				 * è mancante la prima uscita, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla seconda 
				 * entrata alla seconda uscita
				 */
				Stamping enter = stampings.get(2);
				Stamping exit = stampings.get(3);
				timeAtWork = toMinute(exit.date)-toMinute(enter.date);
				
			}
			if(stampings.get(2)==null){
				/**
				 * è mancante la seconda entrata, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla prima
				 * entrata alla prima uscita
				 */
				Stamping enter = stampings.get(0);
				Stamping exit = stampings.get(1);
				timeAtWork = toMinute(exit.date)-toMinute(enter.date);
				
			}
			if(stampings.get(3)==null){
				/**
				 * è mancante la seconda uscita, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla prima
				 * entrata alla prima uscita
				 */
				Stamping enter = stampings.get(0);
				Stamping exit = stampings.get(1);
				timeAtWork = toMinute(exit.date)-toMinute(enter.date);
				
			}
			return timeAtWork;
		}		
		else{
			int size = stampings.size();
			timeAtWork = 0;
			LocalDateTime now = new LocalDateTime().now();
			if(size > 0){
				Stamping s = stampings.get(0);
				if(s.date.getDayOfMonth()==now.getDayOfMonth() && s.date.getMonthOfYear()==now.getMonthOfYear() && 
					s.date.getYear()==now.getYear()){
						if(((size / 2 == 1) && (size % 2 == 1)) || ((size / 2 == 0) && (size % 2 == 1))){
							
							int nowToMinute = toMinute(now);
							int workingTime=0;
							for(Stamping st : stampings){
								if(st.way == Stamping.WayType.in)
									workingTime -= toMinute(st.date);				
								if(st.way == Stamping.WayType.out)
									workingTime += toMinute(st.date);
								if(workingTime < 0)
									timeAtWork = nowToMinute + workingTime;
								else 
									timeAtWork = nowToMinute - workingTime;
								
							}
								
						}				
				}				
				else{
					int workTime=0;
					for(Stamping st : stampings){
						if(st.way == Stamping.WayType.in){
							workTime -= toMinute(st.date);
							//timeAtWork -= toMinute(s.date);		
							System.out.println("Timbratura di ingresso: "+workTime);	
						}
						if(st.way == Stamping.WayType.out){
							workTime += toMinute(st.date);
							//timeAtWork += toMinute(s.date);
							System.out.println("Timbratura di uscita: "+workTime);
						}
						timeAtWork = workTime;
					}
					
				}
			}

		}
			
		return timeAtWork;
	}
	
	/**
	 * 
	 * @param date
	 * @return il progressivo delle ore in più o in meno rispetto al normale orario previsto per quella data
	 */
	public int getProgressive(){
		if(progressive == 0){
			if(date != null){
			
				if((date.getDayOfMonth()==1) && (date.getDayOfWeek()==6 || date.getDayOfWeek()==7))
					return 0;			
				if((date.getDayOfMonth()==2) && (date.getDayOfWeek()==7))
					return 0;
				else{
					progressive = progressive+difference;
				}
			}
		}
		
		return progressive;
	}
	
	
	/**
	 * 
	 * @param date
	 * @return calcola il numero di minuti di cui è composta la data passata come parametro (di cui considera solo
	 * ora e minuti
	 */
	private static int toMinute(LocalDateTime date){
		int dateToMinute = 0;
		
		if (date!=null){
			int hour = date.get(DateTimeFieldType.hourOfDay());
			int minute = date.get(DateTimeFieldType.minuteOfHour());
			
			dateToMinute = (60*hour)+minute;
		}
		return dateToMinute;
	}
	
	/**
	 * 
	 * @param person
	 * @param date
	 * @return la lista di codici di assenza fatti da quella persona in quella data
	 */
	public List<Absence> absenceList() {
		
		List<Absence> absenceList = new ArrayList<Absence>();
		absenceList = Absence.find("SELECT abs FROM Absence abs " +
				" WHERE abs.person = ? AND abs.date = ?", person, date).fetch();

		if(absenceList != null){
			for (Absence abs : absenceList) {
				Logger.warn("Codice: " +abs.absenceType.code);
			}
		}
		else
			Logger.warn("Non ci sono assenze" );
	
		return absenceList;
		
	}
	
	/**
	 * 
	 * @param date
	 * @param timeAtWork
	 * @param person
	 * @return se la persona può usufruire del buono pasto per quella data
	 */
	public boolean mealTicket(){
		boolean isMealTicketAvailable = false;
		if(stampings==null){
			stampings=getStampings();
		}
		if (timeAtWork == null) {
			timeAtWork = timeAtWork();
			
		}
		if(timeAtWork == 0){
			isMealTicketAvailable = false;
		}
		else{
			if(person.workingTimeType.description.equals("normale-mod") && timeAtWork>=432)
				isMealTicketAvailable=true;
			if(person.workingTimeType.description.equals("normale-mod") && timeAtWork>360 && timeAtWork<390 &&(stampings.size()==4 && checkMinTimeForLunch(stampings)<30))
				isMealTicketAvailable=true;
			if(person.workingTimeType.description.equals("normale-mod") && timeAtWork>360 && timeAtWork<390 && (stampings.size()==2))
				isMealTicketAvailable=true;
			if(person.workingTimeType.description.equals("normale-mod") && timeAtWork>390 && timeAtWork<432 && (stampings.size()==4 || stampings.size()==2))
				isMealTicketAvailable=true;
			
		}
		return isMealTicketAvailable;

	}
	

	
	/**
	 * 
	 * @return la differenza tra l'orario di lavoro giornaliero e l'orario standard in minuti
	 */
	public Integer getDifference(){
		if(date != null){
			if((date.getDayOfWeek()==DateTimeConstants.SATURDAY ) && (date.getDayOfMonth()==1))
				return 0;		
			if((date.getDayOfWeek()==DateTimeConstants.SUNDAY) && (date.getDayOfMonth()==1))
				return 0;
			if((date.getDayOfMonth()==2) && (date.getDayOfWeek()==DateTimeConstants.SUNDAY))
				return 0;
			if((date.getDayOfWeek()==DateTimeConstants.SUNDAY) || date.getDayOfWeek()==DateTimeConstants.SATURDAY)
				return 0;
			List<Absence> absenceList = absenceList();
			List<Stamping> stampingList = getStampings();
			if(absenceList.size()!=0){
				difference = 0;
			}
			if(stampingList.size()==0){
				difference = 0;
			}
			else{
				int minTimeWorking = 432;
				timeAtWork = timeAtWork();
				difference = timeAtWork-minTimeWorking;
				//return difference;
			}
				
		}
		
		return difference;
	}
	
	/**
	 * 
	 * @param stamping
	 * @return controlla il numero di timbrature e l'orario in cui sono state fatte
	 */
	private boolean checkStamping(List<Stamping> stamping){
		boolean check = false;
		if(stamping.size()==1){
			Stamping s = stamping.get(0);
			if(s.date.getHourOfDay() > 12 && s.date.getHourOfDay() < 14){
				/**
				 * cosa fare nel caso ci sia una sola timbratura? io sarei per analizzare a che ora è stata fatta e, di conseguenza,
				 * trovare il corretto rimedio
				 */
			}
		}
		if(stamping.size()%2!=0){
			/**
			 * e se ci sono timbrature dispari? cosa bisogna fare?
			 */
			for(Stamping s : stamping){
				
			}
		}
				
		return check;
	}
	
	/**
	 * 
	 * @param stamping
	 * @return l'oggetto StampModificationType che ricorda, eventualmente, se c'è stata la necessità di assegnare la mezz'ora
	 * di pausa pranzo a una giornata a causa della mancanza di timbrature intermedie oppure se c'è stata la necessità di assegnare
	 * minuti in più a una timbratura di ingresso dopo la pausa pranzo poichè il tempo di pausa è stato minore di 30 minuti e il tempo
	 * di lavoro giornaliero è stato maggiore stretto di 6 ore.
	 */
	public StampModificationType checkTimeForLunch(List<Stamping> stamping){
		//String s = "p";
		StampModificationType smt = null;
		if(stamping.size()==2){
			int workingTime=0;
			for(Stamping st : stamping){
				if(st.way == Stamping.WayType.in){
					workingTime -= toMinute(st.date);
					//timeAtWork -= toMinute(s.date);		
					System.out.println("Timbratura di ingresso in checkTimeForLunch: "+workingTime);	
				}
				if(st.way == Stamping.WayType.out){
					workingTime += toMinute(st.date);
					//timeAtWork += toMinute(s.date);
					System.out.println("Timbratura di uscita in checkTimeForLunch: "+workingTime);
				}
				timeAtWork += workingTime;
			}
			if(workingTime >= 390)
				smt = StampModificationType.findById(StampModificationTypeValue.FOR_DAILY_LUNCH_TIME.getId());
			else
				smt = StampModificationType.findById(StampModificationTypeValue.NOTHING_TO_CHANGE.getId());
					
		}	
		if(stamping.size()==4 && !stamping.contains(null)){
			int hourExit = stamping.get(1).date.getHourOfDay();
			int minuteExit = stamping.get(1).date.getMinuteOfHour();
			int hourEnter = stamping.get(2).date.getHourOfDay();
			int minuteEnter = stamping.get(2).date.getMinuteOfHour();
			int workingTime=0;
			for(Stamping st : stamping){
				if(st.way == Stamping.WayType.in){
					workingTime -= toMinute(st.date);
					
				}
				if(st.way == Stamping.WayType.out){
					workingTime += toMinute(st.date);
					
				}
				timeAtWork += workingTime;
			}
			if(((hourEnter*60)+minuteEnter) - ((hourExit*60)+minuteExit) < 30 && workingTime > 360){
				smt = StampModificationType.findById(StampModificationTypeValue.FOR_MIN_LUNCH_TIME.getId());
			}
			
		}
		
		return smt;
	}
	
	/**
	 * 
	 * @param timeAtWork
	 * @return il localdatetime corrispondente al numero di minuti di lavoro giornaliero.
	 * Questo metodo mi occorre per normalizzare il tempo di lavoro secondo il metodo toCalendarTime() creato nella PersonTags
	 */
	public LocalDateTime convertTimeAtWork(int timeAtWork){
		int hour = (int)timeAtWork/60;
		int minute = (int)timeAtWork%60;
		LocalDateTime ldt = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),hour,minute,0);
		return ldt;
	}
	
	/**
	 * 
	 * @param stamping
	 * @return il numero di minuti che sono intercorsi tra la timbratura d'uscita per la pausa pranzo e la timbratura d'entrata
	 * dopo la pausa pranzo. Questo valore verrà poi controllato per stabilire se c'è la necessità di aumentare la durata della pausa
	 * pranzo intervenendo sulle timbrature
	 */
	public int checkMinTimeForLunch(List<Stamping> stamping){
		int min=0;
		if(stamping.size()==4 && !stamping.contains(null)){
			int minuteExit = toMinute(stamping.get(1).date);
				
			int minuteEnter = toMinute(stamping.get(2).date);
			
			min = minuteEnter - minuteExit;			
			
		}
		return min;
	}
	
	/**
	 * funzione che restituisce la timbratura di ingresso dopo la pausa pranzo incrementata del numero di minuti 
	 * che occorrono per arrivare ai 30 minimi per la pausa stessa.
	 * @param ldt1
	 * @param ldt2
	 * @return
	 */
	public LocalDateTime adjustedStamp(LocalDateTime ldt1, LocalDateTime ldt2){
		LocalDateTime ld2mod = null;
		int minuti1 = toMinute(ldt1);
		int minuti2 = toMinute(ldt2);
		int difference = minuti2-minuti1;
		if(difference<30){
			Integer adjust = 30-difference;
			ld2mod = ldt2.plusMinutes(adjust);
			return ld2mod;
		}
		
		return ld2mod;
	}
	
	/**
	 * 
	 * @param difference
	 * @return
	 */
	public LocalDateTime convertDifference(int difference){
		
		if(difference>=0){
			int hour = (int)difference/60;
			int minute = (int)difference%60;
			LocalDateTime ldt = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),hour,minute,0);
			return ldt;
		}
		else{
			difference = -difference;
			int hour = (int)difference/60;
			int minute = (int)difference%60;
			LocalDateTime ldt = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),hour,minute,0);
			return ldt;
		}
	}
	
}
