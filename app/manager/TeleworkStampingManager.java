/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package manager;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import dao.absences.AbsenceComponentDao;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import manager.recaps.personstamping.PersonStampingDayRecap;
import manager.recaps.personstamping.PersonStampingRecap;
import manager.services.absences.AbsenceForm;
import manager.services.absences.AbsenceService;
import manager.services.absences.AbsenceService.InsertReport;
import manager.services.telework.errors.Errors;
import manager.services.telework.errors.TeleworkStampingError;
import manager.telework.service.TeleworkComunication;
import models.Person;
import models.PersonDay;
import models.absences.Absence;
import models.absences.AbsenceType;
import models.absences.GroupAbsenceType;
import models.absences.JustifiedType;
import models.absences.JustifiedType.JustifiedTypeName;
import models.absences.definitions.DefaultGroup;
import models.dto.NewTeleworkDto;
import models.dto.TeleworkDto;
import models.dto.TeleworkPersonDayDto;
import models.enumerate.TeleworkStampTypes;
import org.joda.time.LocalDate;



/**
 * Classe per l'interfacciamento con il servizio REST di Timbrature per telelavoro.
 *
 */
@Slf4j
public class TeleworkStampingManager {

  private TeleworkComunication comunication;
  private AbsenceComponentDao absenceDao;
  private AbsenceManager absenceManager;
  private AbsenceService absenceService;
  private PersonDayManager personDayManager;

  /**
   * Injector.
   *
   * @param comunication il service per la comunicazione con la piattaforma telework-stampings
   * @param absenceDao il dao delle assenze
   * @param absenceManager il manager delle assenze
   * @param absenceService il service delle assenze
   * @param personDayManager il manager del person day
   */
  @Inject
  public TeleworkStampingManager(TeleworkComunication comunication, AbsenceComponentDao absenceDao,
      AbsenceManager absenceManager, AbsenceService absenceService, 
      PersonDayManager personDayManager) {
    this.comunication = comunication;
    this.absenceDao = absenceDao;
    this.absenceManager = absenceManager;
    this.absenceService = absenceService;
    this.personDayManager = personDayManager;
  }

