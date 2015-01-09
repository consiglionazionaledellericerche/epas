package dao;

import java.util.List;

import org.joda.time.LocalDate;

import helpers.ModelQuery;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.enumerate.JustifiedTimeAtWork;
import models.query.QAbsence;

/**
 * 
 * @author dario
 *
 */
public class AbsenceDao {

	/**
	 * 
	 * @param id
	 * @return l'assenza con id specificato come parametro
	 */
	public static Absence getAbsenceById(Long id){
		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
				.where(absence.id.eq(id));
		return query.singleResult(absence);

	}
	
	/**
	 * 
	 * @param person
	 * @param dateFrom
	 * @param dateTo
	 * @param forAttachment
	 * @return la lista di assenze di una persona tra due date se e solo se il campo dateTo isPresent.
	 * In caso non sia valorizzato, verrano ritornate le assenze relative a un solo giorno.
	 * Se il booleano forAttachment è true, si cercano gli allegati relativi a un certo periodo.
	 */
	public static List<Absence> getAbsenceInDay(Optional<Person> person, LocalDate dateFrom, Optional<LocalDate> dateTo, boolean forAttachment){
		QAbsence absence = QAbsence.absence;
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = ModelQuery.queryFactory().from(absence);
		if(person.isPresent())
			condition.and(absence.personDay.person.eq(person.get()));
		if(forAttachment)
			condition.and(absence.absenceFile.isNotNull().and(absence.absenceType.absenceTypeGroup.isNull()));
		if(dateTo.isPresent()){
			condition.and(absence.personDay.date.between(dateFrom, dateTo.get()));
		}
		else{
			condition.and(absence.personDay.date.eq(dateFrom));
		}
		
		query.where(condition).orderBy(absence.absenceType.code.asc());
		return query.list(absence);
		
	}
	
	/**
	 * 
	 * @param person
	 * @param code
	 * @param from
	 * @param to
	 * @param justifiedTimeAtWork
	 * @param forAttachment
	 * @return A seconda dei parametri passati alla funzione, può ritornare la lista dei codici di assenza in un certo periodo, 
	 * le eventuali assenze di una persona in un certo periodo di tempo, le assenze da ritornare con codice giustificativo giornaliero,
	 * le assenze da ritornare per il download degli allegati
	 */
	public static List<Absence> getAbsenceByCodeInPeriod(Optional<Person> person, Optional<String> code, 
			LocalDate from, LocalDate to, Optional<JustifiedTimeAtWork> justifiedTimeAtWork, boolean forAttachment, boolean ordered){
		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence);
		final BooleanBuilder condition = new BooleanBuilder();
		if(forAttachment)
			condition.and(absence.absenceFile.isNotNull());
		if(person.isPresent()){
			condition.and(absence.personDay.person.eq(person.get()));
		}
		if(justifiedTimeAtWork.isPresent()){
			condition.and(absence.absenceType.justifiedTimeAtWork.eq(justifiedTimeAtWork.get()));
		}
		if(code.isPresent()){
			condition.and(absence.absenceType.code.eq(code.get()));
		}
		condition.and(absence.personDay.date.between(from, to));
		query.where(condition);
		if(ordered)
			query.orderBy(absence.personDay.date.asc());
		return query.list(absence);
		
	}
	
	/**
	 * 
	 * @param begin
	 * @param end
	 * @param code
	 * @return il numero di volte in cui viene utilizzato un certo codice di assenza nel periodo che va da begin a end 
	 */
	public static Long howManyAbsenceInPeriod(LocalDate begin, LocalDate end, String code){
		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
				.where(absence.absenceType.code.eq(code).and(absence.personDay.date.between(begin, end)));
		if(query.count() != 0)
			return query.count();
		else
			return new Long(0);
	}
	
	/**
	 * 
	 * @param begin
	 * @param end
	 * @param absenceCode
	 * @return il quantitativo di assenze presenti in un certo periodo temporale delimitato da begin e end che non appartengono
	 * alla lista di codici passata come parametro nella lista di stringhe absenceCode
	 */
	public static Long howManyAbsenceInPeriodNotInList(LocalDate begin, LocalDate end, List<String> absenceCode){
		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
				.where(absence.personDay.date.between(begin, end).and(absence.absenceType.code.notIn(absenceCode)));
		if(query.count() != 0)
			return query.count();
		else
			return new Long(0);
	}
	
	/**
	 * 
	 * @param abt
	 * @param person
	 * @param begin
	 * @param end
	 * @return nella storia dei personDay, l'ultima occorrenza in ordine temporale del codice di rimpiazzamento (abt.absenceTypeGroup.replacingAbsenceType)
	 * relativo al codice di assenza che intendo inserire.
	 *  
	 */
	public static Absence getLastOccurenceAbsenceInPeriod(AbsenceType abt, Person person, Optional<LocalDate> begin, LocalDate end){
		QAbsence absence = QAbsence.absence;
		final BooleanBuilder condition = new BooleanBuilder();
		final JPQLQuery query = ModelQuery.queryFactory().from(absence);				
		if(begin.isPresent())
			condition.and(absence.personDay.date.between(begin.get(), end));
		else
			condition.and(absence.personDay.date.loe(end));
		query.where(condition.and(absence.absenceType.eq(abt.absenceTypeGroup.replacingAbsenceType)
				.and(absence.personDay.person.eq(person))));
		query.orderBy(absence.personDay.date.desc());
		return query.singleResult(absence);
	}
	
	
	/**
	 * 
	 * @param abt
	 * @param person
	 * @param begin
	 * @param end
	 * @return la lista dei codici di rimpiazzamento presenti nel periodo specificato da begin e end utilizzati dalla persona person
	 */
	public static List<Absence> getReplacingAbsenceOccurrenceListInPeriod(AbsenceType abt, Person person, LocalDate begin, LocalDate end){
		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
				.where(absence.absenceType.eq(abt.absenceTypeGroup.replacingAbsenceType)
						.and(absence.personDay.person.eq(person).and(absence.personDay.date.between(begin, end))));
		return query.list(absence);
	}
	
	/**
	 * 
	 * @param abt
	 * @param person
	 * @param begin
	 * @param end
	 * @return la lista dei codici di assenza accomunati dallo stesso label relativo al codice di gruppo nel periodo begin-end
	 * per la persona person
	 */
	public static List<Absence> getAllAbsencesWithSameLabel(AbsenceType abt, Person person, LocalDate begin, LocalDate end){
		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
				.where(absence.absenceType.absenceTypeGroup.label.eq(abt.absenceTypeGroup.label).and(absence.personDay.person.eq(person))
						.and(absence.personDay.date.between(begin, end)));
		return query.list(absence);
	}
	
	
	/**
	 * 
	 * @param person
	 * @param begin
	 * @param end
	 * @return la lista delle assenze contenenti un tipo di assenza con uso interno = false relative a una persona nel periodo
	 * compreso tra begin e end ordinate per codice di assenza e per data
	 */
	public static List<Absence> getAbsenceWithNotInternalUseInMonth(Person person, LocalDate begin, LocalDate end){
		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
				.where(absence.personDay.person.eq(person)
						.and(absence.personDay.date.between(begin, end)
								.and(absence.absenceType.internalUse.eq(false))))
								.orderBy(absence.absenceType.code.asc(),absence.personDay.date.asc());
		return query.list(absence);
	}
}
