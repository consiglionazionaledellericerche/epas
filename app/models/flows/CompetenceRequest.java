package models.flows;

import java.util.List;
import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import com.beust.jcommander.internal.Lists;
import models.Person;
import models.base.MutableModel;
import play.data.validation.Required;

public class CompetenceRequest extends MutableModel {

  @Required
  @NotNull
  @ManyToOne(optional = false)
  public Person person;
  
  /**
   * Descrizione della richiesta
   */
  public String note;
  
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
  
}
