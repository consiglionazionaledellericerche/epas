package models;

import it.cnr.iit.epas.PersonUtility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.Transient;

import models.Stamping.WayType;

import org.hibernate.envers.Audited;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.data.validation.Valid;
import play.db.jpa.JPA;
import play.db.jpa.JPAPlugin;
import play.db.jpa.Model;

/**
 * @author cristian
 *
 */
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

	@Transient
	private PersonMonth mesePrecedenteCache;

	@Column(name = "recuperi_ore_da_anno_precedente")
	public int recuperiOreDaAnnoPrecedente;

	@Column(name = "riposi_compensativi_da_anno_precedente")
	public int riposiCompensativiDaAnnoPrecedente;

	@Column(name = "riposi_compensativi_da_anno_corrente")
	public int riposiCompensativiDaAnnoCorrente;
	
	@Column(name = "riposi_compensativi_da_inizializzazione")
	public int riposiCompensativiDaInizializzazione;
	

	@Column
	public int straordinari;

	@Transient
	public InitializationTime initializationTime;

	/**
	 * Minuti derivanti dalla somma dei progressivi giornalieri del mese 
	 */
	@Column
	@Deprecated
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
	@Deprecated
	public Integer totalRemainingMinutes = 0;

	/**
	 * Minuti di tempo residuo dell'anno passato utilizzati questo
	 * mese (come riposo compensativo o come ore in negativo)
	 */
	@Column(name = "remaining_minute_past_year_taken")
	public Integer remainingMinutesPastYearTaken = 0;

	@Column(name = "compensatory_rest_in_minutes")
	public Integer compensatoryRestInMinutes = 0;

	@Column(name = "residual_past_year")
	public Integer residualPastYear = 0;

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

	public static PersonMonth byPersonAndYearAndMonth(Person person, int year, int month) {
		return PersonMonth.find(
				"Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
				person, month, year).first();
	}

	/**
	 * 
	 * @param month, year
	 * @return il residuo di ore all'ultimo giorno del mese se visualizzo un mese passato, al giorno attuale se visualizzo il mese
	 * attuale, ovvero il progressivo orario all'ultimo giorno del mese (se passato) o al giorno attuale (se il mese è quello attuale)
	 */
	@Deprecated
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
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date <= ? and pd.date > ?" +
					" order by pd.date desc", person, hotDate, hotDate.dayOfMonth().withMinimumValue()).first();
			if(pd == null){
				/**
				 * si sta cercando il personDay di una data ancora non realizzata (ad esempio il personDay dell'ultimo giorno di un mese ancora da 
				 * completare...es.: siamo al 4 gennaio 2013 e si cerca il personDay del 31 gennaio, che ancora non è stato realizzato
				 */
				residual = 0;
			}
			else
				residual = pd.progressive;

		}

		return residual;
	}

	/**
	 * 
	 * @return il numero di giorni di riposo compensativo nel mese
	 */
	@Deprecated
	public int getCompensatoryRest(){
//		int compensatoryRest = 0;
		LocalDate beginMonth = new LocalDate(year, month, 1);
		Query query = JPA.em().createQuery("Select abs from Absence abs where abs.personDay.person = :person and abs.personDay.date between " +
				":begin and :end and abs.absenceType.code = :code");
		query.setParameter("person", person)
		.setParameter("begin", beginMonth)
		.setParameter("end", beginMonth.dayOfMonth().withMaximumValue())
		.setParameter("code", "91");
		List<Absence> absList = query.getResultList();
//		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
//				person, beginMonth, beginMonth.dayOfMonth().withMaximumValue()).fetch();
//		for(PersonDay pd : pdList){
//			if(pd.absences.size() > 0){
//				for(Absence abs : pd.absences){
//					if(abs.absenceType.code.equals("91"))
//						compensatoryRest = compensatoryRest +1;
//				}
//			}
//		}
		return absList.size();
	}
	
	/**
	 * 
	 * @param 
	 * @return il numero di riposi compensativi per quella persona utilizzati fino ad oggi dall'inizio dell'anno
	 */
	public int numberOfCompensatoryRestUntilToday(){
		
		Query query = JPA.em().createQuery("Select abs from Absence abs where abs.personDay.person = :person and abs.absenceType.code = :code " +
				"and abs.personDay.date between :begin and :end");
		query.setParameter("person", this.person)
		.setParameter("code", "91")
		.setParameter("begin", new LocalDate(year,1,1))
		.setParameter("end", new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());
		
		return query.getResultList().size();
	}

	/**
	 * 
	 * @param numberOfCompensatoryRest
	 * @return il numero di minuti corrispondenti al numero di riposi compensativi
	 */
	public int getCompensatoryRestInMinutes(){
		LocalDate begin = new LocalDate(year, month, 1);
		LocalDate end = new LocalDate(year, month, begin.dayOfMonth().withMaximumValue().getDayOfMonth());
		Query query = JPA.em().createQuery("Select abs from Absence abs where abs.personDay.person = :person and abs.personDay.date between :begin and :end" +
				" and abs.absenceType.code = :code").setParameter("person", this.person).setParameter("begin", begin).setParameter("end", end)
				.setParameter("code", "91");
		List<Object> resultList = query.getResultList();
		return resultList.size() * person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(1).workingTime;
	}
	/**
	 * 
	 * @return il numero massimo di coppie di colonne ingresso/uscita ricavato dal numero di timbrature di ingresso e di uscita di quella
	 * persona per quel mese
	 */
	public long getMaximumCoupleOfStampings(){
		
		LocalDate begin = new LocalDate(year, month, 1);
		if(begin.isAfter(new LocalDate()))
			return 0;
		List<PersonDay> pdList = PersonDay.find("Select pd From PersonDay pd where pd.person = ? and pd.date between ? and ?", this.person,begin,begin.dayOfMonth().withMaximumValue() ).fetch();

		long max = 0;
		for(PersonDay pd : pdList)
		{
			int coupleOfStampings = PersonUtility.numberOfInOutInPersonDay(pd);
			
			if(max<coupleOfStampings)
				max = coupleOfStampings;
		}
		
		return max;
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
			Logger.trace("Lo stamp modification type è: %s", smt);

			if(smt != null && !stampCodeList.contains(smt)){
				Logger.debug("Aggiunto %s alla lista", smt.description);
				stampCodeList.add(smt);
			}
			StampModificationType smtMarked = pd.checkMarkedByAdmin();
			if(smtMarked != null && !stampCodeList.contains(smtMarked)){
				stampCodeList.add(smtMarked);
				Logger.trace("Aggiunto %s alla lista", smtMarked.description);
			}
			StampModificationType smtMidnight = pd.checkMissingExitStampBeforeMidnight();
			if(smtMidnight != null && !stampCodeList.contains(smtMidnight)){
				stampCodeList.add(smtMidnight);
				Logger.trace("Aggiunto %s alla lista", smtMidnight.description);
			}
			StampModificationType smtFixedWorkingTime = pd.getFixedWorkingTime();
			if(smtFixedWorkingTime != null && !stampCodeList.contains(smtFixedWorkingTime)){
				stampCodeList.add(smtFixedWorkingTime);
				Logger.trace("Aggiunto %s alla lista", smtFixedWorkingTime.description);
			}

		}
		Logger.debug("La lista degli stamping code per questo mese contiene: %s", stampingCodeList);
		return stampCodeList;
	}
	
	/**
	 * 
	 * @return la lista di eventuali stampType presenti nelle timbrature (es.: timbrature per ingresso/uscita di servizio
	 */
	public List<StampType> getStampType(){
		if(days==null){
			days= getDays();
		}
		List<StampType> stampTypeList = new ArrayList<StampType>();
		StampType type = StampType.find("Select st from StampType st where st.identifier = ?", "s").first();
		for(PersonDay pd : days){
			for(Stamping st : pd.stampings){
				
				if(st.stampType != null && !stampTypeList.contains(type)){
					stampTypeList.add(type);
				}
			}
		}
		return stampTypeList;
	}

	/**
	 * 
	 * @return il numero di riposi compensativi fatti dall'inizio dell'anno a quel momento
	 */
	public int getCompensatoryRestInYear(){
		LocalDate beginYear = new LocalDate(year, 1, 1);
		LocalDate now = new LocalDate();

		Query query = JPA.em().createQuery("Select abs from Absence abs where abs.personDay.person = :person and abs.personDay.date between " +
				":dateBegin and :dateEnd and abs.absenceType.code = :code");
		query.setParameter("person", person).setParameter("dateBegin", beginYear).setParameter("dateEnd", now).setParameter("code", "91");
		List<Absence> absList = query.getResultList();
		return absList.size();
	}

	/**
	 * 
	 * @return il numero di ore di straordinario fatte dall'inizio dell'anno
	 */
	public int getOvertimeHourInYear(LocalDate date){
		Logger.trace("Chiamata funzione di controllo straordinari...");
		int overtimeHour = 0;
		Query query = JPA.em().createQuery("Select comp from Competence comp where comp.person = :person and " +
				"comp.year = :year and comp.competenceCode.code = :code and comp.month = :month");
		query.setParameter("person", person)
		.setParameter("year", date.getYear())
		.setParameter("code", "S1")
		.setParameter("month", date.getMonthOfYear());
		
		List<Competence> compList = query.getResultList();
		
//		List<Competence> compList = Competence.find("Select comp from Competence comp, CompetenceCode code where comp.person = ? and comp.year = ? and " +
//				"comp.competenceCode = code and comp.month = ? and code.code = ?", person, date.getYear(), date.getMonthOfYear(),"S1").fetch();
		Logger.debug("La lista degli straordinari da inizio anno per %s: %s", person, compList);
		if(compList != null){
			for(Competence comp : compList){
				overtimeHour = overtimeHour + comp.valueApproved;
			}
		}
		Logger.debug("Il numero di ore di straordinari per %s è: %s", person, overtimeHour);
		return overtimeHour;
	}

	@Override
	public String toString() {
		//FIXME: da sistemare
		return String.format("PersonMonth[%d] - person.id = %d, year = %s, month = %d, totalRemainingMinutes = %d, " +
				"progressiveAtEndOfMonthInMinutes = %d, compensatoryRestInMinutes = %d, remainingMinutesPastYearTakes = %d",
				id, person.id, year, month, totalRemainingMinutes, progressiveAtEndOfMonthInMinutes, compensatoryRestInMinutes, remainingMinutesPastYearTaken);
	}

	public static PersonMonth build(Person person, int year, int month){
		PersonMonth pm = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.month = ? and pm.year = ?", 
				person, month, year).first();
		if(pm == null){
			pm = new PersonMonth(person, year, month);
			pm.create();
		}
		
		pm.aggiornaRiepiloghi();
		return pm;

	}

	public static PersonMonth getInstance(Person person, int year, int month) {
		PersonMonth personMonth = PersonMonth.find("Select pm from PersonMonth pm where pm.person = ? and pm.year = ? and pm.month = ?", person, year, month).first();
		if (personMonth == null) {
			personMonth = new PersonMonth(person, year, month);
		}
		return personMonth;
	}


	/**
	 * @return la somma dei minuti dei giorni (entro una certa data) che hanno una differenza negativa rispetto all'orario di lavoro
	 */
	public int residuoDelMeseInNegativoAllaData(LocalDate date) {
//		LocalDate startOfMonth = new LocalDate(year, month, 1);
//		Long residuo = JPA.em().createQuery("SELECT sum(pd.difference) FROM PersonDay pd WHERE pd.date BETWEEN :startOfMonth AND :endOfMonth and pd.person = :person " +
//				"AND pd.difference < 0 and pd.date <= :date", Long.class)
//				.setParameter("startOfMonth", startOfMonth)
//				.setParameter("endOfMonth", startOfMonth.dayOfMonth().withMaximumValue())
//				.setParameter("person", person)
//				.setParameter("date", date)
//				.getSingleResult();
		int res = 0;
//		if(residuo != null)
//			res = residuo.intValue();
//
//		return res;
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? " +
				"order by pd.date ", 
				this.person, date.dayOfMonth().withMinimumValue(), date.dayOfMonth().withMaximumValue()).fetch();
		if(pdList == null || pdList.size() == 0)
			return 0;
		for(PersonDay pd : pdList){
			if(pd.difference < 0 && pd.date.isBefore(new LocalDate()))
				res = res + pd.difference;
		}