  /**
   * Ritorna la lista di timbrature in telelavoro nel giorno pd con causale appartenente a quelle
   * riportate nella lista di causali da ricercare.
   *
   * @param pd il personDay di riferimento
   * @param stampTypes la lista di causali da cercare
   * @return la lista di timbrature di lavoro in telelavoro con causale quelle passate come 
   *     parametro.
   */
  public List<TeleworkDto> getSpecificTeleworkStampings(PersonDay pd, 
      List<TeleworkStampTypes> stampTypes) {
    if (pd.id == null) {
      return Lists.newArrayList();
    }
    @val
    java.util.List<models.dto.TeleworkDto> teleworkStampings;
    try {      
      teleworkStampings = comunication.getList(pd.id);
    } catch (NoSuchFieldException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    List<TeleworkDto> list = Lists.newArrayList();
    for (TeleworkDto tws : teleworkStampings) {
      val stampType = tws.getStampType();
      for (TeleworkStampTypes st : stampTypes) {
        if (stampType.equals(st)) {
          list.add(tws);
        }
      }
    }
    return list;
  }

  /**
   * Chiama il metodo di comunicazione con l'applicazione esterna per salvare la timbratura
   * da telelavoro.
   *
   * @param stamping la timbratura in telelavoro da salvare
   * @return 201 se la timbratura è stata salvata correttamente, altro numero altrimenti.
   */
  public int save(TeleworkDto stamping) {
    int result = 0;
    try {
      result = comunication.save(stamping);
    } catch (NoSuchFieldException ex) {
      log.error("Errore nel salvataggio timbratura {}", stamping);
    }
    return result;
  }

  /**
   * Metodo che modifica una timbratura in telelavoro.
   *
   * @param stamping la timbratura in telelavoro da modificare
   * @return il codice HTTP con il risultato della update della timbratura.
   */
  public int update(TeleworkDto stamping) {
    int result = 0;
    try {
      result = comunication.update(stamping);
    } catch (NoSuchFieldException ex) {
      log.error("Errore durante l'aggiornanto della timbratura {}", stamping);
      return result;
    }
    return result;
  }

  /**
   * Chiama la funzionalità di cancellazione della timbratura da telelavoro.
   *
   * @param stampingId l'identificativo della timbratura da eliminare
   * @return 200 se la timbratura è stata eliminata correttamente, altro numero altrimenti.
   */
  public int delete(long stampingId) {
    int result = 0;
    try {
      result = comunication.delete(stampingId);
    } catch (NoSuchFieldException ex) {
      log.error("Errore durante la cancellazione della timbratura id = {}", 
          stampingId);
      return result;
    }
    return result;
  }

  /**
   * Ritorna la timbratura in telelavoro con id specificato.
   *
   * @param stampingId l'identificativo della timbratura da ricercare
   * @return la timbratura in telelavoro con id passato come parametro.
   * @throws ExecutionException eccezione in esecuzione
   */
  public TeleworkDto get(long stampingId) throws ExecutionException {
    TeleworkDto stamping = null;
    try {
      stamping = comunication.get(stampingId);
    } catch (NoSuchFieldException ex) {
      log.error("Errore prelevando la timbratura con id = {}", stampingId);
    }
    return stamping;
  }

  /**
   * Ritorna la lista di dto contenente la lista delle timbrature in telelavoro per ogni personDay.
   *
   * @param psDto il personStampingRecap mensile da cui prendere le info sui personDay
   * @return la lista di dto da ritornare alla vista.
   * @throws NoSuchFieldException eccezione di mancanza di parametro
   * @throws ExecutionException eccezione in esecuzione
   */
  public List<TeleworkPersonDayDto> getMonthlyStampings(PersonStampingRecap psDto) 
      throws NoSuchFieldException, ExecutionException {
    List<TeleworkPersonDayDto> dtoList = Lists.newArrayList();
    List<PersonStampingDayRecap> pastDaysRecap = 
        psDto.daysRecap.stream().filter(d -> {
          return d.personDay.getDate().isBefore(LocalDate.now().plusDays(1));
        }).collect(Collectors.toList());
    for (PersonStampingDayRecap day : pastDaysRecap) {
      List<TeleworkDto> beginEnd = Lists.newArrayList();
      List<TeleworkDto> meal = Lists.newArrayList();
      List<TeleworkDto> interruptions = Lists.newArrayList();
      List<TeleworkDto> list = Lists.newArrayList();
      if (day.personDay.id == null) {
        log.trace("PersonDay con id nullo in data {}, creo l'oggetto.", day.personDay.getDate());
      } else {
        list = comunication.getList(day.personDay.id);        
      }
      
      if (list.isEmpty()) {
        //TODO: aggiungere il pezzo in cui si creano i teleworkDto vuoti nel caso non esistano 
        log.trace("Non ci sono timbrature associate al giorno in applicazione telework-stampings!");

      } else {
        for (TeleworkDto stamping : list) {    
          TeleworkStampTypes stampType = stamping.getStampType();
          if (stampType.equals(TeleworkStampTypes.INIZIO_TELELAVORO) 
              || stampType.equals(TeleworkStampTypes.FINE_TELELAVORO)) {
            beginEnd.add(stamping);
          }
          if (stampType.equals(TeleworkStampTypes.INIZIO_PRANZO_TELELAVORO) 
              || stampType.equals(TeleworkStampTypes.FINE_PRANZO_TELELAVORO)) {
            meal.add(stamping);
          }
          if (stampType.equals(TeleworkStampTypes.INIZIO_INTERRUZIONE) 
              || stampType.equals(TeleworkStampTypes.FINE_INTERRUZIONE)) {
            interruptions.add(stamping);
          }          
        }
      }      

      Comparator<TeleworkDto> comparator = (TeleworkDto m1, TeleworkDto m2) 
          -> m1.getDate().compareTo(m2.getDate());
      Collections.sort(beginEnd, comparator);
      Collections.sort(meal, comparator);
      Collections.sort(interruptions, comparator);      

      TeleworkPersonDayDto teleworkDto = TeleworkPersonDayDto.builder()
          .personDay(day.personDay)
          .beginEnd(beginEnd)
          .meal(meal)
          .interruptions(interruptions)
          .build();

      dtoList.add(teleworkDto);
    }


    return dtoList;
  }
  
  /**
   * Ritorna la lista degli oggetti contenenti le info sulle giornate di telelavoro.
   *
   * @param psDto il recap delle timbrature
   * @return la lista di oggetti NewTeleworkDto contenenti le info per il telelavoro.
   * @throws NoSuchFieldException eccezione di oggetto non trovato
   * @throws ExecutionException eccezione in esecuzione
   */
  public List<NewTeleworkDto> stampingsForReport(PersonStampingRecap psDto) 
      throws NoSuchFieldException, ExecutionException {
    List<PersonStampingDayRecap> pastDaysRecap = 
        psDto.daysRecap.stream().filter(d -> {
          return d.personDay.getDate().isBefore(LocalDate.now().plusDays(1));
        }).collect(Collectors.toList());
    List<NewTeleworkDto> dtoList = Lists.newArrayList();
    for (PersonStampingDayRecap day : pastDaysRecap) {
     
      List<TeleworkDto> list = Lists.newArrayList();
      if (day.personDay.id == null) {
        log.trace("PersonDay con id nullo in data {}, creo l'oggetto.", day.personDay.getDate());
      } else {
        list = comunication.getList(day.personDay.id);        
      }
            
      if (list.isEmpty()) {
        //TODO: aggiungere il pezzo in cui si creano i teleworkDto vuoti nel caso non esistano 
        log.trace("Non ci sono timbrature associate al giorno in applicazione telework-stampings!");

      } else {  
        java.time.LocalDate date = null;
        NewTeleworkDto dto = null;
        for (TeleworkDto stamping : list) {
          if (date == null || !stamping.getDate().toLocalDate().isEqual(date)) {
            dto = new NewTeleworkDto();
            date = stamping.getDate().toLocalDate();
            dto.date = stamping.getDate().toLocalDate();
          }
          
          TeleworkStampTypes stampType = stamping.getStampType();
          if (stampType.equals(TeleworkStampTypes.INIZIO_TELELAVORO)) {
            dto.beginDay = stamping;
          }
          if (stampType.equals(TeleworkStampTypes.FINE_TELELAVORO)) {
            dto.endDay = stamping;
          }
          if (stampType.equals(TeleworkStampTypes.INIZIO_PRANZO_TELELAVORO)) {
            dto.beginMeal = stamping;
          }
          if (stampType.equals(TeleworkStampTypes.FINE_PRANZO_TELELAVORO)) {
            dto.endMeal = stamping;
          }
          if (stampType.equals(TeleworkStampTypes.INIZIO_INTERRUZIONE)) {
            dto.beginInterruption = stamping;
          }
          if (stampType.equals(TeleworkStampTypes.FINE_INTERRUZIONE)) {
            dto.endInterruption = stamping;
          }       
        }
        dtoList.add(dto);
      }      
    }
    return dtoList;
  }
  

  /**
   * Verifica se l'inserimento di una timbratura in un giorno può dare origine ad un errore di 
   * malformazione della lista di timbrature.
   *
   * @param stamping la timbratura in telelavoro
   * @param pd il personday del giorno
   * @return l'opzionale contenente l'errore rilevato dal possibile inserimento della timbratura
   *     in un giorno di telelavoro. 
   */
  public Optional<Errors> checkTeleworkStamping(TeleworkDto stamping, PersonDay pd) {

    val stampType = stamping.getStampType();
    Optional<Errors> error = Optional.absent();
    if (stampType.equals(TeleworkStampTypes.INIZIO_TELELAVORO)) {
      error = checkBeginTelework(pd, stamping);
    }
    if (stampType.equals(TeleworkStampTypes.FINE_TELELAVORO)) {
      error = checkEndInTelework(pd, stamping);
    }
    if (stampType.equals(TeleworkStampTypes.INIZIO_PRANZO_TELELAVORO)) {
      error = checkBeginMealInTelework(pd, stamping);
    }

    if (stampType.equals(TeleworkStampTypes.FINE_PRANZO_TELELAVORO)) {
      error = checkEndMealInTelework(pd, stamping);
    }
    if (stampType.equals(TeleworkStampTypes.INIZIO_INTERRUZIONE)) {
      //TODO: verificare come e se completare...
    }
    if (stampType.equals(TeleworkStampTypes.FINE_INTERRUZIONE)) {
      //TODO: verificare come e se completare...
    }
    return error;
  }

  private Range<LocalDateTime> getStampingRange(List<TeleworkDto> list, LocalDate date) {

    if (list.isEmpty()) {      
      return Range.closed(setBeginOfTheDay(date), setEndOfTheDay(date));
    }
    if (list.size() == 2) {
      return Range.closed(list.get(0).getDate(), list.get(1).getDate());
    }
    if (list.get(0).getStampType().isBeginInTelework()) {
      return Range.closed(list.get(0).getDate(), setEndOfTheDay(date));
    } else {
      return Range.closed(setBeginOfTheDay(date), list.get(0).getDate());
    }
  }

  /**
   * Metodo che consente l'inserimento del codice 103RT per i ricercatori e tecnologi che
   * inseriscono timbrature in telelavoro che concorrono alla formazione di residuo sul cartellino.
   *
   * @param person la persona per cui inserire l'assenza
   * @param date la data in cui inserire l'assenza
   */
  public void insertTeleworkAbsenceCode(Person person, LocalDate date) {
    
    GroupAbsenceType groupAbsenceType = absenceDao
        .groupAbsenceTypeByName(DefaultGroup.TELELAVORO_RICERCATORI_TECNOLOGI.name()).get();
    JustifiedType type = absenceDao
        .getOrBuildJustifiedType(JustifiedTypeName.nothing);
    AbsenceType absenceType = absenceDao.absenceTypeByCode("103RT").get();
    AbsenceForm absenceForm = absenceService.buildAbsenceForm(person,
        date, null, date, null, groupAbsenceType,
        false, absenceType, type, null, null, false, false);
    InsertReport insertReport =
        absenceService.insert(person, absenceForm.groupSelected, absenceForm.from,
            absenceForm.to, absenceForm.absenceTypeSelected, absenceForm.justifiedTypeSelected,
            null, null, false, absenceManager);
    if (insertReport.criticalErrors.isEmpty()) {
      for (Absence absence : insertReport.absencesToPersist) {
        PersonDay personDay = personDayManager.getOrCreateAndPersistPersonDay(person,
            absence.getAbsenceDate());
        absence.setPersonDay(personDay);
        
        personDay.getAbsences().add(absence);
        absence.save();
        personDay.save();
      }
    }
    
  }
  
  /**
   * Cancella l'assenza 103RT dal cartellino del dipendente.
   *
   * @param pd il personDay relativo al giorno in cui rimuovere l'assenza
   */
  public void deleteTeleworkAbsenceCode(PersonDay pd) {
    AbsenceType absenceType = absenceDao.absenceTypeByCode("103RT").get();
    int deleted = absenceManager
        .removeAbsencesInPeriod(pd.getPerson(), pd.getDate(), pd.getDate(), absenceType);

    if (deleted > 0) {
      log.info("Rimossi {} codici assenza di tipo {}", deleted, absenceType.getCode());
    } else {
      log.info("Nessun codice d'assenza eliminato");
    }
  }
  
  
  /**
   * Il localDateTime dell'inizio della giornata.
   *
   * @param date la data di riferimento
   * @return il localdatetime rappresentante l'inizio della giornata.
   */
  private LocalDateTime setBeginOfTheDay(LocalDate date) {
    return LocalDateTime.of(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 0, 0);
  }

  /**
   * Il localDateTime della fine della giornata.
   *
   * @param date la data di riferimento
   * @return il localdatetime rappresentante la fine della giornata.
   */
  private LocalDateTime setEndOfTheDay(LocalDate date) {
    return LocalDateTime.of(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth(), 23, 59);
  }

  /**
   * Verifica se la timbratura stamping è inseribile nel giorno pd come timbratura di inizio
   * lavoro in telelavoro.
   *
   * @param pd il personday del giorno
   * @param stamping la timbratura in telelavoro da inserire
   * @return l'opzionale contenente l'eventuale errore riscontrato nell'inserire 
   *     la timbratura nel giorno.
   */
  private Optional<Errors> checkBeginTelework(PersonDay pd, TeleworkDto stamping) {
    List<TeleworkDto> meal = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndMealInTelework());
    List<TeleworkDto> beginEnd = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndTelework());
    List<TeleworkDto> interruptions = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndInterruptionInTelework());
    if (beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty()) {
      return Optional.absent();
    }
    java.util.Optional<TeleworkDto> stamp = beginEnd.stream()
        .filter(tws -> tws.getStampType().equals(TeleworkStampTypes.INIZIO_TELELAVORO)).findFirst();
    if (stamp.isPresent()) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.BEGIN_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Inizio telelavoro già presente";
      return Optional.of(error);
    }
    Range<LocalDateTime> beginEndRange = getStampingRange(beginEnd, pd.getDate());
    if (!beginEndRange.contains(stamping.getDate())) {
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
   *
   * @param pd il personday del giorno
   * @param stamping la timbratura in telelavoro da inserire
   * @return l'opzionale contenente l'eventuale errore riscontrato nell'inserire 
   *     la timbratura nel giorno.
   */
  private Optional<Errors> checkEndInTelework(PersonDay pd, TeleworkDto stamping) {
    List<TeleworkDto> meal = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndMealInTelework());
    List<TeleworkDto> beginEnd = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndTelework());
    List<TeleworkDto> interruptions = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndInterruptionInTelework());
    if (beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty()) {
      return Optional.absent();
    }
    java.util.Optional<TeleworkDto> stamp = beginEnd.stream()
        .filter(tws -> tws.getStampType().equals(TeleworkStampTypes.FINE_TELELAVORO)).findFirst();
    if (stamp.isPresent()) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.END_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Fine telelavoro già presente";
      return Optional.of(error);
    }
    Range<LocalDateTime> beginEndRange = getStampingRange(beginEnd, pd.getDate());
    if (!beginEndRange.contains(stamping.getDate())) {
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
   *
   * @param pd il personday del giorno
   * @param stamping la timbratura in telelavoro da inserire
   * @return l'opzionale contenente l'eventuale errore riscontrato nell'inserire 
   *     la timbratura nel giorno.
   */
  private Optional<Errors> checkBeginMealInTelework(PersonDay pd, TeleworkDto stamping) {
    List<TeleworkDto> meal = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndMealInTelework());
    List<TeleworkDto> beginEnd = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndTelework());
    List<TeleworkDto> interruptions = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndInterruptionInTelework());
    if (beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty()) {
      return Optional.absent();
    }
    java.util.Optional<TeleworkDto> stamp = beginEnd.stream()
        .filter(tws -> tws.getStampType().equals(TeleworkStampTypes.INIZIO_PRANZO_TELELAVORO))
        .findFirst();
    if (stamp.isPresent()) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.MEAL_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Inizio pranzo in telelavoro già presente";
      return Optional.of(error);
    }
    Range<LocalDateTime> beginEndRange = getStampingRange(beginEnd, pd.getDate());
    Range<LocalDateTime> mealRange = getStampingRange(meal, pd.getDate());
    if (!beginEndRange.contains(stamping.getDate())) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.MEAL_STAMPING_OUT_OF_BOUNDS;
      error.personDay = pd;
      error.advice = 
          "Orario di pausa pranzo in telelavoro fuori dalla fascia inizio-fine telelavoro";
      return Optional.of(error);
    }
    if (!mealRange.contains(stamping.getDate())) {
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
   *
   * @param pd il personday del giorno
   * @param stamping la timbratura in telelavoro da inserire
   * @return l'opzionale contenente l'eventuale errore riscontrato nell'inserire 
   *     la timbratura nel giorno.
   */
  private Optional<Errors> checkEndMealInTelework(PersonDay pd, TeleworkDto stamping) {
    List<TeleworkDto> meal = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndMealInTelework());
    List<TeleworkDto> beginEnd = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndTelework());
    List<TeleworkDto> interruptions = 
        getSpecificTeleworkStampings(pd, TeleworkStampTypes.beginEndInterruptionInTelework());
    if (beginEnd.isEmpty() && meal.isEmpty() && interruptions.isEmpty()) {
      return Optional.absent();
    }
    java.util.Optional<TeleworkDto> stamp = beginEnd.stream()
        .filter(tws -> tws.getStampType().equals(TeleworkStampTypes.FINE_PRANZO_TELELAVORO))
        .findFirst();
    if (stamp.isPresent()) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.MEAL_STAMPING_PRESENT;
      error.personDay = pd;
      error.advice = "Fine pranzo in telelavoro già presente";
      return Optional.of(error);
    }
    Range<LocalDateTime> beginEndRange = getStampingRange(beginEnd, pd.getDate());
    Range<LocalDateTime> mealRange = getStampingRange(meal, pd.getDate());
    if (!beginEndRange.contains(stamping.getDate())) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.MEAL_STAMPING_OUT_OF_BOUNDS;
      error.personDay = pd;
      error.advice = 
          "Orario di pausa pranzo in telelavoro fuori dalla fascia inizio-fine telelavoro";
      return Optional.of(error);
    }
    if (!mealRange.contains(stamping.getDate())) {
      Errors error = new Errors();
      error.error = TeleworkStampingError.EXISTING_BEGIN_STAMPING_AFTER_END_MEAL;
      error.personDay = pd;
      error.advice = "Orario di inizio pausa pranzo in telelavoro successivo "
          + "orario di fine pranzo in telelavoro";
      return Optional.of(error);
    }
    return Optional.absent();
  }

}