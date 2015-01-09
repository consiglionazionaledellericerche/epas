package dao;

import helpers.ModelQuery;

import java.util.List;

import org.joda.time.LocalDate;

import com.google.common.base.Optional;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPQLQuery;

import models.CertificatedData;
import models.Person;
import models.PersonMonthRecap;
import models.query.QCertificatedData;
import models.query.QPersonMonthRecap;

/**
 * 
 * @author dario
 *
 */
public class PersonMonthRecapDao {

	/**
	 * 
	 * @param person
	 * @param year
	 * @return la lista di personMonthRecap relativa all'anno year per la persona person
	 */
	public static List<PersonMonthRecap> getPersonMonthRecapInYearOrWithMoreDetails(Person person, Integer year, Optional<Integer> month, Optional<Boolean> hoursApproved){
		QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;
		final BooleanBuilder condition = new BooleanBuilder();
		if(month.isPresent())
			condition.and(personMonthRecap.month.eq(month.get()));
		if(hoursApproved.isPresent())
			condition.and(personMonthRecap.hoursApproved.eq(hoursApproved.get()));
		final JPQLQuery query = ModelQuery.queryFactory().from(personMonthRecap)
				.where(condition.and(personMonthRecap.person.eq(person).and(personMonthRecap.year.eq(year))));
		return query.list(personMonthRecap);
	}
	
	/**
	 * 
	 * @param id
	 * @return il personMonthRecap relativo all'id passato come parametro
	 */
	public static PersonMonthRecap getPersonMonthRecapById(Long id){
		QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;
		final JPQLQuery query = ModelQuery.queryFactory().from(personMonthRecap)
				.where(personMonthRecap.id.eq(id));
		return query.singleResult(personMonthRecap);
	}
	
	
	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @param begin
	 * @param end
	 * @return
	 */
	public static List<PersonMonthRecap> getPersonMonthRecaps(Person person, Integer year, Integer month, LocalDate begin, LocalDate end){
		QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;
		final JPQLQuery query = ModelQuery.queryFactory().from(personMonthRecap)
				.where(personMonthRecap.person.eq(person).and(personMonthRecap.year.eq(year)
						.and(personMonthRecap.month.eq(month)
								.andAnyOf(personMonthRecap.fromDate.loe(begin).and(personMonthRecap.toDate.goe(end)),
										personMonthRecap.fromDate.loe(end).and(personMonthRecap.toDate.goe(end))))));
		return query.list(personMonthRecap);
	}
	
	
	/**
	 * 
	 * @param person
	 * @param year
	 * @param month
	 * @return il personMonthRecap, se esiste, relativo ai parametri passati come riferimento
	 */
	public static Optional<PersonMonthRecap> getPersonMonthRecapByPersonYearAndMonth(Person person, Integer year, Integer month){
		QPersonMonthRecap personMonthRecap = QPersonMonthRecap.personMonthRecap;
		final JPQLQuery query = ModelQuery.queryFactory().from(personMonthRecap)
				.where(personMonthRecap.person.eq(person).and(personMonthRecap.year.eq(year).and(personMonthRecap.month.eq(month))));
		return Optional.fromNullable(query.singleResult(personMonthRecap));
	}
	
	
	/***************************************************************************************************************************************/
	/*Parte relativa a query su CertificatedData per la quale, essendo unica, non si è deciso di creare un Dao ad hoc                      */
	/***************************************************************************************************************************************/

	/**
	 * 
	 * @param id
	 * @return il certificatedData relativo all'id passato come parametro
	 */
	public static CertificatedData getCertificatedDataById(Long id){
		QCertificatedData cert = QCertificatedData.certificatedData;
		JPQLQuery query = ModelQuery.queryFactory().from(cert)
				.where(cert.id.eq(id));
		return query.singleResult(cert);
	}
	
	
	/**
	 * 
	 * @param person
	 * @param month
	 * @param year
	 * @return il certificatedData relativo alla persona 'person' per il mese 'month' e l'anno 'year'
	 */
	public static CertificatedData getCertificatedDataByPersonMonthAndYear(Person person, Integer month, Integer year){
		QCertificatedData cert = QCertificatedData.certificatedData;
		JPQLQuery query = ModelQuery.queryFactory().from(cert)
				.where(cert.person.eq(person).and(cert.month.eq(month).and(cert.year.eq(year))));
		return query.singleResult(cert);
	}
	
	
}