//		if(pdList.get(0).date.isEqual(new LocalDate()))
//			return pdList.get(0).previousPersonDay().progressive;
//		else			
//			return pdList.get(0).progressive;
		return res;
		
	}

	/**
	 * @return la somma dei minuti dei giorni (alla fine del mese) che hanno una differenza negativa rispetto all'orario di lavoro
	 */
	public int residuoDelMeseInNegativo() {
		LocalDate startOfMonth = new LocalDate(year, month, 1);
		return residuoDelMeseInNegativoAllaData(startOfMonth.dayOfMonth().withMaximumValue());
	}

	/**
	 * @return la somma dei minuti dei giorni (entro una certa data) che hanno una differenza positiva rispetto all'orario di lavoro
	 */
	public int residuoDelMeseInPositivoAllaData(LocalDate date) {
//		Long residuo = JPA.em().createQuery("SELECT sum(pd.difference) FROM PersonDay pd WHERE pd.date BETWEEN :startOfMonth AND :endOfMonth and pd.person = :person " +
//				"AND pd.difference > 0 and pd.date <= :date", Long.class)
//				.setParameter("startOfMonth", new LocalDate(year, month, 1))
//				.setParameter("endOfMonth", (new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()))
//				.setParameter("person", person)
//				.setParameter("date", date)
//				.getSingleResult();
		//PersonDay pd = null;
		int res = 0;
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? " +
				" order by pd.date desc", 
				this.person, date.dayOfMonth().withMinimumValue(), date.dayOfMonth().withMaximumValue()).fetch();
		if(pdList == null || pdList.size() == 0)
			return 0;
		for(PersonDay pd : pdList){
			if(pd.difference >= 0 && pd.date.isBefore(new LocalDate()))
				res = res+ pd.difference;
		}
