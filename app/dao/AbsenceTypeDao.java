package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;

import java.util.List;
import java.util.Map;

import models.AbsenceType;
import models.Person;
import models.query.QAbsence;
import models.query.QAbsenceType;

import org.bouncycastle.util.Strings;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.Projections;

/**
 * 
 * @author dario
 *
 */
public class AbsenceTypeDao {

	public static class AbsenceTypeDto {
		public String code;
		public long count;
		
		public boolean isUsed() {
			return count > 10;
		}
		
		public AbsenceTypeDto(String code, long count) {
			this.code = code;
			this.count = count;
		}
	}
	
	
	public static List<AbsenceTypeDto> countersDto() {
		
		final QAbsenceType absenceType = QAbsenceType.absenceType;
		final QAbsence absence = QAbsence.absence;
		
		return ModelQuery.queryFactory().from(absenceType)
				.join(absenceType.absences, absence)
				.groupBy(absenceType)
				.orderBy(absence.count().desc())
				.list(Projections.bean(AbsenceTypeDto.class, 
						absenceType.code, absence.count()));
	}

	public static Map<AbsenceType, Long> counters() {
		
		final QAbsenceType absenceType = QAbsenceType.absenceType;
		final QAbsence absence = QAbsence.absence;
		
		return ModelQuery.queryFactory().from(absenceType)
				.join(absenceType.absences, absence)
				.groupBy(absenceType)
				.orderBy(absence.count().desc())
				.map(absenceType, absence.count());
	}

	public static List<AbsenceType> getFrequentTypes() {
		
		final QAbsenceType absenceType = QAbsenceType.absenceType;
		final QAbsence absence = QAbsence.absence;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
			.join(absence.absenceType, absenceType)
			.groupBy(absenceType)
			.orderBy(absence.count().desc())
			.limit(20);
		
		return query.list(absenceType);
	} 
	
	
	public static SimpleResults<AbsenceType> getAbsences(Optional<String> name){
		
		final QAbsenceType absenceType = QAbsenceType.absenceType;
		final BooleanBuilder condition = new BooleanBuilder();

		final JPQLQuery query = ModelQuery.queryFactory().from(absenceType)
				.orderBy(absenceType.code.asc());
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(absenceType.code.startsWithIgnoreCase(name.get()),
					absenceType.description.toLowerCase().like("%"+Strings.toLowerCase(name.get())+"%"));
			query.where(condition);
		}
		
		return ModelQuery.simpleResults(query, absenceType);
	}

	/**
	 * 
	 * @param long1
	 * @return l'absenceType relativo all'id passato come parametro
	 */
	public static AbsenceType getAbsenceTypeById(Long long1) {
		
		QAbsenceType absenceType = QAbsenceType.absenceType;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(absenceType)
				.where(absenceType.id.eq(long1));
		
		return query.singleResult(absenceType);
		
	}

	/**
	 * 
	 * @param date
	 * @return la lista di codici di assenza che sono validi da una certa data in poi, ordinati per codice di assenza crescente
	 */
	public static List<AbsenceType> getAbsenceTypeFromEffectiveDate(
			LocalDate date) {
		
		QAbsenceType absenceType = QAbsenceType.absenceType;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(absenceType)
				.where(absenceType.validTo.after(date)).orderBy(absenceType.code.asc());
		
		return query.list(absenceType);
	}

	/**
	 * 
	 * @param string
	 * @return l'absenceType relativo al codice passato come parametro
	 */
	public static Optional<AbsenceType> getAbsenceTypeByCode(String string) {
		
		QAbsenceType absenceType = QAbsenceType.absenceType;
		
		final JPQLQuery query = ModelQuery.queryFactory().from(absenceType)
				.where(absenceType.code.eq(string));
		
		return Optional.fromNullable(query.singleResult(absenceType));
		
	}
	
	/**
	 * Una mappa contenente gli AbsenceType fatte dalle persona nel mese e numero di assenze fatte per ogni tipo.
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public static Map<AbsenceType,Long> getAbsenceTypeInPeriod(Person person, LocalDate fromDate, Optional<LocalDate> toDate){
		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(fromDate);

		QAbsenceType absenceType = QAbsenceType.absenceType;
		QAbsence absence = QAbsence.absence;
		
		return ModelQuery.queryFactory().from(absenceType)
				.join(absenceType.absences, absence).where(absence.personDay.person.eq(person).and(
						absence.personDay.date.between(fromDate, toDate.or(fromDate))))
						.groupBy(absenceType)
						.orderBy(absence.count().desc())
						.map(absenceType, absence.count());
	}

	/**
	 * 
	 * @return la lista di tutti i codici di assenza che prevedono la riduzione
	 * dei giorni dell'anno su cui computare la maturazione delle ferie
	 */
	public static List<AbsenceType> getReducingAccruingDaysForVacations(){
		
		QAbsenceType absenceType = QAbsenceType.absenceType;
		QAbsence absence = QAbsence.absence;
		
		JPQLQuery query = ModelQuery.queryFactory().from(absenceType)
				.where(absenceType.code.startsWith("24")
						.or(absenceType.code.startsWith("25")
								.or(absenceType.code.startsWith("17C")
										.or(absenceType.code.startsWith("C17")
												.or(absenceType.code.startsWith("C18"))))));
		return query.list(absenceType);
		
	}
	
}
