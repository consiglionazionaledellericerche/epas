package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;
import it.cnr.iit.epas.DateUtility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Absence;
import models.AbsenceType;
import models.Person;

import models.PersonDay;

import models.query.QAbsence;
import models.query.QAbsenceType;

import org.bouncycastle.util.Strings;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
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
	
	private final static QAbsenceType absenceType = QAbsenceType.absenceType;
	private final static QAbsence absence = QAbsence.absence;

	
	public static List<AbsenceTypeDto> countersDto() {
//		QAbsenceType absenceType = QAbsenceType.absenceType;
//		QAbsence absence = QAbsence.absence;
		return ModelQuery.queryFactory().from(absenceType)
				.join(absenceType.absences, absence)
				.groupBy(absenceType)
				.orderBy(absence.count().desc())
				.list(Projections.bean(AbsenceTypeDto.class, 
						absenceType.code, absence.count()));
	}

	public static Map<AbsenceType, Long> counters() {
//		QAbsenceType absenceType = QAbsenceType.absenceType;
//		QAbsence absence = QAbsence.absence;
		return ModelQuery.queryFactory().from(absenceType)
				.join(absenceType.absences, absence)
				.groupBy(absenceType)
				.orderBy(absence.count().desc())
				.map(absenceType, absence.count());
	}

	public static List<AbsenceType> getFrequentTypes() {
//		QAbsenceType absenceType = QAbsenceType.absenceType;
//		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
			.join(absence.absenceType, absenceType)
			.groupBy(absenceType)
			.orderBy(absence.count().desc())
			.limit(20);
		
		return query.list(absenceType);
	} 
	
	
	public static SimpleResults<AbsenceType> getAbsences(Optional<String> name){
		
		final BooleanBuilder condition = new BooleanBuilder();
//		QAbsenceType absenceType = QAbsenceType.absenceType;
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
//		QAbsenceType absenceType = QAbsenceType.absenceType;
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
//		QAbsenceType absenceType = QAbsenceType.absenceType;
		final JPQLQuery query = ModelQuery.queryFactory().from(absenceType)
				.where(absenceType.validTo.after(date)).orderBy(absenceType.code.asc());
		
		return query.list(absenceType);
	}

	/**
	 * 
	 * @param string
	 * @return l'absenceType relativo al codice passato come parametro
	 */
	public static AbsenceType getAbsenceTypeByCode(String string) {
//		QAbsenceType absenceType = QAbsenceType.absenceType;
		final JPQLQuery query = ModelQuery.queryFactory().from(absenceType)
				.where(absenceType.code.eq(string));
		
		return query.singleResult(absenceType);
		
	}
	
	/**
	 * Una mappa contenente gli AbsenceType fatte dalle persona nel mese e numero di assenze fatte per ogni tipo.
	 * @param person
	 * @param year
	 * @param month
	 * @return
	 */
	public static Map<AbsenceType,Integer> getAbsenceTypeInPeriod(Person person, LocalDate fromDate, Optional<LocalDate> toDate){
		Preconditions.checkNotNull(person);
		Preconditions.checkNotNull(fromDate);
		
		List<Absence> absences = AbsenceDao.findByPersonAndDate(person, fromDate,toDate).list();	
		Map<AbsenceType,Integer> absenceCodeMap = new HashMap<AbsenceType, Integer>();

		for (Absence absence : absences){
			if(absenceCodeMap.containsKey(absence.absenceType)){
				absenceCodeMap.put(absence.absenceType, absenceCodeMap.get(absence.absenceType)+1);
			}
			else{
				absenceCodeMap.put(absence.absenceType,1);
			}
		}
		return absenceCodeMap;	
	}

	/**
	 * 
	 * @param begin
	 * @param end
	 * @param person
	 * @return la lista dei codici di assenza presi dalla persona person nel periodo compreso tra begin e end
	 */
//	public static List<AbsenceType> getAbsenceTypeInPeriod(LocalDate begin, LocalDate end, Person person){
//		QAbsenceType absenceType = QAbsenceType.absenceType;
//		QAbsence absence = QAbsence.absence;
//		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
//				.where(absence.personDay.date.between(begin, end).and(absence.personDay.person.eq(person)).and(absence.absenceType.eq(absenceType)));
//		return query.list(absenceType);
//	}
	
}
