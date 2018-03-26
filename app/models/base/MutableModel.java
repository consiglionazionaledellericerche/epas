package models.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Version;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDateTime;

@MappedSuperclass
public abstract class MutableModel extends BaseModel {

  private static final long serialVersionUID = 4890911962768274977L;
  
  @JsonIgnore
  @NotAudited
  @Version
  public Integer version;
  
  @Column(name = "created_at")
  public LocalDateTime createdAt;

  @Column(name = "updated_at")
  public LocalDateTime updatedAt;

  @PrePersist
  @PreUpdate
  private void onUpdate() {
    updatedAt = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = updatedAt;
    }
  }
}
