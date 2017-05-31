package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import dao.AbsenceDao;
import dao.PersonDayDao;
import dao.ShiftDao;
import dao.wrapper.IWrapperFactory;
import dao.wrapper.IWrapperPersonDay;

import lombok.extern.slf4j.Slf4j;

import manager.PersonDayManager;
import manager.ShiftManager2;
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
import models.enumerate.ShiftTroubles;
import models.enumerate.Troubles;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.With;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.PersonDayManager;
import manager.ShiftManager2;
import models.Person;
import models.PersonDay;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ShiftType;
import models.User;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.PNotifyObject;
import models.dto.ShiftEvent;
import models.enumerate.CalendarShiftTroubles;
import models.enumerate.EventColor;
import models.enumerate.ShiftSlot;
import org.joda.time.LocalDate;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.StatusCode;
import play.mvc.With;

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
  private static PersonDayDao personDayDao;
  @Inject
  private static PersonDayManager personDayManager;
  @Inject
  private static ShiftManager2 shiftManager2;
  @Inject
  static ObjectMapper mapper;
  @Inject
  private static IWrapperFactory wrapperFactory;
  @Inject
  static AbsenceDao absenceDao;

  public static void show(ShiftType activity, LocalDate date) {

    User currentUser = Security.getUser().get();
    final LocalDate currentDate = Optional.fromNullable(date).or(LocalDate.now());

    final List<ShiftType> activities = currentUser.person.shiftCategories.stream()
        .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
        .sorted(Comparator.comparing(o -> o.type))
        .collect(Collectors.toList());

    final ShiftType activitySelected = activity.id != null ? activity : activities.get(0);

    render(activities, activitySelected, currentDate);
  }

  public static void shiftPeople(ShiftType activity, LocalDate start, LocalDate end) {

    final List<PersonShiftShiftType> people = shiftWorkers(activity, start, end);

    List<ShiftEvent> eventPeople = new ArrayList<>();
    int index = 0;

    for (PersonShiftShiftType personShift : people) {
      // lenght-1 viene fatto per scludere l'ultimo valore che è dedicato alle assenze
      final EventColor eventColor = EventColor.values()[index % (EventColor.values().length - 1)];
      final Person person = personShift.personShift.person;

      final ShiftEvent event = ShiftEvent.builder()
          .allDay(true)
          .title(person.fullName())
          .personId(person.id)
          .eventColor(eventColor)
          .color(eventColor.backgroundColor)
          .textColor(eventColor.textColor)
          .borderColor(eventColor.borderColor)
          .className("removable")
          .mobile(person.mobile)
          .email(person.email)
          .jolly(personShift.jolly)
          .build();

      eventPeople.add(event);
      index++;
    }

    final List<ShiftEvent> shiftWorkers = eventPeople.stream()
        .filter(shiftEvent -> !shiftEvent.getJolly()).sorted(
            Comparator.comparing(ShiftEvent::getTitle))
        .collect(Collectors.toList());

    final List<ShiftEvent> jolly = eventPeople.stream()
        .filter(shiftEvent -> shiftEvent.getJolly()).sorted(
            Comparator.comparing(ShiftEvent::getTitle))
        .collect(Collectors.toList());

    render(shiftWorkers, jolly);
  }


  /*
   * Calvella un turno dal Database
   */
  public static void deleteShift(PersonShiftDay psd) {
    log.debug("lol");
    notFoundIfNull(psd);
    response.status = Http.StatusCode.BAD_REQUEST;
    renderText("Un messaggio qualsiasi");
    //psd.delete();

  }


  /*
   * Carica i turni dal database per essere visualizzati nel calendario
   */
  public static void events(ShiftType shiftType, LocalDate start, LocalDate end)
      throws JsonProcessingException {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci

    List<ShiftEvent> events = new ArrayList<>();

    List<PersonShiftShiftType> people = shiftWorkers(shiftType, start, end);

    int index = 0;

    // prende i turni associati alle persone attive in quel turno
    for (PersonShiftShiftType personShift : people) {
      final Person person = personShift.personShift.person;
      final EventColor eventColor = EventColor.values()[index % (EventColor.values().length - 1)];
      events.addAll(shiftEvents(shiftType, person, start, end, eventColor));
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
      LocalDate end, EventColor color) {

    return shiftDao.getPersonShiftDaysByPeriodAndType(start, end, shiftType, person).stream()
        .map(shiftDay -> {

          return ShiftEvent.builder()
              .allDay(true)
              .shiftSlot(shiftDay.shiftSlot)
              .personShiftDayId(shiftDay.id)
              .title(shiftDay.getSlotTime() + '\n' + person.fullName())
              .start(shiftDay.date)
              .editable(true)
              .color(color.backgroundColor)
              .textColor(color.textColor)
              .borderColor(color.borderColor)
              .className("removable")
              .build();
        }).collect(Collectors.toList());
  }

  /*
   * Carica le assenze di una persona in un certo periodo per essere visualizzate nel calendario
   */
  private static List<ShiftEvent> absenceEvents(Person person, LocalDate start, LocalDate end) {

    final List<JustifiedTypeName> types = ImmutableList
        .of(JustifiedTypeName.all_day, JustifiedTypeName.assign_all_day);

    List<Absence> absences = absenceDao.filteredByTypes(person, start, end, types);
    List<ShiftEvent> events = new ArrayList<>();
    ShiftEvent event = null;

    for (Absence abs : absences) {

      /**
       * Per quanto riguarda gli eventi 'allDay':
       *
       * La convenzione del fullcalendar è quella di avere il parametro end = null
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
            .color(EventColor.RED.backgroundColor)
            .textColor(EventColor.RED.textColor)
            .borderColor(EventColor.RED.borderColor)
            .build();

        events.add(event);
      } else {
        event.setEnd(abs.personDay.date.plusDays(1));
      }

    }
    return events;
  }


  /*
   * Chiamata dal fullCalendar dei turni per ogni evento di drop di un turno sul calendario.
   * Controlla se il turno passato come parametro può essere salvato in un dato giorno 
   * ed eventualmente lo salva, altrimenti restituisce un errore
   * 
   * @param long personShiftDayId: id del persnShiftDay da controllare
   * @param LocalDate newDate: giorno nel quale salvare il turno
   * 
   * @out error 409 con messaggio di ShiftTroubles.PERSON_IS_ABSENT, CalendarShiftTroubles.SHIFT_SLOT_ASSIGNED,
   *                                   CalendarShiftTroubles.SHIFT_SLOT_ASSIGNED
   */
  public static void changeShift(long personShiftDayId, LocalDate newDate) throws JsonProcessingException {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci

    log.debug("Chiamato metodo changeShift: personShiftDayId {} - newDate {} ", personShiftDayId,
        newDate);
    String message = "";

    // legge il turno da spostare
    PersonShiftDay oldShift = shiftDao.getPersonShiftDayById(personShiftDayId);

    // controlla gli eventuali errori di consitenza nel calendario
    List<String> errors = ShiftManager2.checkShiftEvent(oldShift, newDate);
    if (((errors.isEmpty()) || errors.contains(ShiftTroubles.PROBLEMS_ON_OTHER_SLOT.toString()))) {
      //salva il nuovo turno

      oldShift.date = newDate;
      //oldShift.troubles
      oldShift.save();
      log.info("Aggiornato PersonShiftDay con {}\n",
          oldShift);

      // restituisco l'evento
      ShiftEvent event = ShiftEvent.builder()
          .shiftSlot(oldShift.shiftSlot)
          .personShiftDayId(oldShift.id)
          .title(oldShift.getSlotTime() + '\n' + oldShift.personShift.person.fullName())
          .errors(errors)
          .build();

      renderJSON(mapper.writeValueAsString(event));

    } else {
      // prende il messaggi di errore
      for (String error: errors) {
        message.concat(Messages.get(error));
      }
      response.status = 409;
      renderText(message);
    }
  }


  public static void newShift(long personId, LocalDate date, ShiftSlot shiftSlot,
      ShiftType shiftType) throws JsonProcessingException {
    log.debug("CHIAMATA LA CREAZIONE DEL TURNO: personId {} - day {} - slot {} - shiftType {}",
        personId, date, shiftSlot, shiftType);
    // TODO: ricordarsi di controllare se la persona è attiva sull'attività al momento della creazione del
    // personshiftDay

    // Esempio per passare gli errori da renderizzare direttamente col PNotify
//    final PNotifyObject message = PNotifyObject.builder()
//        .title("Prova")
//        .hide(true)
//        .text("testo di prova")
//        .delay(2000)
//        .type("info").build();
//
//    response.status = StatusCode.BAD_REQUEST;
//    renderJSON(message);

    String color = ""; //TODO:
    String message = "";

    // crea il personShiftDay
    PersonShiftDay personShiftDay = new PersonShiftDay();
    personShiftDay.date = date;
    personShiftDay.shiftType = shiftType;
    personShiftDay.setShiftSlot(shiftSlot);
    personShiftDay.personShift = shiftDao.getPersonShiftByPersonAndType(personId, shiftType.type);

    // controlla che possa essere salvato nel giorno
    List<String> errors = shiftManager2.checkShiftDay(personShiftDay, date);
    if (errors.isEmpty()) {
      personShiftDay.save();

      // contruisce l'evento
      //TODO: con gli errori? e poi li prendi nel calendario? (messaggi o errCode?) Oppure?
      ShiftEvent event = ShiftEvent.builder()
          .shiftSlot(personShiftDay.shiftSlot)
          .personShiftDayId(personShiftDay.id)
          .title(personShiftDay.getSlotTime() + '\n' + personShiftDay.personShift.person.fullName())
          .errors(errors)
          .build();

      renderJSON(mapper.writeValueAsString(event));
    } else {
      response.status = 409;
      // prende il messaggi di errore
      for (String error: errors) {
        message.concat(Messages.get(error));
      }
      renderText(message);
    }
  }

  /**
   * Crea il file PDF con il resoconto mensile dei turni dello IIT il mese 'month'
   * dell'anno 'year' (portale sistorg).
   *
   * @author arianna
   */
  //@BasicAuth
  public static void exportMonthAsPDF(int year, int month, Long shiftCategoryId) {

    log.debug("sono nella exportMonthAsPDF con shiftCategory={} year={} e month={}",
        shiftCategoryId, year, month);

    // legge inizio e fine mese
    final LocalDate firstOfMonth = new LocalDate(year, month, 1);
    final LocalDate lastOfMonth = firstOfMonth.dayOfMonth().withMaximumValue();
  }


  private static List<PersonShiftShiftType> shiftWorkers(ShiftType activity, LocalDate start,
      LocalDate end) {
    if (activity.isPersistent() && start != null && end != null) {
      return activity.personShiftShiftTypes.stream()
          .filter(personShiftShiftType -> personShiftShiftType.dateRange().isConnected(
              Range.closed(start, end)))
          .collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }
  }
}
