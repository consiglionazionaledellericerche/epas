package controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import dao.ShiftDao;

import lombok.extern.slf4j.Slf4j;

import manager.ShiftManager;

import models.PersonShift;
import models.PersonShiftDay;
import models.ShiftType;
import models.User;
import models.dto.ShiftEvent;

import org.jcolorbrewer.ColorBrewer;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.With;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

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


  public static void show(ShiftType activity) {
    // log.debug("Id attività: {}", activity.id);
    User currentUser = Security.getUser().get();

    List<ShiftType> activities = currentUser.person.shiftCategories.stream()
        .flatMap(shiftCategories -> shiftCategories.shiftTypes.stream())
        .collect(Collectors.toList());

    final ShiftType activitySelected = activity.id != null ? activity : activities.get(0);
    session.put("currentShiftActivity", activitySelected.id);

    render(activities, activitySelected);
  }

  public static void shifts(LocalDate start, LocalDate end) throws JsonProcessingException {

    final ShiftType type = ShiftType.findById(Long.parseLong(session.get("currentShiftActivity")));

    List<ShiftEvent> events = new ArrayList<>();
    //final AtomicInteger count = new AtomicInteger();
    List<PersonShift> people = type.personShiftShiftTypes.stream()
        .map(personShiftShiftType -> personShiftShiftType.personShift).collect(Collectors.toList());

    int index = 0;
    ColorBrewer sequentialPalettes = ColorBrewer.valueOf("RdYlBu");


    Color[] myGradient = sequentialPalettes.getColorPalette(11);
    for (PersonShift person : people) {

      ShiftEvent event = null;
      for (PersonShiftDay day : shiftDao.getPersonShiftDaysByPeriodAndType(start, end, type, person.person)) {

        /*
         * Per quanto riguarda gli eventi 'allDay':
         * Il fullcalendar considera i giorni unici con data di fine evento = null.
         * A causa di ciò, gli eventi su più giorni hanno come data di fine il giorno successivo all'effettiva
         * data di terminazione dell'evento.
         */
        if (event == null || !(event.getShiftSlot() == day.getShiftSlot()) 
            || (event.getEnd() == null && !event.getStart().plusDays(1).equals(day.date))
            || (event.getEnd() != null && !event.getEnd().plusDays(1).equals(day.date))) {
          
          // Incrementiamo la fine di un giorno per essere coerenti con la gestione delle date del fullcalendar
          if(event != null && event.getEnd() != null) {
            event.setEnd(event.getEnd().plusDays(1));
          }

          event = ShiftEvent.builder()
              .allDay(true)
              .shiftSlot(day.shiftSlot)
              .personId(person.person.id)
              .title(person.person.fullName())
              .start(day.date)
              .start_orig(day.date)
              .color("#"+Integer.toHexString(myGradient[index].getRGB() & 0xffffff))
              .build();

          event.extendTitle(type);
          events.add(event);
        } else {
          event.setEnd(day.date);
          event.setEnd_orig(day.date);
        }

      }
      index++;

    }

    // Usato il jackson per facilitare la serializzazione dei LocalDate
    renderJSON(mapper.writeValueAsString(events));
  }

  public static void changeShift(long id, LocalDate originalStart, LocalDate originalEnd,
      LocalDate start, LocalDate end) {
    log.debug(
        "CHIAMATA LA MODIFICA DEL TURNO: personId {} - start-orig {} "
            + "- end-orig{} - start {} - end {}", id, originalStart, originalEnd, start, end);
        response.status = Http.StatusCode.BAD_REQUEST;
        renderText("erroraccio");
  }
}
