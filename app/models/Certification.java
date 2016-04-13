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
  


}
