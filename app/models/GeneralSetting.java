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
  
  // Parametri gestione invio dati a fine mese
  
  public boolean onlyMealTicket = false;

  // Fine parametri gestione invio dati a fine mese
  
  // Parametri gestione codici di competenza turno
  
  public String startDailyShift = "6:00";
  
  public String endDailyShift = "19:00";
  
  public String startNightlyShift = "19:00";
  
  public String endNightlyShift = "6:00";
  // Fine parametri gestione codici di competenza turno
  
  // Parametri gestione gruppi
  
  public boolean handleGroupsByInstitute = true;
  
  // Fine parametri gestione gruppi
}