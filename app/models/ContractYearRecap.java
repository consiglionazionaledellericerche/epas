package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import models.base.BaseModel;
import play.data.validation.Required;


@Entity
@Table(name="contract_year_recap")
@Deprecated
public class ContractYearRecap extends BaseModel{

	private static final long serialVersionUID = 7025943511706872182L;

	@Required
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="contract_id")
	public Contract contract;
		
	@Column(name="year")
	public Integer year;

	@Column(name="vacation_last_year_used")
	public Integer vacationLastYearUsed = 0;
	
	@Column(name="vacation_current_year_used")
	public Integer vacationCurrentYearUsed = 0;
	
	@Column(name="permission_used")
	public Integer permissionUsed = 0;
	
	@Column(name="recovery_day_used")
	public Integer recoveryDayUsed = 0;
	
	@Column(name="remaining_minutes_last_year")
	public Integer remainingMinutesLastYear = 0;
	
	@Column(name="remaining_minutes_current_year")
	public Integer remainingMinutesCurrentYear = 0;
	
	@Column(name="remaining_meal_tickets")
	public Integer remainingMealTickets = 0;

}
