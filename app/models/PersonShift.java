package models;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name="person_shift")
public class PersonShift extends Model{

	public boolean jolly;
	
	public String description;
	
	@OneToOne
	@JoinColumn(name="person_id", unique=true, nullable=false, updatable=false)
	public Person person;

	@OneToMany(mappedBy="personShift", fetch=FetchType.LAZY)
	public List<ShiftType> shiftTypes;
	
	@OneToOne(mappedBy="personShift", fetch=FetchType.LAZY)
	public PersonShiftDay personShiftDay;
}
