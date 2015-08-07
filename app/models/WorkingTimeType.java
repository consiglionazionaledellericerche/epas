package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import play.data.validation.Required;


/**
 * @author cristian
 * @author dario
 * 
 */
@Entity
@Audited
@Table(name="working_time_types")
public class WorkingTimeType extends BaseModel {

	private static final long serialVersionUID = -3443521979786226461L;

	@Required
	@Column(nullable=false)
	public String description;

	/**
	 * True se il tipo di orario corrisponde ad un "turno di lavoro"
	 * false altrimenti 
	 */
	public boolean shift = false;

	@Column(name="meal_ticket_enabled")
	public boolean mealTicketEnabled = true;	//inutile

	@NotAudited
	@OneToMany(mappedBy="workingTimeType")
	public List<PersonWorkingTimeType> personWorkingTimeType = new ArrayList<PersonWorkingTimeType>();

	@NotAudited
	@OneToMany(mappedBy="workingTimeType")
	public List<ContractWorkingTimeType> contractWorkingTimeType = new ArrayList<ContractWorkingTimeType>();

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "office_id")
	public Office office;

	@Column(name="disabled")
	public boolean disabled = false;

	@OneToMany( mappedBy = "workingTimeType", fetch = FetchType.EAGER)
	@OrderBy("dayOfWeek")
	public List<WorkingTimeTypeDay> workingTimeTypeDays = new ArrayList<WorkingTimeTypeDay>();

	@Override
	public String toString() {
		return description;
	}
}

