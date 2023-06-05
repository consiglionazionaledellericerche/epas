/*
 * Copyright (C) 2021  Consiglio Nazionale delle Ricerche
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package models.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import models.OrganizationShiftSlot;
import models.enumerate.EventColor;
import org.joda.time.LocalDateTime;

/**
 * Evento di turno.
 *
 * @author Daniele Murgia
 * @since 15/05/17
 */
@Data
@Builder
@JsonInclude(Include.NON_NULL)

public class ShiftEvent {

  // Campi di default dell'eventObject fullcalendar
  private String title;
  private boolean allDay;
  private LocalDateTime start;
  private LocalDateTime end;
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
  @JsonIgnore
  private OrganizationShiftSlot organizationShiftslot;
  private EventColor eventColor;
  private List<String> troubles;
  private String email;
  private String mobile;
}
