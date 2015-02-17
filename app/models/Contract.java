package models;

import it.cnr.iit.epas.DateInterval;

import java.util.List;
import java.util.Set;

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
import javax.validation.constraints.NotNull;

import manager.ContractManager;
import manager.PersonManager;
import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.data.validation.Required;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


/**
 * 
 * @author dario
 *
 * il contratto non è gestito direttamente da questa applicazione ma le sue informazioni
 * sono prelevate da un altro servizio
 */

@Entity
@Table(name="contracts")
public class Contract extends BaseModel {
	
	private static final long serialVersionUID = -4472102414284745470L;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="source_date")
	public LocalDate sourceDate = null;
	
	@Column(name="source_vacation_last_year_used")
	public Integer sourceVacationLastYearUsed = null;
	
	@Column(name="source_vacation_current_year_used")
	public Integer sourceVacationCurrentYearUsed = null;
	
	@Column(name="source_permission_used")
	public Integer sourcePermissionUsed = null;
	
	@Column(name="source_recovery_day_used")
	public Integer sourceRecoveryDayUsed = null;
	
	@Column(name="source_remaining_minutes_last_year")
	public Integer sourceRemainingMinutesLastYear = null;
	
	@Column(name="source_remaining_minutes_current_year")
	public Integer sourceRemainingMinutesCurrentYear = null;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;
	
	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	@OrderBy("beginFrom")
	public Set<VacationPeriod> vacationPeriods = Sets.newHashSet();
	
	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	public List<ContractYearRecap> recapPeriods;

	@Required @NotNull
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="begin_contract")
	public LocalDate beginContract;

	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="expire_contract")
	public LocalDate expireContract;

	//data di termine contratto in casi di licenziamento, pensione, morte, ecc ecc...
	@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
	@Column(name="end_contract")
	public LocalDate endContract;
	
	@NotAudited
	@OneToMany(mappedBy = "contract", fetch=FetchType.LAZY, cascade = {CascadeType.REMOVE})
	@OrderBy("beginDate")
	public Set<ContractWorkingTimeType> contractWorkingTimeType = Sets.newHashSet();
	
	@NotAudited
	@OneToMany(mappedBy="contract")
	@OrderBy("startFrom")
	public Set<ContractStampProfile> contractStampProfile = Sets.newHashSet();
	
	@NotAudited
	@OneToMany(mappedBy="contract", fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE})
	public List<MealTicket> mealTickets;
	
	
	@Transient
	private List<ContractWorkingTimeType> contractWorkingTimeTypeAsList;
	
	
	public void setSourceDate(String date){
		this.sourceDate = new LocalDate(date);
	}
	
	/**
	 * I contratti con onCertificate = true sono quelli dei dipendenti CNR e 
	 * corrispondono a quelli con l'obbligo dell'attestato di presenza 
	 * da inviare a Roma
	 */
	@Required
	public boolean onCertificate = false;
	
	@Override
	public boolean crossFieldsValidation() {
		
		if(this.expireContract != null 
				&& this.expireContract.isBefore(this.beginContract))
			return false;
		
		if(this.endContract != null 
				&& this.endContract.isBefore(this.beginContract))
			return false;
		
		if(this.expireContract != null && this.endContract != null 
				&& this.expireContract.isBefore(this.endContract))
			return false;
		
		if(! ContractManager.isProperContract(this) ) 
			return false;
		
		return true;
		
	}
	
	@Override
	public String toString() {
		return String.format("Contract[%d] - person.id = %d, beginContract = %s, expireContract = %s, endContract = %s",
				id, person.id, beginContract, expireContract, endContract);
	}
	

	/**
	 * FIXME usate nel template spostare nel wrapper
	 * Utilizza la libreria DateUtils per costruire l'intervallo attivo per il contratto.
	 * @return
	 */
	public DateInterval getContractDateInterval()
	{
		DateInterval contractInterval;
		if(this.endContract!=null)
			contractInterval = new DateInterval(this.beginContract, this.endContract);
		else
			contractInterval = new DateInterval(this.beginContract, this.expireContract);
		return contractInterval;
	}
	
	/**
	 * FIXME usate nel template spostare nel wrapper
	 * True se il contratto è l'ultimo contratto per mese e anno selezionati.
	 * @param month
	 * @param year
	 * @return
	 */
	public boolean isLastInMonth(Integer month, Integer year)
	{
		List<Contract> contractInMonth = PersonManager.getMonthContracts(this.person,month, year);
		if(contractInMonth.size()==0)
			return false;
		if(contractInMonth.get(contractInMonth.size()-1).id.equals(this.id))
			return true;
		else
			return false;
	}
	
	/**
	 * FIXME variabile transiente richiamata nel template. Spostare nel wrapper.
	 * Conversione della lista dei contractWorkingtimeType da Set a List
	 * @param contract
	 * * @return
	 */
	public List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList() {
		
		return Lists.newArrayList(this.contractWorkingTimeType);
	}
	

}
	
	

