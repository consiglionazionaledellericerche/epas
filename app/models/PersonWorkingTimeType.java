package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 * @author alessandro
 */
@Entity
@Table(name = "persons_working_time_types")
public class PersonWorkingTimeType extends Model {

	@Required
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;
	
	@Required
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="working_time_type_id")
	public WorkingTimeType workingTimeType;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="begin_date")
	public LocalDate beginDate;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_date")
	public LocalDate endDate;
}
