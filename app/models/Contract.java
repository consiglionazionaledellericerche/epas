package models;

import it.cnr.iit.epas.DateInterval;
import it.cnr.iit.epas.DateUtility;
import it.cnr.iit.epas.PersonUtility;

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

	@Embeddable
	public static class SourceData
	{
		@Type(type="org.joda.time.contrib.hibernate.PersistentLocalDate")
		@Column(name="source_date")
		public LocalDate sourceDate;
		
		@Column(name="source_vacation_last_year_used")
		public Integer sourceVacationLastYearUsed = 0;
		
		@Column(name="source_vacation_current_year_used")
		public Integer sourceVacationCurrentYearUsed = 0;
		
		@Column(name="source_permission_used")
		public Integer sourcePermissionUsed = 0;
		
		@Column(name="source_recovery_day_used")
		public Integer sourceRecoveryDayUsed = 0;
		
		@Column(name="source_remaining_minutes_last_year")
		public Integer sourceRemainingMinutesLastYear = 0;
		
		@Column(name="source_remaining_minutes_current_year")
		public Integer sourceRemainingMinutesCurrentYear = 0;
	}
	
	private static final long serialVersionUID = -4472102414284745470L;

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="person_id")
	public Person person;
	
	@Embedded
	public SourceData sourceData;

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
	
	
	public void setRecapPeriods(LocalDate initUse, InitializationTime initPerson)
	{
		//Distruggere quello che c'è prima
		for(ContractYearRecap yearRecap : this.recapPeriods)
		{
			yearRecap.delete();
		}
		
		//Calcolo intervallo con dati utili database
		DateInterval personDatabaseInterval;
		if(initPerson!=null && initPerson.date.isAfter(initUse))
			personDatabaseInterval = new DateInterval(initPerson.date, new LocalDate());
		else
			personDatabaseInterval = new DateInterval(initUse, new LocalDate());

		//Calcolo intervallo esistenza contratto
		DateInterval contractInterval = this.getContractDateInterval();		
		
		//Calcolo intersezione fra contratto e dati utili database
		DateInterval intersection = DateUtility.intervalIntersection(contractInterval, personDatabaseInterval);
		
		//se vuota non costruisco alcun contractYearRecap
		if(intersection==null)
			return;
		
		//verifico quanta informazione ho sul contratto
		if(personDatabaseInterval.getBegin().isBefore(contractInterval.getBegin()))
		{
			//contratto interamente contenuto nel database
			populateContractYearRecapFromDatabase(contractInterval.getBegin().getYear());
		}
		else
		{
			//una parte di contratto non ha dati presenti nel db, devo sfruttare init time se è relativo a tale contratto
			if(initPerson!=null && DateUtility.isDateIntoInterval(initPerson.date, contractInterval))
			{
				if(initPerson.date.getMonthOfYear()==1 && initPerson.date.getDayOfMonth()==1)
				{
					//se init è il primo gennaio costruisco il ContractYearRecap dell'anno precedente con i dati di lastYear
					ContractYearRecap cyr = new ContractYearRecap();
					cyr.year = initPerson.date.getYear()-1;
					cyr.contract = this;
					cyr.remainingMinutesCurrentYear = initPerson.residualMinutesPastYear;
					cyr.vacationCurrentYearUsed = initPerson.vacationLastYearUsed;
					cyr.save();
					
					populateContractYearRecapFromDatabase(cyr.year + 1);
					
				}
				else
				{
					//TODO per irpi
					/*
					//se init time e' a metà anno costruisco ContractYearRecap dell'anno attuale con i campi source
					ContractYearRecap cyr = new ContractYearRecap();
					cyr.year = initPerson.date.getYear();
					cyr.contract = this;
					cyr.sourceRemainingMinutesLastYear = initPerson.residualMinutesPastYear;
					cyr.sourceRemainingMinutesLastYear = initPerson.residualMinutesCurrentYear;
					//TODO quando ho tutti i campi in init
					
					//BUILD ALL PAST YEAR RECAP FROM DB
					*/
				}
			}
			//init time non si riferisce al contratto, non ho sufficienti dati per creare i riepiloghi
			else
			{
				
				return;
			}
		}
	}
	
	
	private void populateContractYearRecapFromDatabase(int yearFrom)
	{
		
		int actualYear = yearFrom;
		int currentYear = new LocalDate().getYear();
		
		//popolo ogni anno dal 1 gennaio
		while(actualYear<=currentYear)
		{
			
			ContractYearRecap cyr = new ContractYearRecap();
			cyr.year = actualYear;
			cyr.contract = this;
			
			//FERIE E PERMESSI
			VacationsRecap vacationRecap = new VacationsRecap(this.person, actualYear, this, new LocalDate(), true);
			cyr.vacationLastYearUsed = vacationRecap.vacationDaysLastYearUsed.size();
			cyr.vacationCurrentYearUsed = vacationRecap.vacationDaysCurrentYearUsed.size();
			cyr.permissionUsed = vacationRecap.permissionUsed;
			
			//RESIDUI
			CalcoloSituazioneAnnualePersona csap = new CalcoloSituazioneAnnualePersona(this, actualYear, new LocalDate());
			Mese lastComputedMonthInYear;
			if(actualYear!=currentYear)
				lastComputedMonthInYear = csap.getMese(actualYear, 12);
			else
				lastComputedMonthInYear = csap.getMese(actualYear, new LocalDate().getMonthOfYear());
			
			cyr.remainingMinutesLastYear = lastComputedMonthInYear.monteOreAnnoPassato;
			cyr.remainingMinutesCurrentYear = lastComputedMonthInYear.monteOreAnnoCorrente;
			
			//RIPOSI COMPENSATIVI
			cyr.recoveryDayUsed = PersonUtility.numberOfCompensatoryRestUntilToday(this.person, actualYear, 12);
			
			cyr.save();
			
			actualYear++;
		}
		
	}
	
	/**
	 * Ritorna il riepilogo annule del contatto.
	 * @param year
	 * @return
	 */
	public ContractYearRecap getYearRecap(int year)
	{
		for(ContractYearRecap cyr : this.recapPeriods)
		{
			if(cyr.year==year)
				return cyr;
		}
		return null;
			
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
	 * Ritorna la lista ordinata di riepiloghi annuali per il contratto. Se il contratto ha dati di inizializzazione forzati
	 * il primo elemento della lista ha i campi source valorizzati.
	 * @return
	 */
	public List<ContractYearRecap> getAllContractYearRecap()
	{
		
		
		return null;
		
	}
	
	/**
	 * True se il contratto ha dati di inizializzazione forzati dall'amministratore
	 * @return
	 */
	public boolean hasInitializationData()
	{
		for(ContractYearRecap cyr : this.recapPeriods)
		{
			if(cyr.hasSource)
				return true;
		}
		return false;
	}

}
