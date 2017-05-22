package models.dto;

import lombok.Builder;
import lombok.Data;
import models.ShiftType;
import models.enumerate.ShiftSlot;
import org.joda.time.LocalDate;

/**
 * @author daniele
 * @since 15/05/17.
 */
@Data
@Builder
public class ShiftEvent {

  // Campi di default dell'eventObject fullcalendar
  private String title;
  private boolean allDay;
  private LocalDate start;
  private LocalDate end;
  private String url;
  private String className;
  private boolean startEditable;
  private boolean durationEditable;
  private String color;
  private String backgroundColor;
  private String borderColor;
  private String textColor;

  private long personId;
  private ShiftSlot shiftSlot;
  // Campi extra che servono per riuscire a passare indietro al server (dal calendario),
  // le vecchie date al quale fa riferimento l'evento in seguito a una sua modifica
  private LocalDate start_orig;
  private LocalDate end_orig;

  private static String timeFormatted = "HH:mm";

  public void extendTitle(ShiftType type) {
    switch (shiftSlot) {
      case MORNING:
        title =
            type.shiftTimeTable.startMorning.toString(timeFormatted) + " - "
                + type.shiftTimeTable.endMorning.toString(timeFormatted) + "\n" + title;
        break;
      case AFTERNOON:
        title =
            type.shiftTimeTable.startAfternoon.toString(timeFormatted) + " - "
                + type.shiftTimeTable.endAfternoon.toString(timeFormatted) + "\n" + title;
        break;
      default:
        break;
    }
  }

}
