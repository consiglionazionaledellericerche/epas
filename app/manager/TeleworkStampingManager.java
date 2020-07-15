package manager;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.StampingDao;
import dao.wrapper.IWrapperFactory;
import it.cnr.iit.epas.DateUtility;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.persistence.Transient;
import lombok.extern.slf4j.Slf4j;
import manager.recaps.personstamping.PersonStampingDayRecapFactory;
import manager.services.telework.errors.Errors;
import manager.services.telework.errors.TeleworkStampingError;
import manager.telework.service.TeleworkComunication;
import models.PersonDay;
import models.TeleworkStamping;
import models.User;
import models.dto.TeleworkDto;
import models.enumerate.StampTypes;
import models.enumerate.TeleworkStampTypes;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.modelmapper.ModelMapper;
import cnr.sync.dto.v2.StampingDto;


@Slf4j
public class TeleworkStampingManager {


  private TeleworkComunication comunication;
  
  @Inject
  public TeleworkStampingManager(TeleworkComunication comunication) {
    this.comunication = comunication;
  }
  
  /**
   * Ritorna la lista di timbrature in telelavoro nel giorno pd con causale appartenente a quelle
   * riportate nella lista di causali da ricercare.
   * @param pd il personDay di riferimento
   * @param stampTypes la lista di causali da cercare
   * @return la lista di timbrature di lavoro in telelavoro con causale quelle passate come 
   *     parametro.
   */
  public List<TeleworkStamping> getSpecificTeleworkStampings(PersonDay pd, 
      List<TeleworkStampTypes> stampTypes) {
    List<TeleworkStamping> list = Lists.newArrayList();
    for (TeleworkStamping tws : pd.teleworkStampings) {
      for (TeleworkStampTypes st : stampTypes) {
        if (tws.stampType.equals(st)) {
          list.add(tws);
        }
      }
    }
    return list;
  }
  
  /**
   * Chiama il metodo di comunicazione con l'applicazione esterna per salvare la timbratura
   * da telelavoro.
   * @param stamping la timbratura in telelavoro da salvare
   * @return 200 se la timbratura è stata salvata correttamente, altro numero altrimenti.
   */
  public int save(TeleworkStamping stamping) {
    int result = 0;
    TeleworkDto dto = TeleworkDto.builder()
        .date(transform(stamping.date).toString())
        .stampType(stamping.stampType.name())
        .note(stamping.note)
        .personDayId(stamping.personDay.id.toString())
        .build();
    try {
      result = comunication.save(dto);
    } catch (NoSuchFieldException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }
    return result;
  }
  
  public int update(TeleworkStamping stamping) {
    int result = 0;
    TeleworkDto dto = TeleworkDto.builder()
        .date(transform(stamping.date).toString())
        .stampType(stamping.stampType.name())
        .note(stamping.note)
        .personDayId(stamping.personDay.id.toString())
        .build();
    try {
      result = comunication.update(dto);
    } catch (NoSuchFieldException ex) {
      // TODO Auto-generated catch block
      ex.printStackTrace();
    }
    return result;
  }
  
  /**
   * Chiama la funzionalità di cancellazione della timbratura da telelavoro.
   * @param stampingId l'identificativo della timbratura da eliminare
   * @return 200 se la timbratura è stata eliminata correttamente, altro numero altrimenti.
   */
  public int delete(long stampingId) {
    int result = 0;
    try {
      result = comunication.delete(stampingId);
    } catch (NoSuchFieldException ex) {
      ex.printStackTrace();
    }
    return result;
  }
  
  public TeleworkStamping get(long stampingId) throws ExecutionException {
    TeleworkStamping stamping = null;
    try {
      stamping = mapper(comunication.get(stampingId));
    } catch(NoSuchFieldException ex) {
      ex.printStackTrace();
    }
    return stamping;
  }

  /**
   * Verifica se l'inserimento di una timbratura in un giorno può dare origine ad un errore di 
   * malformazione della lista di timbrature.
   * @param stamping la timbratura in telelavoro
   * @param pd il personday del giorno
   * @return l'opzionale contenente l'errore rilevato dal possibile inserimento della timbratura
   *     in un giorno di telelavoro. 
   */
  public Optional<Errors> checkTeleworkStamping(TeleworkStamping stamping, PersonDay pd) {
    
    Optional<Errors> error = Optional.absent();
    if (stamping.stampType.equals(TeleworkStampTypes.INIZIO_TELELAVORO)) {
      error = checkBeginTelework(pd, stamping);
    }
    if (stamping.stampType.equals(TeleworkStampTypes.FINE_TELELAVORO)) {
      error = checkEndInTelework(pd, stamping);
    }
    if (stamping.stampType.equals(TeleworkStampTypes.INIZIO_PRANZO_TELELAVORO)) {
      error = checkBeginMealInTelework(pd, stamping);
    }
    
    if (stamping.stampType.equals(TeleworkStampTypes.FINE_PRANZO_TELELAVORO)) {
      error = checkEndMealInTelework(pd, stamping);
    }
    if (stamping.stampType.equals(TeleworkStampTypes.INIZIO_INTERRUZIONE)) {
      //TODO: verificare come e se completare...
    }
    if (stamping.stampType.equals(TeleworkStampTypes.FINE_INTERRUZIONE)) {
    //TODO: verificare come e se completare...
    }
    return error;
  }
  
