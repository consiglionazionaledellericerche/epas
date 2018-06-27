package manager;

import lombok.extern.slf4j.Slf4j;

import models.TimeVariation;
import models.absences.Absence;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

@Slf4j
public class TimeVariationManager {
  
  /**
   * Metodo di utilità che crea un timeVariation.
   * @param absence l'assenza da recuperare
   * @param hours le ore da recuperare
   * @param minutes i minuti da recuperare
   * @return il timevariation creato con i campi passati come parametro.
   */
  public TimeVariation create(Absence absence, int hours, int minutes) {
    TimeVariation timeVariation = new TimeVariation();
    timeVariation.absence = absence;
    timeVariation.dateVariation = LocalDate.now();
    timeVariation.timeVariation = (hours * DateTimeConstants.MINUTES_PER_HOUR) + minutes;
    log.info("Creata variazione oraria per giustificare l'assenza {} di {} del giorno {}", 
        absence.absenceType.code, absence.personDay.person.fullName(), absence.personDay.date);
    return timeVariation;
  }

}
