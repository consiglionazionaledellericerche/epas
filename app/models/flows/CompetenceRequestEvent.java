package models.flows;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.validation.constraints.NotNull;
import org.joda.time.LocalDateTime;
import models.User;
import models.base.BaseModel;
import models.flows.enumerate.AbsenceRequestEventType;
import models.flows.enumerate.CompetenceRequestEventType;
import play.data.validation.Required;

public class CompetenceRequestEvent extends BaseModel {

  @NotNull
  @Column(name = "created_at")
  public LocalDateTime createdAt;
  
  @Required
  @NotNull  
  @ManyToOne(optional = false)
  @JoinColumn(name = "competence_request_id")
  public CompetenceRequest competenceRequest;
  
  @Required
  @NotNull
  @ManyToOne
  public User owner;
  
  public String description;
  
  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type")
  public CompetenceRequestEventType eventType;

  @PrePersist
  private void onUpdate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
