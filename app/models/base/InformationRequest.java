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

package models.base;

import com.google.common.collect.Lists;
import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import models.Person;
import models.enumerate.InformationType;
import models.informationrequests.InformationRequestEvent;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import play.data.validation.Required;

/**
 * Classe di base delle richieste di informazione.
 *
 * @author dario
 *
 */
@Getter
@Setter
@ToString(of = {"informationType", "person", "startAt", "endTo", 
    "officeHeadApproved", "officeHeadApprovalRequired"})
@Audited
@Inheritance(strategy = InheritanceType.JOINED)
@Entity
@Table(name = "information_requests")
public abstract class InformationRequest extends BaseModel {
  
  private static final long serialVersionUID = -3294556588987879116L;

  @Required
  @NotNull
  @ManyToOne(optional = false)
  private Person person;
  
  /**
   * Data e ora di inizio.
   */
  @Required
  @NotNull
  @Column(name = "start_at")
  private LocalDateTime startAt;

  @Column(name = "end_to")
  private LocalDateTime endTo;
  
  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  private InformationType informationType;
  
  /**
   * Data di approvazione del responsabili sede.
   */
  private LocalDateTime officeHeadApproved;
  
  /**
   * Data di approvazione dell'amministratore del personale.
   */
  private LocalDateTime administrativeApproved;
  
  /**
   * Data di approvazione del responsabile di gruppo.
   */
  private LocalDateTime managerApproved;
  
  
  /**
   * Indica se è richieta l'approvazione da parte del responsabile di sede.
   */
  private boolean officeHeadApprovalRequired = true;
  
  /**
   * Indica se è richieta l'approvazione da parte dell'amministrativo.
   */
  @Column(name = "administrative_approval_required")
  private boolean administrativeApprovalRequired = false;
  
  /**
   * Indica se è richiesta l'approvazione del responsabile di gruppo.
   */
  @Column(name = "manager_approval_required")
  private boolean managerApprovalRequired = false;
  
  /**
   * Se il flusso è avviato.
   */
  @Column(name = "flow_started")
  private boolean flowStarted = false; 

  /**
   * Se il flusso è terminato.
   */
  @Column(name = "flow_ended")
  private boolean flowEnded = false;
  
  @NotAudited
  @OneToMany(mappedBy = "informationRequest")
  @OrderBy("createdAt DESC")
  private List<InformationRequestEvent> events = Lists.newArrayList();
  
  @Transient
  public InformationRequestEvent actualEvent() {
    return this.events.get(0);
  }
  
  @Transient
  public boolean isOfficeHeadApproved() {
    return officeHeadApproved != null;
  }
  
  @Transient
  public boolean isAdministrativeApproved() {
    return administrativeApproved != null;
  }
  
  @Transient
  public boolean isManagerApproved() {
    return managerApproved != null;
  }
  
  /**
   * Un flusso è completato se tutte le approvazioni richieste sono state
   * impostate.
   *
   * @return true se è completato, false altrimenti.
   */
  public boolean isFullyApproved() {
    return (!this.officeHeadApprovalRequired || this.isOfficeHeadApproved())
        && (!this.administrativeApprovalRequired 
            || this.isAdministrativeApproved())
            && (!this.managerApprovalRequired || this.isManagerApproved());
  }
  
  /**
   * Se non sono state già rilasciate approvazioni necessarie allora il possessore 
   * può cancellare o modificare la richiesta.
   *
   * @return true se la richiesta di permesso è ancora modificabile o cancellabile.
   */
  @Transient
  public boolean ownerCanEditOrDelete() {
    return !flowStarted && (officeHeadApproved == null || !officeHeadApprovalRequired);
  }

  @Transient
  public boolean autoApproved() {
    return !this.officeHeadApprovalRequired && !this.managerApprovalRequired 
        && !this.administrativeApprovalRequired;
  }
}
