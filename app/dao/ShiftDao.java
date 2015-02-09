package dao;

import java.util.List;

import org.joda.time.LocalDate;

import helpers.ModelQuery;

import com.mysema.query.jpa.JPQLQuery;

import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftType;
import models.query.QPersonShift;
import models.query.QPersonShiftDay;
import models.query.QShiftCancelled;
import models.query.QShiftType;

/**
 * 
 * @author dario
 *
 */
public class ShiftDao {

	private final static QShiftType shiftType = QShiftType.shiftType;
	private final static QPersonShiftDay psd = QPersonShiftDay.personShiftDay;
	private final static QShiftCancelled sc = QShiftCancelled.shiftCancelled;
	
	/**
	 * 
	 * @param type
	 * @return lo shiftType corrispondente al tipo type passato come parametro
	 */
	public static ShiftType getShiftTypeByType(String type){
		JPQLQuery query = ModelQuery.queryFactory().from(shiftType).where(shiftType.type.eq(type));
		return query.singleResult(shiftType);
	}
	
	/**
	 * 
	 * @param begin
	 * @param to
	 * @param type
	 * @return la lista dei personShiftDay con ShiftType 'type' presenti nel periodo tra 'begin' e 'to'
	 */
	public static List<PersonShiftDay> getPersonShiftDaysByPeriodAndType(LocalDate begin, LocalDate to, ShiftType type){
		JPQLQuery query = ModelQuery.queryFactory().from(psd)
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
	public static List<ShiftCancelled> getShiftCancelledByPeriodAndType(LocalDate from, LocalDate to, ShiftType type){
		JPQLQuery query = ModelQuery.queryFactory().from(sc).where(sc.date.between(from, to).and(sc.type.eq(type))).orderBy(sc.date.asc());
		return query.list(sc);
	}
	
	/**
	 * 
	 * @param day
	 * @param type
	 * @return il turno cancellato relativo al giorno 'day' e al tipo 'type' passati come parametro
	 */
	public static ShiftCancelled getShiftCancelled(LocalDate day, ShiftType type){
		JPQLQuery query = ModelQuery.queryFactory().from(sc).where(sc.date.eq(day).and(sc.type.eq(type)));
		return query.singleResult(sc);
	}
	
	/**
	 * 
	 * @param type
	 * @param day
	 * @return il quantitativo di shiftCancelled effettivamente cancellati
	 */
	public static Long deleteShiftCancelled(ShiftType type, LocalDate day){
		Long deleted = ModelQuery.queryFactory().delete(sc).where(sc.date.eq(day).and(sc.type.eq(type))).execute();
		return deleted;
	}
}

