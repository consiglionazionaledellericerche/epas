package models.dto;

import lombok.Builder;
import lombok.Data;
import models.enumerate.ShiftSlot;
import org.joda.time.LocalDate;

/**
 * @author daniele
 * @since 15/05/17.
 */
@Data
@Builder
public class ShiftEvent {

  // id della persona su epas ?
  private long personId;
  // Il nome della persona del turno?
  private String title;
  private boolean allDay;
  private LocalDate start;
  private LocalDate start_orig;
  private LocalDate end;
  private LocalDate end_orig;
  private String url;
  private String color;
  private String backgroundColor;
  private String borderColor;
  private String textColor;
  private ShiftSlot shiftSlot;
  // TODO: 15/05/17  aggiungere tutti i parametri che possono servire
}
