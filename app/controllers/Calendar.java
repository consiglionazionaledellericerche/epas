package controllers;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import dao.AbsenceDao;
import dao.CompetenceCodeDao;
import dao.ShiftDao;
import dao.ShiftTypeMonthDao;
import helpers.Web;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import manager.ShiftManager2;
import models.CompetenceCode;
import models.OrganizationShiftSlot;
import models.Person;
import models.PersonCompetenceCodes;
import models.PersonShiftDay;
import models.PersonShiftShiftType;
import models.ShiftType;
import models.ShiftTypeMonth;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.PNotifyObject;
import models.dto.ShiftEvent;
import models.enumerate.EventColor;
import models.enumerate.ShiftSlot;
import models.enumerate.ShiftTroubles;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.YearMonth;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.With;
import security.SecurityRules;

/**
 * @author arianna
 * @author daniele
 * @since 15/05/17.
 */
@Slf4j
@With(Resecure.class)
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
  @Inject
  static CompetenceCodeDao competenceCodeDao;

  private static String nighCode = "T3";
  
  /**
   * ritorna alla view le info necessarie per creare il calendario.
   *
   * @param activity l'attività
   * @param date la data
   */
  public static void show(ShiftType activity, LocalDate date) {
    // FIXME: 12/06/17 il passaggio del solo id faciliterebbe il redirect dagli altri controller
    // ma va sistemato nella vista in modo che passi l'id con un nome adatto

    final LocalDate currentDate = Optional.fromNullable(date).or(LocalDate.now());

    final List<ShiftType> activities = shiftManager2.getUserActivities();
    log.info("userActivities.size() = {}", activities.size());
    
    final ShiftType activitySelected = activity.id != null 
        ? activity : activities.size() > 0 ?  activities.get(0) : null;

    rules.checkIfPermitted(activitySelected);

    render(activities, activitySelected, currentDate);
  }

  /**
   * ritorna la lista di persone associate all'attività nel periodo passato come parametro.
   *
   * @param activityId l'id dell'attività di cui ritornare la lista di personale associato
   * @param start la data di inizio da considerare
   * @param end la data di fine da considerare
   */
  public static void shiftPeople(long activityId, LocalDate start, LocalDate end) {

    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);
    if (activity.isPresent()) {
      rules.checkIfPermitted(activity.get());
      final List<PersonShiftShiftType> people = shiftManager2
          .shiftWorkers(activity.get(), start, end);

      int index = 0;

      final List<ShiftEvent> shiftWorkers = new ArrayList<>();
      final List<ShiftEvent> jolly = new ArrayList<>();

      for (PersonShiftShiftType personShift : people) {
        // lenght-1 viene fatto per escludere l'ultimo valore che è dedicato alle assenze
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
      List<ShiftSlot> slotList = null;
      List<OrganizationShiftSlot> organizationSlotList = null;
      if (activity.get().shiftTimeTable != null) {
        slotList = shiftManager2.getSlotsFromTimeTable(activity.get().shiftTimeTable);
      } else {
        organizationSlotList = 
            Lists.newArrayList(activity.get().organizaionShiftTimeTable.organizationShiftSlot);
      }
      
      shiftWorkers.sort(Comparator.comparing(ShiftEvent::getTitle));
      jolly.sort(Comparator.comparing(ShiftEvent::getTitle));

      render(shiftWorkers, jolly, slotList, organizationSlotList);
    }
  }

  /**
   * Effettua l'eliminazione di un turno.
   *
   * @param psd Turno da eliminare
   */
  public static void deleteShift(PersonShiftDay psd) {
    final PNotifyObject message;
    if (psd == null) {
      message = PNotifyObject.builder()
          .title("Error")
          .hide(true)
          .text(Messages.get("notFound"))
          .type("error").build();
      response.status = Http.StatusCode.NOT_FOUND;
    } else {
      final ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao.byShiftTypeAndDate(psd.shiftType,
          psd.date).orNull();

      if (rules.check(psd.shiftType) && rules.check(shiftTypeMonth)) {

        shiftManager2.delete(psd);

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

    renderJSON(message);
  }


  /**
   * ritorna la lista di eventi presenti per l'attività nel periodo start/end.
   *
   * @param activityId l'id dell'attività da ricercare
   * @param start la data di inizio del periodo
   * @param end la data di fine del periodo
   * @throws JsonProcessingException eccezione in caso di errore di creazione del json
   */
  public static void events(long activityId, LocalDate start, LocalDate end)
      throws JsonProcessingException {

    List<ShiftEvent> events = new ArrayList<>();
    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);
    if (activity.isPresent() && rules.check(activity.get())) {

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
   * un intervallo di tempo.
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
              .organizationShiftslot(shiftDay.organizationShiftSlot)
              .personShiftDayId(shiftDay.id)
              .title(person.fullName())
              .start(shiftDay.date.toLocalDateTime(shiftDay.slotBegin()))
              .end(shiftDay.date.toLocalDateTime(shiftDay.slotEnd()))
              .durationEditable(false)
              .color(color.backgroundColor)
              .textColor(color.textColor)
              .borderColor(color.borderColor)
              .className("removable")
              .build();
          
          if (shiftDay.date.isBefore(LocalDate.now())) {
            shiftManager2.fixShiftTrouble(shiftDay, ShiftTroubles.FUTURE_DAY);        
          }
          
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
   *        da renderizzare nel fullcalendar.
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
          || event.getEnd() == null && !event.getStart().toLocalDate().plusDays(1)
          .equals(abs.personDay.date)
          || event.getEnd() != null && !event.getEnd().toLocalDate().equals(abs.personDay.date)) {

        event = ShiftEvent.builder()
            .allDay(true)
            .title("Assenza di " + abs.personDay.person.fullName())
            .start(abs.personDay.date.toLocalDateTime(LocalTime.MIDNIGHT))
            .editable(false)
            .color(EventColor.RED.backgroundColor)
            .textColor(EventColor.RED.textColor)
            .borderColor(EventColor.RED.borderColor)
            .build();

        events.add(event);
      } else {
        event.setEnd(abs.personDay.date.plusDays(1).toLocalDateTime(LocalTime.MIDNIGHT));
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
   * @param newDate giorno nel quale salvare il turno error 409 con messaggio di
   *        ShiftTroubles.PERSON_IS_ABSENT, CalendarShiftTroubles.SHIFT_SLOT_ASSIGNED
   */
  public static void changeShift(long personShiftDayId, LocalDate newDate) {

    final PNotifyObject message;
    final PersonShiftDay shift = shiftDao.getPersonShiftDayById(personShiftDayId);

    final ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao.byShiftTypeAndDate(shift
        .shiftType, newDate).orNull();

    if (shift == null) {
      message = PNotifyObject.builder()
          .title("Error")
          .hide(true)
          .text(Messages.get("notFound"))
          .type("error").build();
      response.status = Http.StatusCode.NOT_FOUND;
    } else {
      if (rules.check(shift.shiftType) && rules.check(shiftTypeMonth)) {
        shift.date = newDate;

        // controlla gli eventuali errori di consitenza nel calendario
        Optional<String> error = shiftManager2.shiftPermitted(shift);

        if (error.isPresent()) {
          message = PNotifyObject.builder()
              .title("Errore")
              .hide(true)
              .text(error.get())
              .type("error").build();

          response.status = 409;
        } else {
          //salva il turno modificato
          shiftManager2.save(shift);

          message = PNotifyObject.builder()
              .title("Ok")
              .hide(true)
              .text(Web.msgModified(PersonShiftDay.class))
              .type("success").build();
        }
      } else {
        message = PNotifyObject.builder()
            .title("Forbidden")
            .hide(true)
            .text(Messages.get("forbidden"))
            .type("error").build();
        response.status = Http.StatusCode.FORBIDDEN;
      }

    }

    renderJSON(message);
  }


  /**
   * inserisce un nuovo slot di turno per l'attività al turnista passati come parametro.
   *
   * @param personId l'id della persona in turno
   * @param date la data in cui inserire il turno
   * @param shiftSlot lo slot di turno (mattina/pomeriggio)
   * @param activityId l'id dell'attività su cui inserire il turnista
   */
  public static void newShift(long personId, LocalDate date, 
      OrganizationShiftSlot organizationShiftslot, long activityId) {

    final PNotifyObject message;
    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);

    final ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao.byShiftTypeAndDate(activity.get(), date)
        .orNull();

    if (activity.isPresent()) {
      if (rules.check(activity.get()) && rules.check(shiftTypeMonth)) {
        PersonShiftDay personShiftDay = new PersonShiftDay();
        personShiftDay.date = date;
        personShiftDay.shiftType = activity.get();
        personShiftDay.organizationShiftSlot = organizationShiftslot;
 
        personShiftDay.personShift = shiftDao
            .getPersonShiftByPersonAndType(personId, personShiftDay.shiftType.type);
        Optional<String> error;
        if (validation.valid(personShiftDay).ok) {
          error = shiftManager2.shiftPermitted(personShiftDay);
        } else {
          if (organizationShiftslot == null) {
            error = Optional.of(Messages.get("shift.notSlotSpecified"));
          } else {
            error = Optional.of(Messages.get("validation.invalid"));
          }
          
        }

        if (error.isPresent()) {
          response.status = 409;

          message = PNotifyObject.builder()
              .title("Errore")
              .hide(true)
              .text(error.get())
              .type("error").build();

        } else {
          shiftManager2.save(personShiftDay);

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

    } else { // Lo ShiftType specificato non esiste
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
   * Calcola le ore di turno effettuate in quel periodo per ciascuna persona dell'attività
   * specificata.
   *
   * @param activityId id dell'attività di turno
   * @param start data iniziale
   * @param end data finale.
   */
  public static void recap(long activityId, LocalDate start, LocalDate end) {
    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);

    if (activity.isPresent()) {
      ShiftType shiftType = activity.get();
      rules.checkIfPermitted(activity.get());
      Map<Person, Integer> shiftsCalculatedCompetences = shiftManager2
          .calculateActivityShiftCompetences(shiftType, start, end, false);
      Map<Person, Integer> holidayShifts = null;
      Set<Person> people = shiftsCalculatedCompetences.keySet();
      CompetenceCode code = competenceCodeDao.getCompetenceCodeByCode(nighCode);
      List<PersonCompetenceCodes> list = people.stream()
          .flatMap(p -> p.personCompetenceCodes.stream()
              .filter(c -> c.competenceCode.equals(code) 
                  && !c.beginDate.isAfter(start)))
          .collect(Collectors.toList());
      
      if (!list.isEmpty()) {
        holidayShifts = 
            shiftManager2.calculateActivityShiftCompetences(shiftType, start, end, true);
      }
      

      final ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao
          .byShiftTypeAndDate(shiftType, start).orNull();

      render(shiftsCalculatedCompetences, holidayShifts, shiftTypeMonth, shiftType, start);
    }
  }


  /**
   * @param activityId id dell'attività da verificare
   * @param start data relativa al mese da controllare
   * @return true se l'attività è modificabile nella data richiesta, false altrimenti.
   */
  public static boolean editable(long activityId, @Required LocalDate start) {

    Optional<ShiftType> activity = shiftDao.getShiftTypeById(activityId);

    if (Validation.hasErrors() || !activity.isPresent()) {
      return false;
    }

    final ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao
        .byShiftTypeAndDate(activity.get(), start).orNull();

    return rules.check(activity.get()) && rules.check(shiftTypeMonth);
  }

  /**
   * ritorna informazioni alla vista relative ai turnisti e alle ore già approvate/pagate di turno.
   *
   * @param activityId l'id dell'attività per cui ricercare le approvazioni
   * @param date la data da cui ricercare le approvazioni
   */
  public static void monthShiftsApprovement(long activityId, @Required LocalDate date) {
    if (Validation.hasErrors()) {
      notFound();
    }

    ShiftType shiftType = shiftDao.getShiftTypeById(activityId).orNull();
    notFoundIfNull(shiftType);

    rules.checkIfPermitted(shiftType);

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
    final LocalDate today = LocalDate.now();

    final LocalDate lastDay;

    if (monthEnd.isAfter(today)) {
      lastDay = today;
    } else {
      lastDay = monthEnd;
    }

    final List<Person> people = shiftManager2.involvedShiftWorkers(shiftType, monthbegin, monthEnd);

    final Map<Person, Integer> shiftsCalculatedCompetences = new HashMap<>();
    final Map<Person, Integer> holidayShiftsCalculatedCompetences = new HashMap<>();
    final Map<Person, List<ShiftTroubles>> peopleTrouble = new HashMap<>();

    people.forEach(person -> {
      int competences = 0;
      competences = shiftManager2.calculatePersonShiftCompetencesInPeriod(shiftType, person,
          monthbegin, lastDay, false);
      shiftsCalculatedCompetences.put(person, competences);
      competences = shiftManager2.calculatePersonShiftCompetencesInPeriod(shiftType, person, 
          monthbegin, lastDay, true);
      holidayShiftsCalculatedCompetences.put(person, competences);
      List<ShiftTroubles> shiftsTroubles = 
          shiftManager2.allValidShifts(shiftType, person, monthbegin, monthEnd);
      peopleTrouble.put(person, shiftsTroubles);
    });

    render(shiftTypeMonth, shiftsCalculatedCompetences, 
        holidayShiftsCalculatedCompetences, peopleTrouble);
  }

  /**
   * approva le quantità orarie dei turni nel mese.
   *
   * @param version la versione da verificare
   * @param shiftTypeMonthId l'id dello shiftTypeMonth da controllare
   */
  public static void approveShiftsInMonth(long version, long shiftTypeMonthId) {

    ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao.byId(shiftTypeMonthId).orNull();
    notFoundIfNull(shiftTypeMonth);

    rules.checkIfPermitted(shiftTypeMonth);

    Map<String, Object> args = new HashMap<>();
    // Check tra la richiesta del riepilogo e l'approvazione definitiva dei turni: non ci devono
    // essere state modifiche in modo da evitare che il supervisore validi una situazione diversa 
    // da quella che si aspetta
    if (shiftTypeMonth.version != version) {
      flash.error("I turni sono stati cambiati rispetto al riepilogo mostrato. "
          + "Il nuovo riepilogo è stato ricalcolato");
      flash.keep();
      args.put("date", shiftTypeMonth.yearMonth.toLocalDate(1).toString());
      args.put("activityId", shiftTypeMonth.shiftType.id);
      redirect(Router.reverse("Calendar.monthShiftsApprovement", args).url);
    }

    shiftTypeMonth.approved = true;
    shiftTypeMonth.save();
    // FIXME: 12/06/17 converrebbe automatizzare il ricalcolo in seguito ad ogni cambio di stato
    // del ShiftTypeMonth (approved false->true e viceversa)
    // effettua il ricalcolo delle competenze
    shiftManager2.assignShiftCompetences(shiftTypeMonth);

    args.put("date", shiftTypeMonth.yearMonth.toLocalDate(1).toString());
    args.put("activity.id", shiftTypeMonth.shiftType.id);
    redirect(Router.reverse("Calendar.show", args).url);
  }

  /**
   * permette la rimozione dell'approvazione per le ore di turno.
   *
   * @param shiftTypeMonthId l'id dello shiftTypeMonth contenente le info su approvazione turno
   */
  public static void removeApprovation(long shiftTypeMonthId) {

    ShiftTypeMonth shiftTypeMonth = shiftTypeMonthDao.byId(shiftTypeMonthId).orNull();
    notFoundIfNull(shiftTypeMonth);

    rules.checkIfPermitted(shiftTypeMonth);

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
