package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import play.db.jpa.Model;

@Audited
@Entity
@Table(name= "initialization_times")
public class InitializationTime extends Model{

	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;
	
	@Required
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;	
	
	@Column
	public Integer vacationLastYearUsed = 0;
	
	@Column
	public Integer vacationCurrentYearUsed = 0;
	
	
	@Column
	public Integer permissionUsed = 0;
	
	@Column
	public Integer recoveryDayUsed = 0;
	
	@Required
	@Column
	public Integer residualMinutesPastYear = 0;
	
	@Required
	@Column
	public Integer residualMinutesCurrentYear = 0;
	
	}
