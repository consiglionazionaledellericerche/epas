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
    session.put("currentShiftActivity", activitySelected.id);

    List<Person> people = activitySelected.personShiftShiftTypes.stream()
        .map(personShiftShiftType -> personShiftShiftType.personShift.person)
        .collect(Collectors.toList());

    render(activities, activitySelected, people);
  }

  public static void shifts(LocalDate start, LocalDate end) throws JsonProcessingException {

    final ShiftType type = shiftDao.
        getShiftTypeById(Long.parseLong(session.get("currentShiftActivity"))).orNull();

    List<ShiftEvent> events = new ArrayList<>();
    List<PersonShift> people = type.personShiftShiftTypes.stream()
        .map(personShiftShiftType -> personShiftShiftType.personShift).collect(Collectors.toList());

    int index = 0;
    ColorBrewer sequentialPalettes = ColorBrewer.valueOf("YlOrBr");

    Color[] myGradient = sequentialPalettes.getColorPalette(11);
    for (PersonShift person : people) {

      ShiftEvent event = null;
      for (PersonShiftDay day : shiftDao
          .getPersonShiftDaysByPeriodAndType(start, end, type, person.person)) {

        /**
         * Per quanto riguarda gli eventi 'allDay':
         * Il fullcalendar considera i giorni unici con data di fine evento = null.
         * A causa di ciò, gli eventi su più giorni hanno come data di fine il
         * giorno successivo all'effettiva data di terminazione dell'evento.
         */
        if (event == null || event.getShiftSlot() != day.getShiftSlot()
            || event.getEnd() == null && !event.getStart().plusDays(1).equals(day.date)
            || event.getEnd() != null && !event.getEnd().equals(day.date)) {

          event = ShiftEvent.builder()
              .allDay(true)
              .shiftSlot(day.shiftSlot)
              .personId(person.person.id)
              .title(person.person.fullName())
              .start(day.date)
              .start_orig(day.date)
              .color("#" + Integer.toHexString(myGradient[index].getRGB() & 0xffffff))
              .textColor("black")
              .borderColor("black")
              .durationEditable(true)
              .startEditable(true)
              .build();

          event.extendTitle(type);
          events.add(event);
        } else {
          event.setEnd(day.date.plusDays(1));
          event.setEnd_orig(day.date);
        }

      }
      index++;

      events.addAll(absenceEvents(person.person, start, end));

    }

    // Usato il jackson per facilitare la serializzazione dei LocalDate
    renderJSON(mapper.writeValueAsString(events));
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
       * Il fullcalendar considera i giorni unici con data di fine evento = null.
       * A causa di ciò, gli eventi su più giorni hanno come data di fine il
       * giorno successivo all'effettiva data di terminazione dell'evento.
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

  public static void changeShift(long id, LocalDate originalStart, LocalDate originalEnd,
      LocalDate start, LocalDate end) {
    log.debug(
        "CHIAMATA LA MODIFICA DEL TURNO: personId {} - start-orig {} "
            + "- end-orig{} - start {} - end {}", id, originalStart, originalEnd, start, end);
    //        response.status = Http.StatusCode.BAD_REQUEST;
    //        renderText("erroraccio");
  }
}
