package models;

import helpers.validators.LocalDatePast;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.CheckWith;
import play.data.validation.Required;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author dario questa classe è in relazione con la classe delle persone e serve a tenere traccia
 *         dei figli dei dipendenti per poter verificare se è possibile, per il dipendente in
 *         questione, usufruire dei giorni di permesso per malattia dei figli che sono limitati nel
 *         tempo e per l'età del figlio
 */
@Entity
@Audited
@Table(name = "person_children")
public class PersonChildren extends BaseModel {

  private static final long serialVersionUID = 2528486222814596830L;

  @Required
  public String name;

  @Required
  public String surname;

  @CheckWith(LocalDatePast.class)
  @Required
  public LocalDate bornDate;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  public Person person;
}
