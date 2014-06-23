package models.base;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import play.db.jpa.GenericModel;

import com.google.common.base.Objects;

/**
 * Default base class per sovrascrivere la generazione delle nuove chiavi
 * primarie.
 * 
 * @author marco
 *
 */
@MappedSuperclass
public abstract class BaseModel extends GenericModel {

	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	public Long id;
	
	@Transient
	public Long getId() {
		return id;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("id", id).toString();
	}
}
