package manager;

import helpers.BadRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import models.Absence;
import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonDay;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.enumerate.JustifiedTimeAtWork;
import models.exports.AbsenceReperibilityPeriod;
import models.exports.ReperibilityPeriod;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

import play.Logger;
import play.i18n.Messages;

import com.google.common.base.Optional;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.CompetenceDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonReperibilityDayDao;

/**
 * 
 * @author Arianna e Dario
 *
 */
public class ReperibilityManager {

	@Inject
	public ReperibilityManager(CompetenceDao competenceDao,
			AbsenceDao absenceDao, PersonDao personDao,
			PersonReperibilityDayDao personReperibilityDayDao,
			PersonDayManager personDayManager, PersonDayDao personDayDao, CompetenceCodeDao competenceCodeDao) {
		this.competenceDao = competenceDao;
		this.absenceDao = absenceDao;
		this.personDao = personDao;
		this.personReperibilityDayDao = personReperibilityDayDao;
		this.personDayManager = personDayManager;
		this.personDayDao = personDayDao;
		this.competenceCodeDao = competenceCodeDao;
	}

	private final CompetenceDao competenceDao;
	private final AbsenceDao absenceDao;
	private final PersonDao personDao;
	private final PersonReperibilityDayDao personReperibilityDayDao;
	private final PersonDayManager personDayManager;
	private final PersonDayDao personDayDao;
	private final CompetenceCodeDao competenceCodeDao;

	// Label della tabella delle inconsistenze delle reperibilità con le timbrature
	public static String thNoStampings = Messages.get("PDFReport.thNoStampings");  // nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
	public static String thAbsences = Messages.get("PDFReport.thAbsences");				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	

	private final static String codFr = "207";    						// codice dei turni feriali
	private final static String codFs = "208";							// codice dei turni festivi



