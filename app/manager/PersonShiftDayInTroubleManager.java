package manager;

import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import models.PersonShiftDay;
import models.PersonShiftDayInTrouble;
import models.enumerate.ShiftTroubles;

@Slf4j
public class PersonShiftDayInTroubleManager {

  
  /**
   * Crea il personShiftDayInTrouble per quel giorno (se non esiste già).
   *
   * @param pd    giorno
   * @param cause causa
   */
  public void setTrouble(PersonShiftDay pd, ShiftTroubles cause) {

    for (PersonShiftDayInTrouble pdt : pd.troubles) {
      if (pdt.cause == cause) {
        // Se esiste gia' non faccio nulla
        return;
      }
    }

    // Se non esiste lo creo
    PersonShiftDayInTrouble trouble = new PersonShiftDayInTrouble(pd, cause);
    trouble.save();
    pd.troubles.add(trouble);

    log.info("Nuovo PersonDayInTrouble {} - {} - {}",
        pd.personShift.person.getFullname(), pd.date, cause);
  }


  /**
   * Metodo per rimuovere i problemi con una determinata causale all'interno del
   * personShiftDay.
   */
  public void fixTrouble(final PersonShiftDay pd, final ShiftTroubles cause) {

    Iterables.removeIf(pd.troubles, pdt -> {
      if (pdt.cause == cause) {
        pdt.delete();

        log.info("Rimosso PersonDayInTrouble {} - {} - {}",
            pd.personShift.person.getFullname(), pd.date, cause);
        return true;
      }
      return false;
    });
  }
}
