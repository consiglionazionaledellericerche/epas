package dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.querydsl.jpa.JPQLQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import models.Certification;
import models.Person;
import models.enumerate.CertificationType;
import models.query.QCertification;

/**
 * Dao per l'accesso alle informazioni delle Certification.
 *
 * @author alessandro
 */
public class CertificationDao extends DaoBase {


  @Inject
  CertificationDao(JPQLQueryFactory queryFactory, Provider<EntityManager> emp) {
    super(queryFactory, emp);
  }

  /**
   * Ritorna la lista di certificazioni della persona nell'anno/mese.
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
