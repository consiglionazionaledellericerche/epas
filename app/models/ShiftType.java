package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import models.base.BaseModel;



@Entity
@Audited
@Table(name="shift_type")
public class ShiftType extends BaseModel{

	public String type;
	public String description;
	
	@NotAudited
	@OneToMany(mappedBy="shiftType")
	public List<PersonShiftShiftType> personShiftShiftTypes = new ArrayList<PersonShiftShiftType>();
	
	@NotAudited
	@OneToMany(mappedBy="shiftType", fetch=FetchType.LAZY)
	public List<PersonShiftDay> personShiftDays = new ArrayList<PersonShiftDay>();
	
	@NotAudited
	@OneToMany(mappedBy="type", fetch=FetchType.LAZY)
	public List<ShiftCancelled> shiftCancelled = new ArrayList<ShiftCancelled>();
	
	@NotAudited
	@ManyToOne
	@JoinColumn(name="shift_time_table_id")
	public ShiftTimeTable shiftTimeTable;
	
	/**
	 * responsabile del turno
	 */
	@ManyToOne(optional = false)
	@JoinColumn(name = "supervisor")
	public Person supervisor;
}
