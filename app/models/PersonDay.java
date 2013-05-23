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
	 * importa il  numero di minuti in cui una persona è stata a lavoro in quella data
	 */
	private void updateTimeAtWork(){
		//merge();
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		int tempoLavoro = 0;
		LocalDateTime beginDate = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),0,0);
		LocalDateTime endDate = new LocalDateTime(date.getYear(),date.getMonthOfYear(),date.getDayOfMonth(),23,59);
		StampProfile stampProfile = getStampProfile();
		List<Stamping> reloadedStampings = Stamping.find("Select st from Stamping st where st.personDay = ? and " +
				"st.date > ? and st.date < ? order by st.date", this, beginDate, endDate).fetch();
		/**
		 * controllo sulla possibilità che esistano timbratrure di ingresso nel giorno precedente senza corrispondenti timbrature di uscita
		 * se non la mattina seguente prima dell'orario di scadenza del controllo sulla timbratura stessa
		 */
		PersonUtility.checkExitStampNextDay(this);
		if((stampings.size() == 0 && absences.size() == 0) || (stampings.size() == 1 && stampings.get(0).way == WayType.in)){
			//timeAtWork = (-1)*getWorkingTimeTypeDay().workingTime;
			timeAtWork = 0;
			save();
			return;
		}

		if(isHoliday()==true && stampings.size() > 0){
			if(stampings.size()==1){
				timeAtWork = 0;
				save();
				return;
			}
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
			timeAtWork = workTime;
			save();
			return;
		}

		if(reloadedStampings.contains(null)){
			if((reloadedStampings.size() < 3)&& (reloadedStampings.get(0)==null || reloadedStampings.get(1) == null)){
				timeAtWork = 0;
				save();
				return;
			}
			for(int index = 0; index < reloadedStampings.size(); index ++){
				if(reloadedStampings.get(index) == null && (index == 0 || index == 1)){
					if(reloadedStampings.get(2) != null && reloadedStampings.subList(2, reloadedStampings.size()).size() > 0 
							&& reloadedStampings.subList(2, reloadedStampings.size()).size() % 2 == 0){

						for(Stamping stamp : reloadedStampings.subList(1, reloadedStampings.size())){

							if(stamp.way == Stamping.WayType.in){
								tempoLavoro -= toMinute(stamp.date);									
								Logger.trace("Timbratura di ingresso: %s", tempoLavoro);	
							}
							if(stamp.way == Stamping.WayType.out){
								tempoLavoro += toMinute(stamp.date);								
								Logger.trace("Timbratura di uscita: %s", tempoLavoro);
							}
						}
					}

				}
				if(reloadedStampings.get(index) == null && (index == 2 || index == 3)){
					if(reloadedStampings.size() < 5 || (reloadedStampings.size() > 5 && reloadedStampings.size() % 2 != 0)){
						for(Stamping stamp : reloadedStampings.subList(0, 2)){
							if(stamp.way == Stamping.WayType.in){
								tempoLavoro -= toMinute(stamp.date);									
								Logger.trace("Timbratura di ingresso: %s", tempoLavoro);	
							}
							if(stamp.way == Stamping.WayType.out){
								tempoLavoro += toMinute(stamp.date);								
								Logger.trace("Timbratura di uscita: %s", tempoLavoro);
							}
						}
					}
					if(reloadedStampings.size() > 5 && reloadedStampings.size() % 2 == 0){
						int tempoLavoroPrimaParte = 0;
						int tempoLavoroSecondaParte = 0;
						for(Stamping stamp : reloadedStampings.subList(0, 2)){
							if(stamp.way == Stamping.WayType.in){
								tempoLavoroPrimaParte -= toMinute(stamp.date);									
								Logger.trace("Timbratura di ingresso: %s", tempoLavoro);	
							}
							if(stamp.way == Stamping.WayType.out){
								tempoLavoroPrimaParte += toMinute(stamp.date);								
								Logger.trace("Timbratura di uscita: %s", tempoLavoro);
							}
						}
						for(Stamping stamp2 : reloadedStampings.subList(4, reloadedStampings.size())){
							if(stamp2.way == Stamping.WayType.in){
								tempoLavoroSecondaParte -= toMinute(stamp2.date);									
								Logger.trace("Timbratura di ingresso: %s", tempoLavoro);	
							}
							if(stamp2.way == Stamping.WayType.out){
								tempoLavoroSecondaParte += toMinute(stamp2.date);								
								Logger.trace("Timbratura di uscita: %s", tempoLavoro);
							}
						}
						tempoLavoro = tempoLavoroPrimaParte+tempoLavoroSecondaParte;
					}
				}
				if(reloadedStampings.get(index) == null && (index == 4 || index == 5)){
					for(Stamping stamp : reloadedStampings.subList(0, 4)){
						if(stamp.way == Stamping.WayType.in){
							tempoLavoro -= toMinute(stamp.date);									
							Logger.trace("Timbratura di ingresso: %s", tempoLavoro);	
						}
						if(stamp.way == Stamping.WayType.out){
							tempoLavoro += toMinute(stamp.date);								
							Logger.trace("Timbratura di uscita: %s", tempoLavoro);
						}
					}
				}
			}
			if (stampProfile != null && stampProfile.fixedWorkingTime) {
				timeAtWork = Math.max(tempoLavoro, getWorkingTimeTypeDay().workingTime);
			} else {
				timeAtWork = tempoLavoro;	
			}
			save();
			return;
		}
		else{
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
				save();
				return;
			}
			LocalDateTime now = new LocalDateTime();
			if(reloadedStampings.size() > 0){
				Stamping s = reloadedStampings.get(0);
				if(s.date.toLocalDate().isEqual(now.toLocalDate())){					
					int nowToMinute = toMinute(now);
					if(reloadedStampings.size() == 0){
						timeAtWork = 0;
						save();
						return;
					}
					if(reloadedStampings.size() == 1){
						timeAtWork = toMinute(now)-toMinute(reloadedStampings.get(0).date);
						save();
						return;
					}
					else{
						for(int i = 0; i < reloadedStampings.size(); i++){

							int workingTime=0;

							if(reloadedStampings.get(i).way == Stamping.WayType.in && reloadedStampings.get(i).stampType != null){
								if(reloadedStampings.get(i-1) != null && reloadedStampings.get(i-1).stampType != null){
									//c'è stata un'uscita di servizio, questo è il corrispondente ingresso come lo calcolo?
									//workingTime = workingTime + (toMinute(reloadedStampings.get(i).date) - toMinute(reloadedStampings.get(i-1).date));
									workingTime = workingTime + 0;
								}
								if(reloadedStampings.get(i-1) == null)
									workingTime = workingTime - toMinute(reloadedStampings.get(i).date);
								else{
									//si fa un'entrata di servizio...ad esempio quando siamo in reperibilità?
								}
							}
							else{
								workingTime -= toMinute(reloadedStampings.get(i).date);
							}

							if(reloadedStampings.get(i).way == Stamping.WayType.out && reloadedStampings.get(i).stampType != null){
								if(reloadedStampings.get(i-1) != null && reloadedStampings.get(i-1).stampType != null){
									//uscita di servizio, dopo un ingresso di servizio...come si gestisce??
									workingTime = workingTime + 0;
								}

								else{
									workingTime += toMinute(reloadedStampings.get(i).date);
								}
							}

							else{
								workingTime += toMinute(reloadedStampings.get(i).date);
							}
							if(workingTime < 0)
								tempoLavoro = nowToMinute + workingTime;										
							else 
								tempoLavoro = nowToMinute - workingTime;																		
							timeAtWork = tempoLavoro;
							save();
							return;

						}
					}


				}				
				else{
					int workTime=0;
					for(int i = 0; i < reloadedStampings.size(); i++){
						if(reloadedStampings.get(i).way == Stamping.WayType.in){

							if(reloadedStampings.get(i).stampType != null){
								if((i-1) >= 0){
									if(reloadedStampings.get(i-1).stampType != null){
										//c'è stata un'uscita di servizio, questo è il corrispondente ingresso come lo calcolo? aggiungendo il tempo
										//trascorso fuori per servizio come tempo di lavoro
										Logger.debug("Anche la precedente timbratura era di servizio...calcolo il tempo in servizio come tempo a lavoro per %s %s", 
												person.name, person.surname);

										workTime = workTime + 0;
									}
									else{

									}
								}

							}
							else{
								workTime -= toMinute(reloadedStampings.get(i).date);
								Logger.debug("Normale timbratura di ingresso che mi dà un tempo di lavoro di: %d", workTime);
							}


						}

						if(reloadedStampings.get(i).way == Stamping.WayType.out){

							Logger.trace("Timbratura di uscita con stampType diverso da null per %s %s", person.name, person.surname);
							if(reloadedStampings.get(i).stampType != null){
								if((i-1) >= 0){
									if(reloadedStampings.get(i-1).stampType != null){
										workTime = workTime + 0;
									}
									else{
										workTime = workTime +0;
									}
								}

							}

							else{
								//timbratura normale di uscita
								workTime += toMinute(reloadedStampings.get(i).date);
								Logger.debug("Normale timbratura di uscita %s che mi dà un tempo di lavoro di: %d", reloadedStampings.get(i).date, workTime);
							}

						}

					}

					int minTimeForLunch = checkMinTimeForLunch();
					if((reloadedStampings.size()==4) && (minTimeForLunch < getWorkingTimeTypeDay().breakTicketTime) && (!reloadedStampings.contains(null)))
						tempoLavoro = workTime - (getWorkingTimeTypeDay().breakTicketTime-minTimeForLunch);							
					if((reloadedStampings.size()==2) && (workTime > getWorkingTimeTypeDay().mealTicketTime) && 
							(workTime-getWorkingTimeTypeDay().breakTicketTime > getWorkingTimeTypeDay().mealTicketTime)){

						tempoLavoro = workTime-getWorkingTimeTypeDay().breakTicketTime;	

					}
					else{
						tempoLavoro = workTime;
						Logger.debug("tempo di lavoro a fine metodo: %d", tempoLavoro);
					}

				}
			}
			if (stampProfile != null && stampProfile.fixedWorkingTime) {
				timeAtWork = Math.max(tempoLavoro, getWorkingTimeTypeDay().workingTime);
			} else {
				timeAtWork = tempoLavoro;	
			}

			save();				
		}

	}


	/**
	 * calcola il valore del progressivo giornaliero e lo salva sul db
	 */
	private void updateProgressive(){
		//merge();
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
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
			save();
		}

	}

	/**
	 * chiama le funzioni di popolamento
	 */
	public void populatePersonDay(){
		//merge();
		//PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date).first();
		Contract con = person.getContract(date);
		//Se la persona non ha un contratto attivo non si fanno calcoli per quel giorno
		//Le timbrature vengono comunque mantenute
		if (con != null) {
			if(stampings.size() != 0 && absences.size() != 0){
				Logger.debug("Ci sono sia timbrature che assenze, verifico che le assenze siano giornaliere e non orarie così da" +
						"evitare di fare i calcoli per questo giorno.");
				for(Absence abs : absences){
					if(abs.absenceType.ignoreStamping == true || abs.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay){
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

					else{
						if(!abs.absenceType.code.equals("89")){
							timeAtWork = timeAtWork + abs.absenceType.justifiedTimeAtWork.minutesJustified;

						}
						else{
							int total = 150*60;
							List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
									person, date.monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue(), date.minusDays(1)).fetch();
							for(PersonDay pdy : pdList){
								if(pdy.absences.size() > 0){
									for(Absence absn : pdy.absences){
										if(absn.absenceType.code.contains("89h")){
											total = total - absn.absenceType.justifiedTimeAtWork.minutesJustified;
										}
									}
								}
							}
							timeAtWork = timeAtWork + total;
						}
						merge();
						updateDifference();
						merge();
						updateProgressive();
						merge();
						setTicketAvailable();
						merge();
						return;
					}

				}
			}
			if(timeAtWork != null && (stampings.size() == 0 || stampings == null)){
				updateDifference();
				merge();
				updateProgressive();	
				merge();
				setTicketAvailable();
				merge();
				return;
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

	public StampProfile getStampProfile() {
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

	/**
	 * 
	 * @return il personday precedente a quello attualmente considerato. Questo metodo è utilizzato nella visualizzazione delle timbrature
	 * sia in versione utente che amministratore, quando si deve visualizzare il progressivo nei giorni festivi (che altrimenti risulterebbe
	 * sempre uguale a zero)
	 */
	public PersonDay checkPreviousProgressive(){
		PersonDay pd = null;
		if(date.getDayOfMonth() != 1){
			pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date.minusDays(1)).first();
			if(pd != null)
				return pd;
			else{
				pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, date.minusDays(2)).first();
				return pd;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return String.format("PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, modificationType = %s, progressive = %s, timeAtWork = %s",
				id, person.id, date, difference, isTicketAvailable, modificationType, progressive, timeAtWork);
	}

}
