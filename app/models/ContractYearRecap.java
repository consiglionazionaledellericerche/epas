package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.data.validation.Required;
import play.db.jpa.Model;

@Entity
@Table(name="contract_year_recap")
public class ContractYearRecap extends Model{

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

}
