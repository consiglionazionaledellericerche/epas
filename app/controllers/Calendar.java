package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import dao.AbsenceDao;
import dao.ShiftDao;
import dao.ShiftTypeMonthDao;
import helpers.Web;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ShiftManager2;
import models.Person;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.User;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.PNotifyObject;
import models.dto.ShiftEvent;
import models.enumerate.EventColor;
import models.enumerate.ShiftSlot;
import org.joda.time.LocalDate;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Router;
import play.mvc.With;
import security.SecurityRules;

/**
 * @author arianna
 * @author daniele
 * @since 15/05/17.
 */
@With(Resecure.class)
@Slf4j
public class Calendar extends Controller {

  @Inject
  static SecurityRules rules;
  @Inject
  static ShiftDao shiftDao;
  @Inject
  static ShiftManager2 shiftManager2;
  @Inject
  static ObjectMapper mapper;
  @Inject
  static AbsenceDao absenceDao;
  @Inject
  static ShiftTypeMonthDao shiftTypeMonthDao;


  /**
   * ritorna alla view le info necessarie per creare il calendario.
   *
   * @param activity l'attività
   * @param date la data
   */
  public static void show(ShiftType activity, LocalDate date) {
    // FIXME: 12/06/17 il passaggio del solo id faciliterebbe il redict dagli altro controller
    // ma va sistemato nella vista in modo che passi l'id con un nome adatto
    User currentUser = Security.getUser().get();
    final LocalDate currentDate = Optional.fromNullable(date).or(LocalDate.now());

    // TODO: 12/06/17 da spostare in un metodo da implementare sul templateutility che restituisca
    // le attività di turno gestibili in base al ruolo dell'utente loggato

    final List<ShiftType> activities = currentUser.person.shiftCategories.stream()
        .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
        .sorted(Comparator.comparing(o -> o.type))
        .collect(Collectors.toList());

    final ShiftType activitySelected = activity.id != null ? activity : activities.get(0);

    rules.checkIfPermitted(activitySelected);

    render(activities, activitySelected, currentDate);
  }

  /**
   * ritorna la lista di persone associate all'attività nel periodo passato come parametro.
   *
   * @param activity l'attività di cui ritornare la lista di personale associato
   * @param start la data di inizio da considerare
   * @param end la data di fine da considerare
   */
  public static void shiftPeople(long activityId, LocalDate start, LocalDate end) {

    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);
    if (activity.isPresent()) {
      final List<PersonShiftShiftType> people = shiftManager2
          .shiftWorkers(activity.get(), start, end);

      int index = 0;

      final List<ShiftEvent> shiftWorkers = new ArrayList<>();
      final List<ShiftEvent> jolly = new ArrayList<>();

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
            .build();

        if (personShift.jolly) {
          jolly.add(event);
        } else {
          shiftWorkers.add(event);
        }
        index++;
      }

      shiftWorkers.sort(Comparator.comparing(ShiftEvent::getTitle));
      jolly.sort(Comparator.comparing(ShiftEvent::getTitle));

