package models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.joda.time.LocalDate;
import lombok.Builder;
import lombok.Data;
import models.dto.ReperibilityEvent.ReperibilityEventBuilder;
import models.enumerate.EventColor;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
public class TeleworkEvent {

//Campi di default dell'eventObject fullcalendar
 private String title;
 private boolean allDay;
 private LocalDate start;
 private LocalDate end;
 private String url;
 private String className;
 
 private String color;
 private String backgroundColor;
 private String borderColor;
 private String textColor;
 
 private Long personDayId;
 private Long personId;
 private EventColor eventColor;
 
 
}
