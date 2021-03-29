package models;

import java.time.LocalDate;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.hibernate.envers.Audited;
import models.base.BaseModel;
import play.db.jpa.Model;

@Entity
@Table(name = "telework_validations")
@Audited
public class TeleworkValidation extends BaseModel{

  private static final long serialVersionUID = -4472102414284745470L;
  
  @ManyToOne(fetch = FetchType.LAZY)
  public Person person;
  
  public int year;
  
  public int month;
  
  public boolean approved;
  
  public LocalDate approvationDate;
}
