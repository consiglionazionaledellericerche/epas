package models;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.Transient;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.db.jpa.Model;

import lombok.Data;

public class PersonMonth extends Model {
	
	private int year;
	
	private int month;
	
	private int workingDays;
	
	private LocalDate date;
	
	private Person person;
	
	private int daysAtWorkOnHoliday;
	
	private int daysAtWorkOnWorkingDays;
	
	private Integer workingHours;
	
	private int differenceHoursAtEndOfMonth;
	
	private int justifiedAbsence = 0;
	
	private int notJustifiedAbsence = 0;
	
	private int mealTicketToRender;
	
	@Transient
	public List<PersonMonth> persons = null;
	
	public PersonMonth(Person person, LocalDate data){
		this.person = person;	
		this.date = data;
	}
	
	public List<PersonMonth> getPersons(){
		if(persons != null){
			return persons;
		}
		persons = new ArrayList<PersonMonth>();
		List <Person> persone = Person.findAll();
		for(Person p : persone){
			persons.add(new PersonMonth(p, new LocalDate(date)));
		}
		
		return persons;
	}
//	public PersonMonth(Person person, LocalDate data){
//		this.person = person;
//		this.year = data.getYear();
//		this.month = data.getMonthOfYear();
//	}
	
	
	
	/**
	 * prepara una lista con le timbrature di una giornata
	 * @return
	 */
	private List<Stamping> getStampings(int day) {
		List<Stamping> stampings;
		LocalDateTime startOfDay = new LocalDateTime(date.getYear(), date.getMonthOfYear(), day, 0, 0);
		LocalDateTime endOfDay = new LocalDateTime(date.getYear(), date.getMonthOfYear(), day, 23, 59);
		stampings = Stamping.find("SELECT s FROM Stamping s " +
					"WHERE s.person = ? and date between ? and ? " +
					"ORDER BY date", person, startOfDay, endOfDay).fetch();							
		
		return stampings;
	}
	
	private List<AbsenceType> getAbsences(int day){
		LocalDate data = new LocalDate(date.getYear(),date.getMonthOfYear(), day);
		List<AbsenceType> absences;
		absences = AbsenceType.find("SELECT abt FROM AbsenceType abt, Absence abs, Person p " +
				"WHERE abs.person = p AND abs.absenceType = abt AND p = ? AND abs.date = ?", person, data).fetch();
		return absences;
	}
	
	/**
	 * calcola il numero di giorni lavorativi per il mese in questione.
	 * @return workingDays
	 */
	public int monthWorkingDays(){
		
		int month = date.getMonthOfYear();
		int year = date.getYear();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		int giorniLavorativi = 0;
		for (int day = 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++) {
			LocalDate newDate = new LocalDate(year,month,day);
			int giornoSettimana = newDate.getDayOfWeek();
			int meseAnno = newDate.getMonthOfYear();
			int giornoMese = newDate.getDayOfMonth();
			if(giornoSettimana == DateTimeConstants.SATURDAY || giornoSettimana == DateTimeConstants.SUNDAY 
					|| ((meseAnno == DateTimeConstants.DECEMBER) && (giornoMese == 25))
					|| ((meseAnno == DateTimeConstants.DECEMBER) && (giornoMese == 26)) 
					|| ((meseAnno == DateTimeConstants.DECEMBER) && (giornoMese == 8))
					|| ((meseAnno == DateTimeConstants.JUNE) && (giornoMese == 2)) 
					|| ((meseAnno == DateTimeConstants.APRIL) && (giornoMese == 25))
					|| ((meseAnno == DateTimeConstants.MAY) && (giornoMese == 1)) 
					|| ((meseAnno == DateTimeConstants.AUGUST) && (giornoMese == 15))
					|| ((meseAnno == DateTimeConstants.JANUARY) && (giornoMese == 1)) 
					|| ((meseAnno == DateTimeConstants.JANUARY) && (giornoMese == 6))
					|| ((meseAnno == DateTimeConstants.NOVEMBER) && (giornoMese == 1)))
				giorniLavorativi+=0;
			else
				giorniLavorativi++;
			
		}	
		workingDays = giorniLavorativi;
		return workingDays;
	}
	/**
	 * calcola il numero di giorni lavorativi che la persona ha avuto nel mese in questione a partire dalle timbrature giornaliere.
	 * @return workingDays
	 */
	public int getWorkingDays(){
		int month = date.getMonthOfYear();
		int year = date.getYear();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		int giorniLavoro = 0;
		for (int day = 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++) {			
			List<Stamping> timbrature = getStampings(day);
			if(timbrature.size() != 0){
				//System.out.println("La lista delle timbrature non è vuota e contiene " +timbrature.size()+ "elementi nel giorno " +day);
				giorniLavoro++;
			}
		}		
		daysAtWorkOnWorkingDays = giorniLavoro;
				
		return daysAtWorkOnWorkingDays;
	}
	
