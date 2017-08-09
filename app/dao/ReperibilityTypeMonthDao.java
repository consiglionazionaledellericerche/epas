package dao;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.joda.time.LocalDate;
import org.joda.time.YearMonth;

import com.google.common.base.Optional;
import com.google.inject.Provider;
import com.mysema.query.jpa.JPQLQueryFactory;

import models.Office;
import models.PersonReperibilityType;
import models.ReperibilityTypeMonth;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.query.QPersonReperibilityType;
import models.query.QReperibilityTypeMonth;
import models.query.QShiftCategories;
import models.query.QShiftTypeMonth;

public class ReperibilityTypeMonthDao extends DaoBase {

    @Inject
    ReperibilityTypeMonthDao(JPQLQueryFactory queryFactory,
	    Provider<EntityManager> emp) {
	super(queryFactory, emp);
    }

    public Optional<ReperibilityTypeMonth> byReperibilityTypeAndDate(PersonReperibilityType reperibilityType, LocalDate date) {
	final QReperibilityTypeMonth rtm = QReperibilityTypeMonth.reperibilityTypeMonth;
	final YearMonth yearMonth = new YearMonth(date);

	return Optional.fromNullable(getQueryFactory()
		.from(rtm).where(rtm.personReperibilityType.eq(reperibilityType)
			.and(rtm.yearMonth.eq(yearMonth))).singleResult(rtm));
    }
    
    public Optional<ReperibilityTypeMonth> byId(long id) {
      final QReperibilityTypeMonth rtm = QReperibilityTypeMonth.reperibilityTypeMonth;

      return Optional
          .fromNullable(getQueryFactory().from(rtm).where(rtm.id.eq(id)).singleResult(rtm));
    }
    
    public List<ReperibilityTypeMonth> byOfficeInMonth(Office office, YearMonth month) {
      final QReperibilityTypeMonth rtm = QReperibilityTypeMonth.reperibilityTypeMonth;
      final QPersonReperibilityType prt = QPersonReperibilityType.personReperibilityType;

      return getQueryFactory().from(rtm)
          .leftJoin(rtm.personReperibilityType, prt)
          .where(rtm.yearMonth.eq(month).and(prt.office.eq(office))).distinct().list(rtm);

    }
}
