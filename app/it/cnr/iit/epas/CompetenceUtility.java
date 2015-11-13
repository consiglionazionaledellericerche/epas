package it.cnr.iit.epas;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;
import dao.PersonDayDao;
import manager.PairStamping;
import manager.PersonDayManager;
import manager.PersonManager;
import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.ShiftSlot;
import models.query.QCompetence;
import models.query.QCompetenceCode;
import models.query.QPerson;
import models.query.QPersonShiftShiftType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import play.Logger;
import play.i18n.Messages;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;


public class CompetenceUtility {

	@Inject
	public CompetenceUtility(JPQLQueryFactory queryFactory,
			PersonDayDao personDayDao, PersonManager personManager, 
			PersonDayManager personDayManager) {

		this.queryFactory = queryFactory;
		this.personManager = personManager;
		this.personDayDao = personDayDao;
		this.personDayManager = personDayManager;
	}

	private final JPQLQueryFactory queryFactory;
	private final PersonManager personManager;
	private final PersonDayDao personDayDao;
	private PersonDayManager personDayManager;

	public static String codFr = "207";    						// codice dei turni feriali
	public static String codFs = "208";							// codice dei turni festivi
	public static String codShift = "T1";						// codice dei turni

	public static String thNoStampings = Messages.get("PDFReport.thNoStampings");  // nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
	public static String thMissingTime = Messages.get("PDFReport.thMissingTime");// 
	public static String thAbsences = Messages.get("PDFReport.thAbsences");				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	public static String thMissions = Messages.get("PDFReport.thMissions");				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	public static String thBadStampings = Messages.get("PDFReport.thBadStampings");  // nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni
	public static String thWarnStampings = Messages.get("PDFReport.thWarnStampings");  // nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni ma 
	// con le ore lavorate che discostano meno di 2 ore
	public static String thDays = Messages.get("PDFReport.thDays");				// nome della colonna per i giorni di turno svolti mensilmente da una persona
	public static String thReqHour = Messages.get("PDFReport.thReqHour");	// nome della colonna per le ore di turno svolte mensilmente da una persona
	public static String thAppHour = Messages.get("PDFReport.thAppHour");		// nome della colonna per le ore di turno approvate mensilmente per una persona
	public static String thLackTime = Messages.get("PDFReport.thLackTime");
	public static String thExceededMin = Messages.get("PDFReport.thExceededMin");	// Nome dela colonna contenente i minuti accumulati di turno da riportare nei mesi successivi
	public static String thIncompleteTime = Messages.get("PDFReport.thIncompleteTime");

	/*
	 * @author arianna
	 * Calcola le ore di turno dai giorni (days)
	 * resto = (days%2 == 0) ? 0 : 0.5
	 * ore = days*6 + (int)(days/2) + resto;	
	 */
	public BigDecimal calcShiftHoursFromDays (int days) {
		BigDecimal decDays = new BigDecimal(days);
		BigDecimal due = new BigDecimal("2");

		BigDecimal minutes = (days%2 == 0) ? BigDecimal.ZERO : new BigDecimal("0.5"); 
		BigDecimal hours = decDays.multiply(new BigDecimal(6)).add(decDays.divide(due, RoundingMode.HALF_DOWN)).add(minutes);	

		Logger.debug("La calcShiftHoursFromDays restituisce hours = %s", hours);

		return hours;
	}


	/*
	 * @author arianna
	 * Calcola le oer e i minuti dal numero totale dei minuti 
	 * lavorati
	 */
	public BigDecimal calcDecimalShiftHoursFromMinutes (int minutes) {
		int hours;
		int mins;

		Logger.debug("Nella calcDecimalShiftHoursFromMinutes(%s)", minutes);

		if (minutes < 60) {
			hours = 0;
			mins = minutes;
		} else {
			hours = minutes / 60;
			mins = minutes % 60;		
		}

		Logger.debug("hours = %s mins = %s", hours, mins);	
		return new BigDecimal(Integer.toString(hours).concat(".").concat(Integer.toString(mins)));
	}


	/*
	 * @author arianna
	 * Calcola il LocalTime dal numero dei minuti 
	 * che compongono l'orario
	 */
	public String calcStringShiftHoursFromMinutes (int minutes) {
		int hours;
		int mins;

		if (minutes < 60) {
			hours = 0;
			mins = minutes;
		} else {
			hours = minutes / 60;
			mins = minutes % 60;	
		}

		Logger.debug("hours = %s mins = %s", hours, mins);	

		return  Integer.toString(hours).concat(".").concat(Integer.toString(mins));
	}



	/*
	 * Da chiamare per aggiornare la tabella competences inserendo i 30 minuti avanzati nella colonna
	 * exceeded_min  nel caso in cui ci sia stato un arrotondamento per difetto delle ore approvate
	 * rispetto a quelle richieste
	 */
	public void updateExceedeMinInCompetenceTable () {
		int year = 2015;
		int month = 3;  // Mese attuale del quale dobbiamo ancora fare il pdf

		int exceddedMin;

		CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codShift).first();
		List<Person> personList;