	/**
	 * calcola quanti giorni una persona ha lavorato in giorni festivi
	 * @return daysAtWorkOnHoliday
	 */
	public int workingDaysInHoliday(){
		int month = date.getMonthOfYear();
		int year = date.getYear();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		for (int day = 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++) {
			LocalDate data = new LocalDate(year, month, day);
			List<Stamping> timbrature = getStampings(day);
			if(timbrature.size() != 0){
				boolean festa = isHoliday(data);
				if (festa == true)
					daysAtWorkOnHoliday++;	
			}
				
		}				
				
		return daysAtWorkOnHoliday;
	}
	
//	/**
//	 * calcola, a partire dalla lista di PersonDay per il mese in questione, le ore effettivamente lavorate in quel mese.
//	 * @return workingHours
//	 */
//	public int monthHoursRecap(){
//		int month = date.getMonthOfYear();
//		int year = date.getYear();
//		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
//		firstDayOfMonth.set(year, month, 1);
//		for(int day= 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++){
//			workingHours += dailyTimeAtWork(day);
//		}			
//		
//		return workingHours;
//	}
	
	/**
	 * calcola il numero di buoni pasto da restituire a partire dai giorni di presenza effettuati nel mese precedente con più di
	 * 6 ore e 30 minuti di presenza a lavoro (390 è il conteggio in minuti delle 6 ore e 30 necessarie a poter ottenere un buono mensa)
	 * @return mealTicketToRender
	 */
	public int mealTicketToRender(){
		
		int month = date.getMonthOfYear();
		int year = date.getYear();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		for(int day= 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++){			
			int tempoLavoro = timeAtWork();
			if(tempoLavoro < 390)
				mealTicketToRender++;
		}				
		return mealTicketToRender;
	}
	
	/**
	 * calcola il numero di assenze giustificate (con codice di assenza) fatte dalla persona nel periodo considerato
	 * @return numberOfJustifiedAbsence
	 */
	public int getJustifiedAbsence(){
		int numberOfJustifiedAbsence = 0;
		int month = date.getMonthOfYear();
		int year = date.getYear();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		for(int day= 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++){
			LocalDate data = new LocalDate(year, month, day);
			List<AbsenceType> listaAssenze = AbsenceType.find("SELECT abt FROM AbsenceType abt, Absence abs, Person p " +
					"WHERE abs.person = p AND abs.absenceType = abt AND p = ? AND abs.date = ?", person, data).fetch();
			if(listaAssenze.size()!=0)
				numberOfJustifiedAbsence += listaAssenze.size();
		}
		
		justifiedAbsence = numberOfJustifiedAbsence;
		return justifiedAbsence;
	}
	
