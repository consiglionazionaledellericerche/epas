/**
 * 
 */
package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import manager.PersonManager;
import models.base.BaseModel;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;
import dao.ContractDao;
import dao.WorkingTimeTypeDao;


/**
 * Classe che rappresenta un giorno, sia esso lavorativo o festivo di una persona.
 *  
 * @author cristian
 * @author dario
 *
 */
@Entity
@Audited
@Table(name="person_days", uniqueConstraints = { @UniqueConstraint(columnNames={ "person_id", "date"}) })
public class PersonDay extends BaseModel {

	private static final long serialVersionUID = -5013385113251848310L;

	@Required
	@ManyToOne(optional = false)
	@JoinColumn(name = "person_id", nullable = false)
	public Person person;

	@Required
//	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;

	@Column(name = "time_at_work")
	public Integer timeAtWork;
	
	public Integer difference;

	public Integer progressive;
	
	@Column(name = "is_ticket_available")
	public boolean isTicketAvailable = true;

	@Column(name = "is_ticket_forced_by_admin")
	public boolean isTicketForcedByAdmin = false;

	//@Column(name = "is_time_at_work_auto_certificated")
	//public boolean isTimeAtWorkAutoCertificated = false;
		
	//@Column(name = "is_holiday")
	//public boolean isHoliday = false;	rmHoliday
	
	@Column(name = "is_working_in_another_place")
	public boolean isWorkingInAnotherPlace = false;

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
	@OrderBy("date ASC")
	public List<Stamping> stampings = new ArrayList<Stamping>();

	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
	public List<Absence> absences = new ArrayList<Absence>();

	@ManyToOne
	@JoinColumn(name = "stamp_modification_type_id")
	public StampModificationType stampModificationType;
	
	@NotAudited
	@OneToMany(mappedBy="personDay", fetch = FetchType.LAZY, cascade= {CascadeType.PERSIST, CascadeType.REMOVE})
	public List<PersonDayInTrouble> troubles = new ArrayList<PersonDayInTrouble>();
	
	@Transient
	public PersonDay previousPersonDayInMonth = null;
		
	@Transient
	public Contract personDayContract = null;
	
	@Transient
	private Boolean isHolidayy = null;
	
	@Transient
	private Boolean isFixedTimeAtWorkk = null;
	
	@Transient
	public MealTicket mealTicketAssigned = null;
	
	
	public PersonDay(Person person, LocalDate date, int timeAtWork, int difference, int progressive) {
		this.person = person;
		this.date = date;
		this.timeAtWork = timeAtWork;
		this.difference = difference;
		this.progressive = progressive;
	}

	public PersonDay(Person person, LocalDate date){
		this(person, date, 0, 0, 0);
	}
	
	@Transient
	@Deprecated
	public Contract getPersonDayContract() {

		if(this.personDayContract != null)
			return this.personDayContract;
		
		//this.personDayContract = this.person.getContract(date);
		this.personDayContract = ContractDao.getContract(date, person);
		
		return this.personDayContract;
	}
	
	/**
	 * Controlla che il personDay cada in un giorno festivo
	 * @param data
	 * @return
	 */
	public boolean isHoliday(){
		if(isHolidayy!=null)	//già calcolato
			return isHolidayy;
		//isHolidayy = this.person.isHoliday(this.date);
		isHolidayy = PersonManager.isHoliday(person, date);
		return isHolidayy;
	}
	
	/**
	 * Controlla che il personDay cada nel giorno attuale
	 * @return
	 */
	public boolean isToday(){
		return this.date.isEqual(new LocalDate());
	}

	/** 
	 * True se il personDay cade in uno stampProfile con fixedTimeAtWork = true;
	 * @return
	 */
	public boolean isFixedTimeAtWork()
	{
		if(this.isFixedTimeAtWorkk!=null)	//già calcolato
			return this.isFixedTimeAtWorkk;
		
		this.isFixedTimeAtWorkk = false;
		
		//Contract contract = this.person.getContract(this.date);
		Contract contract = ContractDao.getContract(this.date, this.person);
		if(contract == null)
			return false;
		
		for(ContractStampProfile csp : contract.contractStampProfile){
			if(DateUtility.isDateIntoInterval(this.date, new DateInterval(csp.startFrom, csp.endTo))){
				this.isFixedTimeAtWorkk = csp.fixedworkingtime;
			}
		}

		return this.isFixedTimeAtWorkk;
	}

	/**
	 * FIXME il modello non deve usare i Dao. Spostare nel Dao o nel WrapperPersonDay
	 * Il piano giornaliero di lavoro previsto dal contratto per quella data
	 * @return 
	 */
	public WorkingTimeTypeDay getWorkingTimeTypeDay(){
		
		//return person.getWorkingTimeType(date).workingTimeTypeDays.get(date.getDayOfWeek()-1);
		//WorkingTimeType wtt = person.getWorkingTimeType(date);
		WorkingTimeType wtt = WorkingTimeTypeDao.getWorkingTimeTypeStatic(date, person);
		if(wtt == null)
			return null;
		
		WorkingTimeTypeDay wttd = wtt.workingTimeTypeDays.get(date.getDayOfWeek()-1);
		if(wttd == null)
			return null;
		
		return wttd;
	}
	
	
	@Override
	public String toString() {
		return String.format("PersonDay[%d] - person.id = %d, date = %s, difference = %s, isTicketAvailable = %s, modificationType = %s, progressive = %s, timeAtWork = %s",
				id, person.id, date, difference, isTicketAvailable, stampModificationType, progressive, timeAtWork);
	}

}
