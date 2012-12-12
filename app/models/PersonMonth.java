package models;

import it.cnr.iit.epas.PersonUtility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.JPA;
import play.db.jpa.Model;

import lombok.Data;
import models.Stamping.WayType;

@Audited
@Table(name="person_months")
@Entity
public class PersonMonth extends Model {

	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;	

	@Column
	public Integer year;
	@Column
	public Integer month;	

	/**
	 * Minuti derivanti dalla somma dei progressivi giornalieri del mese 
	 */
	@Column
	public Integer progressiveAtEndOfMonthInMinutes = 0;

	/**
	 * Totale residuo minuti alla fine del mese
	 * 
	 * Per i livelli I - III, deriva da:
	 * 
	 *  progressiveAtEndOfMonthInMinutes + 
	 *  residuo mese precedente (che non si azzera all'inizio dell'anno) -
	 *  recuperi (codice 91)
	 * 
	 *  
	 *  Per i livelli IV - IX, deriva da:
	 *  
	 *  progressiveAtEndOfMonthInMinutes +
	 *  residuo anno precedente (totale anno precedente se ancora utilizzabile) -
	 *  remainingMinutePastYearTaken -
	 *  totale remainingMinutePastYearTaken dei mesi precedenti a questo +
	 *  residuo mese precedente -
	 *  recuperi (91) -
	 *  straordinari (notturni, diurni, etc)  
	 */
	@Column(name = "total_remaining_minutes")
	public Integer totalRemainingMinutes = 0;

	/**
	 * Minuti di tempo residuo dell'anno passato utilizzati questo
	 * mese (come riposo compensativo o come ore in negativo)
	 */
	@Column(name = "remaining_minute_past_year_taken")
	public Integer remainingMinutesPastYearTaken = 0;

	@Column(name = "compensatory_rest_in_minutes")
	public Integer compensatoryRestInMinutes = 0;

	@Transient
	public List<PersonMonth> persons = null;

	@Transient
	public List<PersonDay> days = null;
	
	@Transient
	private Map<AbsenceType, Integer> absenceCodeMap;

	@Transient
	private List<StampModificationType> stampingCodeList = new ArrayList<StampModificationType>();

	/**
	 * aggiunta la date per test di getMaximumCoupleOfStampings ---da eliminare
	 * @param person
	 * @param year
	 * @param month
	 */
	public PersonMonth(Person person, int year, int month){
		this.person = person;	
		this.year = year;
		this.month = month;

	}



	/**
	 * 
	 * @param month, year
	 * @return il residuo di ore all'ultimo giorno del mese se visualizzo un mese passato, al giorno attuale se visualizzo il mese
	 * attuale
	 */
	public int getMonthResidual(){
		int residual = 0;
		LocalDate date = new LocalDate();

		if(month == date.getMonthOfYear() && year == date.getYear()){
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date < ? and pd.progressive != ? " +
					"order by pd.date desc", person, date, 0).first();
			if(pd == null){
				pd = new PersonDay(person, date.minusDays(1));
			}
			residual = pd.progressive;
		}
		else{
			LocalDate hotDate = new LocalDate(year,month,1).dayOfMonth().withMaximumValue();
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, hotDate).first();
			residual = pd.progressive;
		}
		return residual;
	}

	/**
	 * 
	 * @param month
	 * @param year
	 * @return il numero di giorni di riposo compensativo utilizzati in quel mese 
	 */
	public int getCompensatoryRestInMinutes(){
		if (compensatoryRestInMinutes != null) {
			return compensatoryRestInMinutes;
		}

		compensatoryRestInMinutes = 0;
		LocalDate beginMonth = new LocalDate(year, month, 1);

		return ((Long) Absence.find("Select count(abs) from Absence abs where abs.person = ? and abs.date between ? and ? and abs.absenceType.code = ?", 
				person, beginMonth, beginMonth.dayOfMonth().withMaximumValue(), "91").first()).intValue();

	}

	/**
	 * 
	 * @param month
	 * @param year
	 * @return il totale derivante dalla differenza tra le ore residue e le eventuali ore di riposo compensativo
	 */
	public int getTotalOfMonth(){
		int total = 0;
		int compensatoryRest = getCompensatoryRestInMinutes();
		int monthResidual = getMonthResidual();
		LocalDate date = new LocalDate(year, month, 1);
		int residualFromPastMonth = PersonUtility.getResidual(person, date.dayOfMonth().withMaximumValue());
		total = residualFromPastMonth+monthResidual-(compensatoryRest*432); //numero di giorni di riposo compensativo moltiplicati 
		//per il numero di minuti presenti in 7 ore e 12 minuti, ovvero il tempo di lavoro.

		return total;
	}

	/**
	 * 
	 * @return il numero massimo di coppie di colonne ingresso/uscita ricavato dal numero di timbrature di ingresso e di uscita di quella
	 * persona per quel mese
	 */
	public long getMaximumCoupleOfStampings(){
		//EntityManager em = em();
		LocalDate begin = new LocalDate(year, month, 1);

		List<PersonDay> personDayList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, begin, begin.dayOfMonth().withMaximumValue()).fetch();
		int maxExitStamp = 0;
		int maxInStamp = 0;
		for(PersonDay pd : personDayList){
			int localMaxExitStamp = 0;
			int localMaxInStamp = 0;
			for(Stamping st :pd.stampings){
				if(st.way == WayType.out)
					localMaxExitStamp ++;
				if(st.way == WayType.in)
					localMaxInStamp ++;
			}
			if(localMaxExitStamp > maxExitStamp)
				maxExitStamp = localMaxExitStamp;			
			if(localMaxInStamp > maxInStamp)
				maxInStamp = localMaxInStamp;
		}
		return Math.max(maxExitStamp, maxInStamp);
