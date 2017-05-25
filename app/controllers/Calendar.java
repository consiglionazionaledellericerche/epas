package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dao.AbsenceDao;
import dao.PersonDao;
import dao.PersonDayDao;
import dao.PersonShiftDayDao;
import dao.ShiftDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;

import models.Person;
import models.PersonDay;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftCancelled;
import models.ShiftType;
import models.User;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.ShiftEvent;
import models.enumerate.ShiftSlot;

import org.jcolorbrewer.ColorBrewer;
import org.joda.time.LocalDate;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.With;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;

/**
 * @author arianna
 * @author daniele
 * @since 15/05/17.
 */
@With(Resecure.class)
@Slf4j
public class Calendar extends Controller {

  @Inject
  static ShiftDao shiftDao;
  @Inject
  private static PersonDao personDao;
  @Inject
  private static PersonDayDao personDayDao;
  @Inject
  private static PersonShiftDayDao personShiftDayDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  static ObjectMapper mapper;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  static AbsenceDao absenceDao;

  public static void show(ShiftType activity) {
    User currentUser = Security.getUser().get();

    List<ShiftType> activities = currentUser.person.shiftCategories.stream()
        .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
        .collect(Collectors.toList());

    final ShiftType activitySelected = activity.id != null ? activity : activities.get(0);
    
    List<Person> people = activitySelected.personShiftShiftTypes.stream()
        .filter(personShiftShiftType -> personShiftShiftType.dateRange().contains(LocalDate.now()))
        .map(personShiftShiftType -> personShiftShiftType.personShift.person )
        .collect(Collectors.toList());

    render(activities, activitySelected, people);
  }


  /*
   * Carica i turni dal database per essere visualizzati nel calendario
   */
  public static void events(ShiftType shiftType, LocalDate start, LocalDate end)
      throws JsonProcessingException {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci

    List<ShiftEvent> events = new ArrayList<>();

    List<Person> people = shiftType.personShiftShiftTypes.stream()
        .filter(personShiftShiftType -> personShiftShiftType.dateRange().contains(start))
        .map(personShiftShiftType -> personShiftShiftType.personShift.person )
        .collect(Collectors.toList());

    int index = 0;

    ColorBrewer sequentialPalettes = ColorBrewer.valueOf("YlOrBr");
    Color[] myGradient = sequentialPalettes.getColorPalette(11);
    
    // prende i turni associati alle persone attive in quel turno
    for (Person person : people) {
      final String color = "#" + Integer.toHexString(myGradient[index].getRGB() & 0xffffff);
      events.addAll(shiftEvents(shiftType, person, start, end, color));
      events.addAll(absenceEvents(person, start, end));
      index++;
    }

    // Usato il jackson per facilitare la serializzazione dei LocalDate
    renderJSON(mapper.writeValueAsString(events));
  }


  /*
   * Carica la lista dei turni di un certo tipo associati ad una determinata persona in
   * un intervallo di tempo
   * 
   * @param ShiftType shiftType: tipo di turno
   * @param Person person: persona associata ai turni
   * @param LocalDate start: data inizio intervallo di tempo
   * @param LocalDate end: data fine intervallo di tempo
   * 
   * @output List<ShiftEvent>: lista di eventi 
   */
  private static List<ShiftEvent> shiftEvents(ShiftType shiftType, Person person, LocalDate start,
      LocalDate end, String color) {

    return shiftDao.getPersonShiftDaysByPeriodAndType(start, end, shiftType, person).stream()
        .map(shiftDay -> {

          return ShiftEvent.builder()
              .allDay(true)
              .shiftSlot(shiftDay.shiftSlot)
              .personShiftDayId(shiftDay.id)
              .title(shiftDay.getSlotTime() + '\n' + person.fullName())
              .start(shiftDay.date)
              .color(color)
              .className("removable")
              .textColor("black")
              .borderColor("black")
              .build();

        }).collect(Collectors.toList());
  }
  
  public static void deleteShift(PersonShiftDay psd) {
    log.debug("lol");
    notFoundIfNull(psd);
    response.status = Http.StatusCode.BAD_REQUEST;
    renderText("Un messaggio qualsiasi");
    //psd.delete();
    
  }

