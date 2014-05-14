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

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import com.sun.corba.se.spi.orbutil.threadpool.Work;

import play.data.validation.Required;
import play.db.jpa.Model;

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
public class WorkingTimeType extends Model {
	
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

	public List<Contract> getAssociatedContract() {

		List<Contract> contractList = Contract.find(
				"Select distinct c from Contract c "
						+ "left outer join fetch c.contractWorkingTimeType as cwtt "
						+ "where cwtt.workingTimeType = ?", this).fetch();

		return contractList;
	}

	public List<Contract> getAssociatedActiveContract() {
		
		LocalDate today = new LocalDate();

		List<Contract> contractList = new ArrayList<Contract>();
		
		/*
		List<Contract> contractList = Contract.find(
				"Select distinct c from Contract c "
						+ "left outer join fetch c.contractWorkingTimeType as cwtt "
						+ "where cwtt.workingTimeType = ? "
						
						//contratto attivo nel periodo
						+ " and ( "
						//caso contratto non terminato
						+ "c.endContract is null and "
							//contratto a tempo indeterminato che si interseca col periodo 
							+ "( (c.expireContract is null and c.beginContract <= ? )"
							+ "or "
							//contratto a tempo determinato che si interseca col periodo (comanda il campo endContract)
							+ "(c.expireContract is not null and c.beginContract <= ? and c.expireContract >= ? ) ) "
						+ "or "
						//caso contratto terminato che si interseca col periodo		
						+ "c.endContract is not null and c.beginContract <= ? and c.endContract >= ? "
						+ ") "
						, this, today, today, today, today, today).fetch();
		*/
		
		List<Contract> activeContract = Contract.find(
				"Select c from Contract c "
										
						//contratto attivo nel periodo
						+ " where ( "
						//caso contratto non terminato
						+ "c.endContract is null and "
							//contratto a tempo indeterminato che si interseca col periodo 
							+ "( (c.expireContract is null and c.beginContract <= ? )"
							+ "or "
							//contratto a tempo determinato che si interseca col periodo (comanda il campo endContract)
							+ "(c.expireContract is not null and c.beginContract <= ? and c.expireContract >= ? ) ) "
						+ "or "
						//caso contratto terminato che si interseca col periodo		
						+ "c.endContract is not null and c.beginContract <= ? and c.endContract >= ? "
						+ ") "
						, today, today, today, today, today).fetch();
		
		for(Contract contract : activeContract) {
			ContractWorkingTimeType current = contract.getContractWorkingTimeType(today);
			if(current.workingTimeType.id.equals(this.id))
				contractList.add(contract);
		}
		
		return contractList;
	}
	
	
	
	public static List<WorkingTimeType> getDefaultWorkingTimeTypes() {
		
		List<WorkingTimeType> defaultList = WorkingTimeType.find(
				"select wtt from WorkingTimeType wtt where wtt.office is null order by description").fetch();
		return defaultList;
		
	}
	
	
	
	public static List<WorkingTimeType> getOfficesWorkingTimeTypes(List<Office> officeList) {
		
		List<WorkingTimeType> wttList = new ArrayList<WorkingTimeType>();
		for(Office office : officeList) {
			
			for(WorkingTimeType wtt : office.workingTimeType) {
				
				wttList.add(wtt);
			}
		}
		
		return wttList;
	}
	
	@Override
	public String toString() {
		return String.format("WorkingTimeType[%d] - description = %s, shift = %s", 
			id, description, shift);
	}
}

