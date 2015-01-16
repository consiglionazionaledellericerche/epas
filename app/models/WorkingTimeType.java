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
import javax.persistence.Transient;

import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import dao.WorkingTimeTypeDao;


/**
 * Tipologia di orario di lavoro relativa ad un singolo giorno
 * 	(per esempio: Normale, Maternità, 50%...)
 * 
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
	@OneToMany(mappedBy="workingTimeType", fetch=FetchType.LAZY)
	public List<PersonWorkingTimeType> personWorkingTimeType = new ArrayList<PersonWorkingTimeType>();
	
	@NotAudited
	@OneToMany(mappedBy="workingTimeType", fetch=FetchType.LAZY)
	public List<ContractWorkingTimeType> contractWorkingTimeType = new ArrayList<ContractWorkingTimeType>();
	
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name = "office_id")
	public Office office;
	
	@Column(name="disabled")
	public boolean disabled = false;
	
	
	
	/**
	 * relazione con la tabella di specifiche di orario di lavoro
	 */
	@OneToMany( mappedBy = "workingTimeType", fetch = FetchType.EAGER)
	@OrderBy("dayOfWeek")
	public List<WorkingTimeTypeDay> workingTimeTypeDays = new ArrayList<WorkingTimeTypeDay>();
	
	
	
	
	
	@Transient
	public List<ContractWorkingTimeType> getAssociatedPeriodInActiveContract(Long officeId) {
		
		List<ContractWorkingTimeType> cwttList = new ArrayList<ContractWorkingTimeType>();
		
		LocalDate today = new LocalDate();
		
		List<Contract> activeContract = Contract.getActiveContractInPeriod(today, today);
		
		for(Contract contract : activeContract) {
			
			if( !contract.person.office.id.equals(officeId))	//TODO 	questa restrizione andrebbe fatta dentro activeContract
				continue;
			
			for(ContractWorkingTimeType cwtt: contract.contractWorkingTimeType) {
				
				if(cwtt.workingTimeType.id.equals(this.id))
					cwttList.add(cwtt);	
			}
		}
		
		return cwttList;
	}
	
	
	@Transient
	public static List<WorkingTimeType> getDefaultWorkingTimeTypes() {
		
		List<WorkingTimeType> defaultList = WorkingTimeTypeDao.getDefaultWorkingTimeType();
//		List<WorkingTimeType> defaultList = WorkingTimeType.find(
//				"select wtt from WorkingTimeType wtt where wtt.office is null order by description").fetch();
		return defaultList;
		
	}
	
	
	
	@Override
	public String toString() {
		return String.format("WorkingTimeType[%d] - description = %s, shift = %s", 
			id, description, shift);
	}
}

