/**
 * 
 */
package models;

import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Query;
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
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
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

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade=CascadeType.PERSIST)
	@OrderBy("date ASC")
	public List<Stamping> stampings = new ArrayList<Stamping>();

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade=CascadeType.PERSIST)
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


	public void addAbsence(Absence abs){
		this.absences.add(abs);
		abs.personDay = this;
	}

	public void addStamping(Stamping st){
		this.stampings.add(st);
		st.personDay = this;
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

	private final static class TimeAtWork {
		Stamping lastNotPairedInStamping;
		Integer timeAtWork;
		TimeAtWork(Integer timeAtWork, Stamping lastNotPairedInStamping) {
			this.timeAtWork = timeAtWork;
			this.lastNotPairedInStamping = lastNotPairedInStamping;
		}
	}
	
	/**
	 * 
	 * @param abt
	 * @return true se nella lista assenze per un certo giorno esiste un'assenza che appartenga a un gruppo il cui codice di rimpiazzamento non
	 * sia nullo
	 */
	private boolean checkHourlyAbsenceCodeSameGroup(AbsenceType abt){
		Query query = JPA.em().createQuery("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.replacingAbsenceType = :abt and " +
				"abs.personDay = :pd");
		query.setParameter("abt", abt).setParameter("pd", this);
		return query.getResultList().size() > 0;
	}

	public TimeAtWork getCalculatedTimeAtWork() {
		
		//Se hanno il tempo di lavoro fissato non calcolo niente
		StampProfile stampProfile = getStampProfile();
		if (stampProfile != null && stampProfile.fixedWorkingTime) {
			return new TimeAtWork(getWorkingTimeTypeDay().workingTime, null);
		} 

		int tempoLavoro = 0;
		int justifiedTimeAtWork = 0;

		timeAtWork = 0;

		/**
		 * controllo sulla possibilità che esistano timbrature di ingresso nel giorno precedente senza corrispondenti timbrature di uscita
		 * se non la mattina seguente prima dell'orario di scadenza del controllo sulla timbratura stessa
		 */
		PersonUtility.checkExitStampNextDay(this);

	
		for(Absence abs : absences){
			//TODO: verificare con Claudio cosa fare con le timbrature in missione
			if(abs.absenceType.ignoreStamping || 
					(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay && !checkHourlyAbsenceCodeSameGroup(abs.absenceType))){
				return new TimeAtWork(0, null);
			} else{
				if(!abs.absenceType.code.equals("89") && abs.absenceType.justifiedTimeAtWork.minutesJustified != null)
					//ci sono più codici di assenza uno è il codice da inviare a roma dell'assenza oraria corrispondente
					justifiedTimeAtWork = justifiedTimeAtWork + abs.absenceType.justifiedTimeAtWork.minutesJustified;
				else{
					
					//Da capire cosa fare nel caso del codice 89
					Logger.trace("Il codice che sto analizzando è: %s", abs.absenceType.code);
				}
			}

		}

		if (stampings.size() == 0) {
			return new TimeAtWork(justifiedTimeAtWork, null);
		}
		//Se non c'è almeno una coppia di timbrature allora il tempo di lavoro è giustificato solo dalle assenze
		//precendente calcolate
		if(stampings.size() == 1){
			return new TimeAtWork(justifiedTimeAtWork, stampings.get(0).way == WayType.in ? stampings.get(0) : null);
		}

		if(isHoliday()){
			int workTime = 0;
			for(Stamping st : stampings){
				if(st.way == Stamping.WayType.in){
					workTime -= toMinute(st.date);									

				}
				if(st.way == Stamping.WayType.out){
					workTime += toMinute(st.date);								
					Logger.trace("Timbratura di uscita: %s", workTime);
				}
			}
			return new TimeAtWork(justifiedTimeAtWork + workTime, null);
		}

		int workTime=0;
		boolean atLeastOnePairedStamping = false;
		Stamping lastInNotPaired = null;

		List<Stamping> reloadedStampings = Stamping.find("Select st from Stamping st where st.personDay = ? order by st.date", this).fetch();
		for(int i = 0; i < reloadedStampings.size(); i++) {
			Logger.trace("Per il calcolo del tempo di lavoro passo dalla timbratura numero %d", i);
			if(reloadedStampings.get(i).way == WayType.in && reloadedStampings.get(i).considerForCounting){
				Logger.trace("E' timbratura di ingresso per %s alle ore %s", person, reloadedStampings.get(i).date.toString());
				if (i == reloadedStampings.size() - 1 ) {
					Logger.trace("Ultima timbratura del %s e' di ingresso per %s %s. Timbratura per adesso ignorata.", date, person.name, person.surname);
					break;
				}
				if (i >= 1 && reloadedStampings.get(i - 1).way == WayType.in) {
					Logger.trace("Timbratura di ingresso del %s per %s %s ignorata perche' la precedente era di ingresso", date, person.name, person.surname);
					continue;
				}

				if(reloadedStampings.get(i).stampType != null && reloadedStampings.get(i).stampType.identifier.equals("s")){
					if((i-1) >= 0){
						//TODO: mettere ENUM per mappare gli StampType tra db e codice 
						if(reloadedStampings.get(i-1).stampType != null && reloadedStampings.get(i-1).stampType.identifier.equals("s")){
							//c'è stata un'uscita di servizio, questo è il corrispondente ingresso come lo calcolo? aggiungendo il tempo
							//trascorso fuori per servizio come tempo di lavoro
							Logger.debug("Anche la precedente timbratura era di servizio...calcolo il tempo in servizio come tempo a lavoro per %s %s", 
									person.name, person.surname);
						}
						else{
							workTime -= toMinute(reloadedStampings.get(i).date);
							reloadedStampings.get(i).note = "Attenzione: timbratura precedente risulta di servizio, mentre questa non lo e', verificare le proprie timbrature di servizio.";
							reloadedStampings.get(i).save();
						}
					}

				}
				else{
					if(i-1 > 0 && reloadedStampings.get(i-1) != null && reloadedStampings.get(i).stampType != null && reloadedStampings.get(i-1).stampType.identifier.equals("s")){
						reloadedStampings.get(i).note = "Timbratura di ingresso normale, ci si aspettava invece una timbratura di ingresso di servizio. Contattare il dipendente.";
						reloadedStampings.get(i).save();
						continue;
					}
					Logger.trace("Tominute di %s: %d", reloadedStampings.get(i).date, toMinute(reloadedStampings.get(i).date));
					Logger.trace("Tempo di lavoro prima della somma della timbratura: %d", workTime);
					workTime -= toMinute(reloadedStampings.get(i).date);
					Logger.debug("Tempo di lavoro dopo della somma della timbratura: %d", workTime);
					lastInNotPaired = reloadedStampings.get(i);
					Logger.trace("Normale timbratura di ingresso %s che mi dà un tempo di lavoro di: %d", reloadedStampings.get(i).date, workTime);
				}


			}

			if(reloadedStampings.get(i).way == WayType.out && reloadedStampings.get(i).considerForCounting){
				Logger.trace("E' timbratura di uscita alle ore %s", reloadedStampings.get(i).date.toString());
				if (i == 0) {
					Logger.debug("La prima timbratura del %s e' di uscita per %s %s, quindi viene ignorata nei calcoli", date, person.name, person.surname);
					continue;
				}

				if (i >= 1 && reloadedStampings.get(i-i).way == WayType.out) {
					Logger.debug("Timbratura di uscita del %s per %s %s ignorata perche' la precedente era di uscita", date, person.name, person.surname);
					continue;
				}

				if(reloadedStampings.get(i).stampType != null && reloadedStampings.get(i).stampType.identifier.equals("s")){
					if((i-1) >= 0){
						if(reloadedStampings.get(i-1).stampType != null && reloadedStampings.get(i-1).stampType.identifier.equals("s")){
							//c'è stata un'uscita di servizio, questo è il corrispondente ingresso come lo calcolo? aggiungendo il tempo
							//trascorso fuori per servizio come tempo di lavoro
							Logger.debug("Anche la precedente timbratura era di servizio...calcolo il tempo in servizio come tempo a lavoro per %s %s", 
									person.name, person.surname);
						}
						else{
							//workTime += toMinute(reloadedStampings.get(i).date);
							//reloadedStampings.get(i).note = "Attenzione: timbratura precedente risulta di servizio, mentre questa non lo e', verificare le proprie timbrature di servizio.";
							//reloadedStampings.get(i).save();
						}
					}

				}

				else{
					//timbratura normale di uscita
					Logger.trace("Tominute di %s: %d", reloadedStampings.get(i).date, toMinute(reloadedStampings.get(i).date));
					Logger.trace("Tempo di lavoro prima della somma della timbratura: %d", workTime);
					workTime += toMinute(reloadedStampings.get(i).date);
					Logger.trace("Tempo di lavoro dopo la somma della timbratura: %d", workTime);
					atLeastOnePairedStamping = true;
					lastInNotPaired = null;
					Logger.debug("Normale timbratura di uscita %s che mi dà un tempo di lavoro di: %d", reloadedStampings.get(i).date, workTime);
				}

			}
		}

		//Viene conteggiato solo il tempo derivante da assenze giustificate precedentemente calcolato
		if (!atLeastOnePairedStamping) {
			return new TimeAtWork(justifiedTimeAtWork, lastInNotPaired);
		}

		//C'è un ingresso che non ha trovato corrispondenze in timbrature di uscita ci ri-aggiungo quello che ci avevo precedentemente tolto...
		if (lastInNotPaired != null) {
			workTime += toMinute(lastInNotPaired.date);
		}

		//TODO: rivedere bene questa parte
		int minTimeForLunch = checkMinTimeForLunch();
		if((reloadedStampings.size()==4) && (minTimeForLunch < getWorkingTimeTypeDay().breakTicketTime) && (!reloadedStampings.contains(null)))
			tempoLavoro = workTime - (getWorkingTimeTypeDay().breakTicketTime-minTimeForLunch);							
		if((reloadedStampings.size()==2) && (workTime > getWorkingTimeTypeDay().mealTicketTime) && 
				(workTime-getWorkingTimeTypeDay().breakTicketTime > getWorkingTimeTypeDay().mealTicketTime)){

			tempoLavoro = workTime-getWorkingTimeTypeDay().breakTicketTime;	

		}
		else{
			tempoLavoro = workTime;
			Logger.trace("tempo di lavoro a fine metodo: %d", tempoLavoro);
		}



		if (stampProfile != null && stampProfile.fixedWorkingTime) {
			Logger.debug("Il tempo di lavoro per %s %s il giorno %s è %s", person.name, person.surname, date, Math.max(tempoLavoro + justifiedTimeAtWork, getWorkingTimeTypeDay().workingTime));
			return new TimeAtWork(Math.max(tempoLavoro + justifiedTimeAtWork, getWorkingTimeTypeDay().workingTime), lastInNotPaired);
		} 
		
		
		Logger.debug("Il tempo di lavoro per %s %s il giorno %s è %s", person.name, person.surname, date, tempoLavoro + justifiedTimeAtWork);
		return new TimeAtWork(tempoLavoro + justifiedTimeAtWork, lastInNotPaired);

	}

	public class TimeAtWorkToday {
		public Integer timeAtWork;
		public boolean exitingNow;
		
		public TimeAtWorkToday(Integer timeAtWork, boolean exitingNow) {
			this.timeAtWork = timeAtWork;
			this.exitingNow = exitingNow;
		}
	}
	
	/**
	 * Da utilizzare solo nel template di visualizzazione!
	 * @return
	 */
	public TimeAtWorkToday getTimeAtWorkWithToday() {
		if (isToday()) {
			return getTimeAtWorkToday();
		}
		return new TimeAtWorkToday(timeAtWork, false);
	}
	
	private TimeAtWorkToday getTimeAtWorkToday() {
		if (stampings.size() == 0)
			return new TimeAtWorkToday(0, false);
		TimeAtWork calculatedTimeAtWork = getCalculatedTimeAtWork();
		Logger.debug("getTimeAtWorkToday. calculatedTimeAtWork = %s, lastNotPairedInStamping = %s", calculatedTimeAtWork.timeAtWork, calculatedTimeAtWork.lastNotPairedInStamping);
		if (calculatedTimeAtWork.lastNotPairedInStamping == null) {
			return new TimeAtWorkToday(calculatedTimeAtWork.timeAtWork, false); 
		}

		return new TimeAtWorkToday(calculatedTimeAtWork.timeAtWork - toMinute(calculatedTimeAtWork.lastNotPairedInStamping.date) + toMinute(LocalDateTime.now()), true);
	}

	/**
	 * 
	 * importa il  numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	private void updateTimeAtWork(){
		timeAtWork = getCalculatedTimeAtWork().timeAtWork;
		save();
	}


	/**
	 * calcola il valore del progressivo giornaliero e lo salva sul db
	 */
	private void updateProgressive(){
		//merge();
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		Contract con = person.getContract(date);

		if (con == null) {
			Logger.info("Progressivo del giorno %s per %s %s non aggiornato perche' non ha contratti attivi in questa data", date, person.surname, person.name);
			return;
		}

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
			save();
		}

	}

	/**
	 * chiama le funzioni di popolamento
	 */
	public void populatePersonDay(){
		Contract con = person.getContract(date);

		//Se la persona non ha un contratto attivo non si fanno calcoli per quel giorno
		//Le timbrature vengono comunque mantenute
		if (con == null) {
			Logger.info("I calcoli sul giorno %s non vengono effettuati perche' %s non ha un contratto attivo in questa data", date, person);
			return;
		}

		Logger.trace("Dimensione Stampings: %s. Dimensione Absences: %s Per %s %s", stampings.size(), absences.size(), person.name, person.surname);

		updateTimeAtWork();
		merge();
		updateDifference();
		merge();
		updateProgressive();	
		merge();
		setTicketAvailable();
		merge();

	}

	/**
	 * chiamo questa funzione quando sto caricando un'assenza (verosimilmente oraria) per un certo personDay e quindi modifico il timeAtWork
	 * per quella persona in quel giorno. Non ho necessità quindi di ricalcolarmi il timeAtWork, devo solo, sulla base di quello nuovo,
	 * richiamare le altre tre funzioni di popolamento del personDay.
	 */
	public void populatePersonDayAfterJustifiedAbsence(){
		Logger.trace("Chiamata populatePersonDayAfterJustifiedAbsence per popolare il personDay di %s %s senza il timeAtWork nel giorno %s",
				person.name, person.surname, date);
		updateDifference();
		merge();
		updateProgressive();	
		merge();
		setTicketAvailable();
		merge();
	}



	/**
	 * 
	 * @param date
	 * @return calcola il numero di minuti di cui è composta la data passata come parametro (di cui considera solo
	 * ora e minuti
	 */
	public static int toMinute(LocalDateTime date){
		int dateToMinute = 0;
		Logger.trace("La data passata alla toMinute è: %s", date);
		if (date!=null){
			int hour = date.get(DateTimeFieldType.hourOfDay());
			int minute = date.get(DateTimeFieldType.minuteOfHour());

			dateToMinute = (60*hour)+minute;
		}
		Logger.trace("Il risultato dell'elaborazione della toMinute sulla data non nulla %s è: %d minuti", date, dateToMinute);
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
		Logger.trace("Lista timbrature per %s in data %s: %s", person, date, localStampings);
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
		//merge();
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		if(isTicketForcedByAdmin)
			return;
		Logger.trace("Chiamata della setTicketAvailable, il timeAtWork per %s %s è: %s", person.name, person.surname, timeAtWork);
		Logger.trace("Il tempo per avere il buono mensa per %s è: %s", person, getWorkingTimeTypeDay().mealTicketTime);
		if(timeAtWork == 0 || timeAtWork < getWorkingTimeTypeDay().mealTicketTime){
			isTicketAvailable = false;
			return; 
		}		

		if(person.workingTimeType.description.equals("normale-mod") || person.workingTimeType.description.equals("normale")
				|| person.workingTimeType.description.equals("80%") || person.workingTimeType.description.equals("85%")){
			Logger.trace("Sono nella setTicketAvailable per %s %s", person.name, person.surname);
			if(timeAtWork >= getWorkingTimeTypeDay().mealTicketTime){
				isTicketAvailable=true;

			}
			else{
				isTicketAvailable=false;

			}
		}
		Logger.debug("Per %s %s il buono del giorno %s è: %s", person.name, person.surname, date, isTicketAvailable);

		Logger.trace("Il person day visualizzato è: %s", this);

		save();

	}

	/**
	 * 
	 * @return la differenza tra l'orario di lavoro giornaliero e l'orario standard in minuti
	 */

	private void updateDifference(){
		//merge();
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		if((getWorkingTimeTypeDay().holiday) && (date.getDayOfMonth()==1) && stampings.size() == 0){
			difference = 0;
			save();
			return;
		}

		if(absenceList().size() > 0 && stampings.size() == 0){
			difference = 0;
			save();
			return;
		}
		
		if(absences.size() == 1 && stampings.size() > 0){
			Absence abs = absences.get(0);
			if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay){
				difference = 0;
				save();
				return;
			}
				
		}

		if(this.date.isAfter(new LocalDate()) && stampings.size()==0 && absences.size()==0 && timeAtWork == 0){
			difference = 0;
			save();
			return;
		}

		if(getWorkingTimeTypeDay().holiday == false){
			int minTimeWorking = getWorkingTimeTypeDay().workingTime;
			Logger.trace("Time at work: %s. Tempo di lavoro giornaliero: %s", timeAtWork, minTimeWorking);
			difference = timeAtWork - minTimeWorking;
			save();
		}
		else{

			difference = timeAtWork;
			save();
			Logger.trace("Sto calcolando la differenza in un giorno festivo per %s %s e vale: %d", person.name, person.surname, difference);
		}		

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
		//TODO: verificare anche i casi con timbrature diverse da 2 che hanno per esempio due ingressi consecutivi ed una uscita
		if(stampings.size() == 2){
			if(timeAtWork >= getWorkingTimeTypeDay().mealTicketTime+getWorkingTimeTypeDay().breakTicketTime) {
				smt = StampModificationType.findById(StampModificationTypeValue.FOR_DAILY_LUNCH_TIME.getId());
			}
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
			if(st.stampModificationType != null && st.stampModificationType.equals(StampModificationTypeValue.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getStampModificationType()))
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

	public StampProfile getStampProfile() {
		List<StampProfile> stampProfiles = 
				StampProfile.find("SELECT sp FROM StampProfile sp WHERE sp.person = ? " +
						"AND (sp.startFrom <= ? OR sp.startFrom IS NULL) AND (sp.endTo >= ? OR sp.endTo IS NULL)", person, date, date).fetch();

		if (stampProfiles.size() > 1) {
			throw new IllegalStateException(
					String.format("E' presente più di uno StampProfile per %s per la data %s", person, date));
		}
		if (stampProfiles.isEmpty()) {

			return null;
		}
		Logger.trace("E' presente uno StampProfile per %s con data %s", person, date);
		return stampProfiles.get(0);
	}

	/**
	 * 
	 * @return il personday precedente a quello attualmente considerato. Questo metodo è utilizzato nella visualizzazione delle timbrature
	 * sia in versione utente che amministratore, quando si deve visualizzare il progressivo nei giorni festivi (che altrimenti risulterebbe
	 * sempre uguale a zero)
	 */
	public PersonDay checkPreviousProgressive(){
		//TODO: controllare il funzionamento nel caso del 2 giugno
		//		PersonDay pd = null;
		PersonDay lastPreviousPersonDayInMonth = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
				"and pd.date >= ? and pd.date < ? ORDER by pd.date DESC", person, date.dayOfMonth().withMinimumValue(), date).first();
		if(lastPreviousPersonDayInMonth != null)
			return lastPreviousPersonDayInMonth;
		//		if(date.getDayOfMonth() != 1){
		//
		//			pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date.minusDays(1)).first();
		//			if(pd != null)
		//				return pd;
		//			else{
		//				pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date < ? order by pd.date desc", person, date.minusDays(1)).first();
		//				return pd;
		//			}
		//		}
		return null;
	}

	public boolean isToday() {
		return date.equals(LocalDate.now());
	}
	
	public boolean isFuture() {
		return date.isAfter(LocalDate.now());
	}
	
	@Override
	public String toString() {
		return String.format("PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, modificationType = %s, progressive = %s, timeAtWork = %s",
				id, person.id, date, difference, isTicketAvailable, modificationType, progressive, timeAtWork);
	}

}
