package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import dao.wrapper.IWrapperFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.Office;
import models.OrganizationShiftTimeTable;
import models.query.QOrganizationShiftTimeTable;

public class OrganizationShiftTimeTableDao extends DaoBase {

  @Inject
  OrganizationShiftTimeTableDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp,
      IWrapperFactory wrapperFactory) {
    super(queryFactory, emp);
  }
  
  /**
   * Metodo che ritorna la lista delle timetable definite per la sede passata come parametro.
   * @param office la sede di cui si vogliono le timetable associate
   * @return la lista delle timetable definite per una sede.
   */
  public List<OrganizationShiftTimeTable> getAllFromOffice(Office office) {
    final QOrganizationShiftTimeTable timeTable = 
        QOrganizationShiftTimeTable.organizationShiftTimeTable;
    return getQueryFactory()
        .selectFrom(timeTable)
        .where(timeTable.office.eq(office)).fetch();
  }
}
