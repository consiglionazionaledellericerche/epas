package it.cnr.iit.epas;

import helpers.ModelQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.mysema.query.jpa.JPQLQuery;

import dao.PersonDayDao;


public class CompetenceUtility {

	public static String codFr = "207";    						// codice dei turni feriali
	public static String codFs = "208";							// codice dei turni festivi
	public static String codShift = "T1";						// codice dei turni
	
	public static String thNoStampings = "Mancata timbratura";  // nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
	public static String thMissingTime = "Orario insufficiente";// 
	public static String thAbsences = "Assenza";				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	public static String thMissions = "Missione";				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	public static String thBadStampings = "Timbratura errata";  // nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni
	public static String thWarnStampings = "Orario da motivare";  // nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni ma 
																// con le ore lavorate che discostano meno di 2 ore
	
	public static String thDays = "Num. di giorni";				// nome della colonna per i giorni di turno svolti mensilmente da una persona
	public static String thReqHour = "Num. di ore spettanti";	// nome della colonna per le ore di turno svolte mensilmente da una persona
	public static String thAppHour = "Num. di ore approvate";		// nome della colonna per le ore di turno approvate mensilmente per una persona
	public static String thLackTime = "Ore mancanti";
	public static String thExceededMin = "Minuti accumulati";	// Nome dela colonna contenente i minuti accumulati di turno da riportare nei mesi successivi

	
	
