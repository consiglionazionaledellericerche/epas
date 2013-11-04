/**
 * 
 */
package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.persistence.OneToOne;
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
import play.Play;
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

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
	@OrderBy("date ASC")
	public List<Stamping> stampings = new ArrayList<Stamping>();

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
	public List<Absence> absences = new ArrayList<Absence>();

	@Column(name = "modification_type")
	@Enumerated(EnumType.STRING)
	public PersonDayModificationType modificationType;
	
	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY)
	public List<PersonDayInTrouble> troubles = new ArrayList<PersonDayInTrouble>();
	
	/**
	 * lo StampModificationType se il person day presenta situazioni di pausa mensa troppo breve o pausa automaticamente calcolata (e, p)
	 */
	@Transient
	public StampModificationType lunchTimeStampModificationType = null;


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
	 * (checked 17/10 personStamping)
	 * @param data
	 * @return true se il personDay è un giorno di festa per la persona. False altrimenti
	 */
	public boolean isHoliday(){
		if(DateUtility.isHoliday(this.person, this.date))
		{
			return true;
		}
		if(this.person.workingTimeType.workingTimeTypeDays.get(this.date.getDayOfWeek()-1).holiday)
		{
			return true;
		}
		return false;
		
	}
	
	/**
	 * (cheked 21/10 PersonStampingDayRecap)
	 * @return true se nel giorno vi e' una assenza giornaliera
	 */
	public boolean isAllDayAbsences()
	{
		for(Absence ab : absences)
		{
			if(ab.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay))
				return true;
		}
		return false;
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
	
	
	/**
	 * Classe che modella due stampings logicamente accoppiate nel personday (una di ingresso ed una di uscita)
	 * @author alessandro
	 *
	 */
	private final static class PairStamping
	{
		Stamping in;
		Stamping out;
		PairStamping(Stamping in, Stamping out)
		{
			this.in = in;
			this.out = out;
		}
	}
	
	/**
	 * (alessandro)
	 * Ritorna le coppie di stampings valide al fine del calcolo del time at work. All'interno del metodo
	 * viene anche settato il campo valid di ciascuna stampings contenuta nel person day
	 * @return
	 */
	private List<PairStamping> getValidPairStamping()
	{
		Collections.sort(this.stampings);
		//(1)Costruisco le coppie valide per calcolare il worktime
		List<PairStamping> validPairs = new ArrayList<PersonDay.PairStamping>();
		List<Stamping> serviceStampings = new ArrayList<Stamping>();
		Stamping stampEnter = null;
		for(Stamping stamping : this.stampings)
		{
			//le stampings di servizio non entrano a far parte del calcolo del work time ma le controllo successivamente
			//per segnalare eventuali errori di accoppiamento e appartenenza a orario di lavoro valido
			if(stamping.stampType!= null && stamping.stampType.identifier.equals("s"))
			{
				serviceStampings.add(stamping);
				continue;
			}
			//cerca l'entrata
			if(stampEnter==null)
			{
				if(stamping.isIn())
				{
					stampEnter = stamping;
					continue;
				}
				if(stamping.isOut())
				{
					//una uscita prima di una entrata e' come se non esistesse
					stamping.valid = false;
					continue;
				}
			
			}
			//cerca l'uscita
			if(stampEnter!=null)
			{
				if(stamping.isOut())
				{
					validPairs.add(new PairStamping(stampEnter, stamping));
					stampEnter.valid = true;
					stamping.valid = true;
					stampEnter = null;
					continue;
				}
				//trovo un secondo ingresso, butto via il primo
				if(stamping.isIn())
				{
					stampEnter.valid = false;
					stampEnter = stamping;
					continue;
				}
			}
		}
		//(2) scarto le stamping di servizio che non appartengono ad alcuna coppia valida
		List<Stamping> serviceStampingsInValidPair = new ArrayList<Stamping>();
		for(Stamping stamping : serviceStampings)
		{
			boolean belongToValidPair = false;
			for(PairStamping validPair : validPairs)
			{
				LocalDateTime outTime = validPair.out.date;
				LocalDateTime inTime = validPair.in.date;
				if(stamping.date.isAfter(inTime) && stamping.date.isBefore(outTime))
				{
					belongToValidPair = true;
					break;
				}		
			}
			if(belongToValidPair)
			{
				serviceStampingsInValidPair.add(stamping);
			}
			else
			{
				stamping.valid = false;
			}
		}
		
		//(3)aggrego le stamping di servizio per coppie valide ed eseguo il check di sequenza valida
		for(PairStamping validPair : validPairs)
		{
			LocalDateTime outTime = validPair.out.date;
			LocalDateTime inTime = validPair.in.date;
			List<Stamping> serviceStampingsInSinglePair = new ArrayList<Stamping>();
			for(Stamping stamping : serviceStampingsInValidPair)
			{
				if(stamping.date.isAfter(inTime) && stamping.date.isBefore(outTime))
				{
					serviceStampingsInSinglePair.add(stamping);
				}	
			}
			//check		
			Stamping serviceExit = null;
			for(Stamping stamping : serviceStampingsInSinglePair)
			{
				//cerca l'uscita di servizio
				if(serviceExit==null)
				{
					if(stamping.isOut())
					{
						serviceExit = stamping;
						continue;
					}
					if(stamping.isIn())
					{
						//una entrata di servizio prima di una uscita di servizio e' come se non esistesse
						stamping.valid = false;
						continue;
					}
				}
				//cerca l'entrata di servizio
				if(serviceExit!=null)
				{
					if(stamping.isIn())
					{
						stamping.valid = true;
						serviceExit.valid = true;
						serviceExit = null;
						continue;
					}
					//trovo una seconda uscita di servizio, butto via la prima
					if(stamping.isOut())
					{
						serviceExit.valid = false;
						serviceExit = stamping;
						continue;
					}
				}
			}
		}
		
		return validPairs;
	}
	
	/**
	 * (alessandro)
	 * Setta il campo valid per ciascuna stamping contenuta in orderedStampings
	 */
	public void computeValidStampings()
	{
		getValidPairStamping();
	}
	
	/**
	 * (alessandro)
	 * Ordina per orario la lista delle stamping nel person day
	 */
	public void orderStampings()
	{
		Collections.sort(this.stampings);
	}
	
	/**
	 * (Alessandro)
	 * Algoritmo definitivo per il calcolo dei minuti lavorati nel person day
	 * @return
	 */
	public int getCalculatedTimeAtWork() {
		int justifiedTimeAtWork = 0;
		int workTime=0;
		
		
		//Se hanno il tempo di lavoro fissato non calcolo niente
		StampProfile stampProfile = getStampProfile();
		if (stampProfile != null && stampProfile.fixedWorkingTime) 
		{
			return getWorkingTimeTypeDay().workingTime;
		} 

	
		//if(!this.date.isEqual(new LocalDate()))	//tranne oggi
		//{
			//controllo sulla possibilità che esistano timbrature di ingresso nel giorno precedente senza corrispondenti timbrature di uscita
			//se non la mattina seguente prima dell'orario di scadenza del controllo sulla timbratura stessa
		PersonUtility.checkExitStampNextDay(this);
		//}
		
		//assenze all day piu' altri casi di assenze
		for(Absence abs : absences){
			Logger.trace("Il codice che sto analizzando è: %s", abs.absenceType.code);
			//TODO: verificare con Claudio cosa fare con le timbrature in missione
			
			if(abs.absenceType.ignoreStamping || (abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay && !checkHourlyAbsenceCodeSameGroup(abs.absenceType)))
			{
				Logger.debug("Sto inserendo un'assenza giornaliera per %s %s in data %s", person.name, person.surname, date);
				return 0;
			}
			
			if(!abs.absenceType.code.equals("89") && abs.absenceType.justifiedTimeAtWork.minutesJustified != null)
			{
				//ci sono più codici di assenza uno è il codice da inviare a roma dell'assenza oraria corrispondente
				justifiedTimeAtWork = justifiedTimeAtWork + abs.absenceType.justifiedTimeAtWork.minutesJustified;
				continue;
			}
		}

		//in caso di assenza di timbrature considero il justifiedTimeAtwork
		if (stampings.size() == 0) 
		{
			return justifiedTimeAtWork;
		}
		
		//Se non c'è almeno una coppia di timbrature allora il tempo di lavoro è giustificato solo dalle assenze precendente calcolate
		if(stampings.size() == 1)
		{
			return justifiedTimeAtWork;
		}

		if(isHoliday()){
			workTime = 0;
			for(Stamping st : stampings){
				if(st.way == Stamping.WayType.in)
				{
					workTime = workTime - toMinute(st.date);									
				}
				if(st.way == Stamping.WayType.out)
				{
					workTime = workTime + toMinute(st.date);								
					Logger.trace("Timbratura di uscita: %s", workTime);
				}
			}
			return justifiedTimeAtWork + workTime;
		}

			
		Collections.sort(this.stampings);
		List<PairStamping> validPairs = this.getValidPairStamping();
	
		int myWorkTime=0;
		{
			for(PairStamping validPair : validPairs)
			{
				myWorkTime = myWorkTime - toMinute(validPair.in.date);
				myWorkTime = myWorkTime + toMinute(validPair.out.date);
			}
		}
	
		workTime = myWorkTime;

		//Il pranzo e' servito??		
		int breakTicketTime = getWorkingTimeTypeDay().breakTicketTime;	//30 minuti
		int mealTicketTime = getWorkingTimeTypeDay().mealTicketTime;	//6 ore

		List<PairStamping> gapLunchPairs = getGapLunchPairs(validPairs);
		
		//non ha timbrato per il pranzo (due sole timbrature)
		if(validPairs.size()==1) 
		{
			if( workTime > mealTicketTime && workTime - breakTicketTime > mealTicketTime )
			{
				workTime = workTime - breakTicketTime;
				this.isTicketAvailable = true;
				StampModificationType smt = StampModificationType.findById(StampModificationTypeValue.FOR_DAILY_LUNCH_TIME.getId());
				this.lunchTimeStampModificationType = smt;
			}
			else
			{
				this.isTicketAvailable = false;
			}
		}
		
		//potrebbe aver timbrato per il pranzo (più di due timbrature)
		if(validPairs.size()>1)
		{
			//ha timbrato per il pranzo //(TODO considero il primo gap in orario pranzo)
			if(gapLunchPairs.size()>0)
			{
				int minTimeForLunch = 0;
				for(PairStamping gapLunchPair : gapLunchPairs)
				{
					minTimeForLunch = minTimeForLunch - toMinute(gapLunchPair.in.date);
					minTimeForLunch = minTimeForLunch + toMinute(gapLunchPair.out.date);
					break;
				}
				if(workTime - breakTicketTime > mealTicketTime)
				{
					if( minTimeForLunch < breakTicketTime ) 
					{
						workTime = workTime - (breakTicketTime - minTimeForLunch);
						StampModificationType smt = StampModificationType.findById(StampModificationTypeValue.FOR_MIN_LUNCH_TIME.getId());
						this.lunchTimeStampModificationType = smt;
					}
					this.isTicketAvailable = true;
				}
				else
				{
					this.isTicketAvailable = false;
				}
			}
			else
			{
				this.isTicketAvailable = false;
			}
			
		}
		
		return workTime + justifiedTimeAtWork;


	}
	
	
	/**
	 * 
	 * @return lo stamp modification type relativo al tempo di lavoro fisso 
	 */
	public StampModificationType getFixedWorkingTime(){
		StampModificationType smt = null;
		if(StampProfile.getCurrentStampProfile(this.person, this.date) != null && StampProfile.getCurrentStampProfile(this.person, this.date).fixedWorkingTime)
			smt = StampModificationType.findById(StampModificationTypeValue.FIXED_WORKINGTIME.getId());
		
		return smt;
	}

	/* (eliminato perchè sostituito da isFixedTimeAtWork
	public boolean isFixedWorkingTime(){
		if(StampProfile.getCurrentStampProfile(this.person, this.date).fixedWorkingTime)
			return true;
		return false;
	}
	*/
	
	/** (alessandro)
	 * True se il personDay cade in uno stampProfile con fixedTimeAtWork = true;
	 * @return
	 */
	public boolean isFixedTimeAtWork()
	{
		boolean fixedWorkingType = false;
		
		for(StampProfile sp : this.person.stampProfiles)
		{
			if(DateUtility.isDateIntoInterval(this.date, new DateInterval(sp.startFrom,sp.endTo)))
			{
				fixedWorkingType = sp.fixedWorkingTime;
			}
		}
		return fixedWorkingType;
	}
	

	/** (alessandro)
	 * True se il personDay appartiene a un giorno festivo per la persona
	 * @param personWttd
	 * @return
	 */
	/*disabilitato da alessandro il 28 ottobre perche' non piu' utilizzato
	public boolean isPersonHoliday(List<WorkingTimeTypeDay> personWttd)
	{
		if(DateUtility.isGeneralHoliday(this.date) || personWttd.get(this.date.getDayOfWeek()-1).holiday==true)
		{
			return true;
		}
		return false;
	}
	*/
	
	

	/**
	 * 
	 * importa il  numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	private void updateTimeAtWork(){
		timeAtWork = getCalculatedTimeAtWork();
		
		save();
		//Logger.debug("Tempo di lavoro per %s %s nel giorno %s: %d minuti", person.name, person.surname, date, timeAtWork);
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
	 * 
	 * @return
	 */
	public PersonDay previousPersonDay(){
		PersonDay lastPreviousPersonDayInMonth = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
				"and pd.date >= ? and pd.date < ? ORDER by pd.date DESC", person, date.dayOfMonth().withMinimumValue(), date).first();
		return lastPreviousPersonDayInMonth;
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
		
		//il pranzo
		if(this.isTicketForcedByAdmin)
			this.isTicketAvailable = true;
		else
			this.isTicketAvailable = this.isTicketAvailable && checkTicketAvailableForWorkingTime();
		merge();

	}

	/**
	 * 
	 * @param date
	 * @return calcola il numero di minuti di cui è composta la data passata come parametro (di cui considera solo
	 * ora e minuti
	 */
	private static int toMinute(LocalDateTime date){
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
	/*disabilitato da alessandro il 28 ottobre perchè non utilizzato
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
	*/

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
	 * (checked 18/10 personStamping stampare SI/NO nel tamplate)
	 * @param date
	 * @param timeAtWork
	 * @param person
	 * @return se la persona può usufruire del buono pasto per quella data
	 */
	public boolean mealTicket(){
		
		//caso forced by admin
		if(this.isTicketForcedByAdmin)
			return true;
		
		//caso persone fixed
		if(!this.isHoliday() && this.isFixedTimeAtWork() && !this.isAllDayAbsences()) 
			return true;
		if(!this.isHoliday() && this.isFixedTimeAtWork() && this.isAllDayAbsences())
			return false;
		
		//caso generale
		return this.isTicketAvailable && checkTicketAvailableForWorkingTime();

	}

	/**
	 * (checked 18/10 personStamping stampare SI/NO nel tamplate)
	 * @return 
	 */
	private boolean checkTicketAvailableForWorkingTime(){
		
		if(person.workingTimeType.description.equals("normale-mod") || person.workingTimeType.description.equals("normale")
				|| person.workingTimeType.description.equals("80%") || person.workingTimeType.description.equals("85%"))
		{
			return true;
		}
		
		return false;

	}

	/**
	 * 
	 * @return la differenza tra l'orario di lavoro giornaliero e l'orario standard in minuti
	 */

	private void updateDifference(){
		//merge();
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		Logger.debug("Il tempo di lavoro per %s %s in data %s è: %d", person.name, person.surname, date, timeAtWork);
		Logger.debug("La lista delle assenze per %s contiene %d elementi", date, absences.size());
		Logger.debug("La lista delle timbrature per %s contiene %d elementi", date, stampings.size());
		
		StampProfile stampProfile = getStampProfile();
				
		int worktime = this.person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(this.date.getDayOfWeek()).workingTime;
		
		//persona fixed
		if(stampProfile != null && stampProfile.fixedWorkingTime && timeAtWork == 0){
			difference = 0;
			save();
			return;
		}

		//festivo
		if(this.isHoliday())
		{
			difference = timeAtWork;
			save();
			return;
		}
		
		//assenze giornaliere
		if(this.isAllDayAbsences())
		{
			difference = 0;
			save();
			return;
		}
		
		//feriale
		difference = timeAtWork - worktime;
		
		/*
		if((getWorkingTimeTypeDay().holiday) && (date.getDayOfMonth()==1) && stampings.size() == 0){
			difference = 0;
			save();
			return;
		}

		if(absenceList().size() > 0 && stampings.size() == 0){
			int diff = 0;
			for(Absence abs : absenceList()){
				if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay){
					difference = 0;
					save();
					return;
				}
				else{
					diff = diff+abs.absenceType.justifiedTimeAtWork.minutesJustified;
				}
			}
			difference = diff - this.person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(this.date.getDayOfWeek()).workingTime;
			save();
			return;
		}
		
		if(timeAtWork == 0 && absences.size() > 0){
			Logger.debug("Tempo di lavoro = 0 e assenza per %s %s in data %s", person.name, person.surname, date);
			Absence abs = absences.get(0);
			if(abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay){
				difference = 0;
				save();
				return;
			}
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
		 */
		Logger.debug("Differenza per %s %s nel giorno %s: %s", person.surname, person.name, date, difference);

	}

	/**
	 * (checked 18/10 personStamping)
	 * la lista delle timbrature del person day modificata con 
	 * (1) l'inserimento di una timbratura null nel caso in cui esistano due timbrature consecutive di ingresso o di uscita, 
	 * mettendo tale timbratura nulla in mezzo alle due
	 * (2) l'inserimento di una timbratura di uscita fittizia nel caso di today per calcolare il tempo di lavoro provvisorio
	 * (3) l'inserimento di timbrature null per arrivare alla dimensione del numberOfInOut
	 * @param stampings
	 * @return 
	 */
	public List<Stamping> getStampingsForTemplate(int numberOfInOut, boolean today) {

		if(today)
		{
			//aggiungo l'uscita fittizia 'now' nel caso risulti dentro il cnr non di servizio 
			boolean lastStampingIsIn = false;
			this.orderStampings();
			for(Stamping stamping : this.stampings)
			{
				if(stamping.stampType!= null && stamping.stampType.identifier.equals("s"))
					continue;
				if(stamping.isIn())
				{
					lastStampingIsIn = true;
					continue;
				}
				if(stamping.isOut())
				{
					lastStampingIsIn = false;
					continue;
				}
			}
			if(lastStampingIsIn)
			{
				Stamping stamping = new Stamping();
				stamping.way = WayType.out;
				stamping.date = new LocalDateTime();
				stamping.markedByAdmin = false;
				stamping.exitingNow = true;
				this.stampings.add(stamping);
			}
		}
		List<Stamping> stampingsForTemplate = new ArrayList<Stamping>();
		boolean isLastIn = false;

		for (Stamping s : this.stampings) {
			//sono dentro e trovo una uscita 
			if (isLastIn && s.way == WayType.out) 
			{
				//salvo l'uscita
				stampingsForTemplate.add(s);
				isLastIn=false;
				continue;
			}
			//sono dentro e trovo una entrata
			if (isLastIn && s.way == WayType.in) 
			{
				//creo l'uscita fittizia
				Stamping stamping = new Stamping();
				stamping.way = WayType.out;
				stamping.date = null;
				stampingsForTemplate.add(stamping);
				//salvo l'entrata
				stampingsForTemplate.add(s);
				isLastIn=true;
				continue;
			}
			
			//sono fuori e trovo una entrata
			if (!isLastIn && s.way == WayType.in) 
			{
				//salvo l'entrata
				stampingsForTemplate.add(s);
				isLastIn = true;
				continue;
			}
			
			//sono fuori e trovo una uscita
			if (!isLastIn && s.way == WayType.out) 
			{
				//creo l'entrata fittizia
				Stamping stamping = new Stamping();
				stamping.way = WayType.in;
				stamping.date = null;
				stampingsForTemplate.add(stamping);
				//salvo l'uscita
				stampingsForTemplate.add(s);
				isLastIn = false;
				continue;
			}
		}
		while(stampingsForTemplate.size()<numberOfInOut*2)
		{
			if(isLastIn)
			{
				//creo l'uscita fittizia
				Stamping stamping = new Stamping();
				stamping.way = WayType.out;
				stamping.date = null;
				stampingsForTemplate.add(stamping);
				isLastIn = false;
				continue;
			}
			if(!isLastIn)
			{
				//creo l'entrata fittizia
				Stamping stamping = new Stamping();
				stamping.way = WayType.in;
				stamping.date = null;
				stampingsForTemplate.add(stamping);
				isLastIn = true;
				continue;
			}
		}
		
		return stampingsForTemplate;
	}

	
	
	/**
	 * (TODO questo metodo viene usato in personMonth.getStampingCode() che a sua volta è usato nei template. Controllarlo)
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
	 * (TODO questo metodo viene usato in personMonth.getStampingCode() che a sua volta è usato nei template. Controllarlo)
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
	 * (TODO questo metodo viene usato in personMonth.getStampingCode() che a sua volta è usato nei template. Controllarlo)
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
		if(stampings.size()%2 != 0){
			if(timeAtWork >= getWorkingTimeTypeDay().mealTicketTime+getWorkingTimeTypeDay().breakTicketTime)
				smt = StampModificationType.findById(StampModificationTypeValue.FOR_DAILY_LUNCH_TIME.getId());
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
	private List<PairStamping> getGapLunchPairs(List<PairStamping> validPairs)
	{
		//Assumo che la timbratura di uscita e di ingresso debbano appartenere alla finestra 12:00 - 15:00
		Configuration config = Configuration.getCurrentConfiguration();
		LocalDateTime startLunch = new LocalDateTime()
		.withYear(this.date.getYear())
		.withMonthOfYear(this.date.getMonthOfYear())
		.withDayOfMonth(this.date.getDayOfMonth())
		.withHourOfDay(config.mealTimeStartHour)
		.withMinuteOfHour(config.mealTimeStartMinute);
		
		LocalDateTime endLunch = new LocalDateTime()
		.withYear(this.date.getYear())
		.withMonthOfYear(this.date.getMonthOfYear())
		.withDayOfMonth(this.date.getDayOfMonth())
		.withHourOfDay(config.mealTimeEndHour)
		.withMinuteOfHour(config.mealTimeEndMinute);
		
		List<PairStamping> lunchPairs = new ArrayList<PersonDay.PairStamping>();
		for(PairStamping validPair : validPairs)
		{
			 LocalDateTime in = validPair.in.date;
			 LocalDateTime out = validPair.out.date;
			 
			 if( (out.isAfter(startLunch) && out.isBefore(endLunch)) || (in.isAfter(startLunch) && in.isBefore(endLunch)) )
				 lunchPairs.add(validPair);
				 
		}
		
		//costruisco le nuove coppie che sono gli intervalli fra le lunchPair
		List<PairStamping> gapPairs = new ArrayList<PersonDay.PairStamping>();
		PairStamping lastPair = null;
		for(PairStamping lunchPair : lunchPairs)
		{
			//prima coppia
			if(lastPair==null)
			{
				lastPair = lunchPair;
				continue;
			}
			gapPairs.add( new PairStamping(lastPair.out, lunchPair.in) );
		}
		
		return gapPairs;
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

		return null;
	}
	
	/** (alessandro)
	 * True se il personDay contiene una sequenza corretta di stampings
	 * @return false in caso di nessuna timbratura, timbrature dispari, timbrature consecutive
	 */
	public boolean containsProperStampingSequence()
	{
		if(this.stampings.size() == 0)					//zero timbrature
		{
			return false;
		}
		else if(this.stampings.size()%2==1)				//timbrature dispari
		{
			return false;
		}
		else											//timbrature pari ma in/in oppure out/out
		{
			String lastWay = "out";						
			for(Stamping s : this.stampings)
			{
				if(s.way.description.equals(lastWay))
				{
					return false;
				}
				lastWay = s.way.description;
			}
		}
		return true;
	}
	
	/**
	 * TODO appena riscrivo il metodo di riepilogo mensile con PersonDaysInTrouble si puo' cancellare
	 * ed il metodo ufficiale diventa isAllDayAbsences
	 *  (alessandro)
	 * True se il personDay contiene una assenza con tempo giustificato AllDay, false altrimenti
	 * @return
	 */
	public boolean containsAllDayAbsence()
	{
		for(Absence ab : this.absences)
		{
			if(ab.absenceType.justifiedTimeAtWork.description.equals("Tutto il giorno"))
			{
				return true;
			}
		}
		return false;
	}
	
	/** (alessandro)
	 * True se il personDay appartiene a un giorno festivo per la persona
	 * @param personWttd
	 * @return
	 */
	/*disabilitato da alessandro il 28 ottobre perche' non piu' utilizzato
	public boolean isPersonHoliday(List<WorkingTimeTypeDay> personWttd)
	{
		if(DateUtility.isGeneralHoliday(this.date) || personWttd.get(this.date.getDayOfWeek()-1).holiday==true)
		{
			return true;
		}
		return false;
	}
	*/
	
	@Override
	public String toString() {
		return String.format("PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, modificationType = %s, progressive = %s, timeAtWork = %s",
				id, person.id, date, difference, isTicketAvailable, modificationType, progressive, timeAtWork);
	}
	


}
