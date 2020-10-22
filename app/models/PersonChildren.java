package models;

import helpers.validators.LocalDatePast;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import models.base.BaseModel;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;
import play.data.validation.CheckWith;
import play.data.validation.Required;

/**
 * Questa classe è in relazione con la classe delle persone e serve a tenere traccia
 * dei figli dei dipendenti per poter verificare se è possibile, per il dipendente in
 * questione, usufruire dei giorni di permesso per malattia dei figli che sono limitati nel
 * tempo e per l'età del figlio.
 *
 * @author dario
 */
@Entity
@Audited
public class PersonChildren extends BaseModel {

  private static final long serialVersionUID = 2528486222814596830L;

  @Required
  public String name;

  @Required
  public String surname;

  @CheckWith(LocalDatePast.class)
  @Required
  public LocalDate bornDate;
  
  public String taxCode;

  @ManyToOne(fetch = FetchType.LAZY)
  public Person person;
}
