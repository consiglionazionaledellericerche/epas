/**
 * 
 */
package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.data.validation.Unique;


/**
 * Contiene le informazioni per l'eventuale "reperibilità" svolta dalla persona
 * 
 * @author cristian
 *
 */
@Audited
@Entity
@Table(name="person_reperibility")
public class PersonReperibility extends BaseModel {

	private static final long serialVersionUID = 7543768807724174894L;

	@Unique
	@Required
	@OneToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id", unique=true)
	public Person person;
	
	@Column(name="start_date")

	public LocalDate startDate;
	
	@Column(name="end_date")

	public LocalDate endDate;
	
	@Required
	@ManyToOne
	@JoinColumn(name = "person_reperibility_type_id")
	public PersonReperibilityType personReperibilityType;
	
	public String note;
	
	@Override
	public String toString() {
		return String.format("PersonReperibility[%d] - person.id = %d, startDate = %s, endDate = %s, personReperibilityType.id = %s",
			id, person.id, startDate, endDate, personReperibilityType != null ? personReperibilityType.id : "null");
	}
}
