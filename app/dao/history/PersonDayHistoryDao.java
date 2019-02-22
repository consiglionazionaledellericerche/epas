package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.mysema.query.JoinType;
import dao.AbsenceTypeDao;
import lombok.val;
import java.util.ArrayList;
import java.util.List;

import models.Stamping;
import models.absences.Absence;
import models.absences.AbsenceType;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.criteria.AuditCriterion;
import org.joda.time.LocalDate;

/**
 * Dao per lo storico dei PersonDay.
 *
 * @author marco
 */
public class PersonDayHistoryDao {

  private final Provider<AuditReader> auditReader;
  private final AbsenceTypeDao absenceTypeDao;

  @Inject
  PersonDayHistoryDao(Provider<AuditReader> auditReader, AbsenceTypeDao absenceTypeDao) {
    this.auditReader = auditReader;
    this.absenceTypeDao = absenceTypeDao;
  }

  @SuppressWarnings("unchecked")
  public List<HistoryValue<Stamping>> stampings(long personDayId) {
    final AuditQuery query = auditReader.get().createQuery()
        .forRevisionsOfEntity(Stamping.class, false, true)
        .add(AuditEntity.relatedId("personDay").eq(personDayId))
        .addOrder(AuditEntity.property("id").asc())
        .addOrder(AuditEntity.revisionNumber().asc());

    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(Stamping.class))
        .toList();
  }

  @SuppressWarnings("unchecked")
  public List<HistoryValue<Absence>> absences(long personDayId) {
    final AuditQuery query = auditReader.get().createQuery()
        .forRevisionsOfEntity(Absence.class, false, true)
        .add(AuditEntity.relatedId("personDay").eq(personDayId))
        .addOrder(AuditEntity.property("id").asc())
        .addOrder(AuditEntity.revisionNumber().asc());

    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(Absence.class))
        .toList();
  }

  /**
   * 
   * @return la lista dello storico di tutti i codici di missione orari.
   */
  public List<HistoryValue<Absence>> oldMissions() {
    List<AbsenceType> ids = Lists.newArrayList();
    AbsenceType type = absenceTypeDao.getAbsenceTypeByCode("92H1").get();
    AbsenceType type2 = absenceTypeDao.getAbsenceTypeByCode("92H2").get();
    AbsenceType type3 = absenceTypeDao.getAbsenceTypeByCode("92H3").get();
    AbsenceType type4 = absenceTypeDao.getAbsenceTypeByCode("92H4").get();
    AbsenceType type5 = absenceTypeDao.getAbsenceTypeByCode("92H5").get();
    AbsenceType type6 = absenceTypeDao.getAbsenceTypeByCode("92H6").get();
    AbsenceType type7 = absenceTypeDao.getAbsenceTypeByCode("92H7").get();
    ids.add(type);
    ids.add(type2);
    ids.add(type3);
    ids.add(type4);
    ids.add(type5);
    ids.add(type6);
    ids.add(type7);

    final AuditQuery query = auditReader.get().createQuery()
        .forRevisionsOfEntity(Absence.class, false, true)
        .add(AuditEntity.or(AuditEntity.property("absenceType").eq(type), 
            AuditEntity.or(AuditEntity.property("absenceType").eq(type2), 
                AuditEntity.or(AuditEntity.property("absenceType").eq(type3), 
                    AuditEntity.or(AuditEntity.property("absenceType").eq(type4), 
                        AuditEntity.or(AuditEntity.property("absenceType").eq(type5), 
                            AuditEntity.or(AuditEntity.property("absenceType").eq(type6), 
                                AuditEntity.property("absenceType").eq(type7))))))))
        .addOrder(AuditEntity.property("id").asc());


    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(Absence.class))
        .toList(); 

  }

  /**
   * 
   * @param id
   * @return
   */
  public List<HistoryValue<Absence>> specificAbsence(long id) {
    final AuditQuery query = auditReader.get().createQuery()
        .forRevisionsOfEntity(Absence.class, false, true)
        .add(AuditEntity.property("id").eq(id))
        .add(AuditEntity.revisionType().eq(RevisionType.DEL));
    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(Absence.class))
        .toList();
  }


}
