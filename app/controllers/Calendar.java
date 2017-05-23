package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import dao.AbsenceDao;
import dao.ShiftDao;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import models.Person;
import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftType;
import models.User;
import models.absences.Absence;
import models.absences.JustifiedType.JustifiedTypeName;
import models.dto.ShiftEvent;
import models.enumerate.ShiftSlot;
import org.jcolorbrewer.ColorBrewer;
import org.joda.time.LocalDate;
import play.mvc.Controller;
import play.mvc.With;

/**
 * @author daniele
 * @since 15/05/17.
 */
@With(Resecure.class)
@Slf4j
public class Calendar extends Controller {

  @Inject
  static ShiftDao shiftDao;
  @Inject
  static ObjectMapper mapper;
  @Inject
  static AbsenceDao absenceDao;

  public static void show(ShiftType activity) {
    User currentUser = Security.getUser().get();

    List<ShiftType> activities = currentUser.person.shiftCategories.stream()
        .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
        .collect(Collectors.toList());

    final ShiftType activitySelected = activity.id != null ? activity : activities.get(0);

    List<Person> people = activitySelected.personShiftShiftTypes.stream()
        .map(personShiftShiftType -> personShiftShiftType.personShift.person)
        .collect(Collectors.toList());

    render(activities, activitySelected, people);
  }

  public static void events(ShiftType shiftType, LocalDate start, LocalDate end)
      throws JsonProcessingException {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci

    List<ShiftEvent> events = new ArrayList<>();
    List<PersonShift> people = shiftType.personShiftShiftTypes.stream()
        .map(personShiftShiftType -> personShiftShiftType.personShift).collect(Collectors.toList());

    int index = 0;
    ColorBrewer sequentialPalettes = ColorBrewer.valueOf("YlOrBr");

    Color[] myGradient = sequentialPalettes.getColorPalette(11);
    for (PersonShift person : people) {
      final String color = "#" + Integer.toHexString(myGradient[index].getRGB() & 0xffffff);
      events.addAll(shiftEvents(shiftType, person.person, start, end, color));
      events.addAll(absenceEvents(person.person, start, end));
      index++;
    }
    events.addAll(shiftCancelledEvent(shiftType, start, end));
    // Usato il jackson per facilitare la serializzazione dei LocalDate
    renderJSON(mapper.writeValueAsString(events));
  }

  private static List<ShiftEvent> shiftEvents(ShiftType shiftType, Person person, LocalDate start,
      LocalDate end, String color) {

    final List<ShiftEvent> shiftEvents = new ArrayList<>();
    ShiftEvent event = null;
    for (PersonShiftDay day : shiftDao
        .getPersonShiftDaysByPeriodAndType(start, end, shiftType, person)) {

      /**
       * Per quanto riguarda gli eventi 'allDay':
       *
       * La convensione del fullcalendar è quella di avere il parametro end = null
       * nel caso di eventi su un singolo giorno, mentre nel caso di evento su più giorni il
       * parametro end assume il valore del giorno successivo alla fine effettiva
       * (perchè ne imposta l'orario alla mezzanotte).
       */
      if (event == null || event.getShiftSlot() != day.getShiftSlot()
          || event.getEnd() == null && !event.getStart().plusDays(1).equals(day.date)
          || event.getEnd() != null && !event.getEnd().equals(day.date)) {

        event = ShiftEvent.builder()
            .allDay(true)
            .shiftSlot(day.shiftSlot)
            .personId(person.id)
            .title(person.fullName())
            .start(day.date)
            .start_orig(day.date)
            .color(color)
            .textColor("black")
            .borderColor("black")
            .build();

        event.extendTitle(shiftType);
        shiftEvents.add(event);
      } else {
        event.setEnd(day.date.plusDays(1));
        event.setEnd_orig(day.date);
      }

    }

    return shiftEvents;
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

  private static List<ShiftEvent> shiftCancelledEvent(ShiftType shiftType, LocalDate start,
      LocalDate end) {

    return shiftDao.getShiftCancelledByPeriodAndType(start, end, shiftType).stream()
        .map(shiftCancelled -> {
          return ShiftEvent.builder()
              .allDay(true)
              .start(shiftCancelled.date)
              .rendering("background")
              .color("red")
              .build();
        }).collect(Collectors.toList());
  }

  public static void changeShift(ShiftType shiftType, long personId, LocalDate originalStart,
      LocalDate originalEnd, LocalDate start, LocalDate end) {

    // TODO: 23/05/17 Lo shiftType dev'essere valido e l'utente deve avere i permessi per lavorarci
    log.debug(
        "CHIAMATA LA MODIFICA DEL TURNO: shiftType {} -personId {} - start-orig {} "
            + "- end-orig{} - start {} - end {}", shiftType, personId, originalStart, originalEnd,
        start, end);
    //        response.status = Http.StatusCode.BAD_REQUEST;
    //        renderText("erroraccio");
  }

  public static void newShift(long personId, LocalDate date, ShiftSlot shiftSlot,
      ShiftType shiftType) {
    log.debug("CHIAMATA LA CREAZIONE DEL TURNO: personId {} - day {} - slot {} - shiftType {}",
        personId, date, shiftSlot, shiftType);
  }
}
