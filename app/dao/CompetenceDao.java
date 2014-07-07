package dao;

import helpers.ModelQuery;
import helpers.ModelQuery.SimpleResults;

import java.util.List;
import java.util.Map;

import models.AbsenceType;
import models.Competence;
import models.Person;
import models.query.QAbsence;
import models.query.QAbsenceType;
import models.query.QCompetence;

import org.bouncycastle.util.Strings;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.Projections;

public class CompetenceDao {

	public static SimpleResults<Competence> list(
			Optional<Person> person, Optional<String> code, 
			Optional<Integer> year, Optional<Integer> month) {
		
		QCompetence competence = QCompetence.competence;
		final BooleanBuilder condition = new BooleanBuilder();
		if (person.isPresent()) {
			condition.and(competence.person.eq(person.get()));
		}
		if (code.isPresent()) {
			condition.and(competence.competenceCode.code.eq(code.get()));
		}
		if (year.isPresent()) {
			condition.and(competence.year.eq(year.get()));
		}
		if (month.isPresent()) {
			condition.and(competence.month.eq(month.get()));
		}
		final JPQLQuery query = ModelQuery.queryFactory().from(competence).where(condition);
		return ModelQuery.simpleResults(query, competence);
	}
	
}