//		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", params)
//		Query q1 = em.createNativeQuery("select count(*) from stampings as st where st.stamp_type_id in (:in1,:in2) and st.person_id = :per "+
//				"and st.date between :beg and :end group by cast(date as Date) order by count(*) desc")
//				.setParameter("in1", 1L)
//				.setParameter("in2", 4L)
//				.setParameter("per", person.id)
//				.setParameter("beg", begin.toDate())
//				.setParameter("end", begin.dayOfMonth().withMaximumValue().toDate())
//				.setMaxResults(1);
//
//		BigInteger exitStamp = (BigInteger)q1.getSingleResult();
//
//
//		q1.setParameter("in1", 2L).setParameter("in2", 3L);
//		BigInteger inStamp = (BigInteger)q1.getSingleResult();
//		return Math.max(exitStamp.longValue(),inStamp.longValue());
	}



	/**
	 * @return la lista di giorni (PersonDay) associato alla persona nel mese di riferimento
	 */
	public List<PersonDay> getDays() {

		if (days != null) {
			return days;
		}
		days = new ArrayList<PersonDay>();
		Calendar firstDayOfMonth = GregorianCalendar.getInstance();
		//Nel calendar i mesi cominciano da zero
		firstDayOfMonth.set(year, month - 1, 1);

		Logger.trace(" %s-%s-%s : maximum day of month = %s", 
				year, month, 1, firstDayOfMonth.getMaximum(Calendar.DAY_OF_MONTH));

		for (int day = 1; day <= firstDayOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH); day++) {

			Logger.trace("generating PersonDay: person = %s, year = %d, month = %d, day = %d", person.username, year, month, day);
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", 
					person, new LocalDate(year, month, day)).first();
			if(pd == null)
				days.add(new PersonDay(person, new LocalDate(year, month, day), 0, 0, 0));
			else
				days.add(pd);
			Logger.debug("Inserito in days il person day: %s", pd);
		}
		return days;
	}	

	/**
	 * 
	 * !!!!IMPORTANTE!!!! QUANDO SI PASSA A UN NUOVO CONTRATTO NELL'ARCO DI UN MESE, DEVO RICORDARMI DI AZZERARE I RESIDUI DELLE ORE PERCHÈ
	 * NON SONO CUMULABILI CON QUELLE CHE EVENTUALMENTE AVRÒ COL NUOVO CONTRATTO
	 * 
	 * 
	 */

	/**
	 * Aggiorna le variabili d'istanza in funzione dei valori presenti sul db.
	 * Non effettua il salvataggio sul database.
	 */
	public void refreshPersonMonth(){

		Configuration config = Configuration.getCurrentConfiguration();
		
		LocalDate date = new LocalDate(year, month, 1);
		PersonDay lastPersonDayOfMonth = 
				PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date BETWEEN ? AND ? ORDER BY pd.date DESC",
						person, date.dayOfMonth().withMinimumValue(), date.dayOfMonth().withMaximumValue()).first();

		Logger.trace("%s, lastPersonDayOfMonth = %s", toString(), lastPersonDayOfMonth);
		
		progressiveAtEndOfMonthInMinutes = lastPersonDayOfMonth.progressive;  

		LocalDate startOfMonth = new LocalDate(year, month, 1);

		List<Absence> compensatoryRestAbsences = JPA.em().createQuery(
				"SELECT a FROM Absence a JOIN a.personDay pd WHERE a.absenceType.compensatoryRest IS TRUE AND pd.date BETWEEN :startOfMonth AND :endOfMonth AND pd.person = :person")
				.setParameter("startOfMonth", startOfMonth)
				.setParameter("endOfMonth", startOfMonth.dayOfMonth().withMaximumValue())
				.setParameter("person", person)
				.getResultList();

		compensatoryRestInMinutes = 0;

		for (Absence absence : compensatoryRestAbsences) {
			switch (absence.absenceType.justifiedTimeAtWork) {
			case AllDay:
				compensatoryRestInMinutes += absence.personDay.getWorkingTimeTypeDay().workingTime;
				break;
			case HalfDay:
				compensatoryRestInMinutes += absence.personDay.getWorkingTimeTypeDay().workingTime / 2;
				break;
			case ReduceWorkingTimeOfTwoHours:
				throw new IllegalStateException("Il tipo di assenza ReduceWorkingTimeOfTwoHours e' stato impostato come compensatoryRest (Riposo compensativo) ma questo non e' corretto.");
			case TimeToComplete:
				if (absence.personDay.timeAtWork < absence.personDay.getWorkingTimeTypeDay().workingTime) {
					compensatoryRestInMinutes += (absence.personDay.getWorkingTimeTypeDay().workingTime - absence.personDay.timeAtWork);
				}
				break;
			case Nothing:
				//Non c'è riposo compensativo da aggiungere al calcolo
				break;
			default:
				compensatoryRestInMinutes += absence.absenceType.justifiedTimeAtWork.minutesJustified; 
			}
		}
		
		Logger.trace("%s compensatoryRestInMinutes = %s", toString(), compensatoryRestInMinutes);
		
		//TODO: aggiungere eventuali riposi compensativi derivanti da inizializzazioni 
		InitializationAbsence initAbsence = new InitializationAbsence();
		int recoveryDays = initAbsence.recoveryDays;


		PersonMonth previousPersonMonth = PersonMonth.find("byPersonAndYearAndMonth", person, startOfMonth.minusMonths(1).getYear(), startOfMonth.minusMonths(1).getMonthOfYear()).first();
		Logger.trace("%s, previousPersonMonth = %s", toString(), previousPersonMonth);
		
		int totalRemainingMinutesPreviousMonth = previousPersonMonth == null ? 0 : previousPersonMonth.totalRemainingMinutes;

		if (person.qualification.qualification <= 3) {
			/**
			 * si vanno a guardare i residui recuperati dall'anno precedente e si controlla che esista per quella persona sia l'initTime che
			 * il campo dei minuti residui valorizzato. In tal caso vengono aggiunti al totalRemainingMinutes
			 */
			InitializationTime initTime = InitializationTime.find("Select initTime from InitializationTime initTime where initTime.person = ? " +
					"and initTime.date = ?", person, new LocalDate(year-1,12,31)).first();
			if(initTime != null && initTime.residualMinutes > 0)
				totalRemainingMinutes = initTime.residualMinutes + progressiveAtEndOfMonthInMinutes + totalRemainingMinutesPreviousMonth - compensatoryRestInMinutes;
			else
				totalRemainingMinutes = progressiveAtEndOfMonthInMinutes + totalRemainingMinutesPreviousMonth - compensatoryRestInMinutes;
		}
		else{
			PersonYear py = PersonYear.find("byPersonAndYear", person, year-1).first();
			//Se personYyear anno precedente non devo fare niente e totalRemainingMinutePastYearTaken = 0
			
			totalRemainingMinutes = progressiveAtEndOfMonthInMinutes + totalRemainingMinutesPreviousMonth - compensatoryRestInMinutes;
			
			/**
			 * TODO: c'è il caso di persone che hanno terminato il rapporto di lavoro in una certa data e che ritornano a lavoro a causa del 
			 * badge ancora attivo in date successive alla fine del rapporto di lavoro (vedi Fabrizio Leonardi che va in pensione il 31/12/2010
			 * e torna a lavoro timbrando col badge 3 volte nel 2011. Secondo la nostra idea di personMonth e personYear questi sono casi 
			 * spinosi poichè non esistono i personMonth pregressi per fare i calcoli sui residui
			 */
			if (py != null) {
				if(month < config.monthExpireRecoveryDaysFourNine){
					int totalRemainingMinutePastYearTaken = 0;
					
					for(int i = 1; i < month; i++){
						PersonMonth pm = PersonMonth.find("byPersonAndYearAndMonth", person, year, i).first();
						totalRemainingMinutePastYearTaken += pm.remainingMinutesPastYearTaken;						
					}
					
					int remainingMinutesResidualLastYear =  py.remainingMinutes - totalRemainingMinutePastYearTaken;
					if (remainingMinutesResidualLastYear < 0) {
						throw new IllegalStateException(
							String.format("Il valore dei minuti residui dell'anno precedente per %s nel mese %s %s e' %s. " +
								"Non ci dovrebbero essere valori negativi per le ore residue dell'anno precedente", person, year, month, remainingMinutesResidualLastYear));
					}
					
					if (compensatoryRestInMinutes > 0 && remainingMinutesResidualLastYear > 0) {
						remainingMinutesPastYearTaken = Math.min(compensatoryRestInMinutes, remainingMinutesResidualLastYear);
					}
					
					//Se non sono nell'ultimo mese in cui sono valide le ore residue dell'anno passato allora mi porto dietro
					// le ore residue che non ho ancora preso
					if (month < (config.monthExpireRecoveryDaysFourNine - 1)) {
						totalRemainingMinutes += remainingMinutesResidualLastYear - remainingMinutesPastYearTaken;
					}

				}
			}
		}
		
		save();
	}
	
	/**
	 * 
	 * @return il numero di buoni pasto usabili per quel mese
	 */
	public int numberOfMealTicketToUse(){
		int tickets=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			if(pd.mealTicket()==true)
				tickets++;
		}

		return tickets;
	}

	/**
	 * 
	 * @return il numero di buoni pasto da restituire per quel mese
	 */
	public int numberOfMealTicketToRender(){
		int ticketsToRender=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			if(pd.mealTicket()==false && (pd.isHoliday()==false))
				ticketsToRender++;
		}

		return ticketsToRender;
	}

	/**
	 * 
	 * @return il numero di giorni lavorati in sede. Per stabilirlo si controlla che per ogni giorno lavorativo, esista almeno una 
	 * timbratura.
	 */
	public int basedWorkingDays(){
		int basedDays=0;
		if(days==null){
			days= getDays();
		}
		for(PersonDay pd : days){
			List<Stamping> stamp = pd.stampings;
			if(stamp.size()>0 && pd.isHoliday()==false)
				basedDays++;
		}
		return basedDays;
	}
	
	/**
	 * 
	 * @param days lista di PersonDay
	 * @return la lista contenente le assenze fatte nell'arco di tempo dalla persona
	 */

	public Map<AbsenceType,Integer> getAbsenceCode(){

		if(days == null){
			days = getDays();
		}
		absenceCodeMap = new HashMap<AbsenceType, Integer>();
		if(absenceCodeMap.isEmpty()){
			int i = 0;
			for(PersonDay pd : days){
				for (Absence absence : pd.absences) {
					AbsenceType absenceType = absence.absenceType;
					if(absenceType != null){
						boolean stato = absenceCodeMap.containsKey(absenceType);
						if(stato==false){
							i=1;
							absenceCodeMap.put(absenceType,i);            	 
						} else{
							i = absenceCodeMap.get(absenceType);
							absenceCodeMap.remove(absenceType);
							absenceCodeMap.put(absenceType, i+1);
						}
					}            
				}	 
			}       
		}

		return absenceCodeMap;	

	}


	public Map<AbsenceType, Integer> getAbsenceCodeMap() {
		return absenceCodeMap;
	}

	/**
	 * 
	 * @param days
	 * @return lista dei codici delle timbrature nel caso in cui ci siano particolarità sulle timbrature dovute a mancate timbrature
	 * per pausa mensa ecc ecc...
	 */
	public List<StampModificationType> getStampingCode(){
		if(days==null){
			days= getDays();
		}
		List<StampModificationType> stampCodeList = new ArrayList<StampModificationType>();
		for(PersonDay pd : days){
			
			StampModificationType smt = pd.checkTimeForLunch();
			Logger.debug("Lo stamp modification type è: %s", smt);
			
			if(smt != null && !stampCodeList.contains(smt)){
				Logger.debug("Aggiunto %s alla lista", smt.description);
				stampCodeList.add(smt);
			}
			StampModificationType smtMarked = pd.checkMarkedByAdmin();
			if(smtMarked != null && !stampCodeList.contains(smtMarked)){
				stampCodeList.add(smtMarked);
				Logger.debug("Aggiunto %s alla lista", smtMarked.description);
			}

		}
		Logger.debug("La lista degli stamping code per questo mese contiene: %s", stampingCodeList);
		return stampCodeList;
	}
	
	
	
	
	@Override
	public String toString() {
		return String.format("PersonMonth[%d] - person.id = %d, year = %s, month = %d, totalRemainingMinutes = %d, " +
			"progressiveAtEndOfMonthInMinutes = %d, compensatoryRestInMinutes = %d, remainingMinutesPastYearTakes = %d",
			id, person.id, year, month, totalRemainingMinutes, progressiveAtEndOfMonthInMinutes, compensatoryRestInMinutes, remainingMinutesPastYearTaken);
	}
}
