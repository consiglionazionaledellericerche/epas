package dao;

import java.util.List;

import org.joda.time.LocalDate;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.Absence;
import models.AbsenceType;
import models.Person;
import models.enumerate.JustifiedTimeAtWork;
import models.query.QAbsence;

public class AbsenceDao {
	
	private final static QAbsence absence = QAbsence.absence;

	/**
	 * 
	 * @param id
	 * @return l'assenza con id specificato come parametro
	 */
	public static Absence getAbsenceById(Long id){

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

		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
				.where(absence.personDay.date.between(begin, end).and(absence.absenceType.code.notIn(absenceCode)));
		if(query.count() != 0)
			return query.count();
		else
			return new Long(0);
	}
	
	public static SimpleResults<Absence> findByPersonAndDate(Person person, LocalDate fromDate, Optional<LocalDate> toDate,Optional<AbsenceType> absenceType) {
		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(fromDate);
				
		BooleanBuilder conditions = 
			new BooleanBuilder(absence.personDay.person.eq(person).and(absence.personDay.date.goe(fromDate)));
		if (toDate.isPresent()) {
			conditions.and(absence.personDay.date.loe(toDate.get()));
		}
		if(absenceType.isPresent()){
			conditions.and(absence.absenceType.eq(absenceType.get()));
		}
		return ModelQuery.simpleResults(ModelQuery.queryFactory().from(absence).where(conditions), absence);
	}
}
