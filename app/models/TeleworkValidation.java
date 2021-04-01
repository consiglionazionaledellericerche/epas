package models;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

/**
 * Informazioni sull'approvazione mensile delle attività in telelavoro.
 * @author Dario Tagliaferri
 */
@Entity
@Table(name = "telework_validations")
@Audited
public class TeleworkValidation extends BaseModel {

  private static final long serialVersionUID = -4472102414284745470L;
  
  @ManyToOne(fetch = FetchType.LAZY)
  public Person person;
  
  public int year;
  
  public int month;
  
  public boolean approved;
  
  public LocalDate approvationDate;
  
  @Transient
  public boolean isValidated() {
    if (this.approved && this.approvationDate != null) {
      return true;
    }
    return false;
  }
}
