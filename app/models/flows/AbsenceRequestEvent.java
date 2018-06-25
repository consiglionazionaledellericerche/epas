package models.flows;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.ToString;
import models.User;
import models.base.BaseModel;
import models.flows.enumerate.AbsenceRequestEventType;
import org.joda.time.LocalDateTime;
import play.data.validation.Required;

@ToString
@Entity
@Table(name = "absence_request_events")
public class AbsenceRequestEvent extends BaseModel {

  private static final long serialVersionUID = 8098464195779075326L;

  @NotNull
  @Column(name = "created_at")
  public LocalDateTime createdAt;
  
  @Required
  @NotNull  
  @ManyToOne(optional = false)
  @JoinColumn(name = "absence_request_id")
  public AbsenceRequest absenceRequest;
 
  @Required
  @NotNull
  @ManyToOne
  public User owner;
  
  @Required
  @NotNull
  public String description;
  
  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type")
  public AbsenceRequestEventType eventType;

  @PrePersist
  private void onUpdate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();;
    }
  }
}
