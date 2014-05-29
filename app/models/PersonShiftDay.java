package models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;



@Entity
@Table(name="person_shift_days")
public class PersonShiftDay extends BaseModel{

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;
	
	@ManyToOne
	@JoinColumn(name="shift_type_id")
	public ShiftType shiftType;
	
	@ManyToOne
	@JoinColumn(name="shift_time_table_id")
	public ShiftTimeTable shiftTimeTable;
	
	@ManyToOne
	@JoinColumn(name="person_shift_id", nullable=false)
	public PersonShift personShift;
}
