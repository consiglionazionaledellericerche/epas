package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;
import models.personalMonthSituation.CalcoloSituazioneAnnualePersona;
import models.personalMonthSituation.Mese;
import models.rendering.VacationsRecap;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.joda.time.LocalDate;

import play.Logger;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * 
 * @author dario
 *
 * il contratto non è gestito direttamente da questa applicazione ma le sue informazioni
 * sono prelevate da un altro servizio
 */

@Entity
@Table(name="contracts")
public class Contract extends Model {

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

	
	private static final long serialVersionUID = -4472102414284745470L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;
	
	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	public List<VacationPeriod> vacationPeriods;
	
	@OneToMany(mappedBy="contract", fetch=FetchType.LAZY, cascade = CascadeType.REMOVE)
	public List<ContractYearRecap> recapPeriods;

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

	//TODO eliminare e configurare yaml
	public void setBeginContract(String date){
		this.beginContract = new LocalDate(date);
	}
	//TODO eliminare e configurare yaml
	public void setEndContract(String date){
		this.endContract = new LocalDate(date);
	}
	//TODO eliminare e configurare yaml
	public void setExpireContract(String date){
		this.expireContract = new LocalDate(date);
	}
	
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

	@Transient
	public boolean isValidContract(){
		LocalDate date = new LocalDate();
		return endContract==null && beginContract.isBefore(date) && expireContract.isAfter(date);

	}

	@Override
	public String toString() {
		return String.format("Contract[%d] - person.id = %d, beginContract = %s, expireContract = %s, endContract = %s",
				id, person.id, beginContract, expireContract, endContract);
	}

