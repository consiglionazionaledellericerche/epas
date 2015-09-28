package models.base;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.joda.time.LocalDateTime;

@MappedSuperclass
public abstract class MutableModel extends BaseModel {

	@Column(name="created_at")
	public LocalDateTime createdAt;
	
	@Column(name="updated_at")
    public LocalDateTime updatedAt;

	@PrePersist @PreUpdate
	private void onUpdate() {
		updatedAt = LocalDateTime.now();
		if (createdAt == null) {
			createdAt = updatedAt;
		}
	}
}