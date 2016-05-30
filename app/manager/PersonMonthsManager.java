package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;

import dao.CertificationDao;
import dao.PersonMonthRecapDao;

import manager.recaps.trainingHours.DayAndHourRecap;
import manager.recaps.trainingHours.TrainingHoursRecap;

import models.AbsenceType;
import models.CertificatedData;
import models.Certification;
import models.Person;
import models.PersonMonthRecap;
import models.enumerate.CertificationType;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.i18n.Messages;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class PersonMonthsManager {

  private static final String SPLIT = " ";
  private static final String SPLIT_SUBELEMENT_CERTIFICATION = ";";
  private static final String SPLIT_SUBELEMENT_CERTIFICATED_DATA = ",";
  private static final String TRAINING_HOURS = "Ore di formazione";

  private static final Logger log = LoggerFactory.getLogger(PersonMonthsManager.class);

  private final PersonMonthRecapDao personMonthRecapDao;  
  private final CertificationDao certificationDao;

  public Comparator<Person> personNameComparator = new Comparator<Person>() {

    public int compare(Person person1, Person person2) {

      String name1 = person1.surname.toUpperCase();
      String name2 = person2.surname.toUpperCase();

      if (name1.equals(name2)) {
        return person1.name.toUpperCase().compareTo(person2.name.toUpperCase());
      }
      return name1.compareTo(name2);

    }

  };
  public Comparator<String> stringComparator = new Comparator<String>() {

    public int compare(String string1, String string2) {
      return string1.compareTo(string2);

    }

  };


  @Inject
  public PersonMonthsManager(PersonMonthRecapDao personMonthRecapDao, 
      CertificationDao certificationDao) {
    this.certificationDao = certificationDao;
    this.personMonthRecapDao = personMonthRecapDao;
  }

  /**
   * salva le ore di formazione per il periodo specificato.
   * @param pm il personMonthRecao
   * @param approved se sono già approvate o no le ore di formazione
   * @param value la quantità di ore di formazione da approvare
   * @param from data inizio del periodo di formazione
   * @param to data fine del periodo di formazione.
   */
  public void saveTrainingHours(
      PersonMonthRecap pm, boolean approved, Integer value, LocalDate from, LocalDate to) {
    pm.hoursApproved = false;
    pm.trainingHours = value;
    pm.fromDate = from;
    pm.toDate = to;
    pm.save();
  }

  /**
   * @return un Insertable che controlla se è possibile prendere i parametri passati alla funzione
   *     oppure se questi presentano dei problemi.
   */
  public Insertable checkIfInsertable(
      int begin, int end, Integer value, LocalDate beginDate, LocalDate endDate) {
    Insertable rr = new Insertable(true, "");
    if (begin > end) {
      rr.message =
          "La data di inizio del periodo di formazione non può essere successiva a quella di fine";
      rr.result = false;
    }
    if (value == null || value < 0
        || value > 24 * (endDate.getDayOfMonth() - beginDate.getDayOfMonth() + 1)) {
      rr.message =
          "Non sono valide le ore di formazione negative, testuali o che superino la quantità "
              + "massima di ore nell'intervallo temporale inserito.";
      rr.result = false;
    }
    return rr;
  }

  /**
   * @return un Insertable che verifica se esiste già un periodo contenente delle ore di formazione
   *     per la persona person.
   */
  public Insertable checkIfPeriodAlreadyExists(Person person, int year, int month,
      LocalDate beginDate, LocalDate endDate) {

    List<PersonMonthRecap> pmList = personMonthRecapDao
        .getPersonMonthRecaps(person, year, month, beginDate, endDate);

    Insertable rr = new Insertable(true, "");

    if (pmList != null && pmList.size() > 0) {
      rr.message = "Esiste un periodo di ore di formazione "
          + "che contiene uno o entrambi i giorni specificati.";
      rr.result = false;

    }
    return rr;
  }

  /**
   * @return un Insertable che verifica se le ore di formazione per anno e mese richieste sono già
   *     state inviate.
   */
  public Insertable checkIfAlreadySend(Person person, int year, int month) {
    Insertable rr = new Insertable(true, "");
    List<PersonMonthRecap> list = personMonthRecapDao
        .getPersonMonthRecapInYearOrWithMoreDetails(person, year,
            Optional.fromNullable(month), Optional.fromNullable(new Boolean(true)));

    if (list.size() > 0) {
      rr.message =
          "Impossibile inserire ore di formazione per il mese precedente poichè gli "
              + "attestati per quel mese sono già stati inviati";
      rr.result = false;

    }
    return rr;
  }

  /**
   * @return un Insertable che controlla se esiste nel database una entry con l'id passato come
   *     parametro per quelle ore di formazione.
   */
  public Insertable checkIfExist(PersonMonthRecap pm) {
    Insertable rr = new Insertable(true, "");

    if (pm == null) {
      rr.message = "Ore di formazione non trovate. Operazione annullata.";
      rr.result = false;
    }
    return rr;
  }

  public static final class Insertable {
    private boolean result;
    private String message;

    public Insertable(boolean result, String message) {
      this.result = result;
      this.message = message;

    }

    public String getMessage() {
      return this.message;
    }

    public boolean getResult() {
      return this.result;
    }
  }

  /**
   * 
   * @param personList la lista di persone
   * @param year l'anno
   * @param month il mese
   * @return la mappa contenente per ogni persona la propria situazione in termini di ore di
   *     formazione approvate o no.
   */
  public Map<Person, List<PersonMonthRecap>> createMap(
      List<Person> personList, int year, int month) {

    Map<Person, List<PersonMonthRecap>> map = Maps.newHashMap();
    for (Person person : personList) {

      List<PersonMonthRecap> pmrList = personMonthRecapDao
          .getPersonMonthRecapInYearOrWithMoreDetails(person, year, 
              Optional.fromNullable(month), Optional.<Boolean>absent());
      if (!pmrList.isEmpty()) {
        map.put(person, pmrList);
      }      
    }
    return map;
  }

  /**
   * 
   * @param string la stringa da parsare
   * @param year l'anno
   * @param month il mese
   * @return la lista di dayhourrecap da passare al chiamante.
   */
  private List<DayAndHourRecap> parseTrainingHourString(String string, String split, 
      String subSplit, int year, int month) {
    List<DayAndHourRecap> list = Lists.newArrayList();
    String[] tokens = separateTrainingHours(string, split);

    for (int i = 0; i < tokens.length; i++) {
      DayAndHourRecap dhr = new DayAndHourRecap();
      String[] elements = tokens[i].split(subSplit);
      dhr.begin = new LocalDate(year, month, new Integer(elements[0]));
      dhr.end = new LocalDate(year, month, new Integer(elements[1]));
      dhr.trainingHours = new Integer(elements[2].substring(0, 1));
      list.add(dhr);      
    }
    return list;
  }

  /**
   * 
   * @param string la stringa da splittare
   * @return un array di stringhe contenenti ciascuna una stringa nel formato 
   "orediformazione;giornoinizio;giornofine".
   */
  private String[] separateTrainingHours(String string, String split) {
    String[] elements = string.split(split);
    return elements;
  }


}
