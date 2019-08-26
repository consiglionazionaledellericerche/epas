package models;

import javax.persistence.Entity;
import lombok.ToString;
import models.base.BaseModel;
import org.hibernate.envers.Audited;

@ToString
@Entity
@Audited
public class GeneralSetting extends BaseModel {

  private static final long serialVersionUID = 881278299637007974L;

  // Parametri gestione anagrafica
  
  public boolean syncBadgesEnabled = false;
  public boolean syncOfficesEnabled = false;
  public boolean syncPersonsEnabled = false;
  
  // Fine parametri gestione anagrafica

}