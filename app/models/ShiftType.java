package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import play.data.validation.Required;



@Entity
@Audited
@Table(name="shift_type")
public class ShiftType extends BaseModel{

	private static final long serialVersionUID = 3156856871540530483L;
	
	public String type;
	public String description;
	
	@NotAudited
	@OneToMany(mappedBy="shiftType")
	public List<PersonShiftShiftType> personShiftShiftTypes = new ArrayList<PersonShiftShiftType>();
	
	@NotAudited
	@OneToMany(mappedBy="shiftType")
	public List<PersonShiftDay> personShiftDays = new ArrayList<PersonShiftDay>();
	
	@NotAudited
	@OneToMany(mappedBy="type")
	public List<ShiftCancelled> shiftCancelled = new ArrayList<ShiftCancelled>();
	
	@NotAudited
	@ManyToOne
	@JoinColumn(name="shift_time_table_id")
	public ShiftTimeTable shiftTimeTable;
	
	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "shift_categories_id")
	public ShiftCategories shiftCategories;
}
