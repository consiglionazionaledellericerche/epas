package models.flows;

import com.beust.jcommander.internal.Lists;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
@ToString
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
  public String description;
  
  /**
   * Eventuale allegato alla richiesta.
   */
  @Column(name = "attachment", nullable = true)
  public Blob attachment;
  
  
  /**
   * Data di approvazione del responsabile.
   */
  @Column(name = "manager_approved")
  public LocalDate managerApproved;

  /**
   * Data di approvazione dell'amministrativo.
   */
  @Column(name = "administrative_approved")
  public LocalDate administrativeApproved;

  /**
   * Data di approvazione del responsabili sede.
   */
  @Column(name = "office_head_approved")
  public LocalDate officeHeadApproved;
  
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
  
  @NotAudited
  @OneToMany(mappedBy = "absenceRequest")
  public List<AbsenceRequestEvent> events = Lists.newArrayList();
  
  @Transient
  public LocalDate startAtAsDate() {
    return startAt != null ? startAt.toLocalDate() : null;
  }
  
  @Transient
  public LocalDate endToAsDate() {
    return endTo != null ? endTo.toLocalDate() : null;
  }
  
  /**
   * Se non sono state già rilasciate approvazioni necessarie allora il possessore 
   * può cancellare o modificare la richiesta.
   * @return true se la richiesta di permesso è ancora modificabile o cancellabile.
   */
  @Transient
  public boolean ownerCanEditOrDelete() {
    return (officeHeadApproved == null || !officeHeadApprovalRequired) 
        && (managerApproved == null || !managerApprovalRequired
        && (administrativeApproved == null || !administrativeApprovalRequired));
  }
}
