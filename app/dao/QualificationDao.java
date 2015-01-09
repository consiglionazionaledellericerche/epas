package dao;

import helpers.ModelQuery;

import java.util.List;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.AbsenceType;
import models.Qualification;
import models.query.QQualification;

/**
 * 
 * @author dario
 *
 */
public class QualificationDao {

	
	/**
	 * 
	 * @param qualification
	 * @param idQualification
	 * @param findAll
	 * @return la lista di qualifiche a seconda dei parametri passati: nel caso in cui il booleano sia "true" viene ritornata l'intera
	 * lista di qualifiche. Nel caso sia presente la qualifica che si vuole ritornare, viene ritornata sempre una lista, ma con un solo
	 * elemento, corrispondente al criterio di ricerca. Nel caso invece in cui si voglia una lista di elementi sulla base dell'id, 
	 * si controllerà il parametro idQualification, se presente, che determinerà una lista di un solo elemento corrispondente ai criteri
	 * di ricerca. Ritorna null nel caso in cui non dovesse essere soddisfatta alcuna delle opzioni di chiamata
	 */
	public static List<Qualification> getQualification(Optional<Integer> qualification, Optional<Long> idQualification, boolean findAll){
		final BooleanBuilder condition = new BooleanBuilder();
		QQualification qual = QQualification.qualification1;
		final JPQLQuery query = ModelQuery.queryFactory().from(qual);
		if(findAll){
			return query.list(qual);
		}
		if(qualification.isPresent()){
			condition.and(qual.qualification.eq(qualification.get()));
			query.where(condition);
			return query.list(qual);
		}
		if(idQualification.isPresent()){
			condition.and(qual.id.eq(idQualification.get()));
			query.where(condition);
			return query.list(qual);
		}
		return null;
		
	}
	
	/**
	 * 
	 * @param abt
	 * @return la lista di qualifiche che possono usufruire del codice di assenza abt
	 */
	public static List<Qualification> getQualificationByAbsenceTypeLinked(AbsenceType abt){
		QQualification qual = QQualification.qualification1;
		final JPQLQuery query = ModelQuery.queryFactory().from(qual)
				.where(qual.absenceTypes.contains(abt));
		return query.list(qual);
	}
	
	
	/**
	 * 
	 * @param limit
	 * @return la lista di qualifiche superiori o uguali al limite passato come parametro (da usare, ad esempio, per ritornare la
	 * lista delle qualifiche dei tecnici che hanno qualifica superiore a 3)
	 */
	public static List<Qualification> getQualificationGreaterThan(Integer limit){
		QQualification qual = QQualification.qualification1;
		final JPQLQuery query = ModelQuery.queryFactory().from(qual)
				.where(qual.qualification.goe(limit));
		return query.list(qual);
	}
}
