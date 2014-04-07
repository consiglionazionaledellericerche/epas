package dao;

import helpers.ModelQuery;

import java.util.List;

import com.google.common.base.Optional;
import com.mysema.query.jpa.JPQLQuery;

import models.AbsenceType;
import models.Competence;
import models.query.QAbsence;
import models.query.QAbsenceType;
import models.query.QCompetence;

public class AbsenceTypeDao {

	/*
	 * 		return AbsenceType.find("Select abt from AbsenceType abt, Absence abs " +
				"where abs.absenceType = abt group by abt order by sum(abt.id) desc limit 20").fetch();
	 */
	public List<AbsenceType> getFrequentTypes(Optional<Boolean> notInternal) {
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
