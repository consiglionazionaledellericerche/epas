package models;

import models.base.BaseModel;
import models.enumerate.CertificationType;

import org.hibernate.envers.Audited;

import play.data.validation.Required;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


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
  
  /**
   * Se la certificazione da inviare ad attestati.
   */
  @Transient
  public boolean toSend = true;
  
  /**
   * Se la certificazione già presente è stata confermata.
   */
  @Transient
  public boolean confirmed = false;
  

  /**
   * Una chiave che identifica in modo univoco la certificazione di una persona.
   * es. ABSENCE 92;14;15 
   * @return
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



}
