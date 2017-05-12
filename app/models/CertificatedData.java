package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;

import play.data.validation.Required;


/**
 * Contiene le informazioni relative alla richiesta/risposta di elaborazione dati delle
 * assenze/competenze/buoni mensa inviati al sistema degli attestati del CNR.
 *
 * @author cristian
 */
@Audited
@Entity
@Table(name = "certificated_data")
public class CertificatedData extends BaseModel {

  private static final long serialVersionUID = 4909012051833782060L;

  public int year;
  public int month;

  @Required
  @ManyToOne(optional = false)
  @JoinColumn(name = "person_id", nullable = false)
  public Person person;

  @Column(name = "cognome_nome")
  public String cognomeNome;

  @Column(name = "matricola")
  public String matricola;

  @Column(name = "absences_sent")
  public String absencesSent = null;

  @Column(name = "competences_sent")
  public String competencesSent = null;

  @Column(name = "meal_ticket_sent")
  public String mealTicketSent = null;

  @Column(name = "traininghours_sent")
  public String trainingHoursSent = null;

  @Column(name = "problems")
  public String problems = null;

  @Column(name = "is_ok")
  public boolean isOk = false;

  public CertificatedData(
      Person person, String cognomeNome, String matricola, Integer year, Integer month) {
    this.year = year;
    this.month = month;
    this.person = person;
    this.cognomeNome = cognomeNome;
    this.matricola = matricola;
  }

}
