package models;

import models.base.BaseModel;

import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * 
 * @author dario
 *
 */
@Entity
@Table(name = "persons_competence_codes")
public class PersonCompetenceCodes extends BaseModel{

  @Required
  @ManyToOne
  @JoinColumn(name = "person_id")
  public Person person;
  
  @Required
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "competence_code_id")
  public CompetenceCode competenceCode;
  
  @Column(name = "enabling_date", nullable = false)
  public LocalDate enablingDate;
}
