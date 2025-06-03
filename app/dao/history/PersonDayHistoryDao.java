/*
 * Copyright (C) 2023  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dao.history;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.inject.Provider;
import dao.AbsenceTypeDao;
import java.util.List;
import javax.inject.Inject;
import models.PersonDay;
import models.Stamping;
import models.absences.Absence;
import models.absences.AbsenceType;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;

/**
 * Dao per lo storico dei PersonDay.
 *
 * @author Marco Andreini
 */
public class PersonDayHistoryDao {

  private final Provider<AuditReader> auditReader;
  private final AbsenceTypeDao absenceTypeDao;

  @Inject
  PersonDayHistoryDao(Provider<AuditReader> auditReader, AbsenceTypeDao absenceTypeDao) {
    this.auditReader = auditReader;
    this.absenceTypeDao = absenceTypeDao;
  }

  /**
   * La lista delle revisioni della timbratura.
   *
   * @param personDayId l'identificativo del personday cui appartiene la timbratura di cui
   *     si vogliono le revisioni.
   * @return la lista delle revisioni della timbratura relativa al personDay con identificativo
   *     personDayId.
   */
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

  /**
   * La lista delle revisioni delle timbrature.
   *
   * @param personDayId l'identificativo del personday
   * @return la lista delle revisioni delle timbrature alla creazione.
   */
  @SuppressWarnings("unchecked")
  public List<HistoryValue<Stamping>> stampingsAtCreation(long personDayId) {
    final AuditQuery query = auditReader.get().createQuery()
        //Vengono prelevate solo le revisioni delle entity non cancellate
        //tramite il terzo parametro a false del metodo forRevisionsOfEntity
        .forRevisionsOfEntity(Stamping.class, false, false)
        .add(AuditEntity.relatedId("personDay").eq(personDayId))
        .add(AuditEntity.revisionType().eq(RevisionType.ADD))
        .addOrder(AuditEntity.property("id").asc())
        .addOrder(AuditEntity.revisionNumber().asc());

    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(Stamping.class))
        .toList();
  }
  
  /**
   * La lista delle revisioni delle assenze relative al personday con
   * identificativo personDayId.
   *
   * @param personDayId l'identificativo del personday
   * @return la lista delle revisioni delle assenze relative al personday con
   *     identificativo personDayId.
   */
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
   * La lista dello storico di tutte le revisioni dei codici di missione orari.
   *
   * @return la lista dello storico di tutti i codici di missione orari.
   */
  @SuppressWarnings("unchecked")
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
   * La lista delle revisioni relative all'assenza con id passato.
   *
   * @param id l'identificativo dell'assenza inserita
   * @return la lista delle revisioni dell'assenza con id passato.
   */
  @SuppressWarnings("unchecked")
  public List<HistoryValue<Absence>> specificAbsence(long id) {
    final AuditQuery query = auditReader.get().createQuery()
        .forRevisionsOfEntity(Absence.class, false, true)
        .add(AuditEntity.property("id").eq(id))
        .add(AuditEntity.revisionType().eq(RevisionType.DEL));
    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(Absence.class))
        .toList();
  }

  /**
   * La lista delle revisioni del personday con id passato come parametro.
   * 
   * @param id l'identificativo del personday di cui recuperare le revisioni
   * @return la lista delle revisioni del personday identificato dall'id passato come parametro.
   */
  public List<HistoryValue<PersonDay>> personDayHistory(long id) {
    final AuditQuery query = auditReader.get().createQuery()
        .forRevisionsOfEntity(PersonDay.class, false, false)
        .add(AuditEntity.property("id").eq(id))
        .addOrder(AuditEntity.revisionNumber().desc());
    return FluentIterable.from(query.getResultList())
        .transform(HistoryValue.fromTuple(PersonDay.class))
        .toList();
  }
}