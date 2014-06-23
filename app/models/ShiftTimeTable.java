package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;



@Entity
@Table(name="shift_time_table")
public class ShiftTimeTable extends BaseModel{

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDateTime")
	public LocalDateTime startShift;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDateTime")
	public LocalDateTime endShift;
	
	public String description;
	
	@OneToMany(mappedBy="shiftTimeTable", fetch=FetchType.LAZY)
	public List<PersonShiftDay> personShiftDay = new ArrayList<PersonShiftDay>();
	
	// return startShift as a string in hh:mm format
	public String getStartShift(){
		return PersonTags.toCalendarTime(startShift);
	}
	
	// return endShift as a string in hh:mm format
	public String getEndShift(){
		return PersonTags.toCalendarTime(endShift);
	}

	public void setStartShift(String startShift) {
		String[] hmsStart = startShift.split(":");
		this.startShift = new LocalDateTime(1970, 01, 01, Integer.parseInt(hmsStart[0]), Integer.parseInt(hmsStart[1]));
	}

	public void setEndShift(String endShift) {
		String[] hmsEnd = endShift.split(":");
		this.endShift = new LocalDateTime(1970, 01, 01, Integer.parseInt(hmsEnd[0]), Integer.parseInt(hmsEnd[1]));
	}
	
}
