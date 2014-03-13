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
import org.hibernate.envers.NotAudited;
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

	@Column(name = "time_justified")
	public Integer timeJustified;
	//FIXME questo campo non è mai utilizzato
	
	public Integer difference;

	public Integer progressive;
	
	@Column(name = "is_ticket_available")
	public boolean isTicketAvailable = true;

	@Column(name = "is_ticket_forced_by_admin")
	public boolean isTicketForcedByAdmin = false;

	//@Column(name = "is_time_at_work_auto_certificated")
	//public boolean isTimeAtWorkAutoCertificated = false;
		
	//@Column(name = "is_holiday")
	//public boolean isHoliday = false;	rmHoliday
	
	@Column(name = "is_working_in_another_place")
	public boolean isWorkingInAnotherPlace = false;

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
	@OrderBy("date ASC")
	public List<Stamping> stampings = new ArrayList<Stamping>();

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
	public List<Absence> absences = new ArrayList<Absence>();

	@ManyToOne
	@JoinColumn(name = "stamp_modification_type_id")
	public StampModificationType stampModificationType;
	
	@NotAudited
	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
	public List<PersonDayInTrouble> troubles = new ArrayList<PersonDayInTrouble>();
	
	@Transient
	public PersonDay previousPersonDayInMonth = null;
		
	@Transient
	public Contract personDayContract = null;
	
	@Transient
	private Boolean isHolidayy = null;
	
	@Transient
	private Boolean isFixedTimeAtWorkk = null;
	
	
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
	
	//setter implementato per yaml parser TODO toglierlo configurando snakeyaml
	public void setDate(String date){
		this.date = new LocalDate(date);
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
	 * Controlla che il personDay cada in un giorno festivo
	 * @param data
	 * @return
	 */
	public boolean isHoliday(){
		if(isHolidayy!=null)	//già calcolato
			return isHolidayy;
		isHolidayy = DateUtility.isHoliday(this.person, this.date);
		return isHolidayy;
	}
	
	/**
	 * Controlla che il personDay cada nel giorno attuale
	 * @return
	 */
	public boolean isToday(){
		return this.date.isEqual(new LocalDate());
	}
	
	/**
	 * @return true se nel giorno vi e' una assenza giornaliera
	 */
	public boolean isAllDayAbsences()
	{
		for(Absence ab : absences)
		{
			if(ab.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.AllDay) && !checkHourlyAbsenceCodeSameGroup(ab.absenceType))
				return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return true se nel giorno c'è un'assenza oraria che giustifica una quantità oraria sufficiente a decretare la persona
	 * "presente" a lavoro
	 */
	public boolean isEnoughHourlyAbsences(){
		for(Absence abs : absences){
			if(abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FourHours) ||
					abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.FiveHours) ||
					abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SixHours) ||
					abs.absenceType.justifiedTimeAtWork.equals(JustifiedTimeAtWork.SevenHours))
				return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @return true se il person day è in trouble
	 */
	public boolean isInTrouble()
	{
		for(PersonDayInTrouble pdt : this.troubles)
		{
			if(pdt.fixed==false)
				return true;
		}
		return false;
	}

	
	/**
	 * 
	 * @param abt
	 * @return true se nella lista assenze esiste un'assenza che appartenga a un gruppo il cui codice di rimpiazzamento non
	 * sia nullo
	 */
	private boolean checkHourlyAbsenceCodeSameGroup(AbsenceType abt){
		Query query = JPA.em().createQuery("Select abs from Absence abs where abs.absenceType.absenceTypeGroup.replacingAbsenceType = :abt and " +
				"abs.personDay = :pd");
		query.setParameter("abt", abt).setParameter("pd", this);
		return query.getResultList().size() > 0;
	}
	
	
	
	
	
	
	/**
	 * Setta il campo valid per ciascuna stamping contenuta in orderedStampings
	 */
	public void computeValidStampings()
	{
		PairStamping.getValidPairStamping(this.stampings);
	}
	
	/**
	 * Ordina per orario la lista delle stamping nel person day
	 */
	public void orderStampings()
	{
		Collections.sort(this.stampings);
	}
	
	/**
	 * Ritorna l'ultima timbratura in ordine di tempo nel giorno
	 * @return
	 */
	private Stamping getLastStamping()
	{
		Stamping last = null;
		for(Stamping s : this.stampings)
		{
			if(last==null)
				last = s;
			else if(last.date.isBefore(s.date))
				last = s;
		}
		return last;
	}
	
	/**
	 * Algoritmo definitivo per il calcolo dei minuti lavorati nel person day.
	 * Ritorna i minuti di lavoro per la persona nel person day ed in base ad essi assegna il campo isTicketAvailable.
	 * 
	 * @return il numero di minuti trascorsi a lavoro
	 */
	public int getCalculatedTimeAtWork() {
		int justifiedTimeAtWork = 0;
		
		//Se hanno il tempo di lavoro fissato non calcolo niente
		if (this.isFixedTimeAtWork()) 
		{
			if(this.isHoliday())
				return 0;
			return getWorkingTimeTypeDay().workingTime;
		} 

		//assenze all day piu' altri casi di assenze
		for(Absence abs : absences){
			if(abs.absenceType.ignoreStamping || (abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay && !checkHourlyAbsenceCodeSameGroup(abs.absenceType)))
			{
				setIsTickeAvailable(false);
				return 0;
			}
			
			if(!abs.absenceType.code.equals("89") && abs.absenceType.justifiedTimeAtWork.minutesJustified != null)
			{
				//TODO CASO STRANO qua il buono mensa non si capisce se ci deve essere o no
				justifiedTimeAtWork = justifiedTimeAtWork + abs.absenceType.justifiedTimeAtWork.minutesJustified;
				continue;
			}
		}

		//se non c'è almeno una coppia di timbrature considero il justifiedTimeAtwork 
		//(che però non contribuisce all'attribuzione del buono mensa che quindi è certamente non assegnato)
		if (stampings.size() < 2) 
		{
			setIsTickeAvailable(false);
			return justifiedTimeAtWork;
		}
		
		//TODO se è festa si dovrà capire se il tempo di lavoro deve essere assegnato oppure no 
		if(this.isHoliday()){
			orderStampings();
			
			List<PairStamping> validPairs = PairStamping.getValidPairStamping(this.stampings);
			
			int holidayWorkTime=0;
			{
				for(PairStamping validPair : validPairs)
				{
					holidayWorkTime = holidayWorkTime - toMinute(validPair.in.date);
					holidayWorkTime = holidayWorkTime + toMinute(validPair.out.date);
				}
			}
			setIsTickeAvailable(false);
			return justifiedTimeAtWork + holidayWorkTime;
		}

			
		orderStampings();
		List<PairStamping> validPairs = PairStamping.getValidPairStamping(this.stampings);
	
		int workTime=0;
		{
			for(PairStamping validPair : validPairs)
			{
				workTime = workTime - toMinute(validPair.in.date);
				workTime = workTime + toMinute(validPair.out.date);
			}
		}
	
		//Il pranzo e' servito??		
		this.stampModificationType = null;
		int breakTicketTime = getWorkingTimeTypeDay().breakTicketTime;	//30 minuti
		int mealTicketTime = getWorkingTimeTypeDay().mealTicketTime;	//6 ore
		if(mealTicketTime == 0){
			setIsTickeAvailable(false);
			return workTime + justifiedTimeAtWork;
		}
			
		
		List<PairStamping> gapLunchPairs = getGapLunchPairs(validPairs);
		
		//ha timbrato per il pranzo ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if(gapLunchPairs.size()>0)
		{
			int minTimeForLunch = 0;
			for(PairStamping gapLunchPair : gapLunchPairs)
			{
				//per adesso considero il primo gap in orario pranzo)
				minTimeForLunch = minTimeForLunch - toMinute(gapLunchPair.in.date);
				minTimeForLunch = minTimeForLunch + toMinute(gapLunchPair.out.date);
				break;
			}
			
			//gap e worktime sufficienti
			if(minTimeForLunch >= breakTicketTime && workTime >= mealTicketTime)
			{
				setIsTickeAvailable(true);
				return workTime + justifiedTimeAtWork;
			}
			
			//worktime sufficiente gap insufficiente (e)
			if(workTime - breakTicketTime >= mealTicketTime)
			{
				if( minTimeForLunch < breakTicketTime ) //dovrebbe essere certamente true
				{
					if(!isTicketForcedByAdmin || isTicketForcedByAdmin&&isTicketAvailable )		//TODO decidere la situazione intricata se l'amministratore forza a true
						workTime = workTime - (breakTicketTime - minTimeForLunch);
					this.stampModificationType = StampModificationType.getStampModificationTypeByCode(StampModificationTypeCode.FOR_MIN_LUNCH_TIME.getCode());
					
				}
				setIsTickeAvailable(true);
				return workTime + justifiedTimeAtWork;
			}
			
			//worktime insufficiente
			setIsTickeAvailable(false);
			return workTime + justifiedTimeAtWork;
			
		}
		
		//non ha timbrato per il pranzo //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		if( workTime > mealTicketTime && workTime - breakTicketTime >= mealTicketTime )
		{
			//worktime sufficiente (p)
			if(!isTicketForcedByAdmin || isTicketForcedByAdmin&&isTicketAvailable )			//TODO decidere la situazione intricata se l'amministratore forza a true
				workTime = workTime - breakTicketTime;
			setIsTickeAvailable(true);
			this.stampModificationType = StampModificationType.getStampModificationTypeByCode(StampModificationTypeCode.FOR_DAILY_LUNCH_TIME.getCode());
			return workTime + justifiedTimeAtWork;
		}
		else
		{
			//worktime insufficiente
			setIsTickeAvailable(false);
			return workTime + justifiedTimeAtWork;
		}
	}
	
	/**
	 * 
	 * @return lo stamp modification type relativo al tempo di lavoro fisso 
	 */
	public StampModificationType getFixedWorkingTime(){
		//TODO usato solo in PersonStampingDayRecap bisogna metterlo nella cache
		return StampModificationType.findById(StampModificationTypeValue.FIXED_WORKINGTIME.getId());
	}

	/** 
	 * True se il personDay cade in uno stampProfile con fixedTimeAtWork = true;
	 * @return
	 */
	public boolean isFixedTimeAtWork()
	{
		if(this.isFixedTimeAtWorkk!=null)	//già calcolato
			return this.isFixedTimeAtWorkk;
		
		this.isFixedTimeAtWorkk = false;
		//FIXME: invece che ciclare su tutti non si poteva con una select prendere lo
		//stampProfile alla data attuale?
		for(StampProfile sp : this.person.stampProfiles)
		{
			if(DateUtility.isDateIntoInterval(this.date, new DateInterval(sp.startFrom,sp.endTo)))
			{
				this.isFixedTimeAtWorkk = sp.fixedWorkingTime;
			}
		}
		return this.isFixedTimeAtWorkk;
	}
	
	/**
	 * 
	 * importa il  numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	private void updateTimeAtWork()
	{
		timeAtWork = getCalculatedTimeAtWork();
	}


	/**
	 * 
	 * @return la differenza tra l'orario di lavoro giornaliero e l'orario standard in minuti
	 */
	private void updateDifference(){
	
		//int worktime = this.person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(this.date.getDayOfWeek()).workingTime;
		int worktime = this.person.getWorkingTimeType(date).getWorkingTimeTypeDayFromDayOfWeek(this.date.getDayOfWeek()).workingTime;
		
		//persona fixed
		if(this.isFixedTimeAtWork() && timeAtWork == 0){
			difference = 0;
			return;
		}
	
		//festivo
		if(this.isHoliday()){
			difference = timeAtWork;
			return;
		}
		
		//assenze giornaliere
		if(this.isAllDayAbsences()){
			difference = 0;
			return;
		}
		
		//feriale
		difference = timeAtWork - worktime;
	
	}

	/**
	 * calcola il valore del progressivo giornaliero e lo salva sul db
	 */
	private void updateProgressive()
	{

		//primo giorno del mese
		if(previousPersonDayInMonth==null)
		{
			progressive = difference;
			return;
		}
		
		//primo giorno del contratto
		if(previousPersonDayInMonth.personDayContract == null || previousPersonDayInMonth.personDayContract.id != personDayContract.id)
		{
			progressive = difference;
			return;
		}
		
		//caso generale
		progressive = difference + previousPersonDayInMonth.progressive;

	}
	
	/**
	 * Assegna ad ogni person day del mese il primo precedente esistente.
	 * Assegna null al primo giorno del mese.
	 */
	private void associatePreviousInMonth()
	{
		LocalDate beginMonth = this.date.dayOfMonth().withMinimumValue();
		LocalDate endMonth = this.date.dayOfMonth().withMaximumValue();
		
		List<PersonDay> pdList = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? and pd.date >= ? and pd.date <= ? ORDER by pd.date",
				person, beginMonth, endMonth).fetch();
		for(int i=1; i<pdList.size(); i++)
		{
			pdList.get(i).previousPersonDayInMonth = pdList.get(i-1);
		}
	}

	/**
	 * Aggiorna il campo ticket available e persiste il dato. Controllare per le persone fixed nel giorno di festa.
	 */
	private void updateTicketAvailable()
	{
		//caso forced by admin
		if(this.isTicketForcedByAdmin)
		{
			//this.isTicketAvailable = true; SBAGLIATO non devo fare niente
			this.save();
			return;
		}
		
		//caso persone fixed
		if(this.isFixedTimeAtWork())
		{
			if(this.isHoliday())
			{
				this.isTicketAvailable = false;
				this.save();
			}
			else if(!this.isHoliday() && !this.isAllDayAbsences())
			{
				this.isTicketAvailable = true;
				this.save();
			}
			else if(!this.isHoliday() && this.isAllDayAbsences())
			{
				this.isTicketAvailable = false;
				this.save();
			}
			return;
		}

		//caso persone normali
		this.isTicketAvailable = this.isTicketAvailable && isTicketAvailableForWorkingTime();
		return; 
	}
	
	/**
	 * Setta il valore della variabile isTicketAvailable solo se isTicketForcedByAdmin è false
	 * @param value
	 */
	private void setIsTickeAvailable(boolean isTicketAvailable)
	{
		if(!this.isTicketForcedByAdmin)
			this.isTicketAvailable = isTicketAvailable;
	}
	
	/**
	 * Il personDay precedente 
	 * @return
	 */
	public PersonDay previousPersonDay()
	{
		//TODO usato solo in PersonStampingDayRecap, vedere come ottimizzarlo
		PersonDay lastPreviousPersonDayInMonth = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? " +
				"and pd.date >= ? and pd.date < ? ORDER by pd.date DESC", person, date.dayOfMonth().withMinimumValue(), date).first();
		return lastPreviousPersonDayInMonth;
	}

	/**
	 * (1) Controlla che il personDay sia ben formato (altrimenti lo inserisce nella tabella PersonDayInTrouble.
	 * (2) Popola i valori aggiornati del person day e li persiste nel db
	 */
	public void populatePersonDay()
	{
			
		//if(this.person.id == 45) Logger.info("  *PopulatePersonDay date=%s", this.date);
		//controllo problemi strutturali del person day
		if(this.date.isBefore(new LocalDate())){
			this.save();
			this.checkForPersonDayInTrouble();
		}
			//this.checkForPersonDayInTrouble();

		//if(this.person.id == 45 && this.troubles.size()==0) Logger.info("  *  Check Trouble                 NO");
		//if(this.person.id == 45 && this.troubles.size()==1) Logger.info("  *  Check Trouble                 SI");
		
		//Strutture dati transienti necessarie al calcolo
		if(personDayContract==null)
		{
			this.personDayContract = this.person.getContract(date);
			//Se la persona non ha un contratto attivo non si fanno calcoli per quel giorno, le timbrature vengono comunque mantenute
			if(personDayContract==null)
				return;
		}
		
		//if(this.person.id == 45) Logger.info("  *  Associa contratto             contract=%s", this.personDayContract.id);
	
		
		if(previousPersonDayInMonth==null)
		{
			associatePreviousInMonth();
		}
		
		//if(this.person.id == 45 && this.previousPersonDayInMonth!=null) Logger.info("  *  Associa previous in month     previous=%s", this.previousPersonDayInMonth.date);
		//if(this.person.id == 45 && this.previousPersonDayInMonth==null) Logger.info("  *  Associa previous in month     previous=null");
		
		
		
		
		if(previousPersonDayInMonth!=null && previousPersonDayInMonth.personDayContract==null)
		{
			this.previousPersonDayInMonth.personDayContract = this.person.getContract(this.previousPersonDayInMonth.date);
		}
	
		//controllo uscita notturna
		this.checkExitStampNextDay();
		
		//if(this.person.id == 45 && this.previousPersonDayInMonth!=null) Logger.info("  *  After check midnight          previous=%s", this.previousPersonDayInMonth.date);
		//if(this.person.id == 45 && this.previousPersonDayInMonth==null) Logger.info("  *  After check midnight          previous=null");
		
		
		updateTimeAtWork();
		
		//if(this.person.id == 45) Logger.info("  *  Time at work                  %s", this.timeAtWork);
		updateDifference();
		//if(this.person.id == 45) Logger.info("  *  Difference                    %s", this.difference);
		updateProgressive();
		//if(this.person.id == 45) Logger.info("  *  Progressive                   %s", this.progressive);
		updateTicketAvailable();
		//if(this.person.id == 45) Logger.info("  *  Ticket                        %s", this.isTicketAvailable);
		
		//this.merge();
		this.save();
		
		//if(this.person.id == 45) Logger.info("  **********************************************************************");
		
	}
	
	/**
	 * Stessa logica di populatePersonDay ma senza persistere i calcoli (usato per il giorno di oggi)
	 */
	public void queSeraSera()
	{
		//Strutture dati transienti necessarie al calcolo
		if(personDayContract==null)
		{
			this.personDayContract = this.person.getContract(date);
			//Se la persona non ha un contratto attivo non si fanno calcoli per quel giorno, le timbrature vengono comunque mantenute
			if(personDayContract==null)
				return;
		}
		
		if(previousPersonDayInMonth==null)
		{
			associatePreviousInMonth();
		}
		
		if(previousPersonDayInMonth!=null && previousPersonDayInMonth.personDayContract==null)
		{
			this.previousPersonDayInMonth.personDayContract = this.person.getContract(this.previousPersonDayInMonth.date);
		}
		
		updateTimeAtWork();
		updateDifference();
		updateProgressive();
		updateTicketAvailable();

	}
	
	/**
	 * Verifica che nel person day vi sia una situazione coerente di timbrature. Situazioni errate si verificano nei casi 
	 *  (1) che vi sia almeno una timbratura non accoppiata logicamente con nessun'altra timbratura 
	 * 	(2) che le persone not fixed non presentino ne' assenze AllDay ne' timbrature. 
	 * In caso di situazione errata viene aggiunto un record nella tabella PersonDayInTrouble.
	 * Se il PersonDay era presente nella tabella PersonDayInTroubled ed è stato fixato, viene settato a true il campo
	 * fixed.
	 * @param pd
	 * @param person
	 */
	public void checkForPersonDayInTrouble()
	{
		//persona fixed
		if(this.isFixedTimeAtWork())
		{
			if(this.stampings.size()!=0)
			{
				this.computeValidStampings();
				for(Stamping s : this.stampings)
				{
					if(!s.valid)
					{
						PersonDayInTrouble.insertPersonDayInTrouble(this, "timbratura disaccoppiata persona fixed");
						return;
					}
				}
			}			
		}
		//persona not fixed
		else
		{
			//caso no festa, no assenze, no timbrature
			if(!this.isAllDayAbsences() && this.stampings.size()==0 && !this.isHoliday() && !this.isEnoughHourlyAbsences())
			{
				PersonDayInTrouble.insertPersonDayInTrouble(this, "no assenze giornaliere e no timbrature");
				return;
			}
			//caso no festa, no assenze, timbrature disaccoppiate
			if(!this.isAllDayAbsences() && !this.isHoliday())
			{
				this.computeValidStampings();
				for(Stamping s : this.stampings)
				{
					if(!s.valid)
					{
						PersonDayInTrouble.insertPersonDayInTrouble(this, "timbratura disaccoppiata giorno feriale");
						return;
					}
				}
			}
			//caso festa, no assenze, timbrature disaccoppiate
			else if(!this.isAllDayAbsences() && this.isHoliday())
			{
				this.computeValidStampings();
				for(Stamping s : this.stampings)
				{
					if(!s.valid)
					{
						PersonDayInTrouble.insertPersonDayInTrouble(this, "timbratura disaccoppiata giorno festivo");
						return;
					}
				}
			}
		}
		
		//giorno senza problemi, se era in trouble lo fixo
		if(this.troubles!=null && this.troubles.size()>0)
		{
			//per adesso no storia, unico record
			PersonDayInTrouble pdt = troubles.get(0);	
			pdt.fixed = true;
			pdt.save();
			//this.troubles.add(pdt);
			this.save();
		}

	}
	
	/**
	 * Metodo da utilizzare per la modifica del personDay che impatta su tutto il mese
	 */
	public void updatePersonDaysInMonth()
	{
		//TODO renderlo statico
		PersonUtility.updatePersonDaysIntoInterval(this.person, this.date, this.date);
		//TODO: inserire qui una chiamata alla fixPersonSituation di Administration quando le modifiche dovranno ripercuotersi anche 
		//sui personMonth
	}

	/**
	 * Calcola il numero di minuti trascorsi dall'inizio del giorno all'ora presente nella data
	 * @param date
	 * @return
	 */
	private static int toMinute(LocalDateTime date){
		int dateToMinute = 0;
		if (date!=null)
		{
			int hour = date.get(DateTimeFieldType.hourOfDay());
			int minute = date.get(DateTimeFieldType.minuteOfHour());
			dateToMinute = (60*hour)+minute;
		}
		return dateToMinute;
	}

	/**
	 * Il piano giornaliero di lavoro previsto dal contratto per quella data
	 * @return 
	 */
	private WorkingTimeTypeDay getWorkingTimeTypeDay(){
		return person.getWorkingTimeType(date).workingTimeTypeDays.get(date.getDayOfWeek()-1);
	}

	/**
	 * True se la persona ha uno dei WorkingTime abilitati al buono pasto
	 * @return 
	 */
	private boolean isTicketAvailableForWorkingTime(){
		WorkingTimeType wtt = person.getWorkingTimeType(date);
		if(wtt.description.equals("Normale-asd") || wtt.description.equals("Normale") || wtt.description.equals("80%") || wtt.description.equals("85%"))
		{
			return true;
		}
		return false;
	}

	/**
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
	 * @return lo stamp modification type relativo alla timbratura aggiunta dal sistema nel caso mancasse la timbratura d'uscita prima
	 * della mezzanotte del giorno in questione
	 */
	public StampModificationType checkMissingExitStampBeforeMidnight()
	{
		//FIXME renderlo efficiente
		StampModificationType smt = null;
		for(Stamping st : stampings){
			if(st.stampModificationType != null && st.stampModificationType.equals(StampModificationTypeValue.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getStampModificationType()))
				smt = StampModificationType.findById(StampModificationTypeValue.TO_CONSIDER_TIME_AT_TURN_OF_MIDNIGHT.getId());
		}
		return smt;
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
		//Configuration conf = Configuration.getConfiguration(this.date);
		ConfGeneral conf = ConfGeneral.getConfGeneral();
		LocalDateTime startLunch = new LocalDateTime()
		.withYear(this.date.getYear())
		.withMonthOfYear(this.date.getMonthOfYear())
		.withDayOfMonth(this.date.getDayOfMonth())
		.withHourOfDay(conf.mealTimeStartHour)
		.withMinuteOfHour(conf.mealTimeStartMinute);
		
		LocalDateTime endLunch = new LocalDateTime()
		.withYear(this.date.getYear())
		.withMonthOfYear(this.date.getMonthOfYear())
		.withDayOfMonth(this.date.getDayOfMonth())
		.withHourOfDay(conf.mealTimeEndHour)
		.withMinuteOfHour(conf.mealTimeEndMinute);
		
		//List<PairStamping> lunchPairs = new ArrayList<PersonDay.PairStamping>();
		List<PairStamping> gapPairs = new ArrayList<PersonDay.PairStamping>();
		Stamping outForLunch = null;
		
		for(PairStamping validPair : validPairs)
		{
			 LocalDateTime out = validPair.out.date;
			 if(outForLunch==null)
			 {
				 if( (out.isAfter(startLunch.minusMinutes(1))) && (out.isBefore(endLunch.plusMinutes(1))) )
				 {
					 outForLunch = validPair.out;
				 }
			 }
			 else
			 {
				 gapPairs.add( new PairStamping(outForLunch, validPair.in) );
				 outForLunch = null;
				 if( (out.isAfter(startLunch.minusMinutes(1))) && (out.isBefore(endLunch.plusMinutes(1))) )
				 {
					 outForLunch = validPair.out;
				 }
			 }
		}
		
		return gapPairs;
	}
	
	@Override
	public String toString() {
		return String.format("PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, modificationType = %s, progressive = %s, timeAtWork = %s",
				id, person.id, date, difference, isTicketAvailable, stampModificationType, progressive, timeAtWork);
	}

	private void checkExitStampNextDay(){
		
		if(this.isFixedTimeAtWork())
			return;
		
		if(this.date.getDayOfMonth()==1)
			this.previousPersonDayInMonth = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.person = ? and pd.date < ? ORDER by pd.date DESC", this.person, this.date).first();
		
		if(this.previousPersonDayInMonth==null) //primo giorno del contratto
			return;
		if(!this.previousPersonDayInMonth.date.plusDays(1).isEqual(this.date)) //giorni non consecutivi
			return;
		
		Stamping lastStampingPreviousDay = this.previousPersonDayInMonth.getLastStamping();
		
		if(lastStampingPreviousDay != null && lastStampingPreviousDay.isIn())
		{
			this.orderStampings();
			ConfYear config = ConfYear.getConfYear(this.date.getYear());
			if(this.stampings.size() > 0 && this.stampings.get(0).way == WayType.out && config.hourMaxToCalculateWorkTime > this.stampings.get(0).date.getHourOfDay())
			{
				Stamping correctStamp = new Stamping();
				correctStamp.date = new LocalDateTime(this.previousPersonDayInMonth.date.getYear(), this.previousPersonDayInMonth.date.getMonthOfYear(), this.previousPersonDayInMonth.date.getDayOfMonth(), 23, 59);
				correctStamp.way = WayType.out;
				correctStamp.markedByAdmin = false;
				correctStamp.stampModificationType = StampModificationType.findById(4l);
				correctStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
				correctStamp.personDay = this.previousPersonDayInMonth;
				correctStamp.save();
				this.previousPersonDayInMonth.stampings.add(correctStamp);
				this.previousPersonDayInMonth.save();

				this.previousPersonDayInMonth.populatePersonDay();
				Stamping newEntranceStamp = new Stamping();
				newEntranceStamp.date = new LocalDateTime(this.date.getYear(), this.date.getMonthOfYear(), this.date.getDayOfMonth(),0,0);
				newEntranceStamp.way = WayType.in;
				newEntranceStamp.markedByAdmin = false;
				newEntranceStamp.stampModificationType = StampModificationType.findById(4l);
				newEntranceStamp.note = "Ora inserita automaticamente per considerare il tempo di lavoro a cavallo della mezzanotte";
				newEntranceStamp.personDay = this;
				newEntranceStamp.save();
				this.stampings.add(newEntranceStamp);
				this.save();
			}

			if(this.date.getDayOfMonth() == 1)
				this.previousPersonDayInMonth = null;
		}
		
		if(this.date.getDayOfMonth() == 1)
			this.previousPersonDayInMonth = null;

	}
	
	/**
	 * Classe che modella due stampings logicamente accoppiate nel personday (una di ingresso ed una di uscita)
	 */
	public final static class PairStamping
	{
		private static int sequence_id = 1;
		
		int pairId;	//for hover template
		Stamping in;
		Stamping out;

		PairStamping(Stamping in, Stamping out)
		{
			this.in = in;
			this.out = out;
			this.pairId = sequence_id++;
			in.pairId = this.pairId;
			out.pairId = this.pairId;
		}
		
		/**
		 * Ritorna le coppie di stampings valide al fine del calcolo del time at work. All'interno del metodo
		 * viene anche settato il campo valid di ciascuna stampings contenuta nel person day
		 * @return
		 */
		public static List<PairStamping> getValidPairStamping(List<Stamping> stampings)
		{
			Collections.sort(stampings);
			//(1)Costruisco le coppie valide per calcolare il worktime
			List<PairStamping> validPairs = new ArrayList<PersonDay.PairStamping>();
			List<Stamping> serviceStampings = new ArrayList<Stamping>();
			Stamping stampEnter = null;
			for(Stamping stamping : stampings)
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
	}



}
