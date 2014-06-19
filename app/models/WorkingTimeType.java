/**
 * 
 */
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
	
	/**
	 * 
	 * @return se il tale giorno è di riposo o meno a seconda del workingtimetype
	 */
	public boolean getHolidayFromWorkinTimeType(int dayOfWeek, WorkingTimeType wtt){
		boolean holiday = false;
		WorkingTimeTypeDay wttd = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ?" +
				" and wttd.dayOfWeek = ?", wtt, dayOfWeek).first();
		holiday = wttd.holiday;
		return holiday;
	}
	
	/**
	 * 
	 * @param dayOfWeek il giorno della settimana
	 * @return il WorkingTimeTypeDay in quel giorno della settimana
	 */
	public WorkingTimeTypeDay getWorkingTimeTypeDayFromDayOfWeek(int dayOfWeek){
		
		return this.workingTimeTypeDays.get(dayOfWeek-1);
	}
	
	/**
	 * 
	 * @param dayOfWeek
	 * @param wtt
	 * @return il numero di minuti minimo di lavoro per poter usufruire della pausa pranzo
	 */
	public int getMinimalTimeForLunch(int dayOfWeek, WorkingTimeType wtt){
		int minTimeForLunch = 0;
		WorkingTimeTypeDay wttd = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ?" +
				" and wttd.dayOfWeek = ?", wtt, dayOfWeek).first();
		minTimeForLunch = wttd.mealTicketTime;
		return minTimeForLunch;
	}
	
	/**
	 * 
	 * @param dayOfWeek
	 * @param wtt
	 * @return il numero di minuti di pausa pranzo per quel giorno per quell'orario
	 */
	public int getBreakTime(int dayOfWeek, WorkingTimeType wtt){
		int breakTime = 0;
		WorkingTimeTypeDay wttd = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ?" +
				"and wttd.dayOfWeek = ?", wtt, dayOfWeek).first();
		breakTime = wttd.breakTicketTime;
		
		return breakTime;
	}

	//PER la delete quindi per adesso permettiamo l'eliminazione solo di contratti particolari di office
	//bisogna controllare che this non sia default ma abbia l'associazione con office
	@Transient
	public List<Contract> getAssociatedContract() {

		List<Contract> contractList = Contract.find(
				"Select distinct c from Contract c "
						+ "left outer join fetch c.contractWorkingTimeType as cwtt "
						+ "where cwtt.workingTimeType = ?", this).fetch();

		return contractList;
	}

	/**
	 * I contratti attivi che attualmente hanno impostato il WorkingTimeType
	 * @return
	 */
	@Transient
	public List<Contract> getAssociatedActiveContract(Long officeId) {
		
		List<Contract> contractList = new ArrayList<Contract>();
		
		LocalDate today = new LocalDate();
		
		List<Contract> activeContract = Contract.getActiveContractInPeriod(today, today);
		
		for(Contract contract : activeContract) {
			
			if( !contract.person.office.id.equals(officeId))
				continue;
			
			ContractWorkingTimeType current = contract.getContractWorkingTimeType(today);
			if(current.workingTimeType.id.equals(this.id))
				contractList.add(contract);
		}
		
		return contractList;
	}
	
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
		
		List<WorkingTimeType> defaultList = WorkingTimeType.find(
				"select wtt from WorkingTimeType wtt where wtt.office is null order by description").fetch();
		return defaultList;
		
	}
	
	
	
	@Override
	public String toString() {
		return String.format("WorkingTimeType[%d] - description = %s, shift = %s", 
			id, description, shift);
	}
}