      render(shiftWorkers, jolly);
    }
  }

  /**
   * Effettua l'eliminazione di un turno.
   *
   * @param psd Turno da eliminare
   */
  public static void deleteShift(PersonShiftDay psd) {
    // TODO: 06/06/17 verificare i permessi sul turno specificato
    final PNotifyObject message;
    if (psd == null) {
      message = PNotifyObject.builder()
          .title("Error")
          .hide(true)
          .text(Messages.get("notFound"))
          .type("error").build();
    } else {
      psd.delete();
      shiftManager2.checkShiftDayValid(psd.date, psd.shiftType);

      message = PNotifyObject.builder()
          .title("Ok")
          .hide(true)
          .text(Web.msgDeleted(PersonShiftDay.class))
          .type("success").build();
    }

    renderJSON(message);
  }


  /*
   * Carica i turni dal database per essere visualizzati nel calendario
   */
  public static void events(long activityId, LocalDate start, LocalDate end)
      throws JsonProcessingException {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci

    List<ShiftEvent> events = new ArrayList<>();
    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);
    if (activity.isPresent()) {
      List<PersonShiftShiftType> people = shiftManager2.shiftWorkers(activity.get(), start, end);

      int index = 0;

      // prende i turni associati alle persone attive in quel turno
      for (PersonShiftShiftType personShift : people) {
        final Person person = personShift.personShift.person;
        final EventColor eventColor = EventColor.values()[index % (EventColor.values().length - 1)];
        events.addAll(shiftEvents(activity.get(), person, start, end, eventColor));
        events.addAll(absenceEvents(person, start, end));
        index++;
      }
    }

    // Usato il jackson per facilitare la serializzazione dei LocalDate
    renderJSON(mapper.writeValueAsString(events));
  }

  /**
   * Carica la lista dei turni di un certo tipo associati ad una determinata persona in
   * un intervallo di tempo
   *
   * @param shiftType attività di turno
   * @param person persona associata ai turni
   * @param start data inizio intervallo di tempo
   * @param end data fine intervallo di tempo
   * @param color colore da utilizzare per il rendering degli eventi restituiti
   * @return Una lista di DTO da serializzare in Json per renderizzarli nel fullcalendar.
   */
  private static List<ShiftEvent> shiftEvents(ShiftType shiftType, Person person, LocalDate start,
      LocalDate end, EventColor color) {

    return shiftDao.getPersonShiftDaysByPeriodAndType(start, end, shiftType, person).stream()
        .map(shiftDay -> {
          final ShiftEvent event = ShiftEvent.builder()
              .allDay(true)
              .shiftSlot(shiftDay.shiftSlot)
              .personShiftDayId(shiftDay.id)
              .title(shiftDay.getSlotTime() + '\n' + person.fullName())
              .start(shiftDay.date)
              .position(shiftDay.shiftSlot.ordinal() + 1) // ordinati in base allo slot
              .durationEditable(false)
              .color(color.backgroundColor)
              .textColor(color.textColor)
              .borderColor(color.borderColor)
              .className("removable")
              .build();

          if (!shiftDay.troubles.isEmpty()) {
            final List<String> troubles = shiftDay.troubles.stream()
                .map(trouble -> Messages
                    .get(trouble.cause.getClass().getSimpleName() + "." + trouble.cause.name()))
                .collect(Collectors.toList());
            event.setTroubles(troubles);
          }

          return event;
        }).collect(Collectors.toList());
  }

  /**
   * @param person Persona della quale recuperare le assenze
   * @param start data iniziale del periodo
   * @param end data finale del periodo
   * @return Una lista di DTO che modellano le assenze di quella persona nell'intervallo specificato
   * da renderizzare nel fullcalendar.
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
   * Chiamata dal fullCalendar dei turni per ogni evento di drop di un turno sul calendario.
   * Controlla se il turno passato come parametro può essere salvato in un dato giorno
   * ed eventualmente lo salva, altrimenti restituisce un errore
   *
   * @param personShiftDayId id del persnShiftDay da controllare
   * @param newDate giorno nel quale salvare il turno
   * @out error 409 con messaggio di ShiftTroubles.PERSON_IS_ABSENT, CalendarShiftTroubles.SHIFT_SLOT_ASSIGNED
   */
  public static void changeShift(long personShiftDayId, LocalDate newDate) {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci

    final PersonShiftDay shift = shiftDao.getPersonShiftDayById(personShiftDayId);
    LocalDate oldDate = shift.date;
    shift.date = newDate;

    // controlla gli eventuali errori di consitenza nel calendario
    Optional<String> error = shiftManager2.shiftPermitted(shift);
    final PNotifyObject message;
    if (error.isPresent()) {
      message = PNotifyObject.builder()
          .title("Errore")
          .hide(true)
          .text(error.get())
          .type("error").build();

      response.status = 409;
    } else {
      //salva il turno modificato
      shift.save();
      shiftManager2.checkShiftValid(shift);
      shiftManager2.checkShiftDayValid(shift.date, shift.shiftType);

      message = PNotifyObject.builder()
          .title("Ok")
          .hide(true)
          .text(Web.msgModified(PersonShiftDay.class))
          .type("success").build();
    }
    renderJSON(message);
  }


  /**
   * inserisce un nuovo slot di turno per l'attività al turnista passati come parametro.
   *
   * @param personId l'id della persona in turno
   * @param date la data in cui inserire il turno
   * @param shiftSlot lo slot di turno (mattina/pomeriggio)
   * @param shiftType l'attività su cui inserire il turnista
   */
  public static void newShift(long personId, LocalDate date, ShiftSlot shiftSlot,
      long activityId) {

    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);

    if (!activity.isPresent()) {
      // TODO: 12/06/17
    }

    // TODO: 06/06/17 controlli sui permessi 
    PersonShiftDay personShiftDay = new PersonShiftDay();
    personShiftDay.date = date;
    personShiftDay.shiftType = activity.get();
    personShiftDay.shiftSlot = shiftSlot;
    personShiftDay.personShift = shiftDao
        .getPersonShiftByPersonAndType(personId, personShiftDay.shiftType.type);
    // TODO: 06/06/17 verificare che il personshift sia valido prima di assegnarlo e proseguire 
    Optional<String> error = shiftManager2.shiftPermitted(personShiftDay);
    final PNotifyObject message;

    if (error.isPresent()) {
      response.status = 409;

      message = PNotifyObject.builder()
          .title("Errore")
          .hide(true)
          .text(error.get())
          .type("error").build();
    } else {
      personShiftDay.save();

      shiftManager2.checkShiftValid(personShiftDay);

      message = PNotifyObject.builder()
          .title("Ok")
          .hide(true)
          .text(Web.msgCreated(PersonShiftDay.class))
          .type("success").build();
    }
    renderJSON(message);
  }

  /**
   *
   * @param activity
   * @param start
   * @param end
   */
  public static void recap(long activityId, LocalDate start, LocalDate end) {
    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);

    if (!activity.isPresent()) {
      // TODO: 12/06/17
    }

    Map<Person, Integer> shiftsCalculatedCompetences = shiftManager2
        .calculateActivityShiftCompetences(activity.get(), start, end);

    final ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao
        .byShiftTypeAndDate(activity.get(), start).orNull();

    // FIXME trovare un modo per poter recuperare le competenze approvate su questa attività
