package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.LocalTime;

import play.db.jpa.Model;

@Entity
@Table(name="shift_time_table")
public class ShiftTimeTable extends Model{

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDateTime")
	public LocalTime startShift;
	
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDateTime")
	public LocalTime endShift;
	public String description;
	
	@OneToMany(mappedBy="shiftTimeTable", fetch=FetchType.LAZY)
	public List<PersonShiftDay> personShiftDay = new ArrayList<PersonShiftDay>();
}