	/**
	 * @param contract
	 * @return il vacation period associato al contratto con al suo interno la data di oggi
	 */
	public VacationPeriod getCurrentVacationPeriod()
	{
		List<VacationPeriod> vpList = this.getContractVacationPeriods();
		for(VacationPeriod vp : vpList)
		{

			LocalDate now = new LocalDate();

			if(DateUtility.isDateIntoInterval(now, new DateInterval(vp.beginFrom, vp.endTo)))
				return vp;
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
		List<VacationPeriod> vpList = VacationPeriod.find(  "SELECT vp "
				+ "FROM VacationPeriod vp "
				+ "WHERE vp.contract = ? "
				+ "ORDER BY vp.beginFrom",
				this).fetch();

		//se il piano ferie associato al contratto non esiste 
		if(vpList==null)
		{
			Logger.debug("CurrentPersonVacationPeriod: il vacation period è inesistente");
			return null;
		}


		return vpList;
	}

	/**
	 * Costruisce la struttura valida dei vacation period associati al contratto.
	 */
	public void setVacationPeriods(){


		//Distruggo i vacation period precedenti
		if(this.vacationPeriods != null){
			for(VacationPeriod vp : this.vacationPeriods)
			{
				vp.delete();
			}
		}

		//Contratto terminato nessun vacation period
		if(this.endContract!=null)
		{
			this.save();
			return;
		}

		//Tempo indeterminato, creo due vacatio 3 anni più infinito
		if(this.expireContract == null)
		{

			VacationPeriod first = new VacationPeriod();
			first.beginFrom = this.beginContract;
			first.endTo = this.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = this;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = this.beginContract.plusYears(3);
			second.endTo = null;
			second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract =this;
			second.save();
			this.save();
			return;
		}

		//Tempo determinato più lungo di 3 anni
		if(this.expireContract.isAfter(this.beginContract.plusYears(3).minusDays(1))){
			VacationPeriod first = new VacationPeriod();
			first.beginFrom = this.beginContract;
			first.endTo = this.beginContract.plusYears(3).minusDays(1);
			first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
			first.contract = this;
			first.save();
			VacationPeriod second = new VacationPeriod();
			second.beginFrom = this.beginContract.plusYears(3);
			second.endTo = this.expireContract;
			second.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "28+4").first();
			second.contract =this;
			second.save();
			this.save();
			return;
		}

		//Tempo determinato più corto di 3 anni
		VacationPeriod first = new VacationPeriod();
		first.beginFrom = this.beginContract;
		first.endTo = this.expireContract;
		first.contract = this;
		first.vacationCode = VacationCode.find("Select code from VacationCode code where code.description = ?", "26+4").first();
		first.save();
		this.save();
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
	
	public void populateContractYearRecap()
	{
		Logger.info("PopulateContractYearRecap %s %s contract id = %s", this.person.name, this.person.surname, this.id);
		//Distruggere quello che c'è prima (adesso in fase di sviluppo)
		for(ContractYearRecap yearRecap : this.recapPeriods)
		{
			yearRecap.delete();
		}
		this.recapPeriods = new ArrayList<ContractYearRecap>();

		//Controllo se ho sufficienti dati
		LocalDate initUse = ConfGeneral.getConfGeneral().initUseProgram;
		if(this.sourceDate!=null)
			initUse = sourceDate.plusDays(1);
		DateInterval personDatabaseInterval = new DateInterval(initUse, new LocalDate());
		DateInterval contractInterval = this.getContractDateInterval();

		//Se intersezione fra contratto e dati utili database vuota non costruisco alcun contractYearRecap
		if(DateUtility.intervalIntersection(contractInterval, personDatabaseInterval)==null)
			return;

		int yearToCompute = this.beginContract.getYear();
		
		//verifico quanta informazione ho sul contratto
		if(contractInterval.getBegin().isBefore(personDatabaseInterval.getBegin()))
		{
			//contratto non interamente contenuto nel database (serve sourceContract)
			if(this.sourceDate==null)
				return;
			yearToCompute = this.populateContractYearFromSourceWhenSourceIsEndOfYear();
		}
		
		int currentYear = new LocalDate().getYear();
		if(currentYear>contractInterval.getEnd().getYear())
			currentYear = contractInterval.getEnd().getYear();
		while(yearToCompute<currentYear)
		{
			Logger.debug("yearToCompute %s", yearToCompute);
			ContractYearRecap cyr = new ContractYearRecap();
			cyr.year = yearToCompute;
			cyr.contract = this;
			
			//FERIE E PERMESSI
			VacationsRecap vacationRecap = new VacationsRecap(this.person, yearToCompute, this, new LocalDate(), true);
			cyr.vacationLastYearUsed = vacationRecap.vacationDaysLastYearUsed.size();
			cyr.vacationCurrentYearUsed = vacationRecap.vacationDaysCurrentYearUsed.size();
			cyr.permissionUsed = vacationRecap.permissionUsed;
			
			//RESIDUI
			CalcoloSituazioneAnnualePersona csap = new CalcoloSituazioneAnnualePersona(this, yearToCompute, new LocalDate().minusDays(1));
			Mese lastComputedMonthInYear;
			if(yearToCompute!=currentYear)
				lastComputedMonthInYear = csap.getMese(yearToCompute, 12);
			else
				lastComputedMonthInYear = csap.getMese(yearToCompute, new LocalDate().getMonthOfYear());
			
			cyr.remainingMinutesLastYear = lastComputedMonthInYear.monteOreAnnoPassato;
			cyr.remainingMinutesCurrentYear = lastComputedMonthInYear.monteOreAnnoCorrente;
			
			//RIPOSI COMPENSATIVI
			//TODO la logica che persiste il dato sui riposi compensativi utilizzati deve essere ancora implementata in quanto non banale.
			//I riposi compensativi utilizzati sono in funzione del contratto?
			//cyr.recoveryDayUsed = PersonUtility.numberOfCompensatoryRestUntilToday(this.person, yearToCompute, 12);
			
			cyr.save();
			this.recapPeriods.add(cyr);
			
			yearToCompute++;
		}
	}
	
	
	/**
	 * Costruisce il contractYearRecap se contract.SourceDate è l'ultimo giorno dell'anno.
	 * N.B. se source non è l'ultimo giorno dell'anno il contractYearRecap non viene costruito perchè non utile agli algoritmi
	 * di calcolo dei residui e delle ferie che utilizzano direttamente i dati di contract.source. Verificare se per motivi di report
	 * ha senso persistere anche il riepilogo di tale anno.
	 * @return l'anno di cui si deve costruire il prossimo contractYearRecap
	 */
	public int populateContractYearFromSourceWhenSourceIsEndOfYear()
	{
		LocalDate lastDayInYear = new LocalDate(this.sourceDate.getYear(), 12, 31);
		if(! lastDayInYear.isEqual(this.sourceDate))
			return this.sourceDate.getYear()+1;
			
		int yearToCompute = this.sourceDate.getYear();
		ContractYearRecap cyr = new ContractYearRecap();
		cyr.year = yearToCompute;
		cyr.contract = this;
		cyr.remainingMinutesCurrentYear = this.sourceRemainingMinutesCurrentYear;
		cyr.remainingMinutesLastYear = this.sourceRemainingMinutesLastYear;
		cyr.vacationLastYearUsed = this.sourceVacationLastYearUsed;
		cyr.vacationCurrentYearUsed = this.sourceVacationCurrentYearUsed;
		cyr.recoveryDayUsed = this.sourceRecoveryDayUsed;
		cyr.permissionUsed = this.sourcePermissionUsed;
		cyr.save();
		this.recapPeriods.add(cyr);
		return yearToCompute+1;
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
		if(contractInMonth.get(contractInMonth.size()-1).id == this.id)
			return true;
		else
			return false;
	}
	
}
	
	

