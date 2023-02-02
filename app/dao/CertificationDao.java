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

package dao;

import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import models.Certification;
import models.Person;
import models.enumerate.CertificationType;
import models.query.QCertification;

/**
 * Dao per l'accesso alle informazioni delle Certification.
 *
 * @author Alessandro Martelli
 */
public class CertificationDao extends DaoBase {

  @Inject
  CertificationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna la lista di certificazioni della persona nell'anno/mese.
   *
   * @param person la persona di cui cercare le certificazioni.
   * @param year l'anno delle certificazioni.
   * @param month il mese delle certificazioni.
   * @return le certificazioni epas della persona nel mese e nell'anno.
   */
  public List<Certification> personCertifications(Person person, int year, int month) {

    QCertification certification = QCertification.certification;

    return getQueryFactory()
        .selectFrom(certification)
        .where(certification.person.eq(person)
            .and(certification.year.eq(year))
            .and(certification.month.eq(month)))
        .fetch();
  }

  /**
   * Ritorna la lista delle certificazioni della persona nell'anno/mese per il tipo type.
   *
   * @param person la persona di cui cercare le certificazioni.
   * @param year l'anno delle certificazioni.
   * @param month il mese delle certificazioni.
   * @param type il tipo delle certificazioni da cercare.
   * @return le certificationi epas della persona nel mese e nell'anno per il tipo type.
   */
  public List<Certification> personCertificationsByType(
      Person person, int year, int month, CertificationType type) {

    QCertification certification = QCertification.certification;
    return getQueryFactory()
        .selectFrom(certification)
        .where(certification.person.eq(person)
            .and(certification.year.eq(year))
            .and(certification.month.eq(month))
            .and(certification.certificationType.eq(type)))
        .fetch();
  }

}