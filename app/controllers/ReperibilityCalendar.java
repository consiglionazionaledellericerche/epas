package controllers;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dao.AbsenceDao;
import dao.PersonReperibilityDayDao;
import dao.ReperibilityTypeMonthDao;
import helpers.Web;

import lombok.extern.slf4j.Slf4j;

import manager.ReperibilityManager2;

import models.Person;
import models.PersonReperibility;
import models.PersonReperibilityDay;
import models.PersonReperibilityType;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ReperibilityTypeMonth;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.PNotifyObject;
import models.dto.ReperibilityEvent;
import models.dto.ShiftEvent;
import models.enumerate.EventColor;
import models.enumerate.ShiftSlot;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

import security.SecurityRules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

@With(Resecure.class)
@Slf4j
public class ReperibilityCalendar extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static ReperibilityManager2 reperibilityManager2;
  @Inject
  static PersonReperibilityDayDao reperibilityDao;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static ObjectMapper mapper;
  @Inject
  static ReperibilityTypeMonthDao reperibilityTypeMonthDao;
  /**
   * ritorna alla view le info necessarie per creare il calendario.
   *
   * @param activity l'attività
   * @param date la data
   */
  public static void show(PersonReperibilityType reperibility, LocalDate date) {

    final LocalDate currentDate = Optional.fromNullable(date).or(LocalDate.now());

    final List<PersonReperibilityType> reperibilities = reperibilityManager2.getUserActivities();

    final PersonReperibilityType reperibilitySelected = reperibility.id != null ? reperibility : reperibilities.get(0);

    rules.checkIfPermitted(reperibilitySelected);

    render(reperibilities, reperibilitySelected, currentDate);
  }

  /**
   * ritorna la lista di persone associate alla reperibilità nel periodo passato come parametro.
   *
   * @param activityId l'id dell'attività di cui ritornare la lista di personale associato
   * @param start la data di inizio da considerare
   * @param end la data di fine da considerare
   */
  public static void reperibilityPeople(long reperibilityId, LocalDate start, LocalDate end) {

    PersonReperibilityType reperibility = reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    if (reperibility != null) {
      rules.checkIfPermitted(reperibility);
      final List<PersonReperibility> people = reperibilityManager2.reperibilityWorkers(reperibility,start, end);
      int index = 0;
      final List<ReperibilityEvent> reperibilityWorkers = new ArrayList<>();

      for (PersonReperibility personReperibility : people) {
        final EventColor eventColor = EventColor.values()[index % (EventColor.values().length - 1)];
        final Person person = personReperibility.person;
        final ReperibilityEvent event = ReperibilityEvent.builder()
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
            .build();
        reperibilityWorkers.add(event);
        index++;
      }
      reperibilityWorkers.sort(Comparator.comparing(ReperibilityEvent::getTitle));
      render(reperibilityWorkers);
    }

  }

  /**
   * ritorna la lista di eventi presenti per l'attività nel periodo start/end.
   *
   * @param reperibilityId l'id dell'attività da ricercare
   * @param start la data di inizio del periodo
   * @param end la data di fine del periodo
   * @throws JsonProcessingException eccezione in caso di errore di creazione del json
   */
  public static void events(long reperibilityId, LocalDate start, LocalDate end)
      throws JsonProcessingException {

    List<ReperibilityEvent> events = new ArrayList<>();
    PersonReperibilityType reperibility = reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    if (reperibility != null && rules.check(reperibility)) {

      List<PersonReperibility> people = reperibilityManager2.reperibilityWorkers(reperibility, start, end);

      int index = 0;

      // prende i turni associati alle persone attive in quel turno
      for (PersonReperibility personReperibility : people) {
        final Person person = personReperibility.person;
        final EventColor eventColor = EventColor.values()[index % (EventColor.values().length - 1)];
        events.addAll(reperibilityEvents(reperibility, person, start, end, eventColor));
        events.addAll(absenceEvents(person, start, end));
        index++;
      }
    }

    // Usato il jackson per facilitare la serializzazione dei LocalDate
    renderJSON(mapper.writeValueAsString(events));
  }

  /**
   * Chiamata dal fullCalendar dei turni per ogni evento di drop di un turno sul calendario.
   * Controlla se il turno passato come parametro può essere salvato in un dato giorno
   * ed eventualmente lo salva, altrimenti restituisce un errore
   *
   * @param personShiftDayId id del persnShiftDay da controllare
   * @param newDate giorno nel quale salvare il turno error 409 con messaggio di
   * ShiftTroubles.PERSON_IS_ABSENT, CalendarShiftTroubles.SHIFT_SLOT_ASSIGNED
   */
  public static void changeReperibility(long personShiftDayId, LocalDate newDate) {

    //    final PNotifyObject message;
    //    final PersonShiftDay shift = shiftDao.getPersonShiftDayById(personShiftDayId);
    //
    //    final ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao.byShiftTypeAndDate(shift
    //        .shiftType, newDate).orNull();
    //
    //    if (shift == null) {
    //      message = PNotifyObject.builder()
    //          .title("Error")
    //          .hide(true)
    //          .text(Messages.get("notFound"))
    //          .type("error").build();
    //      response.status = Http.StatusCode.NOT_FOUND;
    //    } else {
    //      if (rules.check(shift.shiftType) && rules.check(shiftTypeMonth)) {
    //        shift.date = newDate;
    //
    //        // controlla gli eventuali errori di consitenza nel calendario
    //        Optional<String> error = shiftManager2.shiftPermitted(shift);
    //
    //        if (error.isPresent()) {
    //          message = PNotifyObject.builder()
    //              .title("Errore")
    //              .hide(true)
    //              .text(error.get())
    //              .type("error").build();
    //
    //          response.status = 409;
    //        } else {
    //          //salva il turno modificato
    //          shiftManager2.save(shift);
    //
    //          message = PNotifyObject.builder()
    //              .title("Ok")
    //              .hide(true)
    //              .text(Web.msgModified(PersonShiftDay.class))
    //              .type("success").build();
    //        }
    //      } else {
    //        message = PNotifyObject.builder()
    //            .title("Forbidden")
    //            .hide(true)
    //            .text(Messages.get("forbidden"))
    //            .type("error").build();
    //        response.status = Http.StatusCode.FORBIDDEN;
    //      }
    //
    //    }
    //
    //    renderJSON(message);
  }

  /**
   * inserisce un nuovo slot di turno per l'attività al turnista passati come parametro.
   *
   * @param personId l'id della persona in turno
   * @param date la data in cui inserire il turno
   * @param shiftSlot lo slot di turno (mattina/pomeriggio)
   * @param activityId l'id dell'attività su cui inserire il turnista
   */
  public static void newReperibility(long personId, LocalDate date, long reperibilityId) {

    final PNotifyObject message;
    PersonReperibilityType reperibilityType = reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    final ReperibilityTypeMonth reperibilityTypeMonth = 
        reperibilityTypeMonthDao.byReperibilityTypeAndDate(reperibilityType, date).orNull();
    if (reperibilityType != null) {
      if (rules.check(reperibilityType) && rules.check(reperibilityTypeMonth)) {
        PersonReperibilityDay personReperibilityDay = new PersonReperibilityDay();
        personReperibilityDay.date = date;
        personReperibilityDay.reperibilityType = reperibilityType;
        Optional<String> error;
        if (validation.valid(personReperibilityDay).ok) {
          error = reperibilityManager2.reperibilityPermitted(personReperibilityDay);
        } else {
          error = Optional.of(Messages.get("validation.invalid"));
        }
        if (error.isPresent()) {
          response.status = 409;

          message = PNotifyObject.builder()
              .title("Errore")
              .hide(true)
              .text(error.get())
              .type("error").build();

        } else {
          reperibilityManager2.save(personReperibilityDay);

          message = PNotifyObject.builder()
              .title("Ok")
              .hide(true)
              .text(Web.msgCreated(PersonShiftDay.class))
              .type("success").build();
        }

      } else {  // Le Drools non danno il grant
        message = PNotifyObject.builder()
            .title("Forbidden")
            .hide(true)
            .text(Messages.get("forbidden"))
            .type("error").build();
        response.status = Http.StatusCode.FORBIDDEN;
      }

    } else { // Il ReperibilityType specificato non esiste
      message = PNotifyObject.builder()
          .title("Error")
          .hide(true)
          .text(Messages.get("notFound"))
          .type("error").build();
      response.status = Http.StatusCode.NOT_FOUND;
    }

    renderJSON(message);           
  }

  /**
   * Effettua l'eliminazione di un turno.
   *
   * @param psd Turno da eliminare
   */
  public static void deleteReperibility(PersonReperibilityDay prd) {
    final PNotifyObject message;
    if (prd == null) {
      message = PNotifyObject.builder()
          .title("Error")
          .hide(true)
          .text(Messages.get("notFound"))
          .type("error").build();
      response.status = Http.StatusCode.NOT_FOUND;
    } else {
      final ReperibilityTypeMonth reperibilityTypeMonth = 
          reperibilityTypeMonthDao.byReperibilityTypeAndDate(prd.reperibilityType, 
              prd.date).orNull();

      if (rules.check(prd.reperibilityType) && rules.check(reperibilityTypeMonth)) {

        reperibilityManager2.delete(prd);

        message = PNotifyObject.builder()
            .title("Ok")
            .hide(true)
            .text(Web.msgDeleted(PersonShiftDay.class))
            .type("success").build();
      } else {
        message = PNotifyObject.builder()
            .title("Forbidden")
            .hide(true)
            .text(Messages.get("forbidden"))
            .type("error").build();
        response.status = Http.StatusCode.FORBIDDEN;
      }
    }
    //
    renderJSON(message);
  }

  /**
   * @param activityId id dell'attività da verificare
   * @param start data relativa al mese da controllare
   * @return true se l'attività è modificabile nella data richiesta, false altrimenti.
   */
  public static boolean editable(long reperibilityId, @Required LocalDate start) {

    PersonReperibilityType reperibilityType = reperibilityDao.getPersonReperibilityTypeById(reperibilityId);
    if (reperibilityType == null || Validation.hasErrors()) {
      return false;
    }
    final ReperibilityTypeMonth reperibilityTypeMonth = 
        reperibilityTypeMonthDao.byReperibilityTypeAndDate(reperibilityType, start).orNull();

    return rules.check(reperibilityType) && rules.check(reperibilityTypeMonth);

  }

  /**
   * Calcola le ore di turno effettuate in quel periodo per ciascuna persona dell'attività
   * specificata
   *
   * @param activityId id dell'attività di turno
   * @param start data iniziale
   * @param end data finale.
   */
  public static void recap(long activityId, LocalDate start, LocalDate end) {
    //    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);
    //
    //    if (activity.isPresent()) {
    //      ShiftType shiftType = activity.get();
    //      rules.checkIfPermitted(activity.get());
    //      Map<Person, Integer> shiftsCalculatedCompetences = shiftManager2
    //          .calculateActivityShiftCompetences(shiftType, start, end);
    //
    //      final ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao
    //          .byShiftTypeAndDate(shiftType, start).orNull();
    //
    //      render(shiftsCalculatedCompetences, shiftTypeMonth, shiftType, start);
    //    }
  }

  /**
   * @param person Persona della quale recuperare le assenze
   * @param start data iniziale del periodo
   * @param end data finale del periodo
   * @return Una lista di DTO che modellano le assenze di quella persona nell'intervallo specificato
   * da renderizzare nel fullcalendar.
   */
  private static List<ReperibilityEvent> absenceEvents(Person person, LocalDate start, LocalDate end) {

    final List<JustifiedTypeName> types = ImmutableList
        .of(JustifiedTypeName.all_day, JustifiedTypeName.assign_all_day);

    List<Absence> absences = absenceDao.filteredByTypes(person, start, end, types);
    List<ReperibilityEvent> events = new ArrayList<>();
    ReperibilityEvent event = null;

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
          || event.getEnd() == null && !event.getStart().plusDays(1)
          .equals(abs.personDay.date)
          || event.getEnd() != null && !event.getEnd().equals(abs.personDay.date)) {

        event = ReperibilityEvent.builder()
            .allDay(true)
            .title("Assenza di " + abs.personDay.person.fullName())
            .start(abs.personDay.date)
            .editable(false)
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

  /**
   * Carica la lista delle reperibilità di un certo tipo associati ad una determinata persona in
   * un intervallo di tempo
   *
   * @param reperibility attività di reperibilità
   * @param person persona associata ai turni
   * @param start data inizio intervallo di tempo
   * @param end data fine intervallo di tempo
   * @param color colore da utilizzare per il rendering degli eventi restituiti
   * @return Una lista di DTO da serializzare in Json per renderizzarli nel fullcalendar.
   */
  private static List<ReperibilityEvent> reperibilityEvents(PersonReperibilityType reperibility, 
      Person person, LocalDate start, LocalDate end, EventColor color) {

    return reperibilityDao.getPersonReperibilityDaysByPeriodAndType(start, end, reperibility, person).stream()
        .map(personReperibilityDay -> {
          final ReperibilityEvent event = ReperibilityEvent.builder()

              .personReperibilityDayId(personReperibilityDay.id)
              .title(person.fullName())
              .start(personReperibilityDay.date)
              .end(personReperibilityDay.date)
              .durationEditable(false)
              .color(color.backgroundColor)
              .textColor(color.textColor)
              .borderColor(color.borderColor)
              .className("removable")
              .build();

          return event;
        }).collect(Collectors.toList());
  }
}