		QCompetence com = QCompetence.competence;
		QPerson person = QPerson.person;
		QPersonShiftShiftType personShiftShiftType = QPersonShiftShiftType.personShiftShiftType;

		personList = queryFactory.from(person)
				.join(person.personShift.personShiftShiftTypes, personShiftShiftType)
				.where(
						personShiftShiftType.shiftType.type.in(ImmutableSet.of("A", "B"))
						).list(person);


		//		personList = JPA.em().createQuery("SELECT p FROM PersonShiftShiftType psst JOIN psst.personShift ps JOIN ps.person p WHERE psst.shiftType.type = :type AND (psst.beginDate IS NULL OR psst.beginDate <= now()) AND (psst.endDate IS NULL OR psst.endDate >= now())")
		//				.setParameter("type", type)
		//				.getResultList(); 
		//personList = PersonDao.getPersonForShift(type);

		for (Person p: personList) {

			final JPQLQuery query = queryFactory.query();

			// leggo l'ultima competenza con il numero delle ore approvate
			// diverso da quello richieste
			final Competence myCompetence = query
					.from(com)
					.where(
							com.person.eq(p)
							.and(com.year.eq(year))
							.and(com.month.lt(month))
							.and(com.competenceCode.eq(competenceCode))		
							.and(com.valueApproved.ne(0))
							.and(com.valueRequested.ne(BigDecimal.ZERO))
							.and(com.valueRequested.intValue().ne(com.valueApproved)
									.or(com.valueRequested.floor().ne(com.valueRequested)))
							)
							.orderBy(com.year.desc(), com.month.desc())
							.limit(1)
							.uniqueResult(com);

			// calcolo i minuti in eccesso non ancora remunerati
			if (myCompetence == null) {
				// we are at the first case, so the person has its fist 0.5 hour to accumulate
				Logger.debug("myCompetence is null");
				exceddedMin = 0;
			} else if (myCompetence.valueRequested.setScale(0, RoundingMode.UP).intValue() <= myCompetence.valueApproved) {
				Logger.debug("La query sulle competenze ha trovato %s e myCompetence.valueRequested.ROUND_CEILING=%s <= myCompetence.valueApproved=%d", myCompetence.toString(), myCompetence.valueRequested.ROUND_CEILING, myCompetence.valueApproved);
				// Last rounding was on ceiling, so we round to floor
				//valueApproved = requestedHours.setScale(0, RoundingMode.DOWN).intValue();
				exceddedMin = 0;
			} else {
				Logger.debug("La query sulle competenze ha trovato %s", myCompetence.toString());
				// we round to ceiling
				//valueApproved = requestedHours.setScale(0, RoundingMode.UP).intValue();
				exceddedMin = 30;
			}


			Competence lastCompetence = getLastCompetence(p, year, month, competenceCode);
			// aggiorno la competenza con i minuti in eccesso calcolati
			lastCompetence.setExceededMin(exceddedMin);
			lastCompetence.save();
		}

	}

	
	private Competence getLastCompetence(Person person, int year, int month, CompetenceCode competenceCode) {
		QCompetence com2 = QCompetence.competence;
		QCompetenceCode comCode = QCompetenceCode.competenceCode;
		// prendo la competenza del mese precedente
		return queryFactory
				.from(com2)
				.join(com2.competenceCode, comCode)
				.where(
						com2.person.eq(person)
						.and(com2.year.eq(year))
						.and(com2.month.lt(month))
						.and(comCode.eq(competenceCode))		
						)
						.orderBy(com2.year.desc(), com2.month.desc())
						.limit(1)
						.uniqueResult(com2);
	}

	/*
	 * @author arianna
	 * Aggiorna la tabella totalPersonShiftSumDays per contenere, per ogni persona nella lista dei turni personShiftDays,
	 * e per ogni tipo di turno trovato, il numero di giorni di turno effettuati
	 * 
	 * @param personShiftDays 			- lista di shiftDays
	 * @param totalPersonShiftSumDays	- tabella contenente il numero di giorni di turno effettuati per ogni persona
	 * 									  e tipologia di turno. Questa tabella viene aggiornata contando i giorni di
	 * 									  turno contenuti nella lista personShiftDays passata come parametro
	 */
	public void countPersonsShiftsDays(List<PersonShiftDay> personShiftDays, Table<Person, String, Integer> personShiftSumDaysForTypes) {

		// for each person and dy in the month contains worked shift (A/B)
		ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder(); 
		Table<Person, Integer, String> shiftMonth = null;

		// for each person contains the number of days of working shift divided by shift's type 
		//Table<Person, String, Integer> shiftsSumDays = TreeBasedTable.<Person, String, Integer>create();

		for (PersonShiftDay personShiftDay : personShiftDays) {
			Person person = personShiftDay.personShift.person;

			// registro il turno della persona per quel giorno
			//------------------------------------------------------
			builder.put(person, personShiftDay.date.getDayOfMonth(), personShiftDay.shiftType.type);				
			//Logger.debug("Registro il turno %s di %s per il giorno %d", personShiftDay.shiftType.type, person, personShiftDay.date.getDayOfMonth());			
		}
		shiftMonth = builder.build();

		// for each person and shift type counts the total shift days
		for (Person person: shiftMonth.rowKeySet()) {

			Logger.debug("conto i turni di %s", person);

			// number of competence
			int shiftNum = 0;

			for (int day: shiftMonth.columnKeySet()) {

				if (shiftMonth.contains(person, day)) {
					// get the shift type
					String shift = shiftMonth.get(person, day);
					shiftNum = (personShiftSumDaysForTypes.contains(person, shift)) ? personShiftSumDaysForTypes.get(person, shift) : 0;
					shiftNum++;
					personShiftSumDaysForTypes.put(person, shift, shiftNum);	
				}
			}

		}

		Logger.debug("la countPersonsShiftCompetences ritorna  totalPersonShiftSumDays.size() = %s", personShiftSumDaysForTypes.size());

		// return the number of saved competences
		//return shiftsSumDays;
	}


	/**
	 * @author arianna
	 * crea una tabella con le eventuali inconsistenze tra le timbrature dei reperibili di un certo tipo e i
	 * turni di reperibilit√† svolti in un determinato periodo di tempo
	 * ritorna una tabella del tipo (Person, [thNoStamping, thAbsence], List<'gg/MMM '>)
	 */
	public Table<Person, String, List<String>> getReperibilityInconsistenceAbsenceTable(List<PersonReperibilityDay> personReperibilityDays, LocalDate startDate, LocalDate endDate) {
		// for each person contains days with absences and no-stamping  matching the reperibility days 
		Table<Person, String, List<String>> inconsistentAbsenceTable = TreeBasedTable.<Person, String, List<String>>create();

		// lista dei giorni di assenza e mancata timbratura
		List<String> noStampingDays = new ArrayList<String>();
		List<String> absenceDays = new ArrayList<String>();

		for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
			Person person = personReperibilityDay.personReperibility.person;


			//check for the absence inconsistencies
			//------------------------------------------

			Optional<PersonDay> personDay = personDayDao.getPersonDay(person, personReperibilityDay.date);
			//PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personReperibilityDay.date, person).first();

			// if there are no events and it is not an holiday -> error
			if (!personDay.isPresent() & LocalDate.now().isAfter(personReperibilityDay.date)) {
				//if (!person.isHoliday(personReperibilityDay.date)) {
				if(!personManager.isHoliday(person, personReperibilityDay.date)){
					Logger.info("La reperibilità di %s %s è incompatibile con la sua mancata timbratura nel giorno %s", person.name, person.surname, personReperibilityDay.date);


					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
					noStampingDays.add(personReperibilityDay.date.toString("dd MMM"));
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);	
				}
			} else if (LocalDate.now().isAfter(personReperibilityDay.date)) {
				// check for the stampings in working days
				//if (!person.isHoliday(personReperibilityDay.date) && personDay.get().stampings.isEmpty()) {
				if (!personManager.isHoliday(person, personReperibilityDay.date) && personDay.get().stampings.isEmpty()){
					Logger.info("La reperibilità di %s %s è incompatibile con la sua mancata timbratura nel giorno %s", person.name, person.surname, personDay.get().date);


					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();	
					noStampingDays.add(personReperibilityDay.date.toString("dd MMM"));			
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);					
				}

				// check for absences
				if (!personDay.get().absences.isEmpty()) {
					for (Absence absence : personDay.get().absences) {
						if (absence.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay) {
							Logger.info("La reperibilit√† di %s %s √® incompatibile con la sua assenza nel giorno %s", person.name, person.surname, personReperibilityDay.date);

							absenceDays = (inconsistentAbsenceTable.contains(person, thAbsences)) ? inconsistentAbsenceTable.get(person, thAbsences) : new ArrayList<String>();							
							absenceDays.add(personReperibilityDay.date.toString("dd MMM"));							
							inconsistentAbsenceTable.put(person, thAbsences, absenceDays);
						}
					}
				}	
			}
		}

		return inconsistentAbsenceTable;
	}


	/*
	 * Costruisce la tabella delle inconsistenza tra i giorni di turno dati e le timbrature
	 * ritorna una tabella del tipo (Person, [thNoStamping, thAbsence, thBadStampings], [List<'gg MMM'>, List<'gg MMM'>, 'dd MMM -> HH:mm-HH:mm']) 
	 */
	public void getShiftInconsistencyTimestampTable(List<PersonShiftDay> personShiftDays, Table<Person, String, List<String>> inconsistentAbsenceTable) {

		Logger.debug("---------thBadStampings = %s-----", thBadStampings);
		// lista dei giorni di assenza nel mese, mancata timbratura e timbratura inconsistente	
		List<String> noStampingDays = new ArrayList<String>();		// mancata timbratura
		List<String> badStampingDays = new ArrayList<String>();		// timbrature errate
		List<String> absenceDays = new ArrayList<String>();			// giorni di assenza
		List<String> lackOfTimes = new ArrayList<String>();			// tempo mancante


		for (PersonShiftDay personShiftDay : personShiftDays) {
			Person person = personShiftDay.personShift.person;

			// legge l'orario di inizio e fine turno da rispettare (mattina o pomeriggio)
			LocalTime startShift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.startMorning : personShiftDay.shiftType.shiftTimeTable.startAfternoon;
			LocalTime endShift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.endMorning : personShiftDay.shiftType.shiftTimeTable.endAfternoon;

			// legge l'orario di inizio e fine pausa pranzo del turno
			LocalTime startLunchTime = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.startMorningLunchTime : personShiftDay.shiftType.shiftTimeTable.startAfternoonLunchTime;
			LocalTime endLunchTime = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.endMorningLunchTime : personShiftDay.shiftType.shiftTimeTable.endAfternoonLunchTime;

			//Logger.debug("Turno: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);

			// Add flexibility (15 min.) due to the new rules (PROT. N. 0008692 del 2/12/2014)
			LocalTime roundedStartShift = startShift.plusMinutes(15);
			LocalTime roundedStartLunchTime = startLunchTime.minusMinutes(15);
			LocalTime roundedEndLunchTime = endLunchTime.plusMinutes(15);
			LocalTime roundedEndShift = endShift.minusMinutes(15);

			Logger.debug("Turno: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);

			//check for the absence inconsistencies
			//------------------------------------------
			Optional<PersonDay> personDay = personDayDao.getPersonDay(person, personShiftDay.date);
			//PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personShiftDay.date, person).first();
			Logger.debug("Prelevo il personDay %s per la persona %s", personShiftDay.date, person.surname);

			// if there are no events and it is not an holiday -> error
			if (!personDay.isPresent()) {	

				if (!personManager.isHoliday(person,personShiftDay.date) && personShiftDay.date.isBefore(LocalDate.now())) {
					Logger.info("Il turno di %s %s √® incompatibile con la sua mancata timbratura nel giorno %s (personDay == null)", person.name, person.surname, personShiftDay.date);

					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
					noStampingDays.add(personShiftDay.date.toString("dd MMM"));
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);

					Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
				}
			} else {

				// check for the stampings in working days
				if (!personManager.isHoliday(person,personShiftDay.date) && LocalDate.now().isAfter(personShiftDay.date)) {

					// check no stampings
					//-----------------------------
					if (personDay.get().stampings.isEmpty()) {
						Logger.info("Il turno di %s %s √® incompatibile con la sue mancate timbrature nel giorno %s", person.name, person.surname, personDay.get().date);


						noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
						noStampingDays.add(personShiftDay.date.toString("dd MMM"));
						inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);

						Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
					} else {
						// check consistent stampings
						//-----------------------------
						//Logger.debug("Legge le coppie di timbrature valide");
						// legge le coppie di timbrature valide 
						//FIXME injettare il PersonDayManager
						List<PairStamping> pairStampings = personDayManager.getValidPairStamping(personDay.get());

						//Logger.debug("Dimensione di pairStampings =%s", pairStampings.size());

						// se c'e' una timbratura guardo se e' entro il turno
						if ((personDay.get().stampings.size() == 1) &&
								((personDay.get().stampings.get(0).isIn() && personDay.get().stampings.get(0).date.toLocalTime().isAfter(roundedStartShift)) || 
										(personDay.get().stampings.get(0).isOut() && personDay.get().stampings.get(0).date.toLocalTime().isBefore(roundedStartShift)) )) {


							String stamp = (personDay.get().stampings.get(0).isIn()) ? personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm").concat("- **:**") : "- **:**".concat(personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm"));

							badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
							badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stamp));
							inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);

							Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

							// se e' vuota => manca qualche timbratura		
						} else if (pairStampings.isEmpty()) {							

							Logger.info("Il turno di %s %s e incompatibile con la sue  timbrature disallineate nel giorno %s", person.name, person.surname, personDay.get().date);

							badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
							badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> timbrature disaccoppiate"));
							inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);

							Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));

							// controlla che le coppie di timbrature coprano
							// gli intervalli di prima e dopo pranzo
						} else {

							//Logger.debug("Controlla le timbrature");
							boolean okBeforeLunch = false;  	// intervallo prima di pranzo coperto
							boolean okAfterLunch = false;		// intervallo dopo pranzo coperto

							String strStamp = "";

							// per ogni coppia di timbrature
							for (PairStamping pairStamping : pairStampings) {

								strStamp = strStamp.concat(pairStamping.in.date.toString("HH:mm")).concat(" - ").concat(pairStamping.out.date.toString("HH:mm")).concat("  ");
								Logger.debug("Controllo la coppia %s", strStamp);

								// controlla se la coppia di timbrature interseca l'intervallo prima e dopo pranzo del turno
								// if (pairStamping.out.date.toLocalTime().isAfter(startLunchTime) && pairStamping.in.date.toLocalTime().isBefore(startShift)) {
								if (!pairStamping.out.date.toLocalTime().isBefore(startLunchTime) && !pairStamping.in.date.toLocalTime().isAfter(startShift)) {
									okBeforeLunch = true;
									//Logger.debug("okBeforeLunch=%s", okBeforeLunch);
								}
								// if (pairStamping.out.date.toLocalTime().isAfter(endShift) && pairStamping.in.date.toLocalTime().isBefore(endLunchTime)) {
								if (!pairStamping.out.date.toLocalTime().isBefore(endShift) && !pairStamping.in.date.toLocalTime().isAfter(endLunchTime)) {
									okAfterLunch = true;
									//Logger.debug("okAfterLunch=%s", okAfterLunch);
								} 
							}

							// se non ha coperto interamente i due intervalli, controlla se il tempo mancante al
							// completamento del turno sia <= 2 ore
							if (!okBeforeLunch || !okAfterLunch) {

								int workingMinutes = 0;
								LocalTime lowLimit;
								LocalTime upLimit;
								LocalTime newLimit;

								// scostamenti delle timbrature dalle fasce del turno
								int diffStartShift = 0;
								int diffStartLunchTime = 0;
								int diffEndLunchTime = 0;
								int diffEndShift = 0;

								boolean inTolleranceLimit = true;	// ingressi  euscite nella tolleranza dei 15 min

								String stampings = "";

								Logger.info("Il turno di %s  nel giorno %s non √® stato completato o c'e' stata una uscita fuori pausa pranzo - orario %s", person, personDay.get().date, strStamp);
								Logger.debug("Esamino le coppie di timbrature");

								// per ogni coppia di timbrature
								for (PairStamping pairStamping : pairStampings) {

									//Logger.debug("pairStamping.in.date = %s  pairStamping.out.date = %s", pairStamping.in.date.toLocalTime(), pairStamping.out.date.toLocalTime());

									// l'intervallo di tempo lavorato interseca la parte del turno prima di pranzo
									if ((pairStamping.in.date.toLocalTime().isBefore(startShift) && pairStamping.out.date.toLocalTime().isAfter(startShift)) ||
											(pairStamping.in.date.toLocalTime(). isAfter(startShift) && pairStamping.in.date.toLocalTime().isBefore(startLunchTime))) {

										// conta le ore lavorate in turno prima di pranzo
										lowLimit = (pairStamping.in.date.toLocalTime().isBefore(startShift)) ? startShift : pairStamping.in.date.toLocalTime();
										upLimit = (pairStamping.out.date.toLocalTime().isBefore(startLunchTime)) ? pairStamping.out.date.toLocalTime() : startLunchTime;
										workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
										Logger.debug("N.1 - ss=%s -- slt=%s lowLimit=%s upLimit=%s workingMinutes=%s", startShift, startLunchTime, lowLimit, upLimit, workingMinutes);

										// calcola gli scostamenti dalla prima fascia del turno tenendo conto dei 15 min di comporto
										// se il turnista è entrato prima
										if (pairStamping.in.date.toLocalTime().isBefore(startShift)) {
											newLimit = (pairStamping.in.date.toLocalTime().isBefore(startShift.minusMinutes(15))) ? startShift.minusMinutes(15) : pairStamping.in.date.toLocalTime();
											if (pairStamping.in.date.toLocalTime().isBefore(startShift.minusMinutes(15))) { inTolleranceLimit = false;}
										} else {
											// è entrato dopo
											newLimit = (pairStamping.in.date.toLocalTime().isAfter(startShift.plusMinutes(15))) ? startShift.plusMinutes(15) : pairStamping.in.date.toLocalTime();
											if (pairStamping.in.date.toLocalTime().isAfter(startShift.plusMinutes(15))) {inTolleranceLimit = false;}
										}
										diffStartShift = DateUtility.getDifferenceBetweenLocalTime(newLimit, startShift);
										Logger.debug("diffStartShift=%s", diffStartShift);

										// calcola gli scostamenti dell'ingresso in pausa pranzo tenendo conto dei 15 min di comporto
										// se il turnista è andato a  pranzo prima
										if (pairStamping.out.date.toLocalTime().isBefore(startLunchTime)) {
											Logger.debug("vedo uscita per pranzo prima");
											newLimit = (startLunchTime.minusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) ? startLunchTime.minusMinutes(15) : pairStamping.out.date.toLocalTime();
											diffStartLunchTime = DateUtility.getDifferenceBetweenLocalTime(newLimit, startLunchTime);
											if (startLunchTime.minusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) {inTolleranceLimit = false;}
										} else if (pairStamping.out.date.toLocalTime().isBefore(endLunchTime)) {
											// è andato a pranzo dopo
											Logger.debug("vedo uscita per pranzo dopo");
											newLimit = (startLunchTime.plusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) ? pairStamping.out.date.toLocalTime() : startLunchTime.plusMinutes(15);
											if (startLunchTime.plusMinutes(15).isBefore(pairStamping.out.date.toLocalTime())) {inTolleranceLimit = false;}
											diffStartLunchTime = DateUtility.getDifferenceBetweenLocalTime(startLunchTime, newLimit); /* ? */
										}

										Logger.debug("diffStartLunchTime=getDifferenceBetweenLocalTime(%s, %s)=%s", startLunchTime, newLimit, diffStartLunchTime);
									}

									// l'intervallo di tempo lavorato interseca la parte del turno dopo pranzo
									if ((pairStamping.in.date.toLocalTime().isBefore(endLunchTime) && pairStamping.out.date.toLocalTime().isAfter(endLunchTime)) ||
											(pairStamping.in.date.toLocalTime().isAfter(endLunchTime) && pairStamping.in.date.toLocalTime().isBefore(endShift)))  {

										// conta le ore lavorate in turno dopo pranzo
										lowLimit = (pairStamping.in.date.toLocalTime().isBefore(endLunchTime)) ? endLunchTime : pairStamping.in.date.toLocalTime();
										upLimit = (pairStamping.out.date.toLocalTime().isBefore(endShift)) ? pairStamping.out.date.toLocalTime() : endShift;
										workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
										Logger.debug("N.2 - elt=%s --- es=%s  slowLimit=%s upLimit=%s workingMinutes=%s", endLunchTime, endShift, lowLimit, upLimit, workingMinutes);

										// calcola gli scostamenti dalla seconda fascia del turno tenendo conto dei 15 min di comporto
										// se il turnista è rientrato prima dalla pausa pranzo
										if (pairStamping.in.date.toLocalTime().isBefore(endLunchTime) && pairStamping.in.date.toLocalTime().isAfter(startLunchTime)) {
											Logger.debug("vedo rientro da pranzo prima");
											newLimit = (endLunchTime.minusMinutes(15).isAfter(pairStamping.in.date.toLocalTime())) ?  endLunchTime.minusMinutes(15) : pairStamping.in.date.toLocalTime();
											diffEndLunchTime = DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
											Logger.debug("diffEndLunchTime=getDifferenceBetweenLocalTime(%s, %s)=%s", newLimit, endLunchTime, diffEndLunchTime);
										} else if (pairStamping.in.date.toLocalTime().isBefore(endShift) && pairStamping.in.date.toLocalTime().isAfter(endLunchTime)) {
											// è rientrato dopo
											Logger.debug("vedo rientro da pranzo dopo");
											newLimit = (pairStamping.in.date.toLocalTime().isAfter(endLunchTime.plusMinutes(15))) ? endLunchTime.plusMinutes(15) : pairStamping.in.date.toLocalTime();
											if (pairStamping.in.date.toLocalTime().isAfter(endLunchTime.plusMinutes(15))) {inTolleranceLimit = false;}
											diffEndLunchTime = DateUtility.getDifferenceBetweenLocalTime(newLimit, endLunchTime);
											Logger.debug("diffEndLunchTime=getDifferenceBetweenLocalTime(%s, %s)=%s", endLunchTime, newLimit, diffEndLunchTime);
										}


										// se il turnista è uscito prima del turno
										if (pairStamping.out.date.toLocalTime().isBefore(endShift)) {
											Logger.debug("vedo uscita prima della fine turno");
											newLimit = (endShift.minusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) ? endShift.minusMinutes(15) : pairStamping.out.date.toLocalTime();
											if (endShift.minusMinutes(15).isAfter(pairStamping.out.date.toLocalTime())) {inTolleranceLimit = false;}
										} else {
											Logger.debug("vedo uscita dopo la fine turno");
											// il turnista è uscito dopo la fine del turno
											newLimit = (pairStamping.out.date.toLocalTime().isAfter(endShift.plusMinutes(15))) ? endShift.plusMinutes(15) : pairStamping.out.date.toLocalTime();
										}
										diffEndShift = DateUtility.getDifferenceBetweenLocalTime(endShift, newLimit);
										Logger.debug("diffEndShift=%s", diffEndShift);
									}		

									// write the pair stamping								
									stampings = stampings.concat(pairStamping.in.date.toString("HH:mm")).concat("-").concat(pairStamping.out.date.toString("HH:mm")).concat("  ");									 
								}

								stampings.concat("<br />");

								// controllo eventuali compensazioni di minuti in  ingresso e uscita
								//--------------------------------------------------------------------
								int restoredMin = 0;


								// controlla pausa pranzo:
								// - se è uscito prima dell'inizio PP (è andato a pranzo prima)
								if (diffStartLunchTime < 0) {
									Logger.debug("sono entrata in pausa pranzo prima! diffStartLunchTime=%s", diffStartLunchTime);
									// controlla se è anche rientrato prima dalla PP e compensa
									if (diffEndLunchTime > 0) {
										Logger.debug("E rientrato prima dalla pusa pranzo! diffEndLunchTime=%s", diffEndLunchTime);
										restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));
										Logger.debug("restoredMin=%s Math.abs(diffStartLunchTime)=%s Math.abs(diffEndLunchTime)=%s", restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));

										diffStartLunchTime = ((diffStartLunchTime + diffEndLunchTime) > 0) ? 0 : diffStartLunchTime + diffEndLunchTime;
										diffEndLunchTime = ((diffStartLunchTime + diffEndLunchTime) > 0) ? diffStartLunchTime + diffEndLunchTime : 0;

									} 
									// se necessario e se è entrato prima, compensa con l'ingresso 
									if ((diffStartLunchTime < 0) && (diffStartShift > 0)) {
										Logger.debug("E entrato anche prima! diffStartShift=%s", diffStartShift);
										// cerca di compensare con l'ingresso
										restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffStartShift));
										Logger.debug("restoredMin=%s Math.abs(diffStartLunchTime)=%s Math.abs(diffStartShift)=%s", restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffStartShift));

										diffStartLunchTime = ((diffStartLunchTime + diffStartShift) > 0) ? 0 : diffStartLunchTime + diffStartShift;
										diffStartShift = ((diffStartLunchTime + diffStartShift) > 0) ? diffStartLunchTime + diffStartShift : 0;
									}
								}

								// - se è entrato dopo la fine della pausa pranzo
								if (diffEndLunchTime < 0) {
									Logger.debug("E entrato in ritardo dalla apusa pranzo! diffEndLunchTime=%s", diffEndLunchTime);
									// controlla che sia entrata dopo in pausa pranzo
									if (diffStartLunchTime > 0) {
										Logger.debug("e andata anche dopo in pausa pranzo! diffStartLunchTime=%s", diffStartLunchTime);
										restoredMin += Math.min(Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));
										Logger.debug("restoredMin=%s Math.abs(diffStartLunchTime)=%s Math.abs(diffEndLunchTime)=%s", restoredMin, Math.abs(diffStartLunchTime), Math.abs(diffEndLunchTime));

										diffEndLunchTime = ((diffEndLunchTime + diffStartLunchTime) > 0) ? 0 : diffEndLunchTime + diffStartLunchTime;
										diffStartLunchTime = ((diffEndLunchTime + diffStartLunchTime) > 0) ? diffEndLunchTime + diffStartLunchTime : 0;

									} 
									// se necessario e se è uscito dopo, compensa con l'uscita
									if ((diffEndLunchTime < 0) && (diffEndShift > 0)) {
										Logger.debug("e' uscito dopo! diffEndShift=%s", diffEndShift);
										// cerca di conpensare con l'uscita (è uscito anche dopo)
										restoredMin += Math.min(Math.abs(diffEndLunchTime), Math.abs(diffEndShift));
										Logger.debug("restoredMin=%s Math.abs(diffEndLunchTime)=%s Math.abs(diffEndShift)=%s", restoredMin, Math.abs(diffEndLunchTime), Math.abs(diffEndShift));

										diffEndLunchTime = ((diffEndLunchTime + diffEndShift) > 0) ? 0 : diffEndLunchTime + diffEndShift;
										diffEndShift = ((diffEndLunchTime + diffEndShift) > 0) ? diffEndLunchTime + diffEndShift : 0;	
									}
								}

								// controlla eventuali compensazioni di ingresso e uscita
								// controlla se è uscito dopo
								if ((diffStartShift < 0) && (diffEndShift > 0)) {
									Logger.debug("e entrato dopo ed è uscito dopo! diffStartShift=%s diffEndShift=%s", diffStartShift, diffEndShift);
									restoredMin += Math.min(Math.abs(diffEndShift), Math.abs(diffStartShift)); 
									Logger.debug("restoredMin=%s Math.abs(diffEndShift)=%s Math.abs(diffStartShift)=%s", restoredMin, Math.abs(diffEndShift), Math.abs(diffStartShift));

									diffStartShift = ((diffEndShift + diffStartShift) > 0) ? 0 : diffEndShift + diffStartShift;
									diffEndShift = ((diffEndShift + diffStartShift) > 0) ? diffEndShift + diffStartShift : 0;

								} else if ((diffEndShift < 0) && (diffStartShift > 0)) {
									Logger.debug("e uscito prima ed è entrato dopo! diffStartShift=%s diffEndShift=%s", diffStartShift, diffEndShift);
									restoredMin += Math.min(Math.abs(diffEndShift), Math.abs(diffStartShift)); 
									Logger.debug("restoredMin=%s Math.abs(diffEndShift)=%s Math.abs(diffStartShift)=%s", restoredMin, Math.abs(diffEndShift), Math.abs(diffStartShift));

									diffEndShift = ((diffEndShift + diffStartShift) > 0) ? 0 : diffEndShift + diffStartShift;
									diffStartShift = ((diffEndShift + diffStartShift) > 0) ? diffEndShift + diffStartShift : 0;

								}

								Logger.debug("Minuti recuperati: %s", restoredMin);

								// check if the difference between the worked hours in the shift periods are less than 2 hours (new rules for shift)
								int twoHoursinMinutes = 2 * 60;
								int teoreticShiftMinutes = DateUtility.getDifferenceBetweenLocalTime(startShift, startLunchTime) + DateUtility.getDifferenceBetweenLocalTime(endLunchTime, endShift);
								int lackOfMinutes = teoreticShiftMinutes - workingMinutes;

								Logger.debug("teoreticShiftMinutes = %s workingMinutes = %s lackOfMinutes = %s", teoreticShiftMinutes, workingMinutes, lackOfMinutes);
								lackOfMinutes -= restoredMin;
								workingMinutes+= restoredMin;

								Logger.debug("Minuti mancanti con recupero: %s - Minuti lavorati con recupero: %s", lackOfMinutes, workingMinutes);

								String lackOfTime = calcStringShiftHoursFromMinutes(lackOfMinutes);
								String workedTime = calcStringShiftHoursFromMinutes(workingMinutes);
								String label;

								if (lackOfMinutes > twoHoursinMinutes) {

									Logger.info("Il turno di %s %s nel giorno %s non √® stato completato - timbrature: %s ", person.name, person.surname, personDay.get().date, stampings);

									badStampingDays = (inconsistentAbsenceTable.contains(person, thMissingTime)) ? inconsistentAbsenceTable.get(person, thMissingTime) : new ArrayList<String>();
									badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings).concat("(").concat(workedTime).concat(" ore lavorate)"));
									inconsistentAbsenceTable.put(person, thMissingTime, badStampingDays);

									Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thMissingTime, inconsistentAbsenceTable.get(person, thMissingTime));
								} else if (lackOfMinutes != 0) {
									label = (inTolleranceLimit) ? thIncompleteTime : thWarnStampings;

									Logger.info("Il turno di %s %s nel giorno %s non e'stato completato per meno di 2 ore (%s minuti (%s)) - CONTROLLARE PERMESSO timbrature: %s", person.name, person.surname, personDay.get().date, lackOfMinutes, lackOfTime, stampings);
									Logger.info("Timbrature nella tolleranza dei 15 min. = %s", inTolleranceLimit);

									badStampingDays = (inconsistentAbsenceTable.contains(person, label)) ? inconsistentAbsenceTable.get(person, label) : new ArrayList<String>();
									badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings).concat("(").concat(lackOfTime).concat(" ore mancanti)"));

									lackOfTimes = (inconsistentAbsenceTable.contains(person, thLackTime)) ? inconsistentAbsenceTable.get(person, thLackTime): new ArrayList<String>();
									lackOfTimes.add(Integer.toString(lackOfMinutes));
									inconsistentAbsenceTable.put(person, label, badStampingDays);
									inconsistentAbsenceTable.put(person, thLackTime, lackOfTimes);

									Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thLackTime, inconsistentAbsenceTable.get(person, thLackTime));
								}
							}
						} // fine controllo coppie timbrature
					} // fine if esistenza timbrature
				} // fine se non √® giorno festivo

				// check for absences
				if (!personDay.get().absences.isEmpty()) {
					Logger.debug("E assente!!!! Esamino le assenze(%s)", personDay.get().absences.size());
					for (Absence absence : personDay.get().absences) {
						if (absence.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay) {

							if (absence.absenceType.code.equals("92")) {
								Logger.info("Il turno di %s %s √® coincidente con una missione il giorno %s", person.name, person.surname, personShiftDay.date);

								absenceDays = (inconsistentAbsenceTable.contains(person, thMissions)) ? inconsistentAbsenceTable.get(person, thMissions) : new ArrayList<String>();							
								absenceDays.add(personShiftDay.date.toString("dd MMM"));							
								inconsistentAbsenceTable.put(person, thMissions, absenceDays);


								absenceDays = (inconsistentAbsenceTable.contains(person, thAbsences)) ? inconsistentAbsenceTable.get(person, thAbsences) : new ArrayList<String>();							
								absenceDays.add(personShiftDay.date.toString("dd MMM"));							
								inconsistentAbsenceTable.put(person, thAbsences, absenceDays);
							}
						}
					}
				}	
			} // fine personDay != null
		}
		//return inconsistentAbsenceTable;
	}
}

