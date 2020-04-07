package manager;

import com.google.common.base.Optional;
import dao.AbsenceDao;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.TimeVariation;
import models.absences.Absence;
import models.dto.AbsenceToRecoverDto;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.testng.collections.Maps;



@Slf4j
public class TimeVariationManager {
  
  private final AbsenceDao absenceDao;
  private final PersonManager personManager;
  
  @Inject
  public TimeVariationManager(AbsenceDao absenceDao, PersonManager personManager) {
    this.absenceDao = absenceDao;
    this.personManager = personManager;
  }
  
  /**
   * Metodo di utilità che crea un timeVariation.
   * @param absence l'assenza da recuperare
   * @param hours le ore da recuperare
   * @param minutes i minuti da recuperare
   * @return il timevariation creato con i campi passati come parametro.
   */
  public TimeVariation create(Absence absence, int hours, int minutes, 
      Optional<LocalDate> dateVariation) {
    
    TimeVariation timeVariation = new TimeVariation();
    timeVariation.absence = absence;
    timeVariation.dateVariation = dateVariation.or(LocalDate.now());
    timeVariation.timeVariation = (hours * DateTimeConstants.MINUTES_PER_HOUR) + minutes;
    log.info("Creata variazione oraria per giustificare l'assenza {} di {} del giorno {}", 
        absence.absenceType.code, absence.personDay.person.fullName(), absence.personDay.date);
    return timeVariation;
  }
  
  /**
   * Metodo di utilità che forma la mappa da ritornare al template per la visualizzazione di tutti
   * i dipendenti con la loro situazione relativa ai 91CE.
   * @param personList la lista delle persone attive
   * @param office la sede di appartenenza di quelle persone
   * @return una mappa contenente come chiave la persona e come valore la lista dei dto relativi
   *     alle assenze 91CE.
   */
  public Map<Person, List<AbsenceToRecoverDto>> createMap(List<Person> personList, Office office) {
    Map<Person, List<AbsenceToRecoverDto>> map = Maps.newHashMap();
    for (Person person : personList) {
      List<Absence> absenceList = absenceDao
          .absenceInPeriod(person, office.beginDate, LocalDate.now().plusMonths(2), "91CE");
      if (!absenceList.isEmpty()) {
        map.put(person, personManager.dtoList(absenceList));
      }      
    }
    return map;
  }
  
 
}