	/**
	 * 
	 * @param reperibilityDays la lista dei giorni di reperibilità effettuati
	 * @param prt il tipo di reperibilità
	 * @return la lista dei periodi di reperibilità effettuati
	 */
	public List<ReperibilityPeriod> getPersonReperibilityPeriods(List<PersonReperibilityDay> reperibilityDays, PersonReperibilityType prt){

		List<ReperibilityPeriod> reperibilityPeriods = new ArrayList<ReperibilityPeriod>();
		ReperibilityPeriod reperibilityPeriod = null;

		for (PersonReperibilityDay prd : reperibilityDays) {
			//L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di reperibilità non consecutivi. 
			if (reperibilityPeriod == null || !reperibilityPeriod.person.equals(prd.personReperibility.person) || !reperibilityPeriod.end.plusDays(1).equals(prd.date)) {
				reperibilityPeriod = new ReperibilityPeriod(prd.personReperibility.person, prd.date, prd.date, prt);
				reperibilityPeriods.add(reperibilityPeriod);
				Logger.trace("Creato nuovo reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			} else {
				reperibilityPeriod.end = prd.date;
				Logger.trace("Aggiornato reperibilityPeriod, person=%s, start=%s, end=%s", reperibilityPeriod.person, reperibilityPeriod.start, reperibilityPeriod.end);
			}
		}
		return reperibilityPeriods;
	}


	/*
	 * @param absencePersonReperibilityDays	- lista dei giorni di assenza in reperibilita'
	 * @param reperibilityType				- tipo di reperibilita'
	 * @return 								- lista di periodi di assenza in reperibilità
	 */
	public List<AbsenceReperibilityPeriod> getAbsentReperibilityPeriodsFromAbsentReperibilityDays(List<Absence> absencePersonReperibilityDays, PersonReperibilityType reperibilityType) {
		List<AbsenceReperibilityPeriod> absenceReperibilityPeriods = new ArrayList<AbsenceReperibilityPeriod>();
		AbsenceReperibilityPeriod absenceReperibilityPeriod = null;

		for (Absence abs : absencePersonReperibilityDays) {
			//L'ultima parte dell'if serve per il caso in cui la stessa persona ha due periodi di reperibilità non consecutivi. 
			if (absenceReperibilityPeriod == null || !absenceReperibilityPeriod.person.equals(abs.personDay.person) || !absenceReperibilityPeriod.end.plusDays(1).equals(abs.personDay.date)) {
				absenceReperibilityPeriod = new AbsenceReperibilityPeriod(abs.personDay.person, abs.personDay.date, abs.personDay.date, reperibilityType);
				absenceReperibilityPeriods.add(absenceReperibilityPeriod);
				Logger.trace("Creato nuovo absenceReperibilityPeriod, person=%s, start=%s, end=%s", absenceReperibilityPeriod.person, absenceReperibilityPeriod.start, absenceReperibilityPeriod.end);
			} else {
				absenceReperibilityPeriod.end = abs.personDay.date;
				Logger.trace("Aggiornato reperibilityPeriod, person=%s, start=%s, end=%s", absenceReperibilityPeriod.person, absenceReperibilityPeriod.start, absenceReperibilityPeriod.end);
			}
		}

		return absenceReperibilityPeriods;
	}

	public Set<Integer> savePersonReperibilityDaysFromReperibilityPeriods(PersonReperibilityType reperibilityType, Integer year, Integer month, List<ReperibilityPeriod> reperibilityPeriods) {


		//Il mese e l'anno ci servono per "azzerare" eventuale giorni di reperibilità rimasti vuoti
		LocalDate monthToManage = new LocalDate(year, month, 1);

		//Conterrà i giorni del mese che devono essere attribuiti a qualche reperibile 
		Set<Integer> daysOfMonthToDelete = new HashSet<Integer>();	
		for (int i = 1 ; i <= monthToManage.dayOfMonth().withMaximumValue().getDayOfMonth(); i++) {
			daysOfMonthToDelete.add(i);
		}
		Logger.trace("Lista dei giorni del mese = %s", daysOfMonthToDelete);

		LocalDate day = null;

		for (ReperibilityPeriod reperibilityPeriod : reperibilityPeriods) {

			reperibilityPeriod.reperibilityType = reperibilityType;

			if (reperibilityPeriod.start.isAfter(reperibilityPeriod.end)) {
				throw new IllegalArgumentException(
						String.format("ReperibilityPeriod person.id = %s has start date %s after end date %s", reperibilityPeriod.person.id, reperibilityPeriod.start, reperibilityPeriod.end));
			}

			day = reperibilityPeriod.start;
			while (day.isBefore(reperibilityPeriod.end.plusDays(1))) {

				//La persona deve essere tra i reperibili 
				if (reperibilityPeriod.person.reperibility == null) {
					throw new IllegalArgumentException(
							String.format("Person %s is not a reperible person", reperibilityPeriod.person));
				}

				//Se la persona è assente in questo giorno non può essere reperibile 
				//if (Absence.find("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date = ? and pd.person = ?", day, reperibilityPeriod.person).fetch().size() > 0) {
				if(absenceDao.getAbsencesInPeriod(Optional.fromNullable(reperibilityPeriod.person), day, Optional.<LocalDate>absent(), false).size() > 0){
					String msg = String.format("La reperibilità di %s %s è incompatibile con la sua assenza nel giorno %s", reperibilityPeriod.person.name, reperibilityPeriod.person.surname, day);
					BadRequest.badRequest(msg);
				}

				//Salvataggio del giorno di reperibilità
				//Se c'è un giorno di reperibilità già presente viene sostituito, altrimenti viene creato un PersonReperibilityDay nuovo
				PersonReperibilityDay personReperibilityDay = personReperibilityDayDao.getPersonReperibilityDayByTypeAndDate(reperibilityPeriod.reperibilityType, day); 
				//		PersonReperibilityDay.find("reperibilityType = ? AND date = ?", reperibilityPeriod.reperibilityType, day).first();

				if (personReperibilityDay == null) {
					personReperibilityDay = new PersonReperibilityDay();
					Logger.trace("Creo un nuovo personReperibilityDay per person = %s, day = %s, reperibilityDay = %s", reperibilityPeriod.person, day, reperibilityPeriod.reperibilityType );
				} else {
					Logger.trace("Aggiorno il personReperibilityDay = %s", personReperibilityDay);
				}

				personReperibilityDay.personReperibility = reperibilityPeriod.person.reperibility;
				personReperibilityDay.date = day;
				personReperibilityDay.reperibilityType = reperibilityPeriod.reperibilityType;
				//XXX: manca ancora l'impostazione dell'eventuale holidayDay, ovvero se si tratta di un giorno festivo

				personReperibilityDay.save();

				//Questo giorno è stato assegnato
				daysOfMonthToDelete.remove(day.getDayOfMonth());

				Logger.info("Inserito o aggiornata reperibilità di tipo %s, assegnata a %s per il giorno %s", personReperibilityDay.reperibilityType, personReperibilityDay.personReperibility.person, personReperibilityDay.date);

				day = day.plusDays(1);
			}
		}

		return daysOfMonthToDelete;
	}	


	/*
	 * Cancella dal DB i giorni di reperibilità di un certo tipo associati ad un determinato mese ed anno 
	 * 
	 * @param reperibilityType 	- tipo delle reperibilità da cancellare
	 * @param year				- anno di appartenenza dei giorni di reperibilità da cancellare
	 * @param month				- mese di appartenenza dei giorni di reperibilità da cancellare
	 * @param repDaysToRemove	- lista dei giorni da cancellare
	 * @return					- numero di giorni di reperibilità cancellati
	 */
	public int deleteReperibilityDaysFromMonth(PersonReperibilityType reperibilityType, int year, int month, Set<Integer> repDaysToRemove) {

		long cancelled = 0;

		for (int dayToRemove : repDaysToRemove) {
			LocalDate dateToRemove = new LocalDate(year, month, dayToRemove);
			Logger.trace("Eseguo la cancellazione del giorno %s", dateToRemove);

			//		int cancelled = JPA.em().createQuery("DELETE FROM PersonReperibilityDay WHERE reperibilityType = :reperibilityType AND date = :dateToRemove)")
			//		.setParameter("reperibilityType", reperibilityType)
			//		.setParameter("dateToRemove", dateToRemove)
			//		.executeUpdate();
			//		if (cancelled == 1) {
			//			Logger.info("Rimossa reperibilità di tipo %s del giorno %s", reperibilityType, dateToRemove);
			//		}

			cancelled = personReperibilityDayDao.deletePersonReperibilityDay(reperibilityType, dateToRemove);
			if(cancelled == 1){
				Logger.info("Rimossa reperibilità di tipo %s del giorno %s", reperibilityType, dateToRemove);
			}
		}

		return (int) cancelled;
	}

	/*
	 * Change the person between two reperibility periods of a certain type
	 * @param reperibilityType	- tipo di reperibilità a cui appartengono i periodi
	 * @param periods			- lista di due periodi di repribilità da scambiare
	 * @return					- boolean che indica se il cambio è stato effettuato
	 */
	public Boolean changeTwoReperibilityPeriods(PersonReperibilityType reperibilityType, List<ReperibilityPeriod> periods) {

		LocalDate reqStartDay = null;
		LocalDate subStartDay = null;
		LocalDate reqEndDay = null;
		LocalDate subEndDay = null;
		Person requestor = null;
		Person substitute = null;

		Boolean repChanged = true;

		for (ReperibilityPeriod reperibilityPeriod : periods) {

			reperibilityPeriod.reperibilityType = reperibilityType;

			if (reperibilityPeriod.start.isAfter(reperibilityPeriod.end)) {
				throw new IllegalArgumentException(
						String.format("ReperibilityPeriod person.id = %s has start date %s after end date %s", reperibilityPeriod.person.id, reperibilityPeriod.start, reperibilityPeriod.end));
			}

			//La persona deve essere tra i reperibili 
			if (reperibilityPeriod.person.reperibility == null) {
				throw new IllegalArgumentException(
						String.format("Person %s is not a reperible person", reperibilityPeriod.person));
			}

			// intervallo del richiedente
			if (repChanged) {
				reqStartDay = reperibilityPeriod.start;
				reqEndDay = reperibilityPeriod.end;
				requestor = reperibilityPeriod.person;

				Logger.debug("RICHIEDENTE: requestor=%s inizio=%s, fine=%s", requestor, reqStartDay, reqEndDay);
				repChanged = !repChanged;
			} else {
				subStartDay = reperibilityPeriod.start;
				subEndDay = reperibilityPeriod.end;
				substitute = reperibilityPeriod.person;

				Logger.debug("SOSTITUTO: substitute=%s inizio=%s, fine=%s", substitute, subStartDay, subEndDay);

				int day = 1000*60*60*24;

				// controlla che il numero dei giorni da scambiare coincida
				if (((reqEndDay.toDate().getTime() - reqStartDay.toDate().getTime())/day) != ((subEndDay.toDate().getTime() - subStartDay.toDate().getTime())/day)) {
					throw new IllegalArgumentException(
							String.format("Different number of days between two intervals!"));
				}

				Logger.debug("Aggiorno i giorni del richiedente");
				changePersonInReperibilityPeriod(reperibilityPeriod.reperibilityType, reqStartDay, reqEndDay, requestor, substitute);

				Logger.debug("Aggiorno i giorni del sostituto");
				changePersonInReperibilityPeriod(reperibilityPeriod.reperibilityType, subStartDay, subEndDay, substitute, requestor);

				repChanged = !repChanged;
			}	
		}

		return repChanged;
	}


	/*
	 * @param reperibilityType 	- type of a reperibility
	 * @param startDay			- data di inizio del periodo di reperibilità
	 * @param endDay			- data di fine del periodo di reperibilità
	 * @param requestor			- persona che ha richiesto il cambio reperibilità
	 * @param substitute		- persona che va a sostituire il richiedente nei giorni di reperibilità
	 */

	public void changePersonInReperibilityPeriod(PersonReperibilityType reperibilityType,  LocalDate startDay, LocalDate endDay, Person requestor, Person substitute) {

		LocalDate start = startDay;

		// Esegue il cambio sui giorni del richiedente

		while (start.isBefore(endDay.plusDays(1))) {
			
			//Se il sostituto è in ferie questo giorno non può essere reperibile 
			//if (Absence.find("SELECT a FROM Absence a JOIN a.personDay pd WHERE pd.date = ? and pd.person = ?", reqStartDay, substitute).fetch().size() > 0) {
			if(absenceDao.getAbsencesInPeriod(Optional.fromNullable(substitute), start, Optional.<LocalDate>absent(), false).size() > 0){
				throw new IllegalArgumentException(
						String.format("ReperibilityPeriod substitute.id %s is not compatible with a Absence in the same day %s", substitute, start));
			}

			// cambia le reperibilità mettendo quelle del sostituto al posto di quelle del richiedente
			PersonReperibilityDay personReperibilityDay = personReperibilityDayDao.getPersonReperibilityDayByTypeAndDate(reperibilityType, start); 
			//	PersonReperibilityDay.find("reperibilityType = ? AND date = ?", reperibilityPeriod.reperibilityType, reqStartDay).first();

			Logger.debug("trovato personReperibilityDay.personReperibility.person=%s e reqstart=%s", personReperibilityDay.personReperibility.person, startDay);

			if (personReperibilityDay == null || (personReperibilityDay.personReperibility.person != requestor)) {
				throw new IllegalArgumentException(
						String.format("Impossible to offer the day %s because is not associated to the right requestor %s", start, requestor));
			} else {
				Logger.debug("Aggiorno il personReperibilityDay = %s", personReperibilityDay);
				//PersonReperibility substituteRep = PersonReperibility.find("SELECT pr FROM PersonReperibility pr WHERE pr.person = ? AND pr.personReperibilityType = ?", substitute, reperibilityType).first();
				PersonReperibility substituteRep = personReperibilityDayDao.getPersonReperibilityByPersonAndType(substitute, reperibilityType);
				personReperibilityDay.personReperibility = substituteRep;

				Logger.info("scambio reperibilità del richiedente con personReperibilityDay.personReperibility.person=%s e reqstart=%s", personReperibilityDay.personReperibility.person, personReperibilityDay.date);
			}

			personReperibilityDay.save();

			Logger.info("Aggiornato PersonReperibilityDay del richiedente= %s", personReperibilityDay);

			start = start.plusDays(1);
		}

	}


	/*
	 * @param personReperibilityDays	- lista di giorni di reperibilità effettuati dai reperibili
	 * @return							- lista dei reperibili coinvolti nei giorni di reperibilità
	 * 									passati come parametro
	 */
	public List<Person> getPersonsFromReperibilityDays(List<PersonReperibilityDay> personReperibilityDays) {
		List<Person> personList = new ArrayList<Person>(); 

		for (PersonReperibilityDay prd : personReperibilityDays) {
			if (!personList.contains(prd.personReperibility.person)) {
				Logger.trace("inserisco il reperibile ", prd.personReperibility.person);
				personList.add(prd.personReperibility.person);
				Logger.trace("trovata person=%s", prd.personReperibility.person);
			}
		}

		return personList;
	}


	/*
	 * Costrisce il calendario delle reperibilità lavorate festive e feriali di un determinato anno
	 * 
	 * @param year 				- anno di riferimento del calendario
	 * @param reperibilityType	- tipo di reperibilità di cui costrire il calendario
	 * @return					- Lista di tabelle (una per ogni mese dell'anno) contenenti per ogni persona e giorno del mese
	 * 							l'indicazione se la persona ha effettuato un turno di reperibilità festivo (FS) o feriale (FR)
	 */
	public List<Table<Person, Integer, String>> buildYearlyReperibilityCalendar(int year, PersonReperibilityType reperibilityType) {

		List<Table<Person, Integer, String>> reperibilityMonths = new ArrayList<Table<Person, Integer, String>>();

		// for each month of the year
		for (int i = 1; i<=12; i++) {

			LocalDate firstOfMonth = new LocalDate(year, i, 1);

			List<PersonReperibilityDay> personReperibilityDays = 
					personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(firstOfMonth, firstOfMonth.dayOfMonth().withMaximumValue(), reperibilityType, Optional.<PersonReperibility>absent());

			// table associated to the current month of the year
			ImmutableTable.Builder<Person, Integer, String> builder = ImmutableTable.builder(); 
			Table<Person, Integer, String> reperibilityMonth = null;

			for (PersonReperibilityDay personReperibilityDay : personReperibilityDays) {
				Person person = personReperibilityDay.personReperibility.person;

				//builder.put(person, personReperibilityDay.date.getDayOfMonth(), person.isHoliday(personReperibilityDay.date) ? "FS" : "FR");
				builder.put(person, personReperibilityDay.date.getDayOfMonth(), personDayManager.isHoliday(person, personReperibilityDay.date) ? "FS" : "FR");
			}
			reperibilityMonth = builder.build();
			reperibilityMonths.add(reperibilityMonth);
		}

		return reperibilityMonths;
	}


	/*
	 * Costruisce il resoconto del numero dei giorni lavorati in reperibilità festiva e feriale
	 * sulla base di un calendario annuale passato come parametro
	 * 
	 * @param reperibilityMonths	- contiene una tabella per ogni mese di un anno che contiene per ogni 
	 * 								giorno e persona coinvolta se ha lavorato una reperibilità festiva o feriale
	 * @return						- tabella che contiene per ogni 
	 */
	public Table<Person, String, Integer> buildYearlyReperibilityReport(List<Table<Person, Integer, String>> reperibilityMonths) {
		int i = 0;
		Table<Person, String, Integer> reperibilitySumDays = HashBasedTable.<Person, String, Integer>create();

		// fro each month of the calendar
		for (Table<Person, Integer, String> reperibilityMonth: reperibilityMonths) {
			i++;
			// for each person counts the worked days in reperibility
			// divided by holiday or not
			for (Person person: reperibilityMonth.rowKeySet()) {
				for (Integer dayOfMonth: reperibilityMonth.columnKeySet()) {
					if (reperibilityMonth.contains(person, dayOfMonth)) { 
						//SemRep semRep = SemRep.valueOf( String.format("%s%dS", reperibilityMonth.get(person, dayOfMonth).toUpperCase(), (i<=6 ?1:2)));
						String col = String.format("%s%dS", reperibilityMonth.get(person, dayOfMonth).toUpperCase(), (i<=6 ? 1:2));

						int n = reperibilitySumDays.contains(person, col) ? reperibilitySumDays.get(person, col) + 1 : 1;
						reperibilitySumDays.put(person, col, Integer.valueOf(n));

					} else {

					}
				}
			}
		}

		return reperibilitySumDays;
	}


	/*
	 * @author arianna
	 * Salva il riepilogo dei giorni di reperibilit√† e le reason di un certo mesenel database.
	 * La reason contiene la descrizione dei periodi di reperibilit√† effettuati
	 * in quel mese
	 * 
	 * @param personReperibilityDays	- list of repribility days worked by persons
	 * @param year						- anno di riferimento dei giorni di reperibilità passati come parametro
	 * @param month						- mese di riferimento dei giorni di  reperibilità passati come parametro
	 * @return							- numero di competenze salvate nel DB
	 */
	public int updateDBReperibilityCompetences(List<PersonReperibilityDay> personReperibilityDays, int year, int month) {

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
		CompetenceCode competenceCodeFS = competenceCodeDao.getCompetenceCodeByCode(codFs);
		//CompetenceCode competenceCodeFS = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codFs).first();
		CompetenceCode competenceCodeFR = competenceCodeDao.getCompetenceCodeByCode(codFr);
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
			builder.put(person, personReperibilityDay.date.getDayOfMonth(), personDayManager.isHoliday(person, personReperibilityDay.date) ? codFs : codFr);

		}
		reperibilityMonth = builder.build();


		// for each person in the reperibilitymonth conunts the reperibility day
		// divided by fs and fr and build a string description of the rep periods
		for (Person person: reperibilityMonth.rowKeySet()) {

			// lista dei periodi di reperibilit√† ferieali e festivi
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
			Optional<Competence> FsCompetence = competenceDao.getCompetence(person, year, month, competenceCodeFS);
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
			Optional<Competence> FrCompetence = competenceDao.getCompetence(person, year, month, competenceCodeFR);
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
	 * @param  competenceList 		- lista di competenze 
	 * @param personsApprovedCompetence	- tabella contnente per ogni persona, coinvolta nelle competenze
	 * 								passate come eparametro, e per ogni tipo di competenza, il valore
	 * 								approvato per quella competenza
	 */
	public void updateReperibilityDaysReportFromCompetences(Table<Person, String, Integer> personsApprovedCompetence, List<Competence> competenceList) {
		for (Competence competence : competenceList) {	
			Logger.debug("Metto nella tabella competence = %s", competence.toString());
			personsApprovedCompetence.put(competence.person, competence.competenceCode.codeToPresence, competence.valueApproved);
		}
	}


	/*
	 * @param  competenceList 		- lista di competenze 
	 * @param reperibilityDateDays	- tabella contnente per ogni persona, coinvolta nelle competenze
	 * 								passate come eparametro, e per ogni tipo di competenza, il valore
	 * 								della reason 
	 */
	public void updateReperibilityDatesReportFromCompetences(Table<Person, String, List<String>> reperibilityDateDays, List<Competence> competenceList) {
		for (Competence competence : competenceList) {	
			Logger.debug("Metto nella tabella competence = %s", competence.toString());
			List <String> str = Arrays.asList(competence.reason.split(" "));
			reperibilityDateDays.put(competence.person, competence.competenceCode.codeToPresence, str);
		}
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

			Optional<PersonDay> personDay = personDayDao.getSinglePersonDay(person, personReperibilityDay.date);
			//PersonDay personDay = PersonDay.find("SELECT pd FROM PersonDay pd WHERE pd.date = ? and pd.person = ?", personReperibilityDay.date, person).first();

			// if there are no events and it is not an holiday -> error
			if (!personDay.isPresent() & LocalDate.now().isAfter(personReperibilityDay.date)) {
				//if (!person.isHoliday(personReperibilityDay.date)) {
				if(!personDayManager.isHoliday(person, personReperibilityDay.date)){
					Logger.info("La reperibilità di %s %s è incompatibile con la sua mancata timbratura nel giorno %s", person.name, person.surname, personReperibilityDay.date);


					noStampingDays = (inconsistentAbsenceTable.contains(person, thNoStampings)) ? inconsistentAbsenceTable.get(person, thNoStampings) : new ArrayList<String>();
					noStampingDays.add(personReperibilityDay.date.toString("dd MMM"));
					inconsistentAbsenceTable.put(person, thNoStampings, noStampingDays);	
				}
			} else if (LocalDate.now().isAfter(personReperibilityDay.date)) {
				// check for the stampings in working days
				//if (!person.isHoliday(personReperibilityDay.date) && personDay.get().stampings.isEmpty()) {
				if (!personDayManager.isHoliday(person, personReperibilityDay.date) && personDay.get().stampings.isEmpty()){
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
	 * Export the reperibility calendar in iCal for the person with id = personId with reperibility 
	 * of type 'type' for the 'year' year
	 * If the personId=0, it exports the calendar for all  the reperibility persons of type 'type'
	 */
	public Optional<Calendar> createCalendar(Long type, Long personId, int year) {
		Logger.debug("Crea iCal per l'anno %d della person con id = %d, reperibility type %s", year, personId, type);

		List<PersonReperibility> personsInTheCalList = new ArrayList<PersonReperibility>();

		// check for the parameter
		//---------------------------

		if (personId == 0) {
			// read the reperibility person 
			//List<PersonReperibility> personsReperibility = PersonReperibility.find("SELECT pr FROM PersonReperibility pr WHERE pr.personReperibilityType.id = ?", type).fetch();
			List<PersonReperibility> personsReperibility = personReperibilityDayDao.getPersonReperibilityByType(personReperibilityDayDao.getPersonReperibilityTypeById(type));
			if (personsReperibility.isEmpty()) {
				return Optional.<Calendar>absent();
			}
			personsInTheCalList = personsReperibility;
		} else {
			// read the reperibility person 
			//PersonReperibility personReperibility = PersonReperibility.find("SELECT pr FROM PersonReperibility pr WHERE pr.personReperibilityType.id = ? AND pr.person.id = ?", type, personId).first();
			PersonReperibility personReperibility = personReperibilityDayDao.getPersonReperibilityByPersonAndType(personDao.getPersonById(personId), personReperibilityDayDao.getPersonReperibilityTypeById(type));
			if (personReperibility == null) {
				return Optional.<Calendar>absent();
			}
			personsInTheCalList.add(personReperibility);
		}


		Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
		icsCalendar = createicsReperibilityCalendar(year, personsInTheCalList);

		Logger.debug("Find %s periodi di reperibilità.", icsCalendar.getComponents().size());
		Logger.debug("Crea iCal per l'anno %d della person con id = %d, reperibility type %s", year, personId, type);

		return Optional.of(icsCalendar);
	}
	
	
	public Calendar createicsReperibilityCalendar(int year, List<PersonReperibility> personsInTheCalList) {
		String eventLabel;

		// Create a calendar
		//---------------------------       
		Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
		icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
		icsCalendar.getProperties().add(CalScale.GREGORIAN);
		icsCalendar.getProperties().add(Version.VERSION_2_0);

		// read the person(0) reperibility days for the year
		//-------------------------------------------------
		LocalDate from = new LocalDate(year, 1, 1);
		LocalDate to = new LocalDate(year, 12, 31);


		for (PersonReperibility personReperibility: personsInTheCalList) {

			eventLabel = (personsInTheCalList.size() == 0) ? "Reperibilità Registro" : "Reperibilità ".concat(personReperibility.person.surname);
			List<PersonReperibilityDay> reperibilityDays = personReperibilityDayDao.getPersonReperibilityDayFromPeriodAndType(from, to, personReperibility.personReperibilityType, Optional.fromNullable(personReperibility));
			//		PersonReperibilityDay.find("SELECT prd FROM PersonReperibilityDay prd WHERE prd.date BETWEEN ? AND ? AND reperibilityType = ? AND personReperibility = ? ORDER BY prd.date", from, to, reperibilityType, personReperibility).fetch();

			Logger.debug("Reperibility find called from %s to %s, found %s reperibility days for person id = %s", from, to, reperibilityDays.size(), personReperibility.person.id);

			Date startDate = null;
			Date endDate = null;
			int sequence = 1;

			for (PersonReperibilityDay prd : reperibilityDays) {

				Date date = new Date(prd.date.toDateTimeAtStartOfDay(DateTimeZone.UTC).toDate().getTime());				
				Logger.trace("Data reperibilita' per %s, date=%s", prd.personReperibility.person.surname, date);

				if ( startDate == null) {
					Logger.trace("Nessun periodo, nuovo periodo: startDate=%s", date);

					startDate = endDate = date;
					sequence = 1;
					continue;
				} 

				if ( date.getTime() - endDate.getTime() > 86400*1000 ) {
					Logger.trace("Termine periodo: startDate=%s, sequence=%s", startDate, sequence);
					icsCalendar.getComponents().add(createICalEvent(startDate, sequence, eventLabel));
					startDate = endDate = date;
					sequence = 1;
					Logger.trace("Nuovo periodo: startDate=%s", date);
				} else {
					sequence++;
					endDate = date;
					Logger.trace("Allungamento periodo: startDate=%s, endDate=%s, sequence.new=%s", startDate, endDate, sequence);
				}

			}
		}	

		return icsCalendar;
	}


	private VEvent createICalEvent(Date startDate, int sequence, String eventLabel) {
		VEvent reperibilityPeriod = new VEvent(startDate, new Dur(sequence, 0, 0, 0), eventLabel);
		reperibilityPeriod.getProperties().add(new Uid(UUID.randomUUID().toString()));

		return reperibilityPeriod;
	}

}
