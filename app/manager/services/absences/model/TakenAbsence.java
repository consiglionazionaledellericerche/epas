package manager.services.absences.model;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import models.absences.Absence;
import models.absences.AmountType;

@Builder 
@Getter 
@Setter(AccessLevel.PACKAGE)
public class TakenAbsence {
  public Absence absence;
  public AmountType amountType;     //risalibile dal period..
  
  public int periodTakableTotal;   //situazione prima della assenza
  public int periodTakenBefore;     

  public int takenAmount;
  
  public boolean toInsert;          //segnala che è l'assenza da inserire nella chain
  
  /**
   * Se l'assenza non supera i limiti.
   * @return esito
   */
  public boolean canAddTakenAbsence() {
    if (periodTakableTotal < 0) {
      //TODO: se non c'è limite programmarlo in un booleano
      return true;
    }
    if (periodTakableTotal - periodTakenBefore - takenAmount >= 0) {
      return true;
    }
    return false;
  }
  
  public int periodResidualBefore() {
    return periodTakableTotal - periodTakenBefore;
  }
}
