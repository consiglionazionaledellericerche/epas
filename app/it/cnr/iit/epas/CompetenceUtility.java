package it.cnr.iit.epas;

import helpers.ModelQuery;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import manager.PersonDayManager.PairStamping;
import manager.PersonManager;
import models.Absence;
import models.CertificatedData;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonReperibilityDay;
import models.PersonShiftDay;
import models.enumerate.JustifiedTimeAtWork;
import models.enumerate.ShiftSlot;
import models.query.QCompetence;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.Period;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.mysema.query.jpa.JPQLQuery;

import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDayDao;
import dao.PersonMonthRecapDao;
import play.Logger;


public class CompetenceUtility {

	public static String codFr = "207";    						// codice dei turni feriali
	public static String codFs = "208";							// codice dei turni festivi
	public static String codShift = "T1";						// codice dei turni
	
	public static String thNoStampings = "Mancata timbratura";  // nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
	public static String thAbsences = "Assenza";				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	public static String thMissions = "Missione";				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	public static String thBadStampings = "Timbratura errata";  // nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni
	public static String thWarnStampings = "Orario da motivare";  // nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni ma 
																// con le ore lavorate che discostano meno di 2 ore
	public static String thLackTime = "Tempo mancante";
	
	/*
	 * @author arianna
	 * Salva il riepilogo dei giorni di reperibilità e le reason di un certo mesenel database.
	 * La reason contiene la descrizione dei periodi di reperibilità effettuati
	 * in quel mese
	 */
	public static int updateDBReperibilityCompetences(List<PersonReperibilityDay> personReperibilityDays, int year, int month) {
		
		// single person reperibility period in a month
		class PRP {
			int inizio;
			int fine;
			String mese;
			String tipo;
			
			public PRP (int inizio, int fine, String mese, String tipo) {
				this.inizio = inizio;
				this.fine = fine;
				this.mese = mese;
				this.tipo = tipo;
			}
			
			@Override
			public String toString() {
				return (this.inizio != this.fine) ? String.format("%d-%d/%s", inizio, fine, mese) : String.format("%d/%s", inizio, mese);
			}
		}
		
		// single person reperibility day
		class PRD {
			int giorno;
			String tipo;
			
			public PRD (int giorno, String tipo) {
				this.giorno = giorno;
				this.tipo  = tipo;
			}
		}
		
		
		int numSavedCompetences = 0;
		
		
		// get the Competence code for the reperibility working or non-working days  
		CompetenceCode competenceCodeFS = CompetenceCodeDao.getCompetenceCodeByCode(codFs);
		//CompetenceCode competenceCodeFS = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codFs).first();
		CompetenceCode competenceCodeFR = CompetenceCodeDao.getCompetenceCodeByCode(codFr);
		//CompetenceCode competenceCodeFR = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codFr).first();
		
		// read the first day of the month and the short month description
		LocalDate firstOfMonth = new LocalDate(year, month, 1);
		String shortMonth = firstOfMonth.monthOfYear().getAsShortText();
				
		// for each person contains the reperibility days (fs/fr) in the month
		ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder(); 
		Table<Person, Integer, String> reperibilityMonth = null;
				
		// build the reperibility calendar with the reperibility days
		for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
			Person person = personReperibilityDay.personReperibility.person;
			
			// record the reperibility day
			//builder.put(person, personReperibilityDay.date.getDayOfMonth(), person.isHoliday(personReperibilityDay.date) ? codFs : codFr);
			builder.put(person, personReperibilityDay.date.getDayOfMonth(), PersonManager.isHoliday(person, personReperibilityDay.date) ? codFs : codFr);

		}
		reperibilityMonth = builder.build();
		
		
		// for each person in the reperibilitymonth conunts the reperibility day
		// divided by fs and fr and build a string description of the rep periods
		for (Person person: reperibilityMonth.rowKeySet()) {
			
			// lista dei periodi di reperibilità ferieali e festivi
			List<PRP> fsPeriods = new ArrayList<PRP>();
			List<PRP> frPeriods = new ArrayList<PRP>();
		
			PRD previousPersonReperibilityDay = null;
			PRP currentPersonReperibilityPeriod = null;
			
			// number of working and non-working days
			int NumOfFsDays = 0;
			int NumOfFrDays = 0;
			
			// for each day of month
			for (Integer dayOfMonth: reperibilityMonth.columnKeySet()) {
				
				// counts the reperibility days fs and fr
				if (reperibilityMonth.contains(person, dayOfMonth)) { 
					if (reperibilityMonth.get(person, dayOfMonth).equals(codFr)) {
						NumOfFrDays++;
					} else {
						NumOfFsDays++;
					}
				} 
				
				// create the reperibility periods strings divided by fs and fr
				if (reperibilityMonth.contains(person, dayOfMonth)) {
					if ((previousPersonReperibilityDay == null) || 
						(!reperibilityMonth.get(person, dayOfMonth).equals(previousPersonReperibilityDay.tipo)) ||
						((dayOfMonth - 1) != previousPersonReperibilityDay.giorno)) { 		
							currentPersonReperibilityPeriod = new PRP (dayOfMonth, dayOfMonth, shortMonth, reperibilityMonth.get(person, dayOfMonth));
					
							if (currentPersonReperibilityPeriod.tipo == codFs) {
								fsPeriods.add(currentPersonReperibilityPeriod);
							} else {
								frPeriods.add(currentPersonReperibilityPeriod);
							}
					}
					else {
						currentPersonReperibilityPeriod.fine = dayOfMonth;
					}
					previousPersonReperibilityDay = new PRD (dayOfMonth, reperibilityMonth.get(person, dayOfMonth));
				}
			}
			
			Logger.debug("NumOfFsDays=%d fsPeriods=%s - NumOfFrDays=%d frPeriods=%s ", NumOfFsDays, fsPeriods, NumOfFrDays, frPeriods);
			
			// build the Fs and Fr reasons
			String fsReason = "";
			String frReason = "";			
			for (PRP prp: fsPeriods) {
				fsReason = fsReason.concat(prp.toString().concat(" "));
			}
			for (PRP prp: frPeriods) {
				frReason = frReason.concat(prp.toString().concat(" "));
			}
			Logger.debug("ReasonFS=%s ReasonFR=%s", fsReason, frReason);
			
			Logger.debug("Cerca Competence FS per person=%s id=%d, year=%d, month=%d competenceCodeId=%d", person.surname, person.id, year, month, competenceCodeFS.id);
			// save the FS reperibility competences in the DB
			Optional<Competence> FsCompetence = CompetenceDao.getCompetence(person, year, month, competenceCodeFS);
//			Competence FsCompetence = Competence.find("SELECT c FROM Competence c WHERE c.person = ? AND c.year = ? AND c.month = ? AND c.competenceCode = ?", 
//					person, year, month, competenceCodeFS).first();
			
			if (FsCompetence.isPresent()) {
				Logger.debug("Trovato competenza FS =%s", FsCompetence);
				// update the requested hours
				FsCompetence.get().setValueApproved(NumOfFsDays, fsReason);
				FsCompetence.get().save();
				
				Logger.debug("Aggiornata competenza %s", FsCompetence);
				numSavedCompetences++;
			} else {
				Logger.debug("Trovato nessuna competenza FS");
				// insert a new competence with the requested hours and reason
				Competence competence = new Competence(person, competenceCodeFS, year, month, NumOfFsDays, fsReason);
				competence.save();
				
				Logger.debug("Salvata competenza %s", competence);
				numSavedCompetences++;
			}
			
			Logger.debug("Cerca Competence FR per person=%s id=%d, year=%d, month=%d competenceCodeId=%d", person.surname, person.id, year, month, competenceCodeFR.id);
			// save the FR reperibility competences in the DB
			Optional<Competence> FrCompetence = CompetenceDao.getCompetence(person, year, month, competenceCodeFR);
//			Competence FrCompetence = Competence.find("SELECT c FROM Competence c WHERE c.person = ? AND c.year = ? AND c.month = ? AND c.competenceCode = ?", 
//					person, year, month, competenceCodeFR).first();
			
			if (FrCompetence.isPresent()) {
				// update the requested hours
				Logger.debug("Trovato competenza FR =%s", FsCompetence);
				FrCompetence.get().setValueApproved(NumOfFrDays, frReason);
				FrCompetence.get().save();
				
				Logger.debug("Aggiornata competenza %s", FrCompetence);
				numSavedCompetences++;
				
			} else {
				// insert a new competence with the requested hours an reason
				Logger.debug("Trovato nessuna competenza FR");
				Competence competence = new Competence(person, competenceCodeFR, year, month, NumOfFrDays, fsReason);
				competence.save();
				Logger.debug("Salvata competenza %s", competence);
				numSavedCompetences++;
			}
		}
		
