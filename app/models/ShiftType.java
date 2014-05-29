package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;



@Entity
@Table(name="shift_type")
public class ShiftType extends BaseModel{

	public String type;
	public String description;
	
	@OneToMany(mappedBy="shiftType")
	public List<PersonShiftShiftType> personShiftShiftTypes = new ArrayList<PersonShiftShiftType>();
	
	@OneToMany(mappedBy="shiftType", fetch=FetchType.LAZY)
	public List<PersonShiftDay> personShiftDays = new ArrayList<PersonShiftDay>();
	
	@OneToMany(mappedBy="type", fetch=FetchType.LAZY)
	public List<ShiftCancelled> shiftCancelled = new ArrayList<ShiftCancelled>();
}