  private static List<ShiftEvent> absenceEvents(Person person, LocalDate start, LocalDate end) {

    final List<JustifiedTypeName> types = ImmutableList
        .of(JustifiedTypeName.all_day, JustifiedTypeName.assign_all_day);

    List<Absence> absences = absenceDao
        .findByPersonAndDate(person, start, Optional.fromNullable(end), Optional.absent()).list()
        .stream()
        .filter(absence -> types.contains(absence.justifiedType.name))
        .sorted(Comparator.comparing(ab -> ab.personDay.date)).collect(Collectors.toList());
    List<ShiftEvent> events = new ArrayList<>();
    ShiftEvent event = null;

    for (Absence abs : absences) {

      /**
       * Per quanto riguarda gli eventi 'allDay':
       *
       * La convensione del fullcalendar è quella di avere il parametro end = null
       * nel caso di eventi su un singolo giorno, mentre nel caso di evento su più giorni il
       * parametro end assume il valore del giorno successivo alla fine effettiva
       * (perchè ne imposta l'orario alla mezzanotte).
       */
      if (event == null
          || event.getEnd() == null && !event.getStart().plusDays(1).equals(abs.personDay.date)
          || event.getEnd() != null && !event.getEnd().equals(abs.personDay.date)) {
        event = ShiftEvent.builder()
            .allDay(true)
            .title("Assenza di " + abs.personDay.person.fullName())
            .start(abs.personDay.date)
            .durationEditable(false)
            .editable(false)
            .startEditable(false)
            //.color("#"+Integer.toHexString(myGradient[index].getRGB() & 0xffffff))
            .build();

        events.add(event);
      } else {
        event.setEnd(abs.personDay.date.plusDays(1));
      }

    }

    return events;
  }



  /*
   * Chiamata dal fullCalendar dei turni per ogni evento di drop o resize di un turno sul calendario.
   * Controlla se i nuovi turni possono essere salvati ed eventualmente li salva
   * 
   */
  public static void changeShift(long personShiftDayId, LocalDate newDate) {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci
    log.debug("Chiamato metodo changeShift: personShiftDayId {} - newDate {} ", personShiftDayId, newDate);

    //    final ShiftType type = ShiftType.findById(Long.parseLong(session.get("currentShiftActivity")));
    //    
    //    final Person person = personDao.getPersonById(personId);
    //    if (person == null) {
    //      badRequest("Parametri mancanti: person = {} " + person);
    //    }
    //    
    //    List<String> errors = new ArrayList<>();
    //
    //    // se il turno è cancellato non può essere sovrapposto a turni esistenti
    //    if (cancelled) {
    //      LocalDate day = start;
    //      while (day.isBefore(end.plusDays(1))) {
    //        List<PersonShiftDay> newDays = shiftDao.getShiftDaysByPeriodAndType(day, day, type);
    //        if (!newDays.isEmpty()) {
    //          String msg = "Turno già esistente nel giorno " + day.toString("dd MMM") + "\n";
    //          errors.add(msg);
    //        }  
    //      }
    //      
    //      // se non ci sono stati errori cancella i turni vecchi e salva i nuovi
    //      if (errors.isEmpty()) {
    //        cancDeletedShifts(start, end, type);
    //        saveDeletedShifts(start, end, type);
    //      } else {
    //        String error = errors.toString();
    //        response.status = Http.StatusCode.BAD_REQUEST;
    //        renderText(error);
    //      }
    //      
    //    } else {
    //
    //      List<PersonShiftDay> newEvents = new ArrayList<>();
    //      List<PersonShiftDay> oldEvents = new ArrayList<>();
    //      
    //      // per ogni turno che voglio modificare controllo se è compatibile con la nuova data
    //      oldEvents = shiftDao.getPersonShiftDaysByPeriodAndType(originalStart, originalEnd, type, person);
    //      LocalDate currDate = originalStart;
    //      while (currDate.isBefore(originalEnd.plusDays(1))) {
    //        // TODO: è unico vero?
    //        PersonShiftDay oldEvent = shiftDao.getPersonShiftDaysByPeriodAndType(currDate, currDate, type, person).get(0);
    //   
    //        List<String> messages = new ArrayList<>();
    //        messages = checkShiftDay(oldEvent, currDate);
    //        if (!messages.isEmpty()) {
    //          errors.addAll(messages);
    //        } else if (errors.isEmpty()) {   
    //           // se non ci sono stati errori salvo il nuovo evento
    //           oldEvent.date = currDate;
    //           newEvents.add(oldEvent);
    //        }
    //      } 
    //      
    //      // se non ci sono stati errori cancella i turni vecchi e salva i nuovi
    //      if (errors.isEmpty()) {
    //        cancShifts(oldEvents);
    //        saveShifts(newEvents);
    //      } else {
    //        String error = errors.toString();
    //        response.status = Http.StatusCode.BAD_REQUEST;
    //        renderText(error);
    //      }
    //      
    //    }
  }