  private Range<LocalDateTime> getStampingRange(List<TeleworkStamping> list, LocalDate date) {
    
    if (list.isEmpty()) {      
      return Range.closed(setBeginOfTheDay(date), setEndOfTheDay(date));
    }
    if (list.size() == 2) {
      return Range.closed(list.get(0).date, list.get(1).date);
    }
    if (list.get(0).stampType.isBeginInTelework()) {
      return Range.closed(list.get(0).date, setEndOfTheDay(date));
    } else {
      return Range.closed(setBeginOfTheDay(date), list.get(0).date);
    }
  }
  
  /**
   * Il localDateTime dell'inizio della giornata.
   * @param date la data di riferimento
   * @return il localdatetime rappresentante l'inizio della giornata.
   */
  private LocalDateTime setBeginOfTheDay(LocalDate date) {
    return new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0);
  }
  
  /**
   * Il localDateTime della fine della giornata.
   * @param date la data di riferimento
   * @return il localdatetime rappresentante la fine della giornata.
   */
  private LocalDateTime setEndOfTheDay(LocalDate date) {
    return new LocalDateTime(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0)
        .hourOfDay().withMaximumValue().minuteOfHour().withMaximumValue();
  }
  
  /**
   * Verifica se la timbratura stamping è inseribile nel giorno pd come timbratura di inizio
   * lavoro in telelavoro.
   * @param pd il personday del giorno
   * @param stamping la timbratura in telelavoro da inserire
   * @return l'opzionale contenente l'eventuale errore riscontrato nell'inserire 
   *     la timbratura nel giorno.
   */
  private Optional<Errors> checkBeginTelework(PersonDay pd, TeleworkStamping stamping) {
    List<TeleworkStamping> meal = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndMealInTelework());
    List<TeleworkStamping> beginEnd = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndTelework());
    List<TeleworkStamping> interruptions = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndInterruptionInTelework());
    if (beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty()) {
      return Optional.absent();
    }
    java.util.Optional<TeleworkStamping> stamp = beginEnd.stream()
        .filter(tws -> tws.stampType.equals(TeleworkStampTypes.INIZIO_TELELAVORO)).findFirst();
    if (stamp.isPresent()) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.BEGIN_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Inizio telelavoro già presente";
      return Optional.of(error);
    }
    Range<LocalDateTime> beginEndRange = getStampingRange(beginEnd, pd.date);
    if (!beginEndRange.contains(stamping.date)) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.BEGIN_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Inizio telelavoro successivo alla data di fine telelavoro";
      return Optional.of(error);
    }
    return Optional.absent();
  }
  
  /**
   * Verifica se la timbratura stamping è inseribile nel giorno pd come timbratura di fine
   * lavoro in telelavoro.
   * @param pd il personday del giorno
   * @param stamping la timbratura in telelavoro da inserire
   * @return l'opzionale contenente l'eventuale errore riscontrato nell'inserire 
   *     la timbratura nel giorno.
   */
  private Optional<Errors> checkEndInTelework(PersonDay pd, TeleworkStamping stamping) {
    List<TeleworkStamping> meal = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndMealInTelework());
    List<TeleworkStamping> beginEnd = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndTelework());
    List<TeleworkStamping> interruptions = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndInterruptionInTelework());
    if (beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty()) {
      return Optional.absent();
    }
    java.util.Optional<TeleworkStamping> stamp = beginEnd.stream()
        .filter(tws -> tws.stampType.equals(TeleworkStampTypes.FINE_TELELAVORO)).findFirst();
    if (stamp.isPresent()) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.END_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Fine telelavoro già presente";
      return Optional.of(error);
    }
    Range<LocalDateTime> beginEndRange = getStampingRange(beginEnd, pd.date);
    if (!beginEndRange.contains(stamping.date)) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.END_STAMPING_BEFORE_BEGIN;
      error.personDay = pd;
      error.advice = "Fine telelavoro precedente alla data di inizio telelavoro";
      return Optional.of(error);
    }
    return Optional.absent();
  }
  
  /**
   * Verifica se la timbratura stamping è inseribile nel giorno pd come timbratura di inizio
   * pranzo in telelavoro.
   * @param pd il personday del giorno
   * @param stamping la timbratura in telelavoro da inserire
   * @return l'opzionale contenente l'eventuale errore riscontrato nell'inserire 
   *     la timbratura nel giorno.
   */
  private Optional<Errors> checkBeginMealInTelework(PersonDay pd, TeleworkStamping stamping) {
    List<TeleworkStamping> meal = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndMealInTelework());
    List<TeleworkStamping> beginEnd = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndTelework());
    List<TeleworkStamping> interruptions = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndInterruptionInTelework());
    if (beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty()) {
      return Optional.absent();
    }
    java.util.Optional<TeleworkStamping> stamp = beginEnd.stream()
        .filter(tws -> tws.stampType.equals(TeleworkStampTypes.INIZIO_PRANZO_TELELAVORO)).findFirst();
    if (stamp.isPresent()) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.MEAL_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Inizio pranzo in telelavoro già presente";
      return Optional.of(error);
    }
    Range<LocalDateTime> beginEndRange = getStampingRange(beginEnd, pd.date);
    Range<LocalDateTime> mealRange = getStampingRange(meal, pd.date);
    if (!beginEndRange.contains(stamping.date)) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.MEAL_STAMPING_OUT_OF_BOUNDS;
      error.personDay = pd;
      error.advice = 
          "Orario di pausa pranzo in telelavoro fuori dalla fascia inizio-fine telelavoro";
      return Optional.of(error);
    }
    if (!mealRange.contains(stamping.date)) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.EXISTING_END_STAMPING_BEFORE_BEGIN_MEAL;
      error.personDay = pd;
      error.advice = "Orario di inizio pausa pranzo in telelavoro successivo "
          + "orario di fine pranzo in telelavoro";
      return Optional.of(error);
    }
    return Optional.absent();
  }
  
  /**
   * Verifica se la timbratura stamping è inseribile nel giorno pd come timbratura di fine
   * lavoro in telelavoro.
   * @param pd il personday del giorno
   * @param stamping la timbratura in telelavoro da inserire
   * @return l'opzionale contenente l'eventuale errore riscontrato nell'inserire 
   *     la timbratura nel giorno.
   */
  private Optional<Errors> checkEndMealInTelework(PersonDay pd, TeleworkStamping stamping) {
    List<TeleworkStamping> meal = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndMealInTelework());
    List<TeleworkStamping> beginEnd = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndTelework());
    List<TeleworkStamping> interruptions = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndInterruptionInTelework());
    if (beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty()) {
      return Optional.absent();
    }
    java.util.Optional<TeleworkStamping> stamp = beginEnd.stream()
        .filter(tws -> tws.stampType.equals(TeleworkStampTypes.FINE_PRANZO_TELELAVORO)).findFirst();
    if (stamp.isPresent()) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.MEAL_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Fine pranzo in telelavoro già presente";
      return Optional.of(error);
    }
    Range<LocalDateTime> beginEndRange = getStampingRange(beginEnd, pd.date);
    Range<LocalDateTime> mealRange = getStampingRange(meal, pd.date);
    if (!beginEndRange.contains(stamping.date)) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.MEAL_STAMPING_OUT_OF_BOUNDS;
      error.personDay = pd;
      error.advice = 
          "Orario di pausa pranzo in telelavoro fuori dalla fascia inizio-fine telelavoro";
      return Optional.of(error);
    }
    if (!mealRange.contains(stamping.date)) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.EXISTING_BEGIN_STAMPING_AFTER_END_MEAL;
      error.personDay = pd;
      error.advice = "Orario di inizio pausa pranzo in telelavoro successivo "
          + "orario di fine pranzo in telelavoro";
      return Optional.of(error);
    }
    return Optional.absent();
  }
  
  /**
   * Metodo di utilità che trasforma uno joda LocalDateTime in un java.time LocalDateTime.
   * @param dateTime lo joda contenente la data/ora
   * @return la data/ora nel formato java.time.
   */
  private java.time.LocalDateTime transform(LocalDateTime dateTime) {
    java.time.LocalDateTime time = java.time.LocalDateTime.of(
        dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), 
        dateTime.getHourOfDay(), dateTime.getMinuteOfHour(), dateTime.getSecondOfMinute());
    return time;
  }
  
  /**
   * 
   * @param dto
   * @return
   */
  private TeleworkStamping mapper(TeleworkDto dto) {
    ModelMapper modelMapper = new ModelMapper();
    TeleworkStamping stamping = modelMapper.map(dto, TeleworkStamping.class);
    return stamping;
  }
}
