package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import net.fortuna.ical4j.model.DateTime;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import play.db.jpa.Model;

@Entity
@Table(name="shift_time_table")
public class ShiftTimeTable extends Model{

	@OneToMany(mappedBy="shiftTimeTable", fetch=FetchType.LAZY)
	public List<ShiftType> personShiftDaysshiftTypes = new ArrayList<ShiftType>();

	// start time of morning shift
	@Column(name="start_morning")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime startMorning;
	
	// end time of morning shift
	@Column(name="end_morning")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime endMorning;
	
	// start time of afternoon shift
	@Column(name="start_afternoon")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime startAfternoon;
	
	// end time of afternoon shift
	@Column(name="end_afternoon")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime endAfternoon;
	
	// start time for morning lunch break
	@Column(name="start_morning_lunch_time")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime startMorningLunchTime;
	
	// end time for the morning lunch break
	@Column(name="end_morning_lunch_time")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime endMorningLunchTime;
	
	// start time for the lunch break
	@Column(name="start_afternoon_lunch_time")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime startAfternoonLunchTime;
		
	// end time for the lunch break
	@Column(name="end_afternoon_lunch_time")
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime endAfternoonLunchTime;

	// total amount of working minutes
	@Column(name="total_work_minutes")
	public  Integer totalWorkMinutes;
	
	// Paid minuts per shift
	@Column(name="paid_minutes")
	public Integer paidMinutes;
	
	@OneToMany(mappedBy="shiftType", fetch=FetchType.LAZY)
	public List<ShiftType> shiftTypes = new ArrayList<ShiftType>();
		
}
