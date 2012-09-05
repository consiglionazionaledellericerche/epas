/**
 * 
 */
package models;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.criteria.Fetch;

import lombok.Data;
import models.Stamping.WayType;
import models.enumerate.PersonDayModificationType;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo di una persona.
 *  
 * @author cristian
 *
 */

/**
 * @author dario
 *
 */
@Entity
@Audited
@Table(name="person_days", uniqueConstraints = { @UniqueConstraint(columnNames={ "person_id", "date"}) })
public class PersonDay extends Model {

	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	@Required
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	public Integer timeAtWork;
	
	public Integer difference;
	
	public Integer progressive;
	
	public boolean isTicketAvailable;
	/**
	 * cambiare la configurazione del database: person in relazione uno a molti con personDay, personDay in relazione uno a molti con stampings
	 * e con absences che quindi non saranno più in relazione con person
	 */
	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY)
	public List<Stamping> stampings = new ArrayList<Stamping>();
	
	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY)
	public List<Absence> absences = new ArrayList<Absence>();
	
	@Enumerated(EnumType.STRING)
	public PersonDayModificationType modificationType;

	@Transient
	private AbsenceType absenceType = null;
	@Transient
	private Absence absence = null;	
	

	@Transient
	private boolean isMealTicketAvailable;

	
	public PersonDay(Person person, LocalDate date, int timeAtWork, int difference, int progressive) {
		this.person = person;
		this.date = date;
		this.timeAtWork = timeAtWork;
		this.difference = difference;
		this.progressive = progressive;
		
	}
	
	public PersonDay(Person person, LocalDate date){
		this(person, date, 0, 0, 0);
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
		Logger.debug("In isWorkingDay il giorno chiamato è: " +date.getDayOfWeek() +" mentre la persona è: " +person.id);
		isWorkingDay = wttd2.holiday;
		return isWorkingDay;
	}
	
	/**
	 * 
	 * @return true nel caso in cui la persona sia stata assente da rivedere alla luce del fatto che per un personDay ci possono essere uno
	 * o più giorni di assenza e la cosa si può evincere direttamente dal personDay senza fare select
	 */
	public boolean isAbsent() {
		if (getAbsence() != null) {
			if (!stampings.isEmpty()) {
				Logger.warn("Attenzione il giorno %s è presente un'assenza (%s) (Person = %s), però ci sono anche %d timbrature.", 
					absence, absence.personDay.person, stampings.size());
			}
		}
		return absence != null;
	}
	
	/**
	 * 
	 * @return l'assenza o le assenze relative a quella persona in quella data
	 */
	public List<Absence> getAbsence(){
	
		return absences;
		
	}
	
	/**
	 * metodo che popola la lista delle assenze collegate al personDay solo se la lista è vuota
	 */
	public void setAbsence(){
		if(absences == null){
			absences = Absence.find("Select abs from Absence where abs.date = ? and abs.personDay.person = ?", date, person).fetch();
		}
	}
	/**
	 * 
	 * @return l'absenceType relativo alla persona in quella data
	 */
	public AbsenceType getAbsenceType() {
		if (absenceType == null) {
			
			absenceType = AbsenceType.find("SELECT abt FROM Absence abs, AbsenceType abt, Person p WHERE abt = abs.absenceType AND " +
					"abs.personDay.person = p AND p = ? AND abs.date = ? ", person, date).first();
			
		}
		return absenceType;
	}
	
	/**
	 * 
	 * @param year
	 * @return il giorno in cui cade la pasqua
	 */
	private static final LocalDate findHolyDay(int year) {
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
	
	/**
	 * 
	 * @param data
	 * @return true se il giorno in questione è un giorno di festa. False altrimenti
	 */
	public boolean isHoliday(){
		if (date!=null){
			WorkingTimeType wtt = WorkingTimeType.findById(person.workingTimeType.id);
			
			if(wtt.description.equals("normale-mod")){
				LocalDate easter = findHolyDay(date.getYear());
				LocalDate easterMonday = easter.plusDays(1);
				
				Logger.trace("Il giorno di pasqua é %s", easter);
				Logger.trace("Il giorno di pasquetta é %s", easterMonday);
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
		int tempoLavoro = 0;

		
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
				tempoLavoro = toMinute(exit.date)-toMinute(enter.date);
				
			}
			if(stampings.get(1)==null){
				/**
				 * è mancante la prima uscita, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla seconda 
				 * entrata alla seconda uscita
				 */
				Stamping enter = stampings.get(2);
				Stamping exit = stampings.get(3);
				tempoLavoro = toMinute(exit.date)-toMinute(enter.date);
				
			}
			if(stampings.get(2)==null){
				/**
				 * è mancante la seconda entrata, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla prima
				 * entrata alla prima uscita
				 */
				Stamping enter = stampings.get(0);
				Stamping exit = stampings.get(1);
				tempoLavoro = toMinute(exit.date)-toMinute(enter.date);
				
			}
			if(stampings.get(3)==null){
				/**
				 * è mancante la seconda uscita, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla prima
				 * entrata alla prima uscita
				 */
				Stamping enter = stampings.get(0);
				Stamping exit = stampings.get(1);
				tempoLavoro = toMinute(exit.date)-toMinute(enter.date);

			}
			if(stampings.get(6)==null){
				Stamping enter1 = stampings.get(0);
				Stamping exit1 = stampings.get(1);
				Stamping enter2 = stampings.get(2);
				Stamping exit2 = stampings.get(3);
				Stamping enter3 = stampings.get(4);
				Stamping exit3 = stampings.get(5);
				tempoLavoro = ((toMinute(exit3.date)-toMinute(enter3.date))+(toMinute(exit2.date)-toMinute(enter2.date))+(toMinute(exit1.date)-toMinute(enter1.date)));

			}
			timeAtWork = tempoLavoro;
			return timeAtWork;
		}		
		else{
			int size = stampings.size();
			timeAtWork = 0;
			// questo contatore controlla se nella lista di timbrature c'è almeno una timbratura di ingresso, in caso contrario fa
			// ritornare 0 come tempo di lavoro.
			int count = 0;
			for(Stamping s : stampings){
				if(s.way == Stamping.WayType.in)
					count ++;
			}
			if(count == 0){
				timeAtWork = 0;
			}
			else{
				
				LocalDateTime now = new LocalDateTime();
				if(size > 0){
					Stamping s = stampings.get(0);
					if(s.date.getDayOfMonth()==now.getDayOfMonth() && s.date.getMonthOfYear()==now.getMonthOfYear() && 
						s.date.getYear()==now.getYear()){
							//if(((size / 2 == 1) && (size % 2 == 1)) || ((size / 2 == 0) && (size % 2 == 1))){
							if(size == 3 || size == 1){	
								int nowToMinute = toMinute(now);
								int workingTime=0;
								for(Stamping st : stampings){
									if(st.way == Stamping.WayType.in)
										workingTime -= toMinute(st.date);				
									if(st.way == Stamping.WayType.out)
										workingTime += toMinute(st.date);
									if(workingTime < 0)
										tempoLavoro = nowToMinute + workingTime;										
									else 
										tempoLavoro = nowToMinute - workingTime;																		
								}								
							}				
					}				
					else{
						int workTime=0;
						for(Stamping st : stampings){
							if(st.way == Stamping.WayType.in){
								workTime -= toMinute(st.date);									
								System.out.println("Timbratura di ingresso: "+workTime);	
							}
							if(st.way == Stamping.WayType.out){
								workTime += toMinute(st.date);								
								System.out.println("Timbratura di uscita: "+workTime);
							}
							
						}
						/**
						 * controllare nei casi in cui ci siano 4 timbrature e la pausa pranzo minore di 30 minuti che il tempo di 
						 * lavoro ritornato sia effettivamente calcolato sulle timbrature effettive e non su quella aggiustata.
						 */
						List<WorkingTimeTypeDay> wttd = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ?" +
								"", person.workingTimeType).fetch();
						
						int minTimeForLunch = checkMinTimeForLunch(stampings);
						if((stampings.size()==4) && (minTimeForLunch < getWorkingTimeTypeDay().breakTicketTime) && (!stampings.contains(null)))
							tempoLavoro = workTime - (getWorkingTimeTypeDay().breakTicketTime-minTimeForLunch);							
						if((stampings.size()==2) && (workTime > getWorkingTimeTypeDay().mealTicketTime) && 
								(workTime-getWorkingTimeTypeDay().breakTicketTime > getWorkingTimeTypeDay().mealTicketTime))
							tempoLavoro = workTime-getWorkingTimeTypeDay().breakTicketTime;							
						else
							tempoLavoro = workTime;							
						
					}
					
				}
			}

		}		
		timeAtWork = tempoLavoro;	
		Logger.debug("TimeAtWork in %s for %s %s is %s", date, person.name, person.surname, timeAtWork);
		return timeAtWork;
	}
	
	/**
	 * 
	 * @param date
	 * @return il progressivo delle ore in più o in meno rispetto al normale orario previsto per quella data
	 */
	public int getProgressive(){

		if(progressive == 0){			
			
			if((date.getDayOfMonth()==1) && (date.getDayOfWeek()==6 || date.getDayOfWeek()==7))
				return 0;			
			if((date.getDayOfMonth()==2) && (date.getDayOfWeek()==7))
				return 0;
			if((date.getDayOfMonth()==1) && (date.getDayOfWeek()!=DateTimeConstants.SATURDAY || date.getDayOfWeek()!=DateTimeConstants.SUNDAY)){
				if(difference==0){
					difference = getDifference();
					//Logger.debug("Difference today in case of : "+difference);
				}
				progressive = difference;
			}
			else{
				PersonDay pdYesterday = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date.minusDays(1)).first();
				if(pdYesterday == null){
					pdYesterday = new PersonDay(person, date, 0, 0, 0);
				}
				
				if(difference==0){
					difference = getDifference();
					Logger.debug("Difference today: "+difference);
				}
				
				progressive = difference + pdYesterday.progressive;
				Logger.debug("Progressive today: "+progressive);				
			}
			
		}
		
		return progressive;
	}
	
	/**
	 * salva il valore della differenza giornaliera con l'orario di lavoro sul db
	 */
	public void setDifference(){
		difference = getDifference();	
		
	}
	
	/**
	 * salva il valore del progressivo giornaliero sul db e controlla se siamo al primo giorno del mese e quindi salva sul db il valore 
	 * del mese precedente sul personMonth corrispondente e anche se siamo al primo giorno dell'anno così salva sul personYear 
	 * il valore del cumulativo dell'anno precedente compreso tra aprile e dicembre
	 */
	public void setProgressive(){
		progressive = getProgressive();
		save();
		if(date.getDayOfMonth()==1){
			
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?",person, date.minusDays(1)).first();
			PersonMonth pm = null;
			if(date.getMonthOfYear() == DateTimeConstants.JANUARY){
				int yearProgressive = 0;
				PersonYear py = new PersonYear(person, date.getYear()-1);
				List<PersonMonth> personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and " +
						"pm.year = ? and pm.month between ? and ?", person, date.getYear()-1, DateTimeConstants.APRIL, DateTimeConstants.DECEMBER).fetch();
				for(PersonMonth permon : personMonth){
					yearProgressive = yearProgressive+permon.remainingHours;
					
				}
				py.remainingHours = yearProgressive;
				py.save();
				pm = new PersonMonth(person, date.getYear()-1, 12);
			}
			else{
				pm = new PersonMonth(person,date.getYear(),date.getMonthOfYear()-1);
			}
			pm.compensatoryRest = pm.getCompensatoryRest();

			pm.remainingHours = pd.progressive - pm.compensatoryRest*(432); //durata in minuti di 7:12 ore relative al giorno di riposo compensativo
			
			pm.save();
		}
		
	}
	
	/**
	 * salva il valore del tempo di lavoro giornaliero sul db
	 */
	public void setTimeAtWork(){
		timeAtWork = timeAtWork();
		
	}
	
	/**
	 * chiama le funzioni di popolamento
	 */
	public void populatePersonDay(){
		setDifference();
		setProgressive();
		setTimeAtWork();
		setTicketAvailable();
	}
    
    	
	
	/**
	 * TODO: sistemare nel caso abbia una timbratura d'ingresso la sera tardi senza la corrispondente timbratura d'uscita prima della mezzanotte del giorno stesso
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
		return this.absences;
	}
	
	/**
	 * 
	 * @param date
	 * @param timeAtWork
	 * @param person
	 * @return se la persona può usufruire del buono pasto per quella data
	 */
	
	private WorkingTimeTypeDay getWorkingTimeTypeDay(){
		int day = date.getDayOfWeek();
		WorkingTimeType wtt = person.workingTimeType;
		if (wtt == null)
			throw new IllegalStateException(String.format("Person %s has no working time type set", person));
		for(WorkingTimeTypeDay wttd : wtt.worTimeTypeDays){
			if(wttd.dayOfWeek == day)
				return wttd;
		}
		throw new IllegalStateException(String.format("Non è valorizzato il working time type day del giorno %s del person %s", day, person));
	}
	
	public boolean mealTicket(){
		boolean ticketAvailable = false;
		

		if (timeAtWork == 0) {
			timeAtWork = timeAtWork();
			
		}
		
		if(timeAtWork == 0 || timeAtWork < getWorkingTimeTypeDay().mealTicketTime){
			ticketAvailable = false;
		}				

		
		if(person.workingTimeType.description.equals("normale-mod") || person.workingTimeType.description.equals("normale")
				|| person.workingTimeType.description.equals("80%") || person.workingTimeType.description.equals("85%")){
			if(timeAtWork >= getWorkingTimeTypeDay().mealTicketTime)
				ticketAvailable=true;
			if(timeAtWork > getWorkingTimeTypeDay().mealTicketTime 
					&& timeAtWork < getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime 
					&&(stampings.size()==4 && checkMinTimeForLunch(stampings) < getWorkingTimeTypeDay().mealTicketTime))
				ticketAvailable=true;
			if(timeAtWork > getWorkingTimeTypeDay().mealTicketTime 
					&& timeAtWork < getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime 
					&& (stampings.size()==4))
				ticketAvailable=true;
			if(timeAtWork > getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime 
					&& timeAtWork < getWorkingTimeTypeDay().workingTime 
					&& (stampings.size()==4 || stampings.size()==2))
				ticketAvailable=true;
			if(timeAtWork > getWorkingTimeTypeDay().mealTicketTime 
					&& timeAtWork < getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime 
					&& (stampings.size()==6))
				ticketAvailable=true;
			if(timeAtWork < getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime
					&& timeAtWork > getWorkingTimeTypeDay().mealTicketTime 
					&& stampings.size()==2 )
				ticketAvailable = true;
			isMealTicketAvailable = ticketAvailable;
		}
		
		return isMealTicketAvailable;

	}
	
	/**
	 * setta il campo booleano della tabella PersonDay relativo al mealticket a true se nella giornata è stato raggiunto il tempo 
	 * minimo per l'assegnazione del buono pasto, false altrimenti. Serve a rendere persistenti le info sulla disponibilità del buono
	 * pasto evitando così di doverlo ricalcolare ogni volta. Viene chiamata all'interno della populate personDay e la save() viene
	 * fatta all'interno di essa.
	 */
	public void setTicketAvailable(){
		boolean ticketAvailable = false;
				

		if (timeAtWork == 0) {
			timeAtWork = timeAtWork();
			
		}	
		
		if(timeAtWork == 0 || timeAtWork < getWorkingTimeTypeDay().mealTicketTime){
			ticketAvailable = false;
		}		

		if(person.workingTimeType.description.equals("normale-mod") || person.workingTimeType.description.equals("normale")
				|| person.workingTimeType.description.equals("80%") || person.workingTimeType.description.equals("85%")){
			
			if(timeAtWork >= getWorkingTimeTypeDay().mealTicketTime)
				ticketAvailable=true;

			if(timeAtWork > getWorkingTimeTypeDay().mealTicketTime && 
					timeAtWork < getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime 
					&& (stampings.size()==4 && checkMinTimeForLunch(stampings) < getWorkingTimeTypeDay().breakTicketTime))
				ticketAvailable=true;

			if(timeAtWork > getWorkingTimeTypeDay().mealTicketTime 
					&& timeAtWork < getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime 
					&& (stampings.size()==4))
				ticketAvailable=true;

			if(timeAtWork > getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime
					&& timeAtWork < getWorkingTimeTypeDay().workingTime 
					&& (stampings.size()==4 || stampings.size()==2))
				ticketAvailable=true;

			if(timeAtWork > getWorkingTimeTypeDay().mealTicketTime && 
					timeAtWork < getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime 
					&& (stampings.size()==6))
				ticketAvailable=true;

			if(timeAtWork < getWorkingTimeTypeDay().mealTicketTime + getWorkingTimeTypeDay().breakTicketTime
					&& timeAtWork > getWorkingTimeTypeDay().mealTicketTime 
					&& stampings.size()==2 )
				ticketAvailable = true;

		}
		
		isTicketAvailable = ticketAvailable;

	}
	
	/**
	 * 
	 * @return la differenza tra l'orario di lavoro giornaliero e l'orario standard in minuti
	 */
	public Integer getDifference(){
		
		if(difference == 0){
//			List<Absence> absenceList = absenceList();
//			List<Stamping> stampingList = getStampings();
			if((date.getDayOfWeek()==DateTimeConstants.SATURDAY ) && (date.getDayOfMonth()==1))
				difference =  0;		
			if((date.getDayOfWeek()==DateTimeConstants.SUNDAY) && (date.getDayOfMonth()==1))
				difference =  0;
			if((date.getDayOfMonth()==2) && (date.getDayOfWeek()==DateTimeConstants.SUNDAY))
				difference =  0;
			if((date.getDayOfWeek()==DateTimeConstants.SUNDAY) || date.getDayOfWeek()==DateTimeConstants.SATURDAY)
				difference =  0;
//			if(absenceList.size()!=0){
//				return  0;
//			}
//			if(stampingList.size()==0){
//				return  0;
//			}
			int differenza = 0;
			
			int minTimeWorking = person.workingTimeType.worTimeTypeDays.get(date.getDayOfWeek()).workingTime;
			
			
			timeAtWork = timeAtWork();
			int size = stampings.size();
			
			if(size == 2){
				 if(timeAtWork >= getWorkingTimeTypeDay().mealTicketTime){
					 int delay = getWorkingTimeTypeDay().breakTicketTime;	
					 if(timeAtWork-delay >= getWorkingTimeTypeDay().mealTicketTime){
						 differenza = timeAtWork-minTimeWorking-delay;
						 difference = differenza;
						 
					 }
					 else{						 				
						 	differenza = timeAtWork - minTimeWorking;					
							difference = differenza;							
					 }
					 
				 }
				 else{
					 difference = timeAtWork - minTimeWorking;					 
				 }
				
			}
			int i = checkMinTimeForLunch(stampings);
			if(size == 4){
				 if(i < getWorkingTimeTypeDay().breakTicketTime){
					 differenza = timeAtWork-minTimeWorking+(i-getWorkingTimeTypeDay().breakTicketTime);					
					 difference = differenza;				 
				 }
				 else{
						differenza = timeAtWork-minTimeWorking;
						difference = differenza;					
					}
				
			}
			else{
				differenza = timeAtWork()-minTimeWorking;
				difference = differenza;			
			}
		}		
		Logger.debug("Difference in %s for %s %s is %s",date, person.name, person.surname, difference);		
		return difference;
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
							
					System.out.println("Timbratura di ingresso in checkTimeForLunch: "+workingTime);	
				}
				if(st.way == Stamping.WayType.out){
					workingTime += toMinute(st.date);
					
					System.out.println("Timbratura di uscita in checkTimeForLunch: "+workingTime);
				}
				timeAtWork += workingTime;
			}
			if(workingTime >= getWorkingTimeTypeDay().mealTicketTime+getWorkingTimeTypeDay().breakTicketTime)
				smt = StampModificationType.findById(StampModificationTypeValue.FOR_DAILY_LUNCH_TIME.getId());
					
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
			if(((hourEnter*60)+minuteEnter) - ((hourExit*60)+minuteExit) < getWorkingTimeTypeDay().breakTicketTime && workingTime > getWorkingTimeTypeDay().mealTicketTime){
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
		if(hour <0)
			hour=Math.abs(hour);
		int minute = (int)timeAtWork%60;
		if(minute <0)
			minute=Math.abs(minute);
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
		if(stamping.size()>3 && stamping.size()%2==0 && !stamping.contains(null)){
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
		if(ldt1== null || ldt2 == null)
			throw new RuntimeException("parameters localdatetime1 and localdatetime2 must be not null");
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
	 * @param ldt1
	 * @param ldt2
	 * @return il numero di minuti di differenza tra i 30 minuti minimi di pausa pranzo e quelli effettivamente fatti tra la timbratura
	 * d'uscita per la pausa pranzo e quella di ingresso dopo la pausa stessa
	 */
	public int timeAdjust(LocalDateTime ldt1, LocalDateTime ldt2){
		int timeAdjust=0;
		int minuti1 = toMinute(ldt1);
		int minuti2 = toMinute(ldt2);
		int difference = minuti2-minuti1;
		timeAdjust = getWorkingTimeTypeDay().breakTicketTime-difference;
		return timeAdjust;
		
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
