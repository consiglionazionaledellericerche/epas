package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import dao.PersonsOfficesDao;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Office;
import models.Person;
import models.PersonsOffices;
import org.joda.time.LocalDate;


@Slf4j
public class PersonsOfficesManager {

  private final PersonsOfficesDao personsOfficesDao;
  
  @Inject
  public PersonsOfficesManager(PersonsOfficesDao personsOfficesDao) {
    this.personsOfficesDao = personsOfficesDao;
  }
  
  /**
   * Ritorna gli affiliati di una sede.
   * 
   * @param office la sede di cui ritornare gli associati
   * 
   * @return la lista delle persone associate alla sede passata come parametro.
   */
  public List<Person> affiliatePeople(Office office) {
    
    List<PersonsOffices> peopleList = office.personsOffices.stream()
        .filter(po -> !po.beginDate.isAfter(LocalDate.now()) 
            && (po.endDate == null || !po.endDate.isBefore(LocalDate.now()) ))
        .collect(Collectors.toList());
    return peopleList.stream().map(po -> po.person).collect(Collectors.toList());
  }
  
  /**
   * Verifica se la persona può essere inserita nella sede per il periodo passato.
   * 
   * @param person la persona da inserire
   * @param office la sede di lavoro
   * @param beginDate data inizio di affiliazione alla sede
   * @param endDate (optional) eventuale data di fine affiliazione alla sede
   */
  public void addPersonInOffice(Person person, Office office, 
      LocalDate beginDate, Optional<LocalDate> endDate) {
    
    Range<LocalDate> rangeAffiliation = null;    
    if (endDate.isPresent()) {
      rangeAffiliation = Range.closed(beginDate, endDate.get());
    } else {
      rangeAffiliation = Range.greaterThan(beginDate);
    }
    List<PersonsOffices> personAffiliation = personsOfficesDao.listByPerson(person);
    for (PersonsOffices po : personAffiliation) {
      Range<LocalDate> actualRange = null;
      if (po.endDate == null) {
        actualRange = Range.greaterThan(po.beginDate);
      } else {
        actualRange = Range.closed(po.beginDate, po.endDate);
      }
      if (rangeAffiliation.isConnected(actualRange) && actualRange.test(beginDate)) {
        po.endDate = beginDate.minusDays(1);
        po.save();
        saveAffiliation(person, office, beginDate, endDate);
        String fineAffiliazione = "";
        if (endDate.isPresent()) {
         fineAffiliazione = endDate.get().toString(); 
        } else {
          fineAffiliazione = "tempo indeterminato";
        }
        log.info("Salvata affiliazione per {} nella sede {} da {} a {}", 
            person, office, beginDate, fineAffiliazione);
      }
    }    
        
  }
  
  /**
   * Persiste l'oggetto personsOffices.
   * @param person la persona da affiliare
   * @param office la sede su cui affiliare
   * @param begin la data di inizio affiliazione
   * @param end la data di fine affiliazione
   */
  private void saveAffiliation(Person person, Office office, LocalDate begin, 
      Optional<LocalDate> end) {
    PersonsOffices personsOffices = new PersonsOffices();
    personsOffices.person = person;
    personsOffices.office = office;
    personsOffices.beginDate = begin;
    if (end.isPresent()) {
      personsOffices.endDate = end.get();
    }
    personsOffices.save();    
  }


}