	/*
	 * @author arianna
	 * Calcola le ore di turno dai giorni (days)
	 * resto = (days%2 == 0) ? 0 : 0.5
	 * ore = days*6 + (int)(days/2) + resto;	
	 */
	public static BigDecimal calcShiftHoursFromDays (int days) {
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
	public static BigDecimal calcDecimalShiftHoursFromMinutes (int minutes) {
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
	public static String calcStringShiftHoursFromMinutes (int minutes) {
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
	public static void updateExceedeMinInCompetenceTable () {
		int year = 2015;
		int month = 3;
		
		int exceddedMin;
		
		CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codShift).first();
		List<Person> personList;
		
		
		QCompetence com = QCompetence.competence;
		QPerson person = QPerson.person;
		QPersonShiftShiftType personShiftShiftType = QPersonShiftShiftType.personShiftShiftType;
		
		personList = ModelQuery.queryFactory().from(person)
			.join(person.personShift.personShiftShiftTypes, personShiftShiftType)
			.where(
				personShiftShiftType.shiftType.type.in(ImmutableSet.of("A", "B"))
				).list(person);
		
			
		//		personList = JPA.em().createQuery("SELECT p FROM PersonShiftShiftType psst JOIN psst.personShift ps JOIN ps.person p WHERE psst.shiftType.type = :type AND (psst.beginDate IS NULL OR psst.beginDate <= now()) AND (psst.endDate IS NULL OR psst.endDate >= now())")
		//				.setParameter("type", type)
		//				.getResultList(); 
				//personList = PersonDao.getPersonForShift(type);
			
				for (Person p: personList) {
					
					final JPQLQuery query = ModelQuery.queryFactory().query();
		
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
	
	public static Competence getLastCompetence(Person person, int year, int month, CompetenceCode competenceCode) {
		QCompetence com2 = QCompetence.competence;
		QCompetenceCode comCode = QCompetenceCode.competenceCode;
		// prendo la competenza del mese precedente
		return ModelQuery.queryFactory()
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
	public static void countPersonsShiftsDays(List<PersonShiftDay> personShiftDays, Table<Person, String, Integer> personShiftSumDaysForTypes) {

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
	public static Table<Person, String, List<String>> getReperibilityInconsistenceAbsenceTable(List<PersonReperibilityDay> personReperibilityDays, LocalDate startDate, LocalDate endDate) {
    	// for each person contains days with absences and no-stamping  matching the reperibility days 
    	Table<Person, String, List<String>> inconsistentAbsenceTable = TreeBasedTable.<Person, String, List<String>>create();
    	
    	// lista dei giorni di assenza e mancata timbratura
		List<String> noStampingDays = new ArrayList<String>();
		List<String> absenceDays = new ArrayList<String>();
		
		for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
			Person person = personReperibilityDay.personReperibility.person;
			
			
			//check for the absence inconsistencies
			//------------------------------------------
				
			Optional<PersonDay> personDay = PersonDayDao.getSinglePersonDayStatic(person, personReperibilityDay.date);
			//PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personReperibilityDay.date, person).first();
			
			// if there are no events and it is not an holiday -> error
			if (!personDay.isPresent() & LocalDate.now().isAfter(personReperibilityDay.date)) {
				//if (!person.isHoliday(personReperibilityDay.date)) {
				 if(!PersonManager.isHoliday(person, personReperibilityDay.date)){
					 Logger.info("La reperibilità di %s %s è incompatibile con la sua mancata timbratura nel giorno %s", person.name, person.surname, personReperibilityDay.date);


					 noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
					 noStampingDays.add(personReperibilityDay.date.toString("dd MMM"));
					 inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);	
					   }
					 } else if (LocalDate.now().isAfter(personReperibilityDay.date)) {
					 // check for the stampings in working days
					 //if (!person.isHoliday(personReperibilityDay.date) && personDay.get().stampings.isEmpty()) {
					 if (!PersonManager.isHoliday(person, personReperibilityDay.date) && personDay.get().stampings.isEmpty()){
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
	public static void getShiftInconsistencyTimestampTable(List<PersonShiftDay> personShiftDays, Table<Person, String, List<String>> inconsistentAbsenceTable) {
		
		// tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
		//Table<Person, String, List<String>> inconsistentAbsenceTable = TreeBasedTable.<Person, String, List<String>>create();
		
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
			
			//Logger.debug("Turno flessibile: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);
			
			//check for the absence inconsistencies
			//------------------------------------------
			Optional<PersonDay> personDay = PersonDayDao.getSinglePersonDayStatic(person, personShiftDay.date);
			//PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personShiftDay.date, person).first();
			//Logger.debug("Prelevo il personDay %s per la persona %s - personDay=%s", personShiftDay.date, person, personDay);
			
			// if there are no events and it is not an holiday -> error
			if (!personDay.isPresent()) {	

				if ( !PersonManager.isHoliday(person,personShiftDay.date) && personShiftDay.date.isBefore(LocalDate.now())) {
					Logger.info("Il turno di %s %s √® incompatibile con la sua mancata timbratura nel giorno %s (personDay == null)", person.name, person.surname, personShiftDay.date);
					
					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
					noStampingDays.add(personShiftDay.date.toString("dd MMM"));
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
					
					Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
				}
			} else {

				// check for the stampings in working days
				if (!PersonManager.isHoliday(person,personShiftDay.date) & LocalDate.now().isAfter(personShiftDay.date)) {
					
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
						
						// legge le coppie di timbrature valide 
						//FIXME injettare il PersonDayManager
						List<PairStamping> pairStampings = new PersonDayManager().getValidPairStamping(personDay.get().stampings);
						
						// se c'√® una timbratura guardo se √® entro il turno
						if ((personDay.get().stampings.size() == 1) &&
							((personDay.get().stampings.get(0).isIn() && personDay.get().stampings.get(0).date.toLocalTime().isAfter(roundedStartShift)) || 
							(personDay.get().stampings.get(0).isOut() && personDay.get().stampings.get(0).date.toLocalTime().isBefore(roundedStartShift)) )) {

							
							String stamp = (personDay.get().stampings.get(0).isIn()) ? personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm").concat("- **:**") : "- **:**".concat(personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm"));
								
							badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
							badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stamp));
							inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);
							
							Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));
								
						// se √® vuota => manca qualche timbratura		
						} else if (pairStampings.isEmpty()) {							
								
							Logger.info("Il turno di %s %s √® incompatibile con la sue  timbrature disallineate nel giorno %s", person.name, person.surname, personDay.get().date);
							
							badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
							badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> timbrature disaccoppiate"));
							inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);
							
							Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thBadStampings, inconsistentAbsenceTable.get(person, thBadStampings));
						
						// controlla che le coppie di timbrature coprano
						// gli intervalli di prima e dopo pranzo
						} else {
							
							boolean okBeforeLunch = false;  // intervallo prima di pranzo coperto
							boolean okAfterLunch = false;	// intervallo dopo pranzo coperto
							
							String strStamp = "";
										
							// per ogni coppia di timbrature
							for (PairStamping pairStamping : pairStampings) {
								
								strStamp = strStamp.concat(pairStamping.in.date.toString("HH:mm")).concat(" - ").concat(pairStamping.out.date.toString("HH:mm")).concat("  ");
								
								// controlla se interseca l'intervallo prima e dopo pranzo del turno
								// controlla se interseca la coppia di timbrature
								 if (pairStamping.out.date.toLocalTime().isAfter(roundedStartShift) && pairStamping.in.date.toLocalTime().isBefore(roundedStartLunchTime)) {
									 okBeforeLunch = true;
								 }
								 if (pairStamping.out.date.toLocalTime().isAfter(roundedEndLunchTime) && pairStamping.in.date.toLocalTime().isBefore(roundedEndShift)) {
									 okAfterLunch = true;
								 }
							}
							
							// se non ha coperto interamente i due intervalli, controlla se il tempo mancante al
							// completamento del turno sia <= 2 ore
							if (!okBeforeLunch || !okAfterLunch) {
					
								int workingMinutes = 0;
								LocalTime lowLimit;
								LocalTime upLimit;
								String stampings = "";
								
								Logger.info("Il turno di %s  nel giorno %s non √® stato completato o c'√® stata una uscita fuori pausa pranzo - orario %s", person, personDay.get().date, strStamp);
								Logger.debug("Esamino le coppie di timbrature");

								// calcolare il lowlimit ecc senza i 15 min!!!!!
								//------------------------------------------------
								
								// per ogni coppia di timbrature
								for (PairStamping pairStamping : pairStampings) {
									
									Logger.debug("pairStamping.in.date = %s  pairStamping.out.date = %s", pairStamping.in.date.toLocalTime(), pairStamping.out.date.toLocalTime());
									
									// conta le ore lavorate in turno prima di pranzo
									 if ((pairStamping.in.date.toLocalTime().isBefore(startShift) && pairStamping.out.date.toLocalTime().isAfter(startShift)) ||
									 (pairStamping.in.date.toLocalTime().isAfter(startShift))) {
										 
										 lowLimit = (pairStamping.in.date.toLocalTime().isBefore(startShift)) ? startShift : pairStamping.in.date.toLocalTime();
										 
										 upLimit = (pairStamping.out.date.toLocalTime().isBefore(startLunchTime)) ? pairStamping.out.date.toLocalTime() : startLunchTime;
										 
										 workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
										 Logger.debug("N.1 - lowLimit=%s upLimit=%s workingMinutes=%s", lowLimit, upLimit, workingMinutes);
										 
									 }
									 if ((pairStamping.in.date.toLocalTime().isBefore(endLunchTime) && pairStamping.out.date.toLocalTime().isAfter(endLunchTime)) ||
											 (pairStamping.in.date.toLocalTime().isAfter(endLunchTime)))  {
										 lowLimit = (pairStamping.in.date.toLocalTime().isBefore(endLunchTime)) ? endLunchTime : pairStamping.in.date.toLocalTime();
										 upLimit = (pairStamping.out.date.toLocalTime().isBefore(endShift)) ? pairStamping.out.date.toLocalTime() : endShift;
										 
										 
										 workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
										 
										 Logger.debug("N.2 - lowLimit=%s upLimit=%s workingMinutes=%s", lowLimit, upLimit, workingMinutes);
									 }		
									 
									 // write the pair stamping								
									 stampings = stampings.concat(pairStamping.in.date.toString("HH:mm")).concat("-").concat(pairStamping.out.date.toString("HH:mm")).concat("  ");									 
								}

								
								stampings.concat("<br />");
								
								// check if the difference between the worked hours in the shift periods are less than 2 hours (new rules for shift)
								int twoHoursinMinutes = 2 * 60;
								int teoreticShiftMinutes = DateUtility.getDifferenceBetweenLocalTime(startShift, startLunchTime) + DateUtility.getDifferenceBetweenLocalTime(endLunchTime, endShift);
								int lackOfMinutes = teoreticShiftMinutes - workingMinutes;
								
								Logger.debug("teoreticShiftMinutes = %s workingMinutes = %s lackOfMinutes = %s", teoreticShiftMinutes, workingMinutes, lackOfMinutes);
								String lackOfTime = calcStringShiftHoursFromMinutes(lackOfMinutes);
								String workedTime = calcStringShiftHoursFromMinutes(workingMinutes);
								
								if (lackOfMinutes > twoHoursinMinutes) {
								
									Logger.info("Il turno di %s %s nel giorno %s non √® stato completato - timbrature: %s ", person.name, person.surname, personDay.get().date, stampings);
									
									badStampingDays = (inconsistentAbsenceTable.contains(person, thMissingTime)) ? inconsistentAbsenceTable.get(person, thMissingTime) : new ArrayList<String>();
									badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings).concat("(").concat(workedTime).concat(" ore lavorate)"));
									inconsistentAbsenceTable.put(person, thMissingTime, badStampingDays);
									
									Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thMissingTime, inconsistentAbsenceTable.get(person, thMissingTime));
								} else {
									
									Logger.info("Il turno di %s %s nel giorno %s non √® stato completato per meno di 2 ore (%s minuti (%s)) - CONTROLLARE PERMESSO timbrature: %s", person.name, person.surname, personDay.get().date, lackOfMinutes, lackOfTime, stampings);
									
									badStampingDays = (inconsistentAbsenceTable.contains(person, thWarnStampings)) ? inconsistentAbsenceTable.get(person, thWarnStampings) : new ArrayList<String>();
									badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings).concat("(").concat(lackOfTime).concat(" ore mancanti)"));
									lackOfTimes.add(Integer.toString(lackOfMinutes));
									inconsistentAbsenceTable.put(person, thWarnStampings, badStampingDays);
									inconsistentAbsenceTable.put(person, thLackTime, lackOfTimes);
									