//    Map<Person, Integer> approvedShiftCompetences = shiftManager2
//        .getApprovedShifts(activity, yearMonth);

    // TODO: 07/06/17 se ci sono delle competenze approvate è bene riportare anche quelle
    // per visualizzare eventuali discrepanze
    render(shiftsCalculatedCompetences, shiftTypeMonth, activityId, start);
  }

  /**
   *
   * @param activity
   * @param start
   * @return
   */
  public static boolean editable(long activityId, @Required LocalDate start) {
    // TODO Aggiungere validatori sullo ShiftType

    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);

    if (!activity.isPresent()) {
      // TODO: 12/06/17
    }

    if (Validation.hasErrors()) {
      return false;
    }

    final ShiftTypeMonth shiftTypeMonth = activity.get().monthStatusByDate(start).orElse(null);
    return rules.check(activity.get()) && rules.check(shiftTypeMonth);
  }

  public static void monthShiftsApprovement(long activityId, @Required LocalDate date) {
    if (Validation.hasErrors()) {
      // TODO: 12/06/17
    }

    ShiftType shiftType = shiftDao.getShiftTypeById(activityId).orNull();
    notFoundIfNull(shiftType);

    final YearMonth monthToApprove = new YearMonth(date);

    final Optional<ShiftTypeMonth> monthStatus = shiftTypeMonthDao
        .byShiftTypeAndDate(shiftType, date);

    final ShiftTypeMonth shiftTypeMonth;
    // Nel caso non ci sia ancora uno stato del mese persistito ne creo uno nuovo che mi serve in
    // fase di conferma
    if (monthStatus.isPresent()) {
      shiftTypeMonth = monthStatus.get();
    } else {
      shiftTypeMonth = new ShiftTypeMonth();
      shiftTypeMonth.shiftType = shiftType;
      shiftTypeMonth.yearMonth = monthToApprove;
      shiftTypeMonth.save();
    }

    final LocalDate monthbegin = monthToApprove.toLocalDate(1);
    final LocalDate monthEnd = monthbegin.dayOfMonth().withMaximumValue();

    final Map<Person, Integer> shiftsCalculatedCompetences = shiftManager2
        .calculateActivityShiftCompetences(shiftType, monthbegin, monthEnd);

    render(shiftTypeMonth, shiftsCalculatedCompetences);
  }

  public static void approveShiftsInMonth(long version, long shiftTypeMonthId) {

    Optional<ShiftTypeMonth> optionalShiftTypeMonth = shiftTypeMonthDao.byId(shiftTypeMonthId);

    if (!optionalShiftTypeMonth.isPresent()) {
      // TODO: 12/06/17
    }
    final ShiftTypeMonth shiftTypeMonth = optionalShiftTypeMonth.get();

    // Verifico che tra la richiesta del riepilogo e l'approvazione definitiva dei turni non ci siano
    // state modifiche in modo da evitare che il supervisore validi una situazione diversa da quella
    // che si aspetta
    if (shiftTypeMonth.version != version) {
      flash.error("I turni sono stati cambiati rispetto al riepilogo mostrato. "
          + "Il nuovo riepilogo è stato ricalcolato");
      flash.keep();
      monthShiftsApprovement(shiftTypeMonth.shiftType.id, shiftTypeMonth.yearMonth.toLocalDate(1));
    }

    shiftTypeMonth.approved = true;
    shiftTypeMonth.save();
    // FIXME: 12/06/17 converrebbe automatizzare il ricalcolo in seguito ad ogni cambio di stato
    // del ShiftTypeMonth (approved false -> true p viceversa)
    // effettua il ricalcolo delle competenze
    shiftManager2.assignShiftCompetences(shiftTypeMonth);

    Map<String, Object> args = new HashMap<>();
    args.put("date", shiftTypeMonth.yearMonth.toLocalDate(1).toString());
    args.put("activity.id", shiftTypeMonth.shiftType.id);
    redirect(Router.reverse("Calendar.show", args).url);
  }

  public static void removeApprovation(long shiftTypeMonthId) {

    Optional<ShiftTypeMonth> optionalShiftTypeMonth = shiftTypeMonthDao.byId(shiftTypeMonthId);

    if (!optionalShiftTypeMonth.isPresent()) {
      // TODO: 12/06/17
    }

    final ShiftTypeMonth shiftTypeMonth = optionalShiftTypeMonth.get();

    shiftTypeMonth.approved = false;
    shiftTypeMonth.save();

    // effettua il ricalcolo delle competenze
    shiftManager2.assignShiftCompetences(shiftTypeMonth);

    // FIXME: 12/06/17 un modo più bellino?
    Map<String, Object> args = new HashMap<>();
    args.put("date", shiftTypeMonth.yearMonth.toLocalDate(1).toString());
    args.put("activity.id", shiftTypeMonth.shiftType.id);
    redirect(Router.reverse("Calendar.show", args).url);
  }

}
