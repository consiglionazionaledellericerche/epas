package dao;

import helpers.ModelQuery;

import java.util.List;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.AbsenceTypeGroup;
import models.query.QAbsenceTypeGroup;

/**
 * 
 * @author dario
 *
 */
public class AbsenceTypeGroupDao {

	/**
	 * 
	 * @param codeToReplace, findAll
	 * @return la lista dei gruppi di codici di assenza nel caso in cui sia valorizzato a "true" il parametro findAll.
	 * Nel caso in cui, invece, sia false e sia valorizzato il campo codeToReplace, verrà ritornata una lista con un solo elemento
	 * contenente l'absenceTypeGroup che soddisfa il criterio di codeToReplace.
	 */
	public static List<AbsenceTypeGroup> getAbsenceTypeGroup(Optional<String> codeToReplace, boolean findAll){
		final BooleanBuilder condition = new BooleanBuilder();
		QAbsenceTypeGroup absenceTypeGroup = QAbsenceTypeGroup.absenceTypeGroup;
		final JPQLQuery query = ModelQuery.queryFactory().from(absenceTypeGroup);
		if(findAll)
			return query.list(absenceTypeGroup);
		if(codeToReplace.isPresent()){
			condition.and(absenceTypeGroup.replacingAbsenceType.code.eq(codeToReplace.get()));
			return query.where(condition).list(absenceTypeGroup);
		}
		return null;
	}
}