									Logger.debug("Nuovo inconsistentAbsenceTable(%s, %s) = %s", person, thLackTime, inconsistentAbsenceTable.get(person, thLackTime));
								}
							}
						} // fine controllo coppie timbrature
					} // fine if esistenza timbrature
				} // fine se non √® giorno festivo
				
				// check for absences
				if (!personDay.get().absences.isEmpty()) {
					for (Absence absence : personDay.get().absences) {
						if (absence.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay) {
							
							if (absence.absenceType.code.equals("92")) {
								Logger.info("Il turno di %s %s √® coincidente con una missione il giorno %s", person.name, person.surname, personShiftDay.date);
								
								absenceDays = (inconsistentAbsenceTable.contains(person, thMissions)) ? inconsistentAbsenceTable.get(person, thMissions) : new ArrayList<String>();							
								absenceDays.add(personShiftDay.date.toString("dd MMM"));							
								inconsistentAbsenceTable.put(person, thMissions, absenceDays);
								
							} else {
								Logger.info("Il turno di %s %s √® incompatibile con la sua assenza nel giorno %s", person.name, person.surname, personShiftDay.date);
			
								
								/*absenceDays = (inconsistentAbsence.contains(personName, thAbsences)) ? inconsistentAbsence.get(personName, thAbsences) : new ArrayList<Integer>();							
								absenceDays.add(personShiftDay.date.getDayOfMonth());							
								inconsistentAbsence.put(personName, thAbsences, absenceDays);*/
								
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




