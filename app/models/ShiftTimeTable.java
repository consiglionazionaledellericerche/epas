package models;

import java.util.ArrayList;
import java.util.List;

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
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime startMornig;
	
	// end time of morning shift
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime endMorning;
	
	// start time of afternoon shift
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime startAfternoon;
	
	// end time of afternoon shift
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime endAfternoon;
	
	// start time for morning lunch break
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime startMorningLunchTime;
	
	// end time for the morning lunch break
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime endMorningLunchTime;
	
	// start time for the lunch break
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime startAfternoonLunchTime;
		
	// end time for the lunch break
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalTimeAsString")
	public LocalTime endAfternoonLunchTime;

	// total amount of working minutes
	public  Integer totalWorMinutes;
	
	// Paid minuts per shift
	public Integer paidMinutes;
	
	@OneToMany(mappedBy="shiftType", fetch=FetchType.LAZY)
	public List<ShiftType> shiftTypes = new ArrayList<ShiftType>();
	
	
	// return startMorning as a string in hh:mm format
	public LocalTime getStartMornigShift(){
		return this.startMornig;
	}
	
	// return endMorning as a string in hh:mm format
	public LocalTime getEndMornigShift(){
		return this.endMorning;
	}
	
	// return startAfternoon as a string in hh:mm format
	public LocalTime getStartAfternoonShift(){
		return this.startAfternoon;
	}
	
	// return endAfternoon as a string in hh:mm format
	public LocalTime getEndAfternoonShift(){
		return this.endAfternoon;
	}
	
}
