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
  //  private boolean editable;
  private boolean startEditable;
  private boolean durationEditable;
  //  private boolean resourceEditable;
//  private String rendering;
//  private boolean overlap;
  private String color;
  private String backgroundColor;
  private String borderColor;
  private String textColor;

  // Campi 
  // id della persona su epas ?
  private long personId;
  // Il nome della persona del turno?

  private LocalDate start_orig;

  private LocalDate end_orig;

  private ShiftSlot shiftSlot;
  // TODO: 15/05/17  aggiungere tutti i parametri che possono servire
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