	/**
	 * calcola il numero di giorni in cui non ci sono assenze giustificate a partire dalla lista delle timbrature e dei codici 
	 * di assenza
	 * @return notJustifiedAbsence
	 */
	public int getNotJustifiedAbsence(){
		int numberOfNotJustifiedAbsence = 0;
		int month = date.getMonthOfYear();
		int year = date.getYear();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		for(int day= 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++){
			LocalDate data = new LocalDate(year, month, day);
			if(data.getDayOfWeek()!=6 && data.getDayOfWeek()!= 7){
				List<Stamping> timbrature = getStampings(day);
				List<AbsenceType> listaAssenze = getAbsences(day);
				if(timbrature.size() == 0  && listaAssenze.size()==0)
					numberOfNotJustifiedAbsence++;
			}
			
		}		
		notJustifiedAbsence = numberOfNotJustifiedAbsence;
		return notJustifiedAbsence;
	}
	
//	private int dailyTimeAtWork(int day){
//		LocalDateTime startOfDay = new LocalDateTime(date.getYear(), date.getMonthOfYear(), day, 0, 0);
//		LocalDateTime endOfDay = new LocalDateTime(date.getYear(), date.getMonthOfYear(), day, 23, 59);
//		int timeAtWork=0;
//		List<Stamping> listStamp = Stamping.find("SELECT s FROM Stamping s " +
//				"WHERE s.person = ? and date between ? and ? " +
//				"ORDER BY date", person, startOfDay, endOfDay).fetch();
//		int size = listStamp.size();
//		if(((size / 2 == 1) && (size % 2 == 1)) || ((size / 2 == 0) && (size % 2 == 1))){
//			int orelavoro=0;
//			for(Stamping s : listStamp){
//				if(s.way == Stamping.WayType.in)
//					orelavoro -= toMinute(s.date);				
//				if(s.way == Stamping.WayType.out)
//					orelavoro += toMinute(s.date);
//				if(orelavoro < 0)
//					timeAtWork += orelavoro;
//				else 
//					timeAtWork += -orelavoro;					
//			}
//			//return timeAtWork;	
//		}			
//		else{
//			int orealavoro=0;
//			for(Stamping s : listStamp){
//				if(s.way == Stamping.WayType.in){
//					orealavoro -= toMinute(s.date);								
//					System.out.println("Timbratura di ingresso: "+orealavoro);	
//				}
//				if(s.way == Stamping.WayType.out){
//					orealavoro += toMinute(s.date);						
//					System.out.println("Timbratura di uscita: "+orealavoro);
//				}
//				timeAtWork += orealavoro;
//			}				
//		}
//	
//		return timeAtWork;		
//	}
	/**
	 * 
	 * @return numero di minuti in cui una persona è stata a lavoro in quel mese
	 */
	public int timeAtWork(){
		int month = date.getMonthOfYear();
		int year = date.getYear();
		
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		firstDayOfMonth.set(year, month, 1);
		
		int timeAtWork = 0;
		for (int day = 1; day < firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH); day++){
			List<Stamping> listStamp = getStampings(day);
			
			int orelavoro=0;
			int size = listStamp.size();
			//System.out.println("La dimensione della lista è: " +size);
			if(listStamp.size() != 0){
				System.out.println("La dimensione della lista è: " +size);
				if((size / 2 == 2)&&(size % 2 == 0)){
					
					for(Stamping s : listStamp){
						if(s.way == Stamping.WayType.in){
							orelavoro -= toMinute(s.date);								
							System.out.println("Timbratura di ingresso: "+orelavoro);	
						}
						if(s.way == Stamping.WayType.out){
							orelavoro += toMinute(s.date);						
							System.out.println("Timbratura di uscita: "+orelavoro);
						}
						
					}
					timeAtWork += orelavoro;
				}
				else{
					//int orelavoro=0;
					for(Stamping s : listStamp){
						if(s.way == Stamping.WayType.in)
							orelavoro -= toMinute(s.date);				
						if(s.way == Stamping.WayType.out)
							orelavoro += toMinute(s.date);
						if(orelavoro < 0)
							timeAtWork += orelavoro;
						else 
							timeAtWork += -orelavoro;					
					}
					//return timeAtWork;	
				}
			}
			
		}
		return timeAtWork;		
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
	
	public boolean isHoliday(LocalDate data){
		if (data!=null){

			Logger.warn("Nel metodo isHoliday la data è: " +data);
			
			if((data.getDayOfWeek() == 7)||(data.getDayOfWeek() == 6))
				return true;		
			if((data.getMonthOfYear() == 12) && (data.getDayOfMonth() == 25))
				return true;
			if((data.getMonthOfYear() == 12) && (data.getDayOfMonth() == 26))
				return true;
			if((data.getMonthOfYear() == 12) && (data.getDayOfMonth() == 8))
				return true;
			if((data.getMonthOfYear() == 6) && (data.getDayOfMonth() == 2))
				return true;
			if((data.getMonthOfYear() == 4) && (data.getDayOfMonth() == 25))
				return true;
			if((data.getMonthOfYear() == 5) && (data.getDayOfMonth() == 1))
				return true;
			if((data.getMonthOfYear() == 8) && (data.getDayOfMonth() == 15))
				return true;
			if((data.getMonthOfYear() == 1) && (data.getDayOfMonth() == 1))
				return true;
			if((data.getMonthOfYear() == 1) && (data.getDayOfMonth() == 6))
				return true;
			if((data.getMonthOfYear() == 11) && (data.getDayOfMonth() == 1))
				return true;			
		}
		return false;
	}
}
