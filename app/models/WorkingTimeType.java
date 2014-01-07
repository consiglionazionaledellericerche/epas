/**
 * 
 */
package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OrderBy;


import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

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
	
	/**
	 * true se e solo se è l'orario di lavoro di default da assegnare
	 */
//	public boolean defaultWorkingTimeType = false;
	
//	/**
//	 * relazione con la tabella persone
//	 */
//	@OneToMany(mappedBy="workingTimeType")
//	public List<Person> person = new ArrayList<Person>();
		
	@NotAudited
	@OneToMany(mappedBy="workingTimeType", fetch=FetchType.LAZY)
	public List<PersonWorkingTimeType> personWorkingTimeType = new ArrayList<PersonWorkingTimeType>();
	
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
		/*
		WorkingTimeTypeDay wttd = WorkingTimeTypeDay.find("Select wttd from WorkingTimeTypeDay wttd where wttd.workingTimeType = ?" +
				" and wttd.dayOfWeek = ?", this, dayOfWeek).first();
		
		return wttd;
		*/
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
	
	
	@Override
	public String toString() {
		return String.format("WorkingTimeType[%d] - description = %s, shift = %s", 
			id, description, shift);
	}
}

