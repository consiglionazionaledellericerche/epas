/*
 * Copyright (C) 2026  Consiglio Nazionale delle Ricerche
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

package cnr.sync.dto.v4;

import helpers.JodaConverters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import models.flows.AbsenceRequest;
import models.flows.enumerate.AbsenceRequestType;

/**
 * Una richiesta di assenza del dipendente autenticato, con lo stato del flusso.
 */
@Data
@Builder
public class AbsenceRequestShowDto {

  /**
   * Lo stato sintetico della richiesta.
   */
  public enum Status {
    PENDING, APPROVED, REFUSED, DELETED, EXPIRED
  }

  private Long id;
  private AbsenceRequestType type;
  private Status status;
  private LocalDate startAt;
  private LocalDate endTo;
  private Integer hours;
  private Integer minutes;
  private String note;
  private String absenceCode;

  private boolean managerApprovalRequired;
  private LocalDateTime managerApproved;
  private boolean administrativeApprovalRequired;
  private LocalDateTime administrativeApproved;
  private boolean officeHeadApprovalRequired;
  private LocalDateTime officeHeadApproved;

  // true se il dipendente può ancora ritirare la richiesta
  private boolean deletable;
  private List<AbsenceRequestEventShowDto> events;

  /**
   * Nuova instanza di un AbsenceRequestShowDto contenente i valori della richiesta passata.
   */
  public static AbsenceRequestShowDto build(AbsenceRequest request) {
    return AbsenceRequestShowDto.builder()
        .id(request.id)
        .type(request.getType())
        .status(status(request))
        .startAt(JodaConverters.jodaToJavaLocalDate(request.startAtAsDate()))
        .endTo(request.getEndTo() != null
            ? JodaConverters.jodaToJavaLocalDate(request.endToAsDate()) : null)
        .hours(request.getHours())
        .minutes(request.getMinutes())
        .note(request.getNote())
        .absenceCode(request.getAbsenceCode())
        .managerApprovalRequired(request.isManagerApprovalRequired())
        .managerApproved(JodaConverters.jodaToJavaLocalDateTime(request.getManagerApproved()))
        .administrativeApprovalRequired(request.isAdministrativeApprovalRequired())
        .administrativeApproved(
            JodaConverters.jodaToJavaLocalDateTime(request.getAdministrativeApproved()))
        .officeHeadApprovalRequired(request.isOfficeHeadApprovalRequired()
            || request.isOfficeHeadApprovalForManagerRequired())
        .officeHeadApproved(
            JodaConverters.jodaToJavaLocalDateTime(request.getOfficeHeadApproved()))
        .deletable(isDeletable(request))
        .events(request.getEvents().stream().map(AbsenceRequestEventShowDto::build)
            .collect(Collectors.toList()))
        .build();
  }

  /**
   * Il dipendente può ritirare la richiesta finché il flusso non è terminato e nessuno l'ha
   * ancora approvata (come nella regola EmployCanDeleteSpecificAbsenceRequest).
   */
  public static boolean isDeletable(AbsenceRequest request) {
    return !request.isFlowEnded() && request.getManagerApproved() == null
        && request.getAdministrativeApproved() == null && request.getOfficeHeadApproved() == null;
  }

  private static Status status(AbsenceRequest request) {
    if (!request.isFlowEnded()) {
      return Status.PENDING;
    }
    if (request.getEvents().isEmpty()) {
      return request.isFullyApproved() ? Status.APPROVED : Status.REFUSED;
    }
    // gli eventi sono ordinati dal più recente
    switch (request.actualEvent().eventType) {
      case DELETE:
        return Status.DELETED;
      case EXPIRING:
        return Status.EXPIRED;
      case MANAGER_REFUSAL:
      case ADMINISTRATIVE_REFUSAL:
      case OFFICE_HEAD_REFUSAL:
      case EPAS_REFUSAL:
        return Status.REFUSED;
      default:
        return Status.APPROVED;
    }
  }
}
