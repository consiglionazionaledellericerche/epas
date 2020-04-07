package models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import models.base.BaseModel;
import models.enumerate.Troubles;
import org.hibernate.envers.Audited;

@Audited
@Entity
public class PersonDayInTrouble extends BaseModel {

  private static final long serialVersionUID = 4802468368796902865L;

  @Enumerated(EnumType.STRING)
  public Troubles cause;

  public boolean emailSent;

  @NotNull
  @ManyToOne
  @JoinColumn(updatable = false)
  public PersonDay personDay;

  /**
   * Costruttore.
   * @param pd il personday
   * @param cause la causa del trouble
   */
  public PersonDayInTrouble(PersonDay pd, Troubles cause) {
    this.personDay = pd;
    this.cause = cause;
    this.emailSent = false;
  }
}