  /*
   * Controlla la compatibilità nello spostamento di un turno in un determinato giorno 
   * 
   * @param PersonShiftDay shift: turno da controllare
   * @param LocalDate newDate: data nella quale spostare il turno
   */
  public static List<String> checkShiftDay(PersonShiftDay shift, LocalDate newDate) {
    String message = "";
    List<String> errors = new ArrayList<>();

    // TODO: controlla che i turni che voglio modificare non siano già stati inviati a Roma
    if (false) {

    } else {
      // controlla la compatibilità con le presenze della persona e
      // gli altri turni presenti nel giorno di destinazione
      message = checkShiftDayWhithShiftDays(shift, newDate);
      if (!message.isEmpty()) {
        errors.add(message);
      } 
      message = checkShiftDayWhithPresence(shift, newDate);
      if (!message.isEmpty()) {
        errors.add(message);
      } 
    }

    return errors;
  }


  /*
   * Controlla la compatibilità del giorno di turno con le presenze
   * e l'orraio di lavoro nel giorno passato come parametro 
   * 
   * @param PersonShiftDay day - giorno di turno
   */
  public static String checkShiftDayWhithPresence(PersonShiftDay shift, LocalDate date) {

    Optional<PersonDay> personDay = personDayDao.getPersonDay(shift.personShift.person, date);
    String msg = "";

    // se c'è qualche timbratura o assenza in quel giorno
    if (personDay.isPresent()) {

      // controlla che il nuovo turno non coincida con un giorno di assenza del turnista 
      // ASSENZA o MISSIONE ???????? 
      if (personDayManager.isAllDayAbsences(personDay.get())) {      
        msg = "Il turno di "+ shift.personShift.person.getFullname() +" nel giorno " + shift.date.toString("dd MMM") + " coincide con un suo giorno di assenza";
      } else if (!LocalDate.now().isBefore(shift.date)) {
        // non sono nel futuro controllo le timbrature
        // controlla se non è una giornata valida di lavoro
        // (??????????) MESSAGGI DIVERSI SE 1 TIMBRATURA 0 timbrature o diaccoppiate ecc
        IWrapperPersonDay wrPersonDay = wrapperFactory.create(personDay.get());
        if (!personDayManager.isValidDay(personDay.get(), wrPersonDay)) {
          msg = "Giornata lavorativa non valida il" + shift.date.toString("dd MMM");
        } else {

          // TODO: controlla la compatibilità tra le timbrature e il turno
        }
      } 
    }
    return msg;
  }

  /*
   * Controlla che un turno assegnato possa essere spostato alla data passata come parametro:
   * Nella data non ci devono essere:
   * - turni annullati di quel tipo
   * - turni associati alla stessa persona in slot diversi
   * - turni associati ad altra persona nello stesso slot
   */
  public static String checkShiftDayWhithShiftDays(PersonShiftDay shift, LocalDate date) {
    String msg = "";

    // controlla che non coincida con un turno annullato esistente
    if (shiftDao.getShiftCancelled(shift.date, shift.shiftType) != null) {
      msg = "Il turno è già ANNULLATO nel giorno " + shift.date.toString("dd MMM");
    }

    // per ogni turno esitente in quel periodo di quel tipo 
    for (PersonShiftDay registeredDay : personShiftDayDao.getPersonShiftDayByTypeAndPeriod(shift.date, shift.date, shift.shiftType)) {
      //controlla che il turno in quello slot sia già stato assegnato ad un'altra persona
      if (registeredDay.shiftSlot.equals(shift.shiftSlot) && !registeredDay.personShift.person.equals(shift.personShift.person)) {
        msg = "Turno già esistente il " + shift.date.toString("dd MMM");
      } else if (registeredDay.personShift.person.equals(shift.personShift.person) && !registeredDay.shiftSlot.equals(shift.shiftSlot)) {
        msg = registeredDay.personShift.person.getFullname() + " è già in turno il giorno " + shift.date.toString("dd MMM");
      }
    }

    return msg;
  }

  public static void newShift(long personId, LocalDate date, ShiftSlot shiftSlot,
      ShiftType shiftType) {
    log.debug("CHIAMATA LA CREAZIONE DEL TURNO: personId {} - day {} - slot {} - shiftType {}",
        personId, date, shiftSlot, shiftType);
    // TODO: creare il personShiftDay se rispetta tutti i canoni e le condizioni di possibile esistenza
    // TODO: ricordarsi di controllare se la persona è attiva sull'attività al momento della creazione del 
    // personshiftDay
    // TODO: vediamo se la renderJSON è il metodo migliore per ritornare l'id del personShiftDay creato
    renderJSON(new Random().nextLong());
  }


}
