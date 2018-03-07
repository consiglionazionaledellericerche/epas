package models;

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.ToString;
import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.data.validation.Required;


/**
 * Tabella delle competenze relative alla persona in cui sono memorizzate le competenze in
 * determinate date (espresse attraverso due interi, uno relativo all'anno e uno relativo al mese
 * con relative descrizioni e valori.
 *
 * @author dario
 * @author arianna
 */
@ToString
@Entity
@Table(name = "competences")
@Audited
public class Competence extends BaseModel {

  private static final long serialVersionUID = -36737525666037452L;

  @ManyToOne
  @JoinColumn(name = "person_id")
  public Person person;

  @Required
  @ManyToOne
  @JoinColumn(name = "competence_code_id", nullable = false)
  public CompetenceCode competenceCode;

  public int year;

  public int month;

  public BigDecimal valueRequested = BigDecimal.ZERO;

  @Column(name = "exceeded_mins")
  public Integer exceededMins;

  public int valueApproved;


  public String reason;


  public Competence(
      Person person, CompetenceCode competenceCode, int year, int month) {
    this.person = person;
    this.competenceCode = competenceCode;
    this.year = year;
    this.month = month;
  }

  public Competence(
      Person person, CompetenceCode competenceCode, int year, int month, int value, String reason) {
    this.person = person;
    this.competenceCode = competenceCode;
    this.year = year;
    this.month = month;
    valueApproved = value;
    this.reason = reason;
  }

  public Competence() {
  }

  @Override
  public String toString() {
    return String.format("Competenza %s nel mese %s-%s per %s: %s", competenceCode, month, year,
        person, valueApproved);
  }

}
