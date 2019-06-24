package models.flows;

import com.beust.jcommander.internal.Lists;
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
import lombok.ToString;
import models.Person;
import models.base.MutableModel;
import models.flows.enumerate.AbsenceRequestType;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import play.data.validation.Required;
import play.db.jpa.Blob;

/**
 * Richiesta di assenza.
 * 
 * @author cristian
 *
 */
@ToString(of = {"type", "person", "startAt", "endTo", 
    "managerApproved", "administrativeApproved", "officeHeadApproved",
    "administrativeApprovalRequired", "officeHeadApprovalRequired",
    "officeHeadApprovalForManagerRequired"})
@Audited
@Entity
@Table(name = "absence_requests")
public class AbsenceRequest extends MutableModel {

  private static final long serialVersionUID = 328199210648734558L;

  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  public AbsenceRequestType type;

  @Required
  @NotNull
  @ManyToOne(optional = false)
  public Person person;

  /**
   * Data e ora di inizio.
   */
  @Required
  @NotNull
  @Column(name = "start_at")
  public LocalDateTime startAt;

  @Column(name = "end_to")
  public LocalDateTime endTo;

  /**
   * Descrizione facoltativa della richiesta.
   */
  public String note;

  /**
   * Eventuale allegato alla richiesta.
   */
  @Column(name = "attachment", nullable = true)
  public Blob attachment;


  /**
   * Data di approvazione del responsabile.
   */
  @Column(name = "manager_approved")
  public LocalDateTime managerApproved;

  /**
   * Data di approvazione dell'amministrativo.
   */
  @Column(name = "administrative_approved")
  public LocalDateTime administrativeApproved;

  /**
   * Data di approvazione del responsabili sede.
   */
  @Column(name = "office_head_approved")
  public LocalDateTime officeHeadApproved;

  /**
   * Indica se è richieta l'approvazione da parte del responsabile.
   */
  @Column(name = "manager_approval_required")
  public boolean managerApprovalRequired = true;

  /**
   * Indica se è richieta l'approvazione da parte dell'amministrativo.
   */
  @Column(name = "administrative_approval_required")
  public boolean administrativeApprovalRequired = true;

  /**
   * Indica se è richieta l'approvazione da parte del responsabile di sede.
   */
  @Column(name = "office_head_approval_required")
  public boolean officeHeadApprovalRequired = true;
  
  @Column(name = "office_head_approval_for_manager_required")
  public boolean officeHeadApprovalForManagerRequired = true;

  @NotAudited
  @OneToMany(mappedBy = "absenceRequest")
  @OrderBy("createdAt DESC")
  public List<AbsenceRequestEvent> events = Lists.newArrayList();

  /**
   * Se il flusso è avviato.
   */
  @Column(name = "flow_started")
  public boolean flowStarted = false; 

  /**
   * Se il flusso è terminato.
   */
  @Column(name = "flow_ended")
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
        && (managerApproved == null || !managerApprovalRequired
        && (administrativeApproved == null || !administrativeApprovalRequired));
  }

  
  @Transient
  public AbsenceRequestEvent actualEvent() {
    return this.events.get(0);
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
        && (!this.officeHeadApprovalForManagerRequired 
            || this.isOfficeHeadApproved());
  }
}
