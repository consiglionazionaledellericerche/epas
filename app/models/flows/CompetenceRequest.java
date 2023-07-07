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

package models.flows;

import com.google.common.collect.Lists;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import models.Person;
import models.base.MutableModel;
import models.enumerate.ShiftSlot;
import models.flows.enumerate.CompetenceRequestType;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.data.validation.Required;

/**
 * Richiesta di attribuzione di una competenza mensile.
 *
 * @author Dario Tagliaferri
 *
 */
@Getter
@Setter
@Entity
@Table(name = "competence_requests")
public class CompetenceRequest extends MutableModel {
  
  private static final long serialVersionUID = -2458580703574435339L;

  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  private CompetenceRequestType type;

  @Required
  @NotNull
  @ManyToOne(optional = false)
  private Person person;
  
  /*
   * Descrizione della richiesta
   */
  private String note;
  
  /*
   * Destinatario della richiesta di cambio turno/reperibilità
   */
  @ManyToOne(optional = true)
  private Person teamMate;
  
  /*
   * L'eventuale valore da salvare
   */
  private Integer value;
  
  /*
   * L'eventuale anno in cui salvare la competenza
   */
  private Integer year;
  
  /*
   * L'eventuale mese in cui salvare la competenza
   */
  private Integer month;
  
  /*
   * L'eventuale data inizio da chiedere
   */
  private LocalDate beginDateToAsk;
  /*
   * L'eventuale data fine da chiedere
   */
  private LocalDate endDateToAsk;
  /*
   * L'eventuale data inizio da dare
   */
  private LocalDate beginDateToGive;
  /*
   * L'eventuale data fine da dare
   */
  private LocalDate endDateToGive;
  
  /*
   * Lo slot per cui richiedere il cambio
   */
  @Enumerated(EnumType.STRING)
  private ShiftSlot shiftSlot;
  
  /**
   * Data e ora di inizio.
   */
  @Required
  @NotNull
  @Column(name = "start_at")
  private LocalDateTime startAt;

  @Column(name = "end_to")
  private LocalDateTime endTo;
  
  /*Nuovi campi per la doppia approvazione delle richieste di straordinario*/
  private LocalDateTime firstApproved;
  private boolean firstApprovalRequired = false;
  /*Fine*/  
  
  private LocalDateTime employeeApproved;
  
  private LocalDateTime managerApproved;
  
  private LocalDateTime officeHeadApproved;
  
  private boolean employeeApprovalRequired = true;
  
  private boolean managerApprovalRequired = true;
  
  private boolean officeHeadApprovalRequired = true;
  
  
  @NotAudited
  @OneToMany(mappedBy = "competenceRequest")
  @OrderBy("createdAt DESC")
  private List<CompetenceRequestEvent> events = Lists.newArrayList();
  
  /**
   * Se il flusso è avviato.
   */
  private boolean flowStarted = false; 

  /**
   * Se il flusso è terminato.
   */
  private boolean flowEnded = false;
  
  @Transient
  public LocalDate startAtAsDate() {
    return startAt != null ? startAt.toLocalDate() : null;
  }

  @Transient
  public LocalDate endToAsDate() {
    return endTo != null ? endTo.toLocalDate() : null;
  }

  @Transient
  public boolean isEmployeeApproved() {
    return employeeApproved != null;
  }
  
  public boolean toBeAEmployeeApproved() {
    return employeeApprovalRequired && !isEmployeeApproved();
  }

  @Transient
  public boolean isManagerApproved() {
    return managerApproved != null;
  }

  @Transient
  public boolean isOfficeHeadApproved() {
    return officeHeadApproved != null;
  }
  
  /**
   * Se non sono state già rilasciate approvazioni necessarie allora il possessore 
   * può cancellare o modificare la richiesta.
   *
   * @return true se la richiesta di permesso è ancora modificabile o cancellabile.
   */
  @Transient
  public boolean ownerCanEditOrDelete() {
    return !flowStarted  
        && (managerApproved == null || !managerApprovalRequired)
        && (employeeApproved == null || !employeeApprovalRequired)
        && (officeHeadApproved == null || !officeHeadApprovalRequired);
  }
  
  /**
   * Un flusso è completato se tutte le approvazioni richieste sono state
   * impostate.
   *
   * @return true se è completato, false altrimenti.
   */
  public boolean isFullyApproved() {
    return (!this.managerApprovalRequired || this.isManagerApproved()) 
        && (!this.employeeApprovalRequired
            || this.isEmployeeApproved())
        && (!this.officeHeadApprovalRequired || this.isOfficeHeadApproved());
  }
  
  @Transient
  public CompetenceRequestEvent actualEvent() {
    return this.events.get(0);
  }
  
}
