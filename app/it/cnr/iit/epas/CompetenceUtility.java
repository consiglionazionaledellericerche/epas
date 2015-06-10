package it.cnr.iit.epas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

import dao.PersonDayDao;


public class CompetenceUtility {

	@Inject
	public CompetenceUtility(JPQLQueryFactory queryFactory,
			PersonDayManager personDayManager, PersonDayDao personDayDao) {

		this.queryFactory = queryFactory;
	}

	private final JPQLQueryFactory queryFactory;

	public static String codFr = "207";    						// codice dei turni feriali
	public static String codFs = "208";							// codice dei turni festivi
	public static String codShift = "T1";						// codice dei turni

	public static String thNoStampings = Messages.get("PDFReport.thNoStampings");  // nome della colonna per i giorni di mancata timbratura della tabella delle inconsistenze
	public static String thMissingTime = Messages.get("PDFReport.thMissingTime");// 
	public static String thAbsences = Messages.get("PDFReport.thAbsences");				// nome della colonna per i giorni di assenza della tabella delle inconsistenze
	
	public static String thBadStampings = Messages.get("PDFReport.thBadStampings");  // nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni
	public static String thWarnStampings = Messages.get("PDFReport.thWarnStampings");  // nome della colonna per i giorni con timbratura fuori dalle fasce orarie dei turni ma 
	// con le ore lavorate che discostano meno di 2 ore
	public static String thDays = Messages.get("PDFReport.thDays");				// nome della colonna per i giorni di turno svolti mensilmente da una persona
	public static String thReqHour = Messages.get("PDFReport.thReqHour");	// nome della colonna per le ore di turno svolte mensilmente da una persona
	public static String thAppHour = Messages.get("PDFReport.thAppHour");		// nome della colonna per le ore di turno approvate mensilmente per una persona


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

	




}