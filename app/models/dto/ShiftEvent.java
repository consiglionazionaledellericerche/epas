package models.dto;

import lombok.Builder;
import lombok.Data;

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
  private String start;
  private String end;
  private String url;
  private String color;
  private String backgroundColor;
  private String borderColor;
  private String textColor;
  // TODO: 15/05/17  aggiungere tutti i parametri che possono servire
}