//		if(pdList.get(0).date.isEqual(new LocalDate()))
//			return pdList.get(0).previousPersonDay().progressive;

//		if(residuo != null)
//			res = residuo.intValue();
//		return res;
//		else
//			return pdList.get(0).progressive;
		return res;
	}

	/**
	 * @return la somma dei minuti dei giorni (alla fine del mese) che hanno una differenza positiva rispetto all'orario di lavoro
	 */
	public int residuoDelMeseInPositivo() {
		return residuoDelMeseInPositivoAllaData(new LocalDate(year, month, 1).dayOfMonth().withMaximumValue());
	}

	/**
	 * @return i minuti di lavoro residui dell'anno precedente (il fatto di poterli utilizzare o meno può dipendere dal tipo di contratto e dal mese corrente
	 */
	public int residuoAnnoPrecedente() {
		Contract contractLastYear = person.getContract(new LocalDate(year - 1, 12, 31));
		//		InitializationTime initTime = InitializationTime.find("Select init from InitializationTime init where init.person = ? and init.date = ?" +
		//				"", person, new LocalDate(year-1,12,31)).first();

		//Se il contratto della persona era attivo anche l'anno scorso si prende il personYear dell'anno precedente altrimenti il residuo dell'anno precedente
		if (contractLastYear != null && contractLastYear.equals(person.getCurrentContract())) {
			PersonYear personYear = PersonYear.find("SELECT py FROM PersonYear py WHERE py.year = ? AND py.person = ?", year - 1, person).first();
			if(personYear != null)
				return personYear.getRemainingMinutes();
			else{
				/**
				 * siccome è cambiata la tabella InitializationTime, non contentendo più il campo intero YEAR ma contenendo invece
				 * il campo date DATE, anche la find seguente è stata modificata facendo cercare l'eventuale inizializzazione 
				 * settata al primo gennaio dell'anno in corso
				 */
				InitializationTime initTime = InitializationTime.find("Select init from InitializationTime init where init.person = ? and init.date = ?", 
						person, new LocalDate(year, month,1).monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue()).first();
				if(initTime != null)
					return initTime.residualMinutesPastYear;
				return 0;
			}
		} else {
			return 0;
		}
	}

	/**
	 * @return il PersonMonth del mese precedente
	 */
	public PersonMonth mesePrecedente() {
		Contract currentContract = person.getContract(new LocalDate(year, month, 1));
		LocalDate fineMesePrecedente = (new LocalDate(year, month, 1)).minusMonths(1).dayOfMonth().withMaximumValue();

		if(currentContract == null)
			return null;
		if (currentContract.beginContract != null && fineMesePrecedente.isBefore(currentContract.beginContract)) {
			return null;
		}

		if (mesePrecedenteCache != null) {
			return mesePrecedenteCache;
		}
		LocalDate date = new LocalDate(year, month, 1);
		date = date.minusMonths(1);
		PersonMonth pm =PersonMonth.find("SELECT pm FROM PersonMonth pm WHERE pm.year = ? and pm.month = ? AND pm.person = ?", 
				date.getYear(), date.getMonthOfYear(), person).first(); 
		if(pm != null)
			return pm;
		else{
			//			PersonMonth perMon = new PersonMonth(person, year-1, 12);
			//			InitializationTime initTime = InitializationTime.find("Select init from InitializationTime init where init.person = ? and init.date = ?" +
			//					"", person, new LocalDate(year-1,12,31)).first();
			//			if(initTime != null){
			//				perMon.residualPastYear = initTime.residualMinutes;
			//				perMon.save();
			//			}
			//			else{
			//				perMon.residualPastYear = 0;
			//				perMon.save();
			//			}
			//			return perMon;
			return null;
		}

	}

	public boolean possibileUtilizzareResiduoAnnoPrecedente() {
		//Dipende dal livello.... e da
//		Qualification q = this.person.qualification;
//		if(q == null)
//			return false;
//		if(month <= 3)
//			return true;
//		if (q.qualification <= 3)
//			return true;
//		return false;
		
		Qualification qualification = Qualification.find("SELECT p.qualification FROM Person p WHERE p = ?", person).first(); 
		if(qualification == null)
			return false;

		return month <= 3 || qualification.qualification <= 3;

		//return month <= 3 || ;
	}
	

	/**
	 * @return la somma dei residui positivi e di quelli negativi
	 */
	public int residuoDelMese() {
		/**
		 * nel caso di persone con timbratura fissa, il residuo del mese è OBBLIGATORIAMENTE forzato a zero.
		 * Nel caso in cui ci fossero nuove disposizioni, intervenendo qui si può ovviare al comportamento attuale
		 */
		//FIXME: si fa una select di tutti i giorni del mese e poi si prende il primo valore?
		//Il fatto che per ogni personDay possa esserci l'orario di lavoro fissato, dovrebbe far si che 
		//per il personDay la difference sia zero e questo dovrebbe essere sufficiente ai calcoli successivi
		//Cosi come fatto adesso se il primo giorno non timbravo perdo tutti le differenze nei giorni precedenti.
		PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ? order by pd.date" , 
				this.person, new LocalDate(year, month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).first();
		if(pd != null){
			if(pd.isFixedTimeAtWork())
				return 0;
			else
				return residuoDelMeseInPositivo() + residuoDelMeseInNegativo();
		}
		return 0;
		
		
	}

	/**
	 * @return la somma dei residui positivi e di quelli negativi alla data specificata
	 */
	public int residuoDelMeseAllaData(LocalDate date) {
		return residuoDelMeseInPositivoAllaData(date) + residuoDelMeseInNegativoAllaData(date);
	}

	/**
	 * @return il tempo di lavoro dai mese precedenti eventualmente comprensivo di quello derivante
	 * 	dall'anno precedente
	 */
	public int totaleResiduoAnnoCorrenteAlMesePrecedente() {
		int residuo = 0;
		//Deve esistere un mese precedente ed essere dello stesso anno (quindi a gennaio il mese precedente di questo anno non esiste)
		if (mesePrecedente() != null && month != 1) {
			return residuo + mesePrecedente().totaleResiduoAnnoCorrenteAFineMese();
			//return mesePrecedente().totaleResiduoAnnoCorrenteAFineMese();
		}
		return 0;

	}

	public int residuoAnnoPrecedenteDisponibileAllInizioDelMese() {
		if (possibileUtilizzareResiduoAnnoPrecedente()) {
			LocalDate startOfMonth = new LocalDate(year, month, 1); 
			InitializationTime initTime = 
					InitializationTime.find("Select init from InitializationTime init where person = ? and date between ? and ?", 
						person, startOfMonth, new LocalDate(year, month, startOfMonth.dayOfMonth().getMaximumValue())).first();
			if(initTime != null)
				return initTime.residualMinutesPastYear;
			
			if (month.equals(1)) {
				return residuoAnnoPrecedente(); 
			} 
			if (mesePrecedente() == null) {
				return 0;
			}
			return mesePrecedente().residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		} 

		return 0;

	}

	public int residuoAnnoPrecedenteDisponibileAllaFineDelMese() {
		int residuoAnnoPrecedenteDisponibileAllInizioDelMese = residuoAnnoPrecedenteDisponibileAllInizioDelMese();
		//System.out.println("mese: " + month + ". residuoAnnoPrecedenteDisponibileAllInizioDelMese = " + residuoAnnoPrecedenteDisponibileAllInizioDelMese);

		int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllInizioDelMese + recuperiOreDaAnnoPrecedente + riposiCompensativiDaAnnoPrecedente;

		//System.out.println("mese: " + month + ". residuoAnnoPrecedenteDisponibileAllaFineDelMese() = " + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
		return residuoAnnoPrecedenteDisponibileAllaFineDelMese;
	}
	
	public int residuoDaInizializzazioneDisponibileAllaFineDelMese(){
		int residuoDaInizializzazioneDisponibileAllInizioDelMese = residuoDaInizializzazioneDisponibileAllInizioDelMese();
		int residuoDaInizializzazioneDisponibileALlaFineDelMese = residuoDaInizializzazioneDisponibileAllInizioDelMese + riposiCompensativiDaInizializzazione;
		return residuoDaInizializzazioneDisponibileALlaFineDelMese;
	}

	private int residuoDaInizializzazioneDisponibileAllInizioDelMese() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int totaleResiduoAnnoCorrenteAFineMese() {
		int residuoMese = residuoDelMese();
		int totaleResiduoAnnoCorrenteAlMesePrecedente = totaleResiduoAnnoCorrenteAlMesePrecedente();
		return residuoMese + totaleResiduoAnnoCorrenteAlMesePrecedente + riposiCompensativiDaAnnoCorrente - straordinari - recuperiOreDaAnnoPrecedente;  
	}

	public int totaleResiduoAnnoCorrenteAllaData(LocalDate date) {
		int residuoDelMeseAllaData = residuoDelMeseAllaData(date);
		int totaleResiduoAnnoCorrenteAlMesePrecedente = totaleResiduoAnnoCorrenteAlMesePrecedente();
		int residuoAnnoPrecedenteDaInizializzazione = residuoAnnoPrecedenteDaInizializzazione();
		return residuoDelMeseAllaData + totaleResiduoAnnoCorrenteAlMesePrecedente + residuoAnnoPrecedenteDaInizializzazione + riposiCompensativiDaAnnoCorrente - straordinari - recuperiOreDaAnnoPrecedente;  
	}

	public int totaleResiduoAnnoCorrenteAFineMesePiuResiduoAnnoPrecedenteDisponibileAFineMese() {
		int totaleResiduoAnnoCorrenteAFineMese = totaleResiduoAnnoCorrenteAFineMese();
		int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		return totaleResiduoAnnoCorrenteAFineMese + residuoAnnoPrecedenteDisponibileAllaFineDelMese;
	}

	
	public int totaleRecuperiDaAnnoPrecedente() {
		return recuperiOreDaAnnoPrecedente + riposiCompensativiDaAnnoPrecedente;
	}
	
	public void aggiornaRiepiloghi() {
		Logger.debug("Aggiornamento dei riepiloghi del mese %s per %s", month, person);

		int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();

		int residuoDelMeseInNegativo = residuoDelMeseInNegativo();
		int residuoDelMeseInPositivo = residuoDelMeseInPositivo();

		if (residuoDelMeseInNegativo != 0 && residuoAnnoPrecedenteDisponibileAllaFineDelMese > 0) {

			Logger.debug("mese = %s. Residuo del mese in negativo (%s) != 0 e residuoAnnoPrecedenteDisponibileAllaFineDelMese (%s) > 0, recupero dall'anno scorso il recuperabile",
					month , residuoDelMeseInNegativo, residuoAnnoPrecedenteDisponibileAllaFineDelMese);

			if (residuoAnnoPrecedenteDisponibileAllaFineDelMese > -residuoDelMeseInNegativo) {
				Logger.debug("mese = %s. residuoAnnoPrecedenteDisponibileAllaFineDelMese > del residuo del mese in negativo, aumento i recuperiOreDaAnnoPrecedente (adesso %s) di %s minuti",
						month, recuperiOreDaAnnoPrecedente, residuoDelMeseInNegativo);

				recuperiOreDaAnnoPrecedente += residuoDelMeseInNegativo;

				Logger.debug("mese = %s. recuperiOreDaAnnoPrecedente = %s minuti", month, recuperiOreDaAnnoPrecedente);
			} else {
				recuperiOreDaAnnoPrecedente -= residuoAnnoPrecedenteDisponibileAllaFineDelMese;
			}
			this.save();
		}

		if (residuoDelMeseInPositivo != 0 && residuoAnnoPrecedenteDisponibileAllaFineDelMese < 0) {
			if (residuoDelMeseInPositivo > -residuoAnnoPrecedenteDisponibileAllaFineDelMese) {
				recuperiOreDaAnnoPrecedente -= residuoAnnoPrecedenteDisponibileAllaFineDelMese;
			} else {
				recuperiOreDaAnnoPrecedente += residuoDelMeseInPositivo;
			}
			this.save();
		}
		this.save();
	}

	public int tempoDisponibilePerRecuperi(LocalDate date) {
		int totaleResiduoAnnoCorrenteAllaData = totaleResiduoAnnoCorrenteAllaData(date);

		//System.out.println("totaleResiduoAnnoCorrenteAllaData = " + totaleResiduoAnnoCorrenteAllaData);

		int tempoDisponibile = totaleResiduoAnnoCorrenteAllaData + residuoAnnoPrecedenteDisponibileAllaFineDelMese();

		if (tempoDisponibile <= 0) {
			tempoDisponibile = 0;
		}
		//System.out.println("Data = " + date + ". Tempo disponibile per recuperi = " + tempoDisponibile);
		return tempoDisponibile;

	}	

	public int tempoDisponibilePerStraordinari() {

		int residuoDelMeseInPositivo = residuoDelMeseInPositivo();

		if (residuoDelMeseInPositivo <= 0) {
			return 0;
		}

		int residuoAllaDataRichiesta = residuoDelMese();

		int tempoDisponibile = 0;
		if(mesePrecedente() != null)
			tempoDisponibile = residuoAnnoPrecedenteDisponibileAllaFineDelMese() + mesePrecedente().totaleResiduoAnnoCorrenteAFineMese() + residuoAllaDataRichiesta;
		else
			tempoDisponibile = residuoAnnoPrecedenteDisponibileAllaFineDelMese() + residuoAnnoPrecedenteDaInizializzazione() + residuoAllaDataRichiesta;

		if (tempoDisponibile <= 0) {
			return 0;
		}

		return Math.min(residuoDelMeseInPositivo, tempoDisponibile);

	}

	public boolean assegnaStraordinari(int ore) {
		if (tempoDisponibilePerStraordinari() > ore * 60) {
			straordinari = ore * 60;
			this.save();
			return true;
		}
		return false;
	}
	
	/**
	 * Aggiorna il campo straordinari. TODO da inserire nel job??
	 */
	public void updateOvertimesFromCompetences()
	{
		
		Competence comp = Competence.find("Select comp from Competence comp where comp.person = ? and comp.year = ? and comp.month = ? and comp.competenceCode.code = ?",
				this.person,
				this.year,
				this.month,
				"S1").first();
		if(comp!=null && comp.competenceCode.code.equals("S1"))
			this.straordinari = comp.valueApproved;
		
		this.save();
		
	}

	public boolean prendiRiposoCompensativo(LocalDate date) {
		
		int minutiRiposoCompensativo = minutiRiposoCompensativo(date);
		
		if (-minutiRiposoCompensativo > tempoDisponibilePerRecuperi(date)) {
			return false;
		}

		int residuoAnnoPrecedenteDisponibileAllaFineDelMese = residuoAnnoPrecedenteDisponibileAllaFineDelMese();
		
		if (residuoAnnoPrecedenteDisponibileAllaFineDelMese < 0) {
			throw new IllegalStateException(
					String.format("Richiesto riposo compensativo per l'utente %s nella data %s: ci sono ore disponibili " +
							"ma il residuo dell'anno scorso è negativo, questo non dovrebbe essere possibile, contattare Dario <dario.tagliaferri@iit.cnr.it>",
							person, date));
		}
		

		//System.out.println("residuoAnnoPrecedenteDisponibileAllaFineDelMese = " + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
		if (residuoAnnoPrecedenteDisponibileAllaFineDelMese == 0) {
			//Per esempio per i tecnici/amministrativi da aprile in poi
			Logger.debug("Passo di qui per %s %s nel mese %s perchè non ha residuo positivo dall'anno precedente!!!", person.name, person.surname, month);
			riposiCompensativiDaAnnoCorrente += minutiRiposoCompensativo;
		} else {
			if (residuoAnnoPrecedenteDisponibileAllaFineDelMese > -minutiRiposoCompensativo ) {
				riposiCompensativiDaAnnoPrecedente += minutiRiposoCompensativo;
				
			} else {
				riposiCompensativiDaAnnoPrecedente += residuoAnnoPrecedenteDisponibileAllaFineDelMese;
				riposiCompensativiDaAnnoCorrente += (minutiRiposoCompensativo + residuoAnnoPrecedenteDisponibileAllaFineDelMese);
			}				
		}
		save();
		//Creare l'assenza etc....
		aggiornaRiepiloghi();
		
		return true;
	}

	public int residuoAnnoCorrenteDaInizializzazione() {
		if(possibileUtilizzareResiduoAnnoPrecedente()){
			initializationTime = InitializationTime.find("Select i from InitializationTime i where i.person = ?" , person).first();
			if (initializationTime != null && (initializationTime.date.isBefore(new LocalDate(year, month, 1).dayOfMonth().withMaximumValue())) &&
					initializationTime.date.getYear() == year.intValue() && initializationTime.date.getMonthOfYear() == month.intValue()) {
				return initializationTime.residualMinutesCurrentYear != null ? initializationTime.residualMinutesCurrentYear : 0;
			}
		}

		return 0;
	}

	public int residuoAnnoPrecedenteDaInizializzazione() {

//		Query query = JPA.em().createQuery("Select i from InitializationTime i where i.person = :person");
//		query.setParameter("person", person);
//		initializationTime = (InitializationTime) query.getSingleResult();
		initializationTime = InitializationTime.find("Select i from InitializationTime i where i.person = ?" , person).first();
		if (initializationTime != null && (initializationTime.date.isBefore(new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()))
				&& possibileUtilizzareResiduoAnnoPrecedente()) {
			return initializationTime.residualMinutesPastYear != null ? initializationTime.residualMinutesPastYear : 0;
		}	


		return 0;
	}

	/**
	 * @return il valore (negativo) dei minuti a cui corrisponde un riposo compensativo
	 */
	public int minutiRiposoCompensativo(LocalDate date) {
		//Cambia in funzione del tipo di orario di lavoro
		return - person.workingTimeType.getWorkingTimeTypeDayFromDayOfWeek(date.getDayOfWeek()).workingTime;

	}

	/**
	 * 
	 * @return il numero di giorni di indennità di reperibilità festiva per quella persona in quel mese di quell'anno
	 */
	public int holidaysAvailability(int year, int month){
		int holidaysAvailability = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "208").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		Logger.warn("competence: " +competence);
		if(competence != null)
			holidaysAvailability = competence.valueApproved;
		else
			holidaysAvailability = 0;
		return holidaysAvailability;
	}

	/**
	 * 
	 * @return il numero di giorni di indennità di reperibilità feriale per quella persona in quel mese di quell'anno
	 */
	public int weekDayAvailability(@Valid int year, @Valid int month){
		int weekDayAvailability = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "207").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);

		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			weekDayAvailability = competence.valueApproved;
		else
			weekDayAvailability = 0;
		return weekDayAvailability;
	}

	/**
	 * 
	 * @param year
	 * @param month
	 * @return il numero di giorni di straordinario diurno nei giorni lavorativi 
	 */
	public int daylightWorkingDaysOvertime(int year, int month){
		int daylightWorkingDaysOvertime = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "S1").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			daylightWorkingDaysOvertime = competence.valueApproved;
		else
			daylightWorkingDaysOvertime = 0;
		return daylightWorkingDaysOvertime;
	}

	/**
	 * 
	 * @param year
	 * @param month
	 * @return il numero di giorni di straordinario diurno nei giorni festivi o notturno nei giorni lavorativi
	 */
	public int daylightholidaysOvertime(int year, int month){
		int daylightholidaysOvertime = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "S2").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			daylightholidaysOvertime = competence.valueApproved;
		else
			daylightholidaysOvertime = 0;
		return daylightholidaysOvertime;
	}

	/**
	 * 
	 * @return il numero di giorni di turno ordinario
	 */
	public int ordinaryShift(int year, int month){
		int ordinaryShift = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "T1").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			ordinaryShift = competence.valueApproved;
		else
			ordinaryShift = 0;
		return ordinaryShift;
	}

	/**
	 * 
	 * @return il numero di giorni di turno notturno
	 */
	public int nightShift(int year, int month){
		int nightShift = 0;
		CompetenceCode cmpCode = CompetenceCode.find("Select cmp from CompetenceCode cmp where cmp.code = ?", "T2").first();
		Logger.debug("Il codice competenza é: %s", cmpCode);
		if(cmpCode == null)
			return 0;
		Competence competence = Competence.find("Select comp from Competence comp, CompetenceCode cmpCode where comp.person = ? and " +
				"comp.year = ? and comp.month = ? and comp.competenceCode = cmpCode and cmpCode = ?", person, year, month, cmpCode).first();
		if(competence != null)
			nightShift = competence.valueApproved;
		else
			nightShift = 0;
		return nightShift;
	}


	/**
	 * 
	 * @return il numero di ore di lavoro in eccesso/difetto dai mesi precedenti, calcolate a partire da gennaio dell'anno corrente.
	 * nel caso in cui ci trovassimo a gennaio, le ore di lavoro in eccesso/difetto provengono dall'anno precedente
	 */
	public int pastRemainingHours(int month, int year){
		int pastRemainingHours = 0;
		/**
		 * per adesso ricorro al metodo di ricerca del progressivo all'ultimo giorno dell'anno precedente, non appena sarà pronta 
		 * la classe PersonYear, andrò a fare la ricerca direttamente dentro quella classe sulla base della persona e dell'anno che mi
		 * interessa.
		 */
		if(month == DateTimeConstants.JANUARY){
			LocalDate lastDayOfYear = new LocalDate(year-1,DateTimeConstants.DECEMBER,31);
			//			PersonYear py = PersonYear.find("Select py from PersonYear py where py.person = ?",person).first();
			//			pastRemainingHours = py.remainingHours;
			PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date = ?", person, lastDayOfYear).first();
			pastRemainingHours = pd.progressive;
		}
		else{
			//int month = date.getMonthOfYear();
			int counter = 0;
			for(int i = 1; i < month; i++){
				int day = 0;
				if(i==1 || i==3 || i==5 || i==7 || i==8 || i==10 || i==12)
					day = 31;
				if(i==4 || i==6 || i==9 || i==11)
					day = 30;
				if(i==2){
					if(year==2012 || year==2016 || year==2020)
						day = 29;
					else
						day = 28;
				}			
				LocalDate endOfMonth = new LocalDate(year,i,day);	
				PersonDay pd = PersonDay.find("Select pd from PersonDay pd where pd.person = ? " +
						"and pd.date = ?", person, endOfMonth).first();
				counter = counter+pd.progressive;
			}
			pastRemainingHours = counter;

		}

		return pastRemainingHours;
	}
	
	/**
	 * metodo di utilità per il controller UploadSituation
	 * @return la lista delle assenze fatte da quella persona in quel mese. Prima di inserirle in lista controlla che le assenze non siano
	 * a solo uso interno 
	 */
	public List<Absence> getAbsenceInMonthForUploadSituation(){
		List<Absence> absenceList = new ArrayList<Absence>();
		List<PersonDay> pdList = PersonDay.find("Select pd from PersonDay pd where pd.person = ? and pd.date between ? and ?", 
				person, new LocalDate(year,month, 1), new LocalDate(year, month, 1).dayOfMonth().withMaximumValue()).fetch();
		for(PersonDay pd : pdList){
			if(pd.absences.size() > 0){
				for(Absence abs : pd.absences){
					if(!abs.absenceType.internalUse)
						absenceList.add(abs);
				}
			}
				
		}
		
		return absenceList;
	}
	
	/**
	 * metodo di utilità per il controller UploadSituation
	 * @return la lista delle competenze del dipendente in questione per quel mese in quell'anno
	 */
	public List<Competence> getCompetenceInMonthForUploadSituation(){
		List<Competence> competenceList = Competence.find("Select comp from Competence comp where comp.person = ? and comp.month = ? " +
				"and comp.year = ?", person, month, year).fetch();
		
		return competenceList;
	}
	
	public static void main(String[] args) {
		Integer a = 1;
		if (a > 2)
			System.out.println("a > 2");
		else
			System.out.println("a < 2");
	}

}
