package dao;

import it.cnr.iit.epas.CompetenceUtility;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import models.Competence;
import models.CompetenceCode;
import models.Person;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftType;
import models.query.QCompetence;
import models.query.QPersonShiftDay;
import models.query.QShiftCancelled;
import models.query.QShiftType;

import org.joda.time.LocalDate;

import play.Logger;

import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.JPQLQueryFactory;

/**
 * 
 * @author dario
 *
 */
public class ShiftDao extends DaoBase{

	private final CompetenceCodeDao competenceCodeDao;
	private final CompetenceUtility competenceUtility;

	@Inject
	ShiftDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp
			,CompetenceCodeDao competenceCodeDao
			,CompetenceUtility competenceUtility) {
		super(queryFactory, emp);
		this.competenceCodeDao = competenceCodeDao;
		this.competenceUtility = competenceUtility;
	}

	private final static String codShift = "T1";

	/**
	 * 
	 * @param type
	 * @return lo shiftType corrispondente al tipo type passato come parametro
	 */
	public ShiftType getShiftTypeByType(String type){
		final QShiftType shiftType = QShiftType.shiftType;
		JPQLQuery query = getQueryFactory().from(shiftType).where(shiftType.type.eq(type));
		return query.singleResult(shiftType);
	}

	/**
	 * 
	 * @param begin
	 * @param to
	 * @param type
	 * @return la lista dei personShiftDay con ShiftType 'type' presenti nel periodo tra 'begin' e 'to'
	 */
	public List<PersonShiftDay> getPersonShiftDaysByPeriodAndType(LocalDate begin, LocalDate to, ShiftType type){
		final QPersonShiftDay psd = QPersonShiftDay.personShiftDay;
		JPQLQuery query = getQueryFactory().from(psd)
				.where(psd.date.between(begin, to)
						.and(psd.shiftType.eq(type))).orderBy(psd.shiftSlot.asc(), psd.date.asc());
		return query.list(psd);
	}

	/**
	 * 
	 * @param from
	 * @param to
	 * @param type
	 * @return la lista dei turni cancellati relativi al tipo 'type' nel periodo compreso tra 'from' e 'to'
	 */
	public List<ShiftCancelled> getShiftCancelledByPeriodAndType(LocalDate from, LocalDate to, ShiftType type){
		final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
		JPQLQuery query = getQueryFactory().from(sc).where(sc.date.between(from, to).and(sc.type.eq(type))).orderBy(sc.date.asc());
		return query.list(sc);
	}

	/**
	 * 
	 * @param day
	 * @param type
	 * @return il turno cancellato relativo al giorno 'day' e al tipo 'type' passati come parametro
	 */
	public ShiftCancelled getShiftCancelled(LocalDate day, ShiftType type){
		final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
		JPQLQuery query = getQueryFactory().from(sc).where(sc.date.eq(day).and(sc.type.eq(type)));
		return query.singleResult(sc);
	}

	/**
	 * 
	 * @param type
	 * @param day
	 * @return il quantitativo di shiftCancelled effettivamente cancellati
	 */
	public Long deleteShiftCancelled(ShiftType type, LocalDate day){
		final QShiftCancelled sc = QShiftCancelled.shiftCancelled;
		return getQueryFactory().delete(sc).where(sc.date.eq(day).and(sc.type.eq(type))).execute();
	}

	/*
	 * @author arianna
	 * Calcola le ore di turno da approvare date quelle richieste.
	 * Poich√® le ore approvate devono essere un numero intero e quelle
	 * calcolate direttamente dai giorni di turno possono essere decimali,
	 * le ore approvate devono essere arrotondate per eccesso o per difetto a seconda dell'ultimo
	 * arrotondamento effettuato in modo che questi vengano alternati 
	 */
	public int[] calcShiftValueApproved(Person person, int year, int month, int requestedMins) {
		int hoursApproved = 0;
		int exceedMins = 0;
		int oldExceedMins = 0;


		Logger.debug("Nella calcShiftValueApproved person =%s, year=%s, month=%s, requestedMins=%s)", person, year, month, requestedMins);

		String workedTime = competenceUtility.calcStringShiftHoursFromMinutes(requestedMins);
		int hoursOfWorkedTime = Integer.parseInt(workedTime.split("\\.")[0]);
		int minsOfWorkedTime = Integer.parseInt(workedTime.split("\\.")[1]);

		Logger.debug("hoursOfWorkedTime = %s minsOfWorkedTime = %s", hoursOfWorkedTime, minsOfWorkedTime);

		// get the Competence code for the ordinary shift  
		CompetenceCode competenceCode = competenceCodeDao.getCompetenceCodeByCode(codShift);
		//CompetenceCode competenceCode = CompetenceCode.find("Select code from CompetenceCode code where code.code = ?", codShift).first();

		final QCompetence com = new QCompetence("competence");
		final JPQLQuery query = getQueryFactory().query();
		final Competence myCompetence = query
				.from(com)
				.where(
						com.person.eq(person)
						.and(com.year.eq(year))
						.and(com.month.lt(month))
						.and(com.competenceCode.eq(competenceCode))		
						)
						.orderBy(com.month.desc())
						.limit(1)
						.uniqueResult(com);

		Logger.debug("prendo i minuti in eccesso dal mese %s", myCompetence.getMonth());

		// get the old exceede mins in the DB
		oldExceedMins = ((myCompetence == null) || ((myCompetence != null) && myCompetence.getExceededMin() == null)) ? 0 : myCompetence.getExceededMin();

		Logger.debug("oldExceedMins in the DB=%s", oldExceedMins);


		// if there are no exceeded mins, the approved hours 
		// match with the worked hours
		if (minsOfWorkedTime == 0) {
			hoursApproved = hoursOfWorkedTime;
			exceedMins = oldExceedMins;

			//Logger.debug("minsOfWorkedTime == 0 , hoursApproved=%s exceedMins=%s", hoursApproved, exceedMins);
		} else {		
			// check if the exceeded mins of this month plus those
			// worked in the previous months make up an hour
			exceedMins = oldExceedMins + minsOfWorkedTime;
			if (exceedMins >= 60) {
				hoursApproved = hoursOfWorkedTime + 1;
				exceedMins -= 60; 
			} else {
				hoursApproved = hoursOfWorkedTime;
			}

			//Logger.debug("minsOfWorkedTime = %s , hoursApproved=%s exceedMins=%s", minsOfWorkedTime, hoursApproved, exceedMins);
		}

		Logger.debug("hoursApproved=%s exceedMins=%s", hoursApproved, exceedMins);

		int[] result = {hoursApproved, exceedMins};

		Logger.debug("La calcShiftValueApproved restituisce %s", result);

		return result;
	}
}

