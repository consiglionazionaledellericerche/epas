package models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
@JsonInclude(Include.NON_NULL)
public class ShiftEvent {

  // Campi di default dell'eventObject fullcalendar
  private String title;
  private boolean allDay;
  private LocalDate start;
  private LocalDate end;
  private String url;
  private String className;
  // Usata la classe Boolean per poter lasciare i valori null in modo che
  // non vengano serializzati nel Json
  private Boolean editable;
  private Boolean startEditable;
  private Boolean durationEditable;
  private Boolean resourceEditable;
  private Boolean overlap;
  private String rendering;
  private String constraint;
  private String color;
  private String backgroundColor;
  private String borderColor;
  private String textColor;

  // CAMPI CUSTOM
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