		// return the number of saved competences
		return numSavedCompetences;
	}

	
	/*
	 * @author arianna
	 * Calcola le ore di turno dai giorni (days)
	 * resto = (days%2 == 0) ? 0 : 0.5
	 * ore = days*6 + (int)(days/2) + resto;	
	 */
	public static BigDecimal calcShiftHoursFromDays (int days) {
		BigDecimal decDays = new BigDecimal(days);
		BigDecimal due = new BigDecimal("2");
		
		Logger.debug("In calcShiftHoursFromDays days = %s", days);
		
		BigDecimal minutes = (days%2 == 0) ? BigDecimal.ZERO : new BigDecimal(5); 
		
		Logger.debug("In calcShiftHoursFromDays minutes = %s", minutes);
		
		BigDecimal hours = decDays.multiply(new BigDecimal(6)).add(decDays.divide(due, RoundingMode.HALF_DOWN)).add(minutes);	
		
		Logger.debug("In calcShiftHoursFromDays hours = %s", hours);
		
		return hours;
	}
	
	/*
	 * @author arianna
	 * Calcola i giorni di reperibilità giorni (days)
	 * dalle ore 
	 * days = 2 * ore / 13	
	 */
	public static int calcShiftDaysFromHour (BigDecimal hours) {
		BigDecimal due = new BigDecimal("2");
		BigDecimal tredici = new BigDecimal("13");
		
		int days = due.multiply(hours).divide(tredici).intValue();

		return days;
	}
	
	/*
	 * @author arianna
	 * Calcola il LocalTime dal numero dei minuti 
	 * che compongono l'orario
	 */
	public static String calcLocalTimeFromMinutes (int minutes) {
		int hours;
		int mins;
		
		if (minutes < 60) {
			hours = 0;
			mins = minutes;
		} else {
			BigDecimal sessanta = new BigDecimal("60");
			BigDecimal decMinutes = new BigDecimal(minutes);
			
			Logger.debug("decMinutes = %s", decMinutes);
			
			BigDecimal decHours = decMinutes.divide(sessanta, RoundingMode.HALF_UP);
			
			hours = decHours.intValue();
			
			Logger.debug("hours = %s decHours = %s", hours, decHours);
		
			mins = minutes - (hours * 60);
			Logger.debug("mins = %s", mins);
		}
		
		return  Integer.toString(hours).concat(":").concat(Integer.toString(mins));
	}
	
	/*
	 * @author arianna
	 * Calcola le ore di turno da approvare date quelle richieste.
	 * Poichè le ore approvate devono essere un numero intero e quelle
	 * calcolate direttamente dai giorni di turno possono essere decimali,
	 * le ore approvate devono essere arrotondate per eccesso o per difetto a seconda dell'ultimo
	 * arrotondamento effettuato in modo che questi vengano alternati 
	 */
	public static int calcShiftValueApproved(Person person, int year, int month, BigDecimal requestedHours) {
		int valueApproved = 0;
		
		// get the Competence code for the ordinary shift  
		CompetenceCode competenceCode = CompetenceCodeDao.getCompetenceCodeByCode(codShift);
		//CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codShift).first();
				
		final QCompetence com = new QCompetence("competence");
		final JPQLQuery query = ModelQuery.queryFactory().query();
		final Competence myCompetence = query
				.from(com)
				.where(
						com.person.eq(person)
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
		 
		 if (myCompetence == null) {
			 // we are at the first case, so the person has its fist 0.5 hour to accumulate
			 Logger.debug("myCompetence is null");
			 valueApproved = requestedHours.setScale(0, RoundingMode.DOWN).intValue();
		 } else if (myCompetence.valueRequested.setScale(0, RoundingMode.UP).intValue() <= myCompetence.valueApproved) {
			 Logger.debug("La query sulle competenze ha trovato %s e myCompetence.valueRequested.ROUND_CEILING=%s <= myCompetence.valueApproved=%d", myCompetence.toString(), myCompetence.valueRequested.ROUND_CEILING, myCompetence.valueApproved);
			 // Last rounding was on ceiling, so we round to floor
			 valueApproved = requestedHours.setScale(0, RoundingMode.DOWN).intValue();
		 } else {
			 Logger.debug("La query sulle competenze ha trovato %s", myCompetence.toString());
			 // we round to ceiling
			 valueApproved = requestedHours.setScale(0, RoundingMode.UP).intValue();
		 }
		
		
		Logger.debug("La calcShiftValueApproved ha preso il requsestHour=%s e restituisce %s", requestedHours, valueApproved);
			
		return valueApproved;
	}

	
	/*
	 * @author arianna
	 * Salva le ore di turno di un certo mese nelle competenze.
	 * Per ogni persona riceve le ore effettive di turno svolte, calcola quelle da approvare e le salva 
	 * nei campi valueRequested e valueApproved rispettvamente
	 */
	public static List<Competence> updateDBShiftCompetences(Table<Person, String, BigDecimal> personsShiftHours, int year, int month) {

		List<Competence> savedCompetences = new ArrayList<Competence>();
		
		// get the Competence code for the ordinary shift  
		CompetenceCode competenceCode = CompetenceCodeDao.getCompetenceCodeByCode(codShift);
		//CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codShift).first();
	
		
		// for each person
		for (Person person: personsShiftHours.rowKeySet()) {
			
			Logger.debug("Esamino person= %s %s", person.surname, person.name);
			
			BigDecimal numOfShiftHours = new BigDecimal(0);  // number of real shift hours
			int calcApproved = 0;							 // number of approved shift hours
			
			// for each shift type
			for (String shiftType: personsShiftHours.columnKeySet()) {
				
				// counts the shift days 
				if (personsShiftHours.contains(person, shiftType)) { 						
					numOfShiftHours = numOfShiftHours.add(personsShiftHours.get(person, shiftType));
				} 
			}
		
			// save the FS reperibility competences in the DB
			Optional<Competence> shiftCompetence = CompetenceDao.getCompetence(person, year, month, competenceCode);
//			Competence shiftCompetence = Competence.find("SELECT c FROM Competence c WHERE c.person = ? AND c.year = ? AND c.month = ? AND c.competenceCode = ?", 
//					person, year, month, competenceCode).first();
			
			//BigDecimal roundNumOfShiftHours = numOfShiftHours.setScale(0, RoundingMode.FLOOR);
			//Boolean res = roundNumOfShiftHours.compareTo(numOfShiftHours) == 0;
		
			//Logger.debug("Calcola la calcApproved -> numOfShiftHours.abs()=%s  != numOfShiftHours=%s  comparison= %s", roundNumOfShiftHours, numOfShiftHours, res);
			
			calcApproved = (numOfShiftHours.setScale(0, RoundingMode.FLOOR).compareTo(numOfShiftHours) == 0) ? numOfShiftHours.intValue() : calcShiftValueApproved(person, year, month, numOfShiftHours);
			
			//Competence appCompetence = new Competence(person, competenceCode, year, month);
			
			if (shiftCompetence.isPresent()) {
				// update the requested hours
				//int calcApproved1 = (shiftCompetence.valueApproved != 0) ? shiftCompetence.valueApproved : calcApproved;
				
				// check if the competence has been processed to be sent to Rome
				// and and this case we don't change the valueApproved
				CertificatedData certData = PersonMonthRecapDao.getCertificatedDataByPersonMonthAndYear(person, month, year);
				//CertificatedData certData = CertificatedData.find("SELECT cd FROM CertificatedData cd WHERE cd.person = ? AND cd.year = ? AND cd.month = ?", person, year, month).first();
				
				//Logger.debug("certData=%s isOK=%s competencesSent=%s", certData, certData.isOk, certData.competencesSent);
				
				Logger.debug("certData=%s", certData);
				
				int calcApproved1 = (certData != null && certData.isOk && (certData.competencesSent != null)) ? shiftCompetence.get().valueApproved : calcApproved;
				//Logger.debug("competenza è inviata = %s vecchia valueApproved=%d, calcolata=%d salvata=%d", certData.isOk, shiftCompetence.valueApproved, calcApproved, calcApproved1);
				
				shiftCompetence.get().setValueApproved(calcApproved1);
				shiftCompetence.get().setValueRequested(numOfShiftHours);
				shiftCompetence.get().save();
				
				Logger.debug("Aggiornata competenza di %s %s: valueApproved=%s, valueRequested=%s, calcApproved=%s", shiftCompetence.get().person.surname, shiftCompetence.get().person.name, shiftCompetence.get().valueApproved, shiftCompetence.get().valueRequested, calcApproved);
				
				//---------------				
				//appCompetence.setValueApproved(calcApproved1);
				//appCompetence.setValueRequested(numOfShiftHours);
				//savedCompetences.add(appCompetence);
				
				savedCompetences.add(shiftCompetence.get());
			} else {
				// insert a new competence with the requested hours an reason
				Competence competence = new Competence(person, competenceCode, year, month);
				competence.setValueApproved(calcApproved);
				competence.setValueRequested(numOfShiftHours);
				competence.save();
				
				//appCompetence.setValueApproved(calcApproved);
				//appCompetence.setValueRequested(numOfShiftHours);
				//savedCompetences.add(appCompetence);
				
				savedCompetences.add(competence);
				
				Logger.debug("Salvata competenza %s", shiftCompetence);
			}
		}
		
		// return the number of saved competences
		return savedCompetences;
	}
	
	
	/*
	 * @author arianna
	 * Restituisce la tabella che contiene, per ogni persona nella lista dei turni presi come parametro,
	 * e per ogni tipo di turno trovato, il numero di giorni di turno effettuati
	 */
	public static Table<Person, String, Integer> getShiftCompetences(List<PersonShiftDay> personShiftDays) {

		// for each person contains the shift days (A/B) in the month
		ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder(); 
		Table<Person, Integer, String> shiftMonth = null;
		
		// for each person contains the number of days of working shift divided by shift's type 
    	Table<Person, String, Integer> shiftsSumDays = TreeBasedTable.<Person, String, Integer>create();
		
		for (PersonShiftDay personShiftDay : personShiftDays) {
			Person person = personShiftDay.personShift.person;
				
			// registro il turno della persona per quel giorno
			//------------------------------------------------------
			builder.put(person, personShiftDay.date.getDayOfMonth(), personShiftDay.shiftType.type);				
			//Logger.debug("Registro il turno %s di %s per il giorno %d", personShiftDay.shiftType.type, person, personShiftDay.date.getDayOfMonth());			
		}
		shiftMonth = builder.build();
		
		// for each person
		for (Person person: shiftMonth.rowKeySet()) {
			Logger.debug("conto i turni di %s", person);
			
			// number of competence
			int shiftNum = 0;
			
			for (int day: shiftMonth.columnKeySet()) {
				
				if (shiftMonth.contains(person, day)) {
				
					String shift = shiftMonth.get(person, day);
					shiftNum = (shiftsSumDays.contains(person, shift)) ? shiftsSumDays.get(person, shift) : 0;
					shiftNum++;
					shiftsSumDays.put(person, shift, shiftNum);	
				}
			}

		}
		
		// return the number of saved competences
		return shiftsSumDays;
	}
	
	
	/**
	 * @author arianna
	 * crea una tabella con le eventuali inconsistenze tra le timbrature dei reperibili di un certo tipo e i
	 * turni di reperibilità svolti in un determinato periodo di tempo
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
				
			Optional<PersonDay> personDay = PersonDayDao.getSinglePersonDay(person, personReperibilityDay.date);
			//PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personReperibilityDay.date, person).first();
			//Logger.info("Prelevo il personDay %s per la persona %s - personDay=%s", personReperibilityDay.date, person, personDay);
			
			// if there are no events and it is not an holiday -> error
			if (!personDay.isPresent() & LocalDate.now().isAfter(personReperibilityDay.date)) {
				//if (!person.isHoliday(personReperibilityDay.date)) {
				if(PersonManager.isHoliday(person, personReperibilityDay.date)){
					Logger.info("La reperibilità di %s %s è incompatibile con la sua mancata timbratura nel giorno %s", person.name, person.surname, personReperibilityDay.date);
				
					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
					noStampingDays.add(personReperibilityDay.date.toString("dd MMM"));
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);	
				}
			} else if (LocalDate.now().isAfter(personReperibilityDay.date)) {
				// check for the stampings in working days
				//if (!person.isHoliday(personReperibilityDay.date) && personDay.get().stampings.isEmpty()) {
				if(PersonManager.isHoliday(person, personReperibilityDay.date) && personDay.get().stampings.isEmpty()){
					Logger.info("La reperibilità di %s %s è incompatibile con la sua mancata timbratura nel giorno %s", person.name, person.surname, personDay.get().date);
					
					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();	
					noStampingDays.add(personReperibilityDay.date.toString("dd MMM"));			
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
					
				}
				
				// check for absences
				if (!personDay.get().absences.isEmpty()) {
					for (Absence absence : personDay.get().absences) {
						if (absence.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay) {
							Logger.info("La reperibilità di %s %s è incompatibile con la sua assenza nel giorno %s", person.name, person.surname, personReperibilityDay.date);
							
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
	public static Table<Person, String, List<String>> getShiftInconsistencyTimestampTable(List<PersonShiftDay> personShiftDays) {
		
		// tabella per registrare le assenze e le timbrature inconsistenti con i turni trovati
		Table<Person, String, List<String>> inconsistentAbsenceTable = TreeBasedTable.<Person, String, List<String>>create();
		
		// lista dei giorni di assenza nel mese, mancata timbratura e timbratura inconsistente
		//List<Integer> noStampingDays = new ArrayList<Integer>();
		List<String> noStampingDays = new ArrayList<String>();
		
		//List<Integer> badStampingDays = new ArrayList<Integer>();
		List<String> badStampingDays = new ArrayList<String>();
		
		//List<Integer> absenceDays = new ArrayList<Integer>();
		List<String> absenceDays = new ArrayList<String>();
		
		List<String> lackOfTimes = new ArrayList<String>();
		
		for (PersonShiftDay personShiftDay : personShiftDays) {
			Person person = personShiftDay.personShift.person;
				
			// legge l'orario di inizio e fine turno da rispettare (mattina o pomeriggio)
			LocalTime startShift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.startMorning : personShiftDay.shiftType.shiftTimeTable.startAfternoon;
			LocalTime endShift = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.endMorning : personShiftDay.shiftType.shiftTimeTable.endAfternoon;
			
			// legge l'orario di inizio e fine pausa pranzo
			LocalTime startLunchTime = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.startMorningLunchTime : personShiftDay.shiftType.shiftTimeTable.startAfternoonLunchTime;
			LocalTime endLunchTime = (personShiftDay.shiftSlot.equals(ShiftSlot.MORNING)) ? personShiftDay.shiftType.shiftTimeTable.endMorningLunchTime : personShiftDay.shiftType.shiftTimeTable.endAfternoonLunchTime;
			
			//Logger.debug("Turno: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);
			
			// Add flexibility (15 min.) due to the new rules (PROT. N. 0008692 del 2/12/2014)
			startShift = startShift.plusMinutes(15);
			startLunchTime = startLunchTime.minusMinutes(15);
			endLunchTime = endLunchTime.plusMinutes(15);
			endShift = endShift.minusMinutes(15);
			
			//Logger.debug("Turno flessibile: %s-%s  %s-%s", startShift, startLunchTime, endLunchTime, endShift);
			
			//check for the absence inconsistencies
			//------------------------------------------
			Optional<PersonDay> personDay = PersonDayDao.getSinglePersonDay(person, personShiftDay.date);
			//PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personShiftDay.date, person).first();
			//Logger.debug("Prelevo il personDay %s per la persona %s - personDay=%s", personShiftDay.date, person, personDay);
			
			// if there are no events and it is not an holiday -> error
			if (!personDay.isPresent()) {	
				

				if ( !PersonManager.isHoliday(person,personShiftDay.date) && personShiftDay.date.isBefore(LocalDate.now())) {
					Logger.info("Il turno di %s %s è incompatibile con la sua mancata timbratura nel giorno %s (personDay == null)", person.name, person.surname, personShiftDay.date);

					
					/*noStampingDays = (inconsistentAbsence.contains(personName, thNoStampings)) ? inconsistentAbsence.get(personName, thNoStampings) : new ArrayList<Integer>();
					noStampingDays.add(personShiftDay.date.getDayOfMonth());
					inconsistentAbsence.put(personName, thNoStampings, noStampingDays);*/
					
					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
					noStampingDays.add(personShiftDay.date.toString("dd MMM"));
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
				}
			} else {

				// check for the stampings in working days
				if (!PersonManager.isHoliday(person,personShiftDay.date) & LocalDate.now().isAfter(personShiftDay.date)) {
					
					// check no stampings
					//-----------------------------
					if (personDay.get().stampings.isEmpty()) {
						Logger.info("Il turno di %s %s è incompatibile con la sue mancate timbrature nel giorno %s", person.name, person.surname, personDay.get().date);
						
						/*noStampingDays = (inconsistentAbsence.contains(personName, thNoStampings)) ? inconsistentAbsence.get(personName, thNoStampings) : new ArrayList<Integer>();
						noStampingDays.add(personShiftDay.date.getDayOfMonth());
						inconsistentAbsence.put(personName, thNoStampings, noStampingDays);*/
						
						noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
						//Logger.debug("leggo da inconsistentAbsenceTable(person=%s, thNoStampings=%s) %s", person, thNoStampings, noStampingDays);
						
						noStampingDays.add(personShiftDay.date.toString("dd MMM"));
						inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);
						
						Logger.debug("Nuovo inconsistentAbsenceTable(person=%s, thNoStampings=%s) = %s", person, thNoStampings, inconsistentAbsenceTable.get(person, thNoStampings));
					} else {
						// check consistent stampings
						//-----------------------------
						
						// legge le coppie di timbrature valide 
						List<PairStamping> pairStampings = PairStamping.getValidPairStamping(personDay.get().stampings);
						
						// se c'è una timbratura guardo se è entro il turno

						if ((personDay.get().stampings.size() == 1) &&
							((personDay.get().stampings.get(0).isIn() && personDay.get().stampings.get(0).date.toLocalTime().isAfter(startShift)) || 
							(personDay.get().stampings.get(0).isOut() && personDay.get().stampings.get(0).date.toLocalTime().isBefore(startShift)) )) {

							
							String stamp = (personDay.get().stampings.get(0).isIn()) ? personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm").concat("- **:**") : "- **:**".concat(personDay.get().stampings.get(0).date.toLocalTime().toString("HH:mm"));
								
							badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
							badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stamp));
							inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);
								
						// se è vuota -> manca qualche timbratura		
						} else if (pairStampings.isEmpty()) {							
								
							Logger.info("Il turno di %s %s è incompatibile con la sue  timbrature disallineate nel giorno %s", person.name, person.surname, personDay.get().date);
							
							/*badStampingDays = (inconsistentAbsence.contains(personName, thBadStampings)) ? inconsistentAbsence.get(personName, thBadStampings) : new ArrayList<Integer>();
							badStampingDays.add(personShiftDay.date.getDayOfMonth());
							inconsistentAbsence.put(personName, thBadStampings, badStampingDays);*/
							
							badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
							badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> timbrature disaccoppiate"));
							inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);
						
						// controlla che le coppie di timbrature coprano
						// gli intervalli di prima e dopo pranzo
						} else {
							
							boolean okBeforeLunch = false;  // intervallo prima di pranzo coperto
							boolean okAfterLunch = false;	// intervallo dopo pranzo coperto
										
							// per ogni coppia di timbrature
							for (PairStamping pairStamping : pairStampings) {
								
								// controlla se interseca l'intervallo prima e dopo pranzo del turno
								// controlla se interseca la coppia di timbrature
								 if (pairStamping.out.date.toLocalTime().isAfter(startShift) && pairStamping.in.date.toLocalTime().isBefore(startLunchTime)) {
									 okBeforeLunch = true;
								 }
								 if (pairStamping.out.date.toLocalTime().isAfter(endLunchTime) && pairStamping.in.date.toLocalTime().isBefore(endShift)) {
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
								

								Logger.info("Il turno di %s %s nel giorno %s non è stato completato o c'è stata una uscita fuori pausa pranzo - entrata alle %s, uscita alle %s - " +
										"pausa pranzo da %s a %s", person.name, person.surname, personDay.get().date, pairStampings.get(0).in.date.toString("HH:mm"), pairStampings.get(1).out.date.toString("HH:mm"), pairStampings.get(0).out.date.toString("HH:mm"), pairStampings.get(1).in.date.toString("HH:mm"));
							
								/*badStampingDays = (inconsistentAbsence.contains(personName, thBadStampings)) ? inconsistentAbsence.get(personName, thBadStampings) : new ArrayList<Integer>();
								badStampingDays.add(personShiftDay.date.getDayOfMonth());
								inconsistentAbsence.put(personName, thBadStampings, badStampingDays);*/

								// per ogni coppia di timbrature
								for (PairStamping pairStamping : pairStampings) {
									
									Logger.debug("pairStamping.in.date = %s  pairStamping.out.date = %s", pairStamping.in.date.toLocalTime(), pairStamping.out.date.toLocalTime());
									
									// conta le ore lavorate in turno prima di pranzo
									 if ((pairStamping.in.date.toLocalTime().isBefore(startShift) && pairStamping.out.date.toLocalTime().isAfter(startShift)) ||
									 (pairStamping.in.date.toLocalTime().isAfter(startShift))) {
										 
										 lowLimit = (pairStamping.in.date.toLocalTime().isBefore(startShift)) ? startShift : pairStamping.in.date.toLocalTime();
										 upLimit = (pairStamping.out.date.toLocalTime().isBefore(startLunchTime)) ? pairStamping.out.date.toLocalTime() : startLunchTime;
										 
										 workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
										 
									 }
									 if ((pairStamping.in.date.toLocalTime().isBefore(endLunchTime) && pairStamping.out.date.toLocalTime().isAfter(endLunchTime)) ||
											 (pairStamping.in.date.toLocalTime().isAfter(endLunchTime)))  {
										 lowLimit = (pairStamping.in.date.toLocalTime().isBefore(endLunchTime)) ? endLunchTime : pairStamping.in.date.toLocalTime();
										 upLimit = (pairStamping.out.date.toLocalTime().isBefore(endShift)) ? pairStamping.out.date.toLocalTime() : endShift;
										 
										 
										 workingMinutes += DateUtility.getDifferenceBetweenLocalTime(lowLimit, upLimit);
										 
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
								String lackOfTime = calcLocalTimeFromMinutes(lackOfMinutes);
								
								
								if (lackOfMinutes > twoHoursinMinutes) {
								
									Logger.info("Il turno di %s %s nel giorno %s non è stato completato - timbrature: %s ", person.name, person.surname, personDay.get().date, stampings);
								
									/*badStampingDays = (inconsistentAbsence.contains(personName, thBadStampings)) ? inconsistentAbsence.get(personName, thBadStampings) : new ArrayList<Integer>();
									badStampingDays.add(personShiftDay.date.getDayOfMonth());
									inconsistentAbsence.put(personName, thBadStampings, badStampingDays);*/
									
									badStampingDays = (inconsistentAbsenceTable.contains(person, thBadStampings)) ? inconsistentAbsenceTable.get(person, thBadStampings) : new ArrayList<String>();
									badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings));
									inconsistentAbsenceTable.put(person, thBadStampings, badStampingDays);
								} else {
									
									Logger.info("Il turno di %s %s nel giorno %s non è stato completato per meno di 2 ore (%s minuti (%s)) - CONTROLLARE PERMESSO timbrature: %s", person.name, person.surname, personDay.get().date, lackOfMinutes, lackOfTime, stampings);
									
									badStampingDays = (inconsistentAbsenceTable.contains(person, thWarnStampings)) ? inconsistentAbsenceTable.get(person, thWarnStampings) : new ArrayList<String>();
									badStampingDays.add(personShiftDay.date.toString("dd MMM").concat(" -> ").concat(stampings).concat("(").concat(lackOfTime).concat(" ore mancanti)"));
									lackOfTimes.add(lackOfTime.toString());
									inconsistentAbsenceTable.put(person, thWarnStampings, badStampingDays);
									inconsistentAbsenceTable.put(person, thLackTime, lackOfTimes);
								}
							}
						} // fine controllo coppie timbrature
					} // fine if esistenza timbrature
				} // fine se non è giorno festivo
				
				// check for absences
				if (!personDay.get().absences.isEmpty()) {
					for (Absence absence : personDay.get().absences) {
						if (absence.absenceType.justifiedTimeAtWork == JustifiedTimeAtWork.AllDay) {
							
							if (absence.absenceType.code.equals("92")) {
								Logger.info("Il turno di %s %s è coincidente con una missione il giorno %s", person.name, person.surname, personShiftDay.date);
								
								absenceDays = (inconsistentAbsenceTable.contains(person, thMissions)) ? inconsistentAbsenceTable.get(person, thMissions) : new ArrayList<String>();							
								absenceDays.add(personShiftDay.date.toString("dd MMM"));							
								inconsistentAbsenceTable.put(person, thMissions, absenceDays);
								
							} else {
								Logger.info("Il turno di %s %s è incompatibile con la sua assenza nel giorno %s", person.name, person.surname, personShiftDay.date);
			
								
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
		
		
		return inconsistentAbsenceTable;
	}
	
}




