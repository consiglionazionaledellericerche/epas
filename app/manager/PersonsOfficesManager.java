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
        .filter(po -> !po.getBeginDate().isAfter(LocalDate.now()) 
            && (po.getEndDate() == null || !po.getEndDate().isBefore(LocalDate.now())))
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
    if (personAffiliation == null || personAffiliation.isEmpty()) {
      saveAffiliation(person, office, beginDate, endDate);
      log.info("Salvata affiliazione per {} nella sede {} dal {} ", 
          person, office, beginDate);
      return;
    }
    for (PersonsOffices po : personAffiliation) {
      Range<LocalDate> actualRange = null;
      if (po.getEndDate() == null) {
        actualRange = Range.greaterThan(po.getBeginDate());
      } else {
        actualRange = Range.closed(po.getBeginDate(), po.getEndDate());
      }
      if (rangeAffiliation.isConnected(actualRange) && actualRange.test(beginDate)) {
        po.setEndDate(beginDate.minusDays(1));
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
    personsOffices.setBeginDate(begin);
    if (end.isPresent()) {
      personsOffices.setEndDate(end.get());
    }
    personsOffices.save();  
    person.personsOffices.add(personsOffices);
    person.save();
  }
  
  /**
   * Ritorna il range di date di affiliazione della persona alla sede nel periodo.
   * @param personOffice l'affiliazione persona/sede
   * @param begin la data di inizio in cui cercare
   * @param end la data di fine in cui cercare
   * @return il range contenente l'affiliazione mensile della persona sulla sede.
   */
  public Range<LocalDate> monthlyAffiliation(PersonsOffices personOffice, 
      LocalDate begin, LocalDate end) {
    Range<LocalDate> periodRange = Range.closed(begin, end);
    Range<LocalDate> affiliationRange = null;
    if (personOffice.getEndDate() != null) {
      affiliationRange = Range.closed(personOffice.getBeginDate(), personOffice.getEndDate());
    } else {
      affiliationRange = Range.atLeast(personOffice.getBeginDate());
    }
    if (affiliationRange.encloses(periodRange)) {
      return periodRange;
    }
    if (periodRange.contains(affiliationRange.lowerEndpoint())) {
      return Range.closed(affiliationRange.lowerEndpoint(), periodRange.upperEndpoint());
    }
    if (periodRange.contains(affiliationRange.upperEndpoint())) {
      return Range.closed(periodRange.lowerEndpoint(), affiliationRange.upperEndpoint());
    }
    return null;
  }


}
