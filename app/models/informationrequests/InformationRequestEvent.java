package models.informationrequests;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.joda.time.LocalDateTime;
import lombok.Builder;
import lombok.ToString;
import models.User;
import models.base.BaseModel;
import models.base.InformationRequest;
import models.flows.enumerate.InformationRequestEventType;
import play.data.validation.Required;

@Builder
@ToString
@Entity
@Table(name = "information_request_events")
public class InformationRequestEvent extends BaseModel{

  private static final long serialVersionUID = 8098464195779075326L;

  @NotNull
  @Column(name = "created_at")
  public LocalDateTime createdAt;
  
  @Required
  @NotNull  
  @ManyToOne(optional = false)
  @JoinColumn(name = "information_request_id")
  public InformationRequest informationRequest;
 
  @Required
  @NotNull
  @ManyToOne
  public User owner;
  
  public String description;
  
  @Required
  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "event_type")
  public InformationRequestEventType eventType;

  @PrePersist
  private void onUpdate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
