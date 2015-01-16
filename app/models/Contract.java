package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;

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

import models.base.BaseModel;

import org.hibernate.annotations.Type;
import org.hibernate.envers.NotAudited;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import dao.ContractDao;
import dao.MealTicketDao;
import dao.VacationPeriodDao;


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
		
		if(!this.isProperContract()) 
			return false;
		
		return true;
		
	}
	
	@Override
	public String toString() {
		return String.format("Contract[%d] - person.id = %d, beginContract = %s, expireContract = %s, endContract = %s",
				id, person.id, beginContract, expireContract, endContract);
	}


	@Transient
	public boolean isValidContract(){
		LocalDate date = new LocalDate();
		return endContract==null && beginContract.isBefore(date) && expireContract.isAfter(date);

	}

	@Transient
	public List<ContractWorkingTimeType> getContractWorkingTimeTypeAsList() {
		
		return Lists.newArrayList(this.contractWorkingTimeType);
	}
	
	@Transient
	public List<ContractStampProfile> getContractStampProfileAsList() {
		
		return Lists.newArrayList(this.contractStampProfile);
	}
	
	@Transient
	public ContractStampProfile getContractStampProfile(LocalDate date) {
		
		for(ContractStampProfile csp : this.contractStampProfile) {
			DateInterval interval = new DateInterval(csp.startFrom, csp.endTo);
			if(DateUtility.isDateIntoInterval(date, interval))
				return csp;
			
		}
		return null;
	}
	
	@Transient
	public ContractStampProfile getCurrentContractStampProfile() {
		
		return getContractStampProfile(LocalDate.now());
	}
	
	
	
	/**
	 * @param contract
	 * @return il vacation period associato al contratto con al suo interno la data di oggi
	 */
	public VacationPeriod getCurrentVacationPeriod()
	{
		for(VacationPeriod vp : this.vacationPeriods) {

			LocalDate now = new LocalDate();

			if(DateUtility.isDateIntoInterval(now, new DateInterval(vp.beginFrom, vp.endTo)))
				return vp;
		}
		return null;
	}
	
	/**
	 * @param date
	 * @return il periodo di validità del WorkingTimeType per il contratto alla data passata come argomento
	 */
	public ContractWorkingTimeType getContractWorkingTimeType(LocalDate date) {
		
		for(ContractWorkingTimeType cwtt: this.contractWorkingTimeType) {
			
			if(DateUtility.isDateIntoInterval(date, new DateInterval(cwtt.beginDate, cwtt.endDate) ))
				return cwtt;
		}
		return null;
	}
		

	/**
	 * @param contract
	 * @return i vacation period associati al contratto, ordinati in ordine crescente per data inizio
	 * 		 	null in caso di vacation period inesistente
	 */
	public List<VacationPeriod> getContractVacationPeriods()
	{
		//vacation period piu' recente per la persona
		List<VacationPeriod> vpList = VacationPeriodDao.getVacationPeriodByContract(this);
//		List<VacationPeriod> vpList = VacationPeriod.find(  "SELECT vp "
//				+ "FROM VacationPeriod vp "
//				+ "WHERE vp.contract = ? "
//				+ "ORDER BY vp.beginFrom",
//				this).fetch();

		//se il piano ferie associato al contratto non esiste 
		if(vpList==null)
		{
			Logger.debug("CurrentPersonVacationPeriod: il vacation period è inesistente");
			return null;
		}


		return vpList;
	}


	/**
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
	 * Ritorna l'intervallo valido ePAS per il contratto. 
	 * (scarto la parte precedente a source contract se definita)
	 * @return
	 */
	public DateInterval getContractDatabaseDateInterval() {
		
		if(this.sourceDate != null && this.sourceDate.isAfter(this.beginContract)) {
			
			DateInterval contractInterval;
			if(this.endContract!=null)
				contractInterval = new DateInterval(this.sourceDate, this.endContract);
			else
				contractInterval = new DateInterval(this.sourceDate, this.expireContract);
			return contractInterval;
		}
		
		return this.getContractDateInterval();
		
	}
	
	/**
	 * Ritorna l'intervallo valido ePAS per il contratto riguardo la gestione dei buoni pasto.
	 * (scarto la parte precedente a source se definita, e la parte precedente alla data inizio 
	 * utilizzo per la sede della persona).
	 * @return null in caso non vi siano giorni coperti dalla gestione dei buoni pasto.
	 */
	public DateInterval getContractMealTicketDateInterval() {
		
		DateInterval contractDataBaseInterval = this.getContractDatabaseDateInterval();
		
		LocalDate officeStartDate = MealTicketDao.getMealTicketStartDate(this.person.office);
		if(officeStartDate == null)
			return null;
		
		if(officeStartDate.isBefore(contractDataBaseInterval.getBegin()))
			return contractDataBaseInterval;
		
		if(DateUtility.isDateIntoInterval(officeStartDate, contractDataBaseInterval))
			return new DateInterval(officeStartDate, contractDataBaseInterval.getEnd());
		
		return null;
	}

	
	/**
	 * Ritorna il riepilogo annule del contatto.
	 * @param year
	 * @return
	 */
	public ContractYearRecap getContractYearRecap(int year)
	{
		for(ContractYearRecap cyr : this.recapPeriods)
		{
			if(cyr.year==year)
				return cyr;
		}
		return null;
			
	}
	

	/**
	 * True se il contratto è l'ultimo contratto per mese e anno selezionati.
	 * @param month
	 * @param year
	 * @return
	 */
	public boolean isLastInMonth(Integer month, Integer year)
	{
		List<Contract> contractInMonth = this.person.getMonthContracts(month, year);
		if(contractInMonth.size()==0)
			return false;
		if(contractInMonth.get(contractInMonth.size()-1).id.equals(this.id))
			return true;
		else
			return false;
	}
	
	
	/**
	 * True se il contratto non si interseca con nessun altro contratto per la persona. False altrimenti
	 * @return
	 */
	public boolean isProperContract() {

		DateInterval contractInterval = this.getContractDateInterval();
		for(Contract c : person.contracts) {
			
			if(this.id != null && c.id.equals(this.id)) {
				continue;
			}
			
			if(DateUtility.intervalIntersection(contractInterval, c.getContractDateInterval()) != null) {
				return false;
			}
		}
		return true;
	}
	
	/**
	
	 */
	
		
	/**
	 * La lista con tutti i contratti attivi nel periodo selezionato.
	 * @return
	 */
	public static List<Contract> getActiveContractInPeriod(LocalDate begin, LocalDate end) {
		
		//TODO queryDSL e spostare nel ContractDao
		if(end == null)
			end = new LocalDate(9999,1,1);
		
		/**
		 * TODO: verificare nell'unico metodo in cui è chiamata (WorkingTimes.executeChangeWorkingTimeTypeToAll) se funziona correttamente,
		 * visto che quel metodo ha un discreto impatto sui dati
		 */
		List<Contract> activeContract = ContractDao.getActiveContractsInPeriod(begin, end);
//		List<Contract> activeContract = Contract.find(
//				"Select c from Contract c "
//										
//						//contratto attivo nel periodo
//						+ " where ( "
//						//caso contratto non terminato
//						+ "c.endContract is null and "
//							//contratto a tempo indeterminato che si interseca col periodo 
//							+ "( (c.expireContract is null and c.beginContract <= ? )"
//							+ "or "
//							//contratto a tempo determinato che si interseca col periodo (comanda il campo endContract)
//							+ "(c.expireContract is not null and c.beginContract <= ? and c.expireContract >= ? ) ) "
//						+ "or "
//						//caso contratto terminato che si interseca col periodo		
//						+ "c.endContract is not null and c.beginContract <= ? and c.endContract >= ? "
//						+ ") "
//						, end, end, begin, end, begin).fetch();
		
		return activeContract;
		
		
	}
		
}
	
	

