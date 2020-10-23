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
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import models.Person;
import models.base.MutableModel;
import models.enumerate.ShiftSlot;
import models.flows.enumerate.CompetenceRequestType;
import play.data.validation.Required;

@Entity
@Table(name = "competence_requests")
public class CompetenceRequest extends MutableModel {
  
  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  public CompetenceRequestType type;

  @Required
  @NotNull
  @ManyToOne(optional = false)
  public Person person;
  
  /**
   * Descrizione della richiesta
   */
  public String note;
  
  /**
   * L'eventuale valore da salvare
   */
  public Integer value;
  
  /**
   * L'eventuale anno in cui salvare la competenza
   */
  public Integer year;
  
  /**
   * L'eventuale mese in cui salvare la competenza
   */
  public Integer month;
  
  /**
   * L'eventuale data da cambiare
   */
  public LocalDate dateToChange;
  
  /**
   * Lo slot per cui richiedere il cambio
   */
  @Enumerated(EnumType.STRING)
  public ShiftSlot shiftSlot;
  
  /**
   * Data e ora di inizio.
   */
  @Required
  @NotNull
  @Column(name = "start_at")
  public LocalDateTime startAt;

  @Column(name = "end_to")
  public LocalDateTime endTo;
  
  public LocalDateTime employeeApproved;
  
  public LocalDateTime managerApproved;
  
  public LocalDateTime officeHeadApproved;
  
  public LocalDateTime administrativeApproved;
  
  public boolean employeeApprovalRequired = true;
  
  public boolean managerApprovalRequired = true;
  
  public boolean officeHeadApprovalRequired = true;
  
  public boolean administrativeApprovalRequired = true;
  
  @NotAudited
  @OneToMany(mappedBy = "competenceRequest")
  @OrderBy("createdAt DESC")
  public List<CompetenceRequestEvent> events = Lists.newArrayList();
  
  /**
   * Se il flusso è avviato.
   */
  public boolean flowStarted = false; 

  /**
   * Se il flusso è terminato.
   */
  public boolean flowEnded = false;
  
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
  
  @Transient
  public boolean isManagerApproved() {
    return managerApproved != null;
  }

  @Transient
  public boolean isAdministrativeApproved() {
    return administrativeApproved != null;
  }

  @Transient
  public boolean isOfficeHeadApproved() {
    return officeHeadApproved != null;
  }
  
  /**
   * Se non sono state già rilasciate approvazioni necessarie allora il possessore 
   * può cancellare o modificare la richiesta.
   * @return true se la richiesta di permesso è ancora modificabile o cancellabile.
   */
  @Transient
  public boolean ownerCanEditOrDelete() {
    return !flowStarted && (officeHeadApproved == null || !officeHeadApprovalRequired) 
        && (managerApproved == null || !managerApprovalRequired)
        && (administrativeApproved == null || !administrativeApprovalRequired)
        && (employeeApproved == null || !employeeApprovalRequired);
  }
  
  /**
   * Un flusso è completato se tutte le approvazioni richieste sono state
   * impostate.
   * 
   * @return true se è completato, false altrimenti.
   */
  public boolean isFullyApproved() {
    return (!this.managerApprovalRequired || this.isManagerApproved()) 
        && (!this.administrativeApprovalRequired 
            || this.isAdministrativeApproved())
        && (!this.officeHeadApprovalRequired || this.isOfficeHeadApproved())
        && (!this.employeeApprovalRequired
            || this.isEmployeeApproved());
  }
  
  @Transient
  public CompetenceRequestEvent actualEvent() {
    return this.events.get(0);
  }
  
}
