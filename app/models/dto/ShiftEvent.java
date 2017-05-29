package models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Data;
import models.ShiftType;
import models.enumerate.EventColor;
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
  private Long personShiftDayId;
  private Long personId;
  private ShiftSlot shiftSlot;
  private EventColor eventColor;
}
