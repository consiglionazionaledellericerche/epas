package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;

import java.util.List;
import java.util.Map;

import models.AbsenceType;
import models.query.QAbsence;
import models.query.QAbsenceType;

import org.bouncycastle.util.Strings;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.Projections;

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
		QAbsenceType absenceType = QAbsenceType.absenceType;
		QAbsence absence = QAbsence.absence;
		return ModelQuery.queryFactory().from(absenceType)
				.join(absenceType.absences, absence)
				.groupBy(absenceType)
				.orderBy(absence.count().desc())
				.list(Projections.bean(AbsenceTypeDto.class, 
						absenceType.code, absence.count()));
	}

	public static Map<AbsenceType, Long> counters() {
		QAbsenceType absenceType = QAbsenceType.absenceType;
		QAbsence absence = QAbsence.absence;
		return ModelQuery.queryFactory().from(absenceType)
				.join(absenceType.absences, absence)
				.groupBy(absenceType)
				.orderBy(absence.count().desc())
				.map(absenceType, absence.count());
	}

	/*
	 * 		return AbsenceType.find("Select abt from AbsenceType abt, Absence abs " +
				"where abs.absenceType = abt group by abt order by sum(abt.id) desc limit 20").fetch();
	 */
	public static List<AbsenceType> getFrequentTypes(Optional<Boolean> notInternal) {
		QAbsenceType absenceType = QAbsenceType.absenceType;
		QAbsence absence = QAbsence.absence;
		final JPQLQuery query = ModelQuery.queryFactory().from(absence)
			.join(absence.absenceType, absenceType)
			.groupBy(absenceType)
			.orderBy(absence.count().desc())
			.limit(20);
		
		return query.list(absenceType);
	} 
	
	
	public static SimpleResults<AbsenceType> getAbsences(Optional<String> name){
		
		final BooleanBuilder condition = new BooleanBuilder();
		QAbsenceType absenceType = QAbsenceType.absenceType;
		final JPQLQuery query = ModelQuery.queryFactory().from(absenceType)
				.orderBy(absenceType.code.asc());
		if (name.isPresent() && !name.get().trim().isEmpty()) {
			condition.andAnyOf(absenceType.code.startsWithIgnoreCase(name.get()),
					absenceType.description.toLowerCase().like("%"+Strings.toLowerCase(name.get())+"%"));
			query.where(condition);
		}
		
		return ModelQuery.simpleResults(query, absenceType);
	}
}
