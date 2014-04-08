package dao;

import helpers.ModelQuery;

import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.Projections;

import models.AbsenceType;
import models.Competence;
import models.query.QAbsence;
import models.query.QAbsenceType;
import models.query.QCompetence;

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
}
