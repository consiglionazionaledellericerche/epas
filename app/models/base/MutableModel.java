package models.base;

import org.joda.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

@MappedSuperclass
public abstract class MutableModel extends BaseModel {

  private static final long serialVersionUID = 4890911962768274977L;

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
