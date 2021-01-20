package manager;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Contract;
import org.joda.time.LocalDate;


public class HistoricalManager {
  
  /**
   * Controllo sulle date passate.
   * @param date1 la prima data da controllare
   * @param date2 la seconda data da controllare
   * @return true se le date sono uguali, false altrimenti.
   */
  public boolean checkDates(LocalDate date1, LocalDate date2) {
    
    if (date1 == null || date2 == null) {
      return false;
    }
    if (!date1.isEqual(date2)) {
      return false;
    }
    return true;
  }
  
  /**
   * Controlla i vari parametri dei due oggetti per verificare che siano identici.
   * @param obj1 il primo oggetto da verificare
   * @param obj2 il secondo oggetto da verificare
   * @return true se i due oggetti sono uguali, false altrimenti.
   */
  public boolean checkObjects(Object obj1, Object obj2) {
    if (obj1 instanceof Contract && obj2 instanceof Contract) {
      Contract c1 = (Contract) obj1;
      Contract c2 = (Contract) obj2;
      if (c1.getPreviousContract() == null && c2.getPreviousContract() != null) {
        return false;
      }
      if (c1.getPreviousContract() != null && c2.getPreviousContract() == null) {
        return false;
      }
      if (c1.getPreviousContract() != null && c2.getPreviousContract() != null
          && c1.getPreviousContract() != c2.getPreviousContract()) {
        return false;
      }
      return true;
    } else {
      //TODO: completare per altre istanze
    }
    return false;
    
  }
}
