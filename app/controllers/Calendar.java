package controllers;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import models.dto.ShiftEvent;
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

  public static void show() {
    render();
  }

  public static void shifts() {

    List<ShiftEvent> events = ImmutableList.of(
        ShiftEvent.builder()
            .personId(49L)
            .title("Descrizione turno")
            .allDay(true)
            .start(LocalDate.now().toString())
            .end(LocalDate.now().plusDays(2).toString())
            .url("#")
            .build()
    );

    log.debug("DATA EVENTO {} - {}", events.get(0).getStart(), events.get(0).getEnd());
    renderJSON(events);
  }

  public static void changeShift(long id, LocalDate start, LocalDate end) {
    log.debug("CHIAMATA LA MODIFICA DEL TURNO: personId {} - start {} - end {}", id, start,
        end);
  }
}
