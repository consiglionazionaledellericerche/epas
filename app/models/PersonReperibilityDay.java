package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * 
 * Rappresenta un giorno di reperibilità di una persona reperibile
 * 
 * @author cristian
 */
@Audited
@Entity
@Table(name = "person_reperibility_days", uniqueConstraints = { @UniqueConstraint(columnNames={ "person_reperibility_id", "date"}) })
public class PersonReperibilityDay extends Model {
	
	@Required
	@ManyToOne	
	@JoinColumn(name = "person_reperibility_id", nullable = false)
	public PersonReperibility personReperibility;

	@Required
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;

	@Column(name = "holiday_day")
	public Boolean holidayDay;
	
}
