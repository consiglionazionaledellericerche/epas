/**
 * 
 */
package models;

import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
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
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import models.Stamping.WayType;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.PersonDayModificationType;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;
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

	@Column(name = "time_at_work")
	public Integer timeAtWork;

	public Integer difference;

	public Integer progressive;

	@Column(name = "is_ticket_available")
	public boolean isTicketAvailable = false;

	@Column(name = "is_ticket_forced_by_admin")
	public boolean isTicketForcedByAdmin = false;

	@Column(name = "is_time_at_work_auto_certificated")
	public boolean isTimeAtWorkAutoCertificated = false;

	@Column(name = "is_working_in_another_place")
	public boolean isWorkingInAnotherPlace = false;

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY)
	@OrderBy("date")
	public List<Stamping> stampings = new ArrayList<Stamping>();

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY)
	public List<Absence> absences = new ArrayList<Absence>();

	@Column(name = "modification_type")
	@Enumerated(EnumType.STRING)
	public PersonDayModificationType modificationType;


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
		isWorkingDay = wttd2.holiday;
		Logger.trace("%s. isWorkingDay = %s", this.toString(), isWorkingDay);
		return isWorkingDay;
	}


	/**
	 * 
	 * @param data
	 * @return true se il giorno in questione è un giorno di festa. False altrimenti
	 */
	public boolean isHoliday(){
		return DateUtility.isHoliday(person, date);
	}


	/**
	 * 
	 * @param date
	 * @return numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	private void updateTimeAtWork(){
		int tempoLavoro = 0;
		List<Stamping> reloadedStampings = null;
		LocalDateTime beginDate = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),0,0);
		LocalDateTime endDate = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),23,59);
		reloadedStampings = Stamping.find("Select st from Stamping st where st.personDay = ? and " +
				"st.date > ? and st.date < ? order by st.date", this, beginDate, endDate).fetch();

		StampProfile stampProfile = getStampProfile();

		/**
		 * prima di compiere le operazioni sul tempo di lavoro giornaliero, si controlla se per caso ci fossero timbrature inevase dal giorno 
		 * precedente, ovvero se per caso il giorno precedente ci sia stato un ingresso ma nessuna corrispondente timbratura d'uscita
		 */
		Configuration config = Configuration.getCurrentConfiguration();
		// il personDay contenente le timbrature del giorno precedente
		PersonDay pdPastDay = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date.minusDays(1)).first();
		if(pdPastDay != null){			
			//lista delle timbrature del giorno precedente
			List<Stamping> reloadedStampingYesterday = Stamping.find("Select st from Stamping st where st.personDay = ? and " +
					"st.date between ? and ? order by st.date", pdPastDay, beginDate.minusDays(1), endDate.minusDays(1)).fetch();
			//controllo che la lista di timbrature ne contenga una sola e che sia una timbratura di ingresso
			if(reloadedStampingYesterday.size() == 1 && reloadedStampingYesterday.get(0).way == WayType.in){
				Logger.debug("Sono nel caso in cui ci sia una sola timbratura ed è di ingresso nel giorno precedente nel giorno %s", 
						pdPastDay.date);
				if(stampProfile == null || !stampProfile.fixedWorkingTime){
					for(Stamping st : reloadedStampings){
						
						if(st != null){
							//controllo nelle timbrature del giorno attuale se la prima che incontro è una timbratura di uscita sulla base
							//del confronto con il massimo orario impostato in configurazione per considerarla timbratura di uscita relativa
							//al giorno precedente
							if(st.way == WayType.out && config.hourMaxToCalculateWorkTime > st.date.getHourOfDay()){
								Logger.debug("Esiste una timbratura di uscita come prima timbratura del giorno %s", date);
								//in caso esista quella timbratura di uscita come prima timbratura del giorno attuale, creo una nuova timbratura
								// di uscita e la inserisco nella lista delle timbrature relative al personDay del giorno precedente.
								//E svolgo i calcoli su tempo di lavoro, differenza e progressivo
								Stamping correctStamp = new Stamping();
								Logger.debug("Aggiungo una nuova timbratura di uscita al giorno precedente alla mezzanotte ");
								correctStamp.date = new LocalDateTime(pdPastDay.date.getYear(), pdPastDay.date.getMonthOfYear(), pdPastDay.date.getDayOfMonth(), 23, 59);
								correctStamp.way = WayType.out;
								correctStamp.markedByAdmin = false;
								correctStamp.stampModificationType = StampModificationType.findById(4l);
								correctStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
								correctStamp.personDay = pdPastDay;
								correctStamp.save();
								Logger.debug("Aggiunta nuova timbratura %s con valore %s", correctStamp, correctStamp.date);
								Logger.debug("Devo rifare i calcoli in funzione di questa timbratura aggiunta");
								
								pdPastDay.timeAtWork = toMinute(correctStamp.date) - toMinute(reloadedStampingYesterday.get(0).date);
								pdPastDay.merge();
								pdPastDay.difference = pdPastDay.timeAtWork - getWorkingTimeTypeDay().workingTime;
								pdPastDay.merge();
								PersonDay pdPast = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date <= ? order by pd.date desc", person, date.minusDays(2)).first();
								if(pdPast != null)
									pdPastDay.progressive = pdPast.progressive + pdPastDay.difference;

								else
									pdPastDay.progressive = pdPastDay.difference;
								pdPastDay.merge();
								Logger.debug("Fatti i calcoli, ora aggiungo una timbratura di ingresso alla mezzanotte del giorno %s", date);
								//a questo punto devo aggiungere una timbratura di ingresso prima della prima timbratura di uscita che è anche
								//la prima timbratura del giorno attuale
								Stamping newEntranceStamp = new Stamping();
								newEntranceStamp.date = new LocalDateTime(this.date.getYear(), this.date.getMonthOfYear(), this.date.getDayOfMonth(),0,0);
								newEntranceStamp.way = WayType.in;
								newEntranceStamp.markedByAdmin = false;
								newEntranceStamp.stampModificationType = StampModificationType.findById(4l);
								newEntranceStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
								newEntranceStamp.personDay = this;
								newEntranceStamp.save();
								Logger.debug("Aggiunta la timbratura %s con valore %s", newEntranceStamp, newEntranceStamp.date);
								
								this.save();
							}
						}
					}
				}

			}
		}
		/**
		 * a questo punto, se la prima timbratura giornaliera è una timbratura di uscita ed è minore come tempo dell'ora massima per calcolare 
		 * il timeatwork del giorno precedente, bisogna estrometterla dal calcolo delle informazioni sul personday giornaliero, cancellandola 
		 * dalla lista delle timbrature ordinate per date.
		 */
		reloadedStampings = Stamping.find("Select st from Stamping st where st.personDay = ? and " +
				"st.date > ? and st.date < ? order by st.date", this, beginDate, endDate).fetch();
		if((reloadedStampings.size() == 0 && this.absences.size() != 0) ||(reloadedStampings.size()==1 && reloadedStampings.get(0).way == WayType.in)){
			timeAtWork = 0;
			merge();
			return;

		}

		if(reloadedStampings.get(0).way == WayType.out && reloadedStampings.get(0).date.getHourOfDay() < config.hourMaxToCalculateWorkTime){
			reloadedStampings.remove(0);

		}

		/**
		 * per ora la gestione la facciamo considerando il caso di 3 timbrature nella lista del personDay
		 */

		if(reloadedStampings.size()==3 && !reloadedStampings.contains(null)){
			int workingTime = 0;
			Stamping stamp = reloadedStampings.get(0);

			if(stamp.way == WayType.in && reloadedStampings.get(1).way == WayType.out){
				workingTime = -toMinute(stamp.date) + toMinute(reloadedStampings.get(1).date);
				timeAtWork = workingTime;
				merge();
				return;
			}
			if(stamp.way == WayType.in && reloadedStampings.get(1).way == WayType.in){
				workingTime = -toMinute(reloadedStampings.get(1).date) + toMinute(reloadedStampings.get(2).date);
				timeAtWork = workingTime;
				merge();
				return;
			}

		}
		//FIXME: trovare un algoritmo più efficiente senza considerare casi specifici...
		if(reloadedStampings.contains(null)){
			/**
			 * in questo caso si guarda quale posizione della linkedList è null per stabilire se sia mancante un ingresso o un'uscita
			 */
			if((reloadedStampings.size() < 3)&& (reloadedStampings.get(0)==null || reloadedStampings.get(1) == null)){
				tempoLavoro = 0;
			}
			if(reloadedStampings.size() > 3 && (reloadedStampings.get(0)==null || reloadedStampings.get(1) == null)){
				/**
				 * è mancante la prima entrata o la prima uscita, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla seconda 
				 * entrata alla seconda uscita
				 */
				if(reloadedStampings.get(0)==null){
					/**
					 * manca la prima entrata
					 */
					Stamping enter = reloadedStampings.get(2);
					Stamping exit = reloadedStampings.get(3);
					tempoLavoro = toMinute(exit.date)-toMinute(enter.date);
				}
				if(reloadedStampings.get(1)== null){
					/**
					 * manca la prima uscita
					 */
					Stamping enter = reloadedStampings.get(0);
					Stamping exit = reloadedStampings.get(3);
					tempoLavoro = toMinute(exit.date)-toMinute(enter.date);
				}
			}
			if(reloadedStampings.size() > 3 && (reloadedStampings.get(2)==null || reloadedStampings.get(3) == null)){
				/**
				 * è mancante la seconda entrata o la seconda uscita, quindi bisogna fare il calcolo del tempo a lavoro sul tempo trascorso dalla prima
				 * entrata alla prima uscita
				 */
				Stamping enter = reloadedStampings.get(0);
				Stamping exit = reloadedStampings.get(1);
				tempoLavoro = toMinute(exit.date)-toMinute(enter.date);
			}
			if( (reloadedStampings.size() > 4) && (reloadedStampings.get(4) == null || reloadedStampings.get(5) == null)){
				/**
				 * è mancante la terza entrata o la terza uscita, quindi devo fare il calcolo del tempo a lavoro sul tempo trascorso dalla
				 * prima entrata alla seconda uscita
				 */

				Stamping enter1 = reloadedStampings.get(0);
				Stamping exit1 = reloadedStampings.get(1);
				Stamping enter2 = reloadedStampings.get(2);
				Stamping exit2 = reloadedStampings.get(3);
				tempoLavoro = ((toMinute(exit2.date)-toMinute(enter2.date))+(toMinute(exit1.date)-toMinute(enter1.date)));

			}
			if(reloadedStampings.get(6)==null){
				Stamping enter1 = reloadedStampings.get(0);
				Stamping exit1 = reloadedStampings.get(1);
				Stamping enter2 = reloadedStampings.get(2);
				Stamping exit2 = reloadedStampings.get(3);
				Stamping enter3 = reloadedStampings.get(4);
				Stamping exit3 = reloadedStampings.get(5);
				tempoLavoro = ((toMinute(exit3.date)-toMinute(enter3.date))+(toMinute(exit2.date)-toMinute(enter2.date))+(toMinute(exit1.date)-toMinute(enter1.date)));
			}
			//Per le persone che hanno impostato nello StampProfile la timbrature di default si imposta 
			//il tempo previsto
			if (stampProfile != null && stampProfile.fixedWorkingTime) {
				timeAtWork = Math.max(tempoLavoro, getWorkingTimeTypeDay().workingTime);
			} else {
				timeAtWork = tempoLavoro;	
			}
			merge();
			return;

		}		
		/**
		 * in caso non ci sia nessuna timbratura nulla
		 */
		int size = reloadedStampings.size();
		// questo contatore controlla se nella lista di timbrature c'è almeno una timbratura di ingresso, in caso contrario fa
		// ritornare 0 come tempo di lavoro.
		int count = 0;
		for(Stamping s : reloadedStampings){
			if(s.way == Stamping.WayType.in)
				count ++;
		}
		if(count == 0){
			//Per le persone che hanno impostato nello StampProfile la timbrature di default si imposta 
			//il tempo previsto
			if (stampProfile != null && stampProfile.fixedWorkingTime) {
				timeAtWork = getWorkingTimeTypeDay().workingTime;
			} else {
				timeAtWork = 0;	
			}
			merge();
			return;
		}

		/**
		 * se ci sono timbrature
		 */
		LocalDateTime now = new LocalDateTime();
		if(size > 0){
			Stamping s = reloadedStampings.get(0);
			if(s.date.getDayOfMonth()==now.getDayOfMonth() && s.date.getMonthOfYear()==now.getMonthOfYear() && 
					s.date.getYear()==now.getYear()){
				if(size == 3 || size == 1){	
					int nowToMinute = toMinute(now);
					int workingTime=0;
					for(Stamping st : reloadedStampings){
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
				for(Stamping st : reloadedStampings){
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
				int minTimeForLunch = checkMinTimeForLunch();
				if((reloadedStampings.size()==4) && (minTimeForLunch < getWorkingTimeTypeDay().breakTicketTime) && (!reloadedStampings.contains(null)))
					tempoLavoro = workTime - (getWorkingTimeTypeDay().breakTicketTime-minTimeForLunch);							
				if((reloadedStampings.size()==2) && (workTime > getWorkingTimeTypeDay().mealTicketTime) && 
						(workTime-getWorkingTimeTypeDay().breakTicketTime > getWorkingTimeTypeDay().mealTicketTime)){
					//Logger.debug("La dimensione della lista di timbrature è %s e quindi devo togliere la pausa pranzo", reloadedStampings.size());
					tempoLavoro = workTime-getWorkingTimeTypeDay().breakTicketTime;	
					//Logger.debug("Il tempo lavoro è: %s mentre il breakTickeTime è: %s", tempoLavoro, getWorkingTimeTypeDay().breakTicketTime);
				}
				else
					tempoLavoro = workTime;							

			}

		}
		//Per le persone che hanno impostato nello StampProfile la timbrature di default si imposta 
		//il tempo previsto
		if (stampProfile != null && stampProfile.fixedWorkingTime) {
			timeAtWork = Math.max(tempoLavoro, getWorkingTimeTypeDay().workingTime);
		} else {
			timeAtWork = tempoLavoro;	
		}

		Logger.trace("PersonDay[%d] - personId = %s, date = %s. TimeAtWork is %d", id, person.id, date, timeAtWork);

	}


	public void calculateTimeAtWork(){
		LocalDateTime beginDate = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),0,0);
		LocalDateTime endDate = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),23,59);
		Logger.debug("Lista timbrature: ", stampings.toString());
		List<Stamping> reloadedStampings = Stamping.find("Select st from Stamping st where st.date > ? and st.date < ? order by st.date", 
				beginDate, endDate).fetch();
		List<Stamping> withNullStampings = returnStampingsList(reloadedStampings);
		//		boolean stampingForLunch = false;
		if(withNullStampings.size() <= 1)
			return;
		Stamping lastValidEntrance = null;
		//		Stamping lastValidExit = null;
		Stamping lastStamping = null;
		int numberOfValidEntranceExitCouple = 0;
		//		int timeAtWork = 0;
		for(Stamping st : withNullStampings){
			if(st.way == WayType.out && (lastValidEntrance != null)){
				timeAtWork = toMinute(st.date)-toMinute(lastValidEntrance.date);
				numberOfValidEntranceExitCouple++;
			}
			if(st.way == WayType.in){
				if(lastStamping.way == WayType.in){

				}
			}
		}
	}

	/**
	 * calcola il valore del progressivo giornaliero e lo salva sul db
	 */
	private void updateProgressive(){

		Contract con = person.getContract(date);
		if(con.beginContract == null || 
				(date.isAfter(con.beginContract) && (con.expireContract == null || date.isBefore(con.expireContract)))) {
			PersonDay lastPreviousPersonDayInMonth = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
					"and pd.date >= ? and pd.date < ? ORDER by pd.date DESC", person, date.dayOfMonth().withMinimumValue(), date).first();
			if (lastPreviousPersonDayInMonth == null || (con != null && con.beginContract != null && lastPreviousPersonDayInMonth.date.isBefore(con.beginContract))) {
				progressive = difference;
				Logger.debug("%s - %s. Non c'è nessun personDay nel contratto attuale prima di questa data. Progressive di oggi = %s", person, date, progressive);
			} else {

				progressive = difference + lastPreviousPersonDayInMonth.progressive;
				Logger.debug("%s - %s. Il PersonDay precedente è %s. Difference di oggi = %s, progressive = %s", person, date, lastPreviousPersonDayInMonth, difference, progressive);
			}
			this.save();
		}

	}

	/**
	 * chiama le funzioni di popolamento
	 */
	public void populatePersonDay(){
		Contract con = person.getContract(date);
		//Se la persona non ha un contratto attivo non si fanno calcoli per quel giorno
		//Le timbrature vengono comunque mantenute
		if (con != null) {
			if(stampings.size() != 0 && absences.size() != 0){
				Logger.debug("Ci sono sia timbrature che assenze, verifico che le assenze siano giornaliere e non orarie così da" +
						"evitare di fare i calcoli per questo giorno.");
				for(Absence abs : absences){
					if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay){
						timeAtWork = 0;
						merge();
						difference = 0;
						merge();
						updateProgressive();
						merge();
						isTicketAvailable = false;
						merge();
						return;
					}
						
				}
			}
			updateTimeAtWork();
			merge();
			updateDifference();
			merge();
			updateProgressive();	
			merge();
			setTicketAvailable();
			merge();
		} else {
			Logger.info("I calcoli sul giorno %s non vengono effettuati perché %s non ha un contratto attivo in questa data", date, person);
		}
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
	 * @return la lista di codici di assenza fatti da quella persona in quella data
	 */
	public List<Absence> absenceList() {
		return this.absences;
	}

	/**
	 * 
	 * @return la lista delle timbrature in quel personday
	 */
	public List<Stamping> stampingList(){
		return this.stampings;
	}

	/**
	 * 
	 * @return la lista di timbrature comprensiva di timbrature nulle nel caso in cui manchino timbrature di ingresso tra quelle di uscita
	 * o che manchino timbrature di uscita tra quelle di ingresso
	 */
	public List<Stamping> returnStampingsList(List<Stamping> stampingList) {	

		List<Stamping> localStampings = new LinkedList<Stamping>();

		Stamping lastStamping = null;
		for(Stamping st : stampingList){
			if(st.way == WayType.out && (lastStamping == null || lastStamping.way == WayType.out)){
				localStampings.add(null);

			}
			if(st.way == WayType.in && (lastStamping != null && lastStamping.way == WayType.in)){
				localStampings.add(null);

			}
			lastStamping = st;
			localStampings.add(st);
		}
		Logger.debug("Lista timbrature per %s in data %s: %s", person, date, localStampings);
		return localStampings;
	}

	/**
	 * 
	 * @return il workingTimeTypeDay relativo al giorno specifico della data
	 */
	public WorkingTimeTypeDay getWorkingTimeTypeDay(){
		int day = date.getDayOfWeek();
		WorkingTimeType wtt = person.workingTimeType;
		if (wtt == null)
			throw new IllegalStateException(String.format("Person %s has no working time type set", person));
		for(WorkingTimeTypeDay wttd : wtt.workingTimeTypeDays){
			if(wttd.dayOfWeek == day)
				return wttd;
		}
		throw new IllegalStateException(String.format("Non è valorizzato il working time type day del giorno %s del person %s", day, person));
	}

	/**
	 * 
	 * @param date
	 * @param timeAtWork
	 * @param person
	 * @return se la persona può usufruire del buono pasto per quella data
	 */
	public boolean mealTicket(){

		setTicketAvailable();
		return isTicketAvailable;

	}

	/**
	 * setta il campo booleano della tabella PersonDay relativo al mealticket a true se nella giornata è stato raggiunto il tempo 
	 * minimo per l'assegnazione del buono pasto, false altrimenti. Serve a rendere persistenti le info sulla disponibilità del buono
	 * pasto evitando così di doverlo ricalcolare ogni volta. Viene chiamata all'interno della populate personDay e la save() viene
	 * fatta all'interno di essa.
	 */
	private void setTicketAvailable(){

		if(isTicketForcedByAdmin)
			return;
		Logger.debug("Chiamata della setTicketAvailable, il timeAtWork per %s %s è: %s", person.name, person.surname, timeAtWork);
		Logger.debug("Il tempo per avere il buono mensa è: %s", getWorkingTimeTypeDay().mealTicketTime);
		if(timeAtWork == 0 || timeAtWork < getWorkingTimeTypeDay().mealTicketTime){
			isTicketAvailable = false;
			return; 
		}		

		boolean ticketAvailable = false;

		if(person.workingTimeType.description.equals("normale-mod") || person.workingTimeType.description.equals("normale")
				|| person.workingTimeType.description.equals("80%") || person.workingTimeType.description.equals("85%")){

			if(timeAtWork >= getWorkingTimeTypeDay().mealTicketTime)
				ticketAvailable=true;
			else
				ticketAvailable=false;
		}

		isTicketAvailable = ticketAvailable;
		Logger.debug("Quindi il valore del buono pasto è %s", isTicketAvailable);
		save();

	}

	/**
	 * 
	 * @return la differenza tra l'orario di lavoro giornaliero e l'orario standard in minuti
	 */

	private void updateDifference(){

		if((getWorkingTimeTypeDay().holiday) && (date.getDayOfMonth()==1)){
			difference = 0;
			save();
			return;
		}

		if(absenceList().size() > 0){
			difference = 0;
			save();
			return;
		}

		if(timeAtWork == 0){
			difference = 0;
			save();
			return;
		}

		int minTimeWorking = getWorkingTimeTypeDay().workingTime;
		Logger.debug("Time at work: %s. Tempo di lavoro giornaliero: %s", timeAtWork, minTimeWorking);
		difference = timeAtWork - minTimeWorking;
		save();		
		Logger.debug("Differenza per %s %s nel giorno %s: %s", person.surname, person.name, date, difference);

	}

	/**
	 * 
	 * @param stamping
	 * @return l'oggetto StampModificationType che ricorda, eventualmente, se c'è stata la necessità di assegnare la mezz'ora
	 * di pausa pranzo a una giornata a causa della mancanza di timbrature intermedie oppure se c'è stata la necessità di assegnare
	 * minuti in più a una timbratura di ingresso dopo la pausa pranzo poichè il tempo di pausa è stato minore di 30 minuti e il tempo
	 * di lavoro giornaliero è stato maggiore stretto di 6 ore.
	 */
	public StampModificationType checkTimeForLunch(){
		StampModificationType smt = null;
		if(stampings.size()==2){
			if(timeAtWork >= getWorkingTimeTypeDay().mealTicketTime+getWorkingTimeTypeDay().breakTicketTime)
				smt = StampModificationType.findById(StampModificationTypeValue.FOR_DAILY_LUNCH_TIME.getId());
		}
		if(stampings.size()==4 && !stampings.contains(null)){
			int hourExit = stampings.get(1).date.getHourOfDay();
			int minuteExit = stampings.get(1).date.getMinuteOfHour();
			int hourEnter = stampings.get(2).date.getHourOfDay();
			int minuteEnter = stampings.get(2).date.getMinuteOfHour();
			int workingTime=0;
			for(Stamping st : stampings){
				if(st.way == Stamping.WayType.in){
					workingTime -= toMinute(st.date);

				}
				if(st.way == Stamping.WayType.out){
					workingTime += toMinute(st.date);

				}
			}
			if(((hourEnter*60)+minuteEnter) - ((hourExit*60)+minuteExit) < getWorkingTimeTypeDay().breakTicketTime && workingTime > getWorkingTimeTypeDay().mealTicketTime)
				smt = StampModificationType.findById(StampModificationTypeValue.FOR_MIN_LUNCH_TIME.getId());
		}
		return smt;
	}

	/**
	 * 
	 * @return lo stamp modification type relativo alla timbratura modificata/inserita dall'amministratore
	 */
	public StampModificationType checkMarkedByAdmin(){
		StampModificationType smt = null;
		for(Stamping st : stampings){
			if(st.markedByAdmin == true)
				smt = StampModificationType.findById(StampModificationTypeValue.MARKED_BY_ADMIN.getId());
		}

		return smt;
	}
	
	/**
	 * 
	 * @return lo stamp modification type relativo alla timbratura aggiunta dal sistema nel caso mancasse la timbratura d'uscita prima
	 * della mezzanotte del giorno in questione
	 */
	public StampModificationType checkMissingExitStampBeforeMidnight(){
		StampModificationType smt = null;
		for(Stamping st : stampings){
			if(st.stampModificationType != null)
				smt = StampModificationType.findById(StampModificationTypeValue.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getId());
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
	public int checkMinTimeForLunch(){
		int min=0;
		if(stampings.size()>3 && stampings.size()%2==0 && !stampings.contains(null)){
			int minuteExit = toMinute(stampings.get(1).date);

			int minuteEnter = toMinute(stampings.get(2).date);

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

	private StampProfile getStampProfile() {
		List<StampProfile> stampProfiles = 
				StampProfile.find("SELECT sp FROM StampProfile sp WHERE sp.person = ? " +
						"AND (sp.startFrom <= ? OR sp.startFrom IS NULL) AND (sp.endTo >= ? OR sp.endTo IS NULL)", person, date, date).fetch();

		if (stampProfiles.size() > 1) {
			throw new IllegalStateException(
					String.format("E' presente più di uno StampProfile per %s per la data %s", person, date));
		}
		if (stampProfiles.isEmpty()) {
			Logger.warn("Non è presente uno StampProfile per %s con data %s", person, date);
			return null;
		}
		return stampProfiles.get(0);
	}

	@Override
	public String toString() {
		return String.format("PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, modificationType = %s, progressive = %s, timeAtWork = %s",
				id, person.id, date, difference, isTicketAvailable, modificationType, progressive, timeAtWork);
	}

}
