package models;

import com.google.common.base.MoreObjects;
import java.util.Comparator;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import manager.attestati.dto.show.SeatCertification.PersonCertification;
import models.base.BaseModel;
import models.enumerate.CertificationType;
import org.hibernate.envers.Audited;
import org.joda.time.YearMonth;
import play.data.validation.Required;


/**
 * Contiene le informazioni relative alla richiesta/risposta di elaborazione dati delle
 * assenze/competenze/buoni mensa inviati al nuovo sistema degli attestati del CNR.
 *
 * @author alessandro
 */
@Audited
@Entity
@Table(name = "certifications")
public class Certification extends BaseModel {

  private static final long serialVersionUID = 4909012051833782060L;

  @Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  public Person person;

  public int year;

  public int month;

  @Enumerated(EnumType.STRING)
  @Column(name = "certification_type")
  public CertificationType certificationType;

  public String content;

  @Column(name = "problems")
  public String problems = null;

  @Column(name = "warnings")
  public String warnings = null;

  @Column(name = "attestati_id")
  public Integer attestatiId;

  @Transient
  public boolean containProblems() {
    return this.problems != null && !this.problems.isEmpty();
  }

  @Transient
  public boolean containWarnings() {
    return this.warnings != null && !this.warnings.isEmpty();
  }

  /**
   * Una chiave che identifica in modo univoco la certificazione di una persona.
   * es. ABSENCE 92;14;15
   */
  @Transient
  public String aMapKey() {
    return this.certificationType.name() + this.content;
  }

  public static String serializeTrainingHours(int begin, int end, int value) {
    return begin + ";" + end + ";" + value;
  }

  public static String serializeAbsences(String code, int begin, int end) {
    return code + ";" + begin + ";" + end;
  }

  public static String serializeCompetences(String code, int value) {
    return code + ";" + value;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(PersonCertification.class)
        .add("person", person.fullName())
        .add("matricola", person.number)
        .add("year", year)
        .add("month", month)
        .add("key", aMapKey())
        .toString();
  }

  public YearMonth getYearMonth() {
    return new YearMonth(year, month);
  }

  /**
   * @return un Comparator che compara per fullname poi id.
   */
  public static Comparator<Certification> comparator() {
    return Comparator.comparing(Certification::getYearMonth);

  }


}
